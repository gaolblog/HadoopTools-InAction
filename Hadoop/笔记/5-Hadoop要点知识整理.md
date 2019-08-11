### 1、要点复习
- HDFS
    - 读、写数据流程
        - 读流程  
            - 客户端指定HDFS上的一个文件路径，请求NameNode下载文件
            - NameNode返回给客户端目标文件的一些元数据，元数据的主要内容是：文件名称、文件的副本数、文件的各个Block分别在哪些DataNode上
            - 客户端根据收到的元数据信息逐一找到存储分块的DataNode，请求读取各个Block内容
            - 客户端在本地不断地追加各个Block的内容，最终形成一个完整的文件
        - 写流程
            - 客户端向NameNode请求上传一个文件
            - NameNode根据客户端的请求（例如一个Block的副本数、文件块大小等信息）分配给客户端上传文件的DataNode列表
            - 客户端在收到的DataNode之间建立一个PIPELINE，然后向PIPELINE中的第一台DataNode传送第一个Block的packet（64K）。与此同时，第一台DataNode也会将收到的packet传送给PIPELINE中的第二台DataNode，第二台DataNode再传送给第三台DataNode，以此类推。直到第一个Block的所有packet的副本都传送完毕后，然后客户端开始传送第二个Block的packet到PIPELINE……
            - 如果在上传过程中失败了，客户端就会通知NameNode，NameNode会重新分配DataNode给客户端让其重传，如果再一次失败，就又会分配新的DataNode给客户端。如果三次上传失败后，就会抛异常宣告真正的上传失败，此时就需要手动上传
    - NameNode、Secondary NameNode管理元数据的机制（除了查询操作，增删文件都会涉及元数据的更新）
        - 元数据的更新涉及更新在NameNode的内存中，并在日志（`edits.inprogress`、`edits.002`、`edits.003`……）中记录更新操作
        - 当日志文件积累到一定量时，NameNode就会请求Secondary NameNode进行日志文件的合并。第一次合并时，Secondary NameNode只会将所有的日志文件下载到本地，然后在其内存中合并更新后的元数据成`fs.image`，然后将`fs.image`上传至NameNode；以后每次Secondary NameNode合并的是新生成的日志文件和旧的`fs.image`。这样做的目的就是让NameNode内存中的元数据和`fs.image`中的元数据始终保持一个小的差距
    - 建立在HDFS上的运算框架（例如MapReduce、Spark、Storm等）实质上就是HDFS的一个客户端。这些运算框架的每一个并发实例都会读取一个大任务的部分数据，即利用HDFS的API打开一个流，seek读取数据的起始偏移量和结束偏移量，根本不需要了解HDFS的底层
- MapReduce
    - MapReduce思想：  
    MapReduce利用了分治法的思想。它把整个数据处理的过程分成了两个阶段：Map阶段（映射）和Reduce阶段（聚合）。  
    MapReduce操作数据的最小单位是一个键值对。用户在使用MapReduce编程模型的时候，第一步就需要++将数据抽象为++**键值对**的形式，接着map函数会以键值对作为输入，经过map函数的处理，产生一系列新的键值对作为中间结果输出到本地。MapReduce计算框架会自动将这些中间结果数据按照键做聚合处理，并将键相同的数据分发给reduce函数处理（用户可以设置分发规则）。reduce函数以键和对应的值的集合作为输入，经过reduce函数的处理后，产生了另外一系列键值对作为最终输出。MapReduce过程用表达式表示如下：
    ```
    {Keyl,Value1}~{Key2,List<Value2>}~{Key3,Value3}
    ```
    - MapReduce编程框架中的三种角色：
        - Map Task：各个Map Task各自为政，自己干自己的。在Running Job的时候，Map Task的进程名称为：YarnChild
            - Map Task的并行度：Map Task的数量取决于MapReduce要处理的目录下的文件数量和文件大小、硬件配置
                - HDFS上是对一个文件做了真正的物理上的划分，而MapReduce中对文件的切片是逻辑上的划分。对于一次数据处理任务，应该启动多少个Map Task：
                    - 如果处理的都是大文件（例如文件大小超过了HDFS一个文件切片128M的大小），那么最好的做法就是一个Map Task处理一个文件切片。这样尽可能地保证Map Task处理的就是本地的文件切片的话，那么效率就会很高。**MapReduce的默认实现就是这种。** 默认实现中，对于小文件就是一个文件被一个Map Task处理。
                    - 如果处理的都是小文件（例如只有几M，甚至Kb量级的文件），那么最好的做法就是让一个Map Task处理多个小文件以提高效率，而不必启动多个Map Task来处理多个小文件，造成资源的浪费（启动Map Task后销毁的时间都比数据处理的数据可能要长）
                    - 视现有的集群硬件配置决定启动Map Task的数量：例如不要将一个几G大小的文件只交给一个Map Task处理，其它的Map Task都空闲着；也不要将N多个小文件交给N个Map Task处理
                - *MapReduce文件切片源码流程*
                    - 什么时候切片？  
                        一个job的map阶段并行度由客户端在提交job时决定。
                    - 切片流程：源码跟踪
                    - 切片机制
                        - 切片时不考虑数据集整体，而是逐个针对每一个文件单独切片。
                        - 将待处理数据执行++逻辑切片++（即按照一个特定切片大小，将待处理数据划分成逻辑上的多个split，然后**每一个split分配一个mapTask并行实例处理**。
                        - 切片大小，默认等于block大小。切片大小可通过配置`mapreduce.input.fileinputformat.split.minsize`和`mapreduce.input.fileinputformat.split.maxsize`参数应用FileInputFormat中的getsplits()方法中的计算切片大小的方式`Math.max(minSize, Math.min(maxSize, blockSize))`计算得到。
            - Map Task输出数据分区：
                - MapReduce 提供了两个Partitioner 实现：`HashPartitioner`和`TotalOrderPartitioner`。其中 HashPartitioner是默认实现：
                    ```
                    (key.hashCode() & Integer.MAX_VALUE) % numReduceTasks
                    ```  
                    主要做的就是“将相同的key分配到同一个Reduce Tasks上”，这样是没法保证某一种key到哪一种Reduce Task的。
                - 自定义数据分区：继承并重写抽象类Partitioner中的getPartition方法，按自定义逻辑返回分区号。然后需要在MapReduce程序的Driver类中做以下两件事情：
                    - 指定自定义的数据分区器：`job.setPartitionerClass()`
                    - 指定Reduce Task数量（如果指定了自定义分区，则Reduce Task的数量应>=分区数量；Reduce Task数量为1，则会忽略分区）：`job.setNumReduceTasks()`
            - Map Task输出数据按`KEYOUT`排序
                - 实现要点：
                    - 待输出数据（例如自定义的对象）要实现`WritableComparable`接口中的`compareTo`方法。Map Reduce默认对输出的键值对是按键的字典序排序的，所以要排序谁，就把谁作为KEYOUT输出（如果要排序的是自定义对象中的某个属性，就把这个对象作为KEYOUT），并且让自定义对象实现`compareTo`方法
                    - 全局有序的话，Reduce Task的数量只能有一个（即不能对输出数据做分区，否则只能保证分区内有序，但全局不有序）
                - ***疑点***：MapReduce程序的Driver类中不设置Reduce Task时：
                    - 若`job.setNumReduceTasks(0)`：在输出结果目录只有Map Task的输出结果part-m-00000，但是并未做排序。那么问题是`compareTo`方法是什么时候被调的？——*难道不是在Map Task将环形缓冲区中的数据做完排序后再序列化到文件part-m-00000这样的过程？*  
                    **释疑：** `FileOutputFormat.setOutputPath()`中指定的输出结果目录中的`part-m-00000`文件应该是MapReduce的shuffle阶段之前产生的，和Map Task工作目录中产生的从环形缓冲区中dump出来的文件（这个文件已经过分区、排序操作）不是一回事！
                    - 若`job.setNumReduceTasks(1)`：此时输出结果目录中的文件是part-r-00000，并且做了排序。*为什么是这样的？——是Reduce Task参与了排序吗？*  
                    **释疑：** 就算MapReduce程序中不写自定义的Reducer.reduce()方法，将Reduce Task的数量设为1，也会使用默认的reduce()方法：
                        ```
                        protected void reduce(KEYIN key, Iterable<VALUEIN> values, Context context
                        ) throws IOException, InterruptedException {
                            for(VALUEIN value: values) {
                              context.write((KEYOUT) key, (VALUEOUT) value);
                            }
                          }
                        ```
            - combiner组件对当前Map Task中的<k,v>做聚合
                - 调用combiner组件的阶段：
                    - spiller组件在将环形缓冲区中分完区、排好序的所有<k,v> dump到文件之前，调用combiner组件对这些<k,v>做一次聚合
                    - Map Task在将某一分区的全部小文件合并为一个大文件时，可调用一次combiner组件对所有小文件中的<k,v>再做一次聚合
                - 好处：
                    - spiller组件不用频繁地溢出到内存
                    - 减少了reduce端的reduce()方法做聚合操作的次数。**Combiner和Reducer的工作机制是一样的，但是Map Task做的聚合操作是针对本map端的<k,v>做的，Reduce Task做的聚合操作是针对所有Map Task的同一分区做的**
                - **注意：** combiner组件的使用不应影响到业务逻辑，例如求平均数：  
                ![image](https://wx1.sinaimg.cn/mw690/006CX93ply1fzprsmghkij30lg08nwf4.jpg)
            - 解决数据倾斜：DistributedCache机制
                - 什么是数据倾斜：  
                    >数据倾斜就是我们在计算数据的时候，数据的分散度不够，导致大量的数据集中到了一台或者几台机器上计算，这些数据的计算速度远远低于平均计算速度，导致整个计算过程过慢。
                    
                - 导致数据倾斜的原因：多个Reduce Task处理数据时，由于Map端不同的partition可能会有不同数量的数据（默认是按Map端KEYOUT的hashcode分区的），甚至一个分区的数据超多，而另一个分区的数据很少。这样就会导致一个Reduce Task要处理大量的数据，效率较低；而另一个Reduce Task处理少量数据，空闲率较高。
                - **解决数据倾斜：**
                    - distributed cache解决数据倾斜：Map端join方式
                    - 参考博客：[大数据数据倾斜](https://www.cnblogs.com/gala1021/p/8552302.html)
        - Reduce Task：各个Reduce Task自己处理自己的Map Tasks的任务，自己输出自己的分类聚合结果。
            - Reduce Task的数量可在MapReduce程序的Driver类中设定：`job.setNumReduceTasks(1);`。每个Reduce Task会独立生成自己的聚合结果文件，例如`part-00000x`
            - Reduce端做类似于SQL的join操作
        - MapReduce Application Master：是Map Task和Reduce Task的主管，主要负责这两者的调度、监控和协调。主要做的事情有以下几点：
            - 为Map Task分配数据处理任务，各个Map Task处理的数据范围
            - 为Reduce Task分配数据聚合任务
            - 负责Map Task和Reduce Task之间的衔接（哪个Reduce Task到哪个目录下去取哪个Map Task的处理结果）
            - 当有Map Task运行任务失败时，启动新的Map Task来运行失败的任务
            - Map Task输出结果文件的分区应该是由MapReduce Application Master完成
    - MapReduce运行流程：
        - 一、job提交流程：`job.submit()`
            - job客户端做数据集的逻辑切片工作，生成切片规划文件job.split、job客户端参数配置文件job.xml、job的MapReduce jar包，然后将这些文件提交到yarn  
            ![MapReduce Job提交流程](https://wx2.sinaimg.cn/mw690/006CX93ply1fzm3w0ganij30ry11fdjo.jpg)
            - yarn按集群节点空闲状况启动MR Application Master，将相关文件写入
            - MR Application Master根据切片规划文件启动Map Task，一个Map Task分配一个split执行
        - 二、shuffle阶段（Mapper输出数据到Reducer接收数据的这段过程）  
            shuffle阶段Map Task做的事和Reduce Task做的事：  
            - Map Task：
                - 环形缓冲区缓存<k,v>数据
                - 内存中分区（Partitioner）、排序（Key.CompareTo）、聚合（Combiner）
                - 从内存溢出到小文件
                - 按分区合并小文件为大文件
            - Reduce Task：
                - 通过网络下载每个Map Task最终输出的某一分区的文件
                - 按分区合并文件
            - shuffle阶段流程图解：  
            ![image](https://wx2.sinaimg.cn/mw690/006CX93ply1fzpso9ohkaj33gk0p4tka.jpg)
        - 三、Reduce聚合阶段
    - MapReduce的数据序列化实现
    - MapReduce的输入输出数据组件
        - `InputFormat`
            - `FileInputFormat`：job客户端使用`FileInputFormat`中的`getSplits()`方法做切片规划，Map Task使用`FileInputFormat`子类的`TextInputFormat`中的RecordReader去读取文本数据
                - `TextInputFormat`
                - `SequenceFileInputFormat`：SequenceFile是hadoop中特有的一种文件格式
                - `NLineInputFormat`：一次读多行
                - `FixedLengthInputFormat`
                - `CombineFileInputFormat`：该`InputFormat`的实现类可以将多个小文件合并到一个切片中交由一个Map Task处理，避免了默认实现`TextInputFormat`一个文件大于128M时进行切片，不满128M的小文件就当做一个切片时浪费Map Task的情况
            - `DBInputFormat`
            - `CompositeInputFormat`
            - ……
        - `OutputFormat`