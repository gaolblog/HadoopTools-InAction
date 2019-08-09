### 1、Spark编程套路
- 1、获取编程入口SparkContext
    - SparkContext主要用于初始化Spark应用程序所需的一些核心组件，例如调度器（DAGScheduler、TaskScheduler）
- 2、通过编程入口加载输入源数据：核心数据抽象RDD
    - 输入源中的数据被打散分布在RDD的每个partition中
    - 如果输入源是文本数据，那么RDD中的每个元素就是文本文件中的一行
- 3、对数据进行各种业务处理——Transformation操作：把一种RDD转换为另一种RDD
    - shuffle：数据是否具有相同特征，按相同的特征聚合在一起。任务之间是有数据交互的
- 4、对结果数据进行处理——真正触发Transformation操作的执行，Action操作
    - 最终得到的结果数据有可能是一个RDD，或scala集合（例如3个班的成绩加和结果），或单个对象（例如十亿条数据的加和结果）
- 5、关闭编程入口
### 2、Spark编程核心概念
- RDD：弹性分布式数据集模型（**可以认为Spark是基于RDD模型的系统**）
    - 对RDD加深理解：RDD代表的是一个不可变、可分区、里面的元素可并行计算的集合
        - RDD的概念：
            - 不可变：RDD所描述的数据集是只读的，可通过RDD的transformation操作把数据集从一种形式转换为另一种新的数据集，但是RDD本身所描述的数据集是不能去修改的
            - 弹性：
                - 分区数可按需求自行指定
                - 数据可缓存在内存或持久化到磁盘
                - RDD某个分区中的数据若丢失了，可通过计算重新找到
        - RDD的分类：
            - `RDD[T]`：This class contains the basic operations available on all RDDs, such as `map`, `filter` and `persist`
            - `RDD[(T,S)]`：contains operations available only on RDDs of key-value pairs, such as `groupByKey` and `join`
        - RDD与MapReduce中的block/split之间的异同：在考虑存储情况时，RDD中的分区类似于MapReduce中的block；考虑数据计算，RDD中的每一个分区（亦即每一个task要执行的数据整体），此时就相当于MapReduce中的一个split。
        - RDD的5大特性（源码）：
            - A list of partitions
            - A function for computing each split（每个split就是一个分区，每个分区不仅有待计算的数据，还有计算逻辑——函数f）  
            ![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/qR8Hoyq*FDrZfQs8TKjRJ6mZIFLB7whfew4eg0*cWVc!/b/dLYAAAAAAAAA&bo=CwNUAQAAAAADB38!&rf=viewer_4)
            - A list of dependencies on other RDDs
                - 窄依赖：一个父分区只有一个子分区  
                ![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/HJEIXvZelV.4mzId2r*Kc2KaBiQaGU3AFq6cmkjgxeY!/b/dL8AAAAAAAAA&bo=EgPyAAAAAAADB8E!&rf=viewer_4)
                - 宽依赖：一个父分区有多个子分区，一般就是父分区的数据被平分给子分区。有shuffle的算子就是宽依赖算子。为什么要按照shuffle或宽依赖算子要把一个job划分成多个stage呢？——因为在执行宽依赖算子之前，所有的数据分区都必须准备好后才可以执行，这也就是上一个阶段、下一个阶段之间的顺序关系。**一个Application中的宽依赖算子是越少越好。**   
                ![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/HB5eIBpZtzlbUyWR1vedJpejatL9X.x11miJ7xeiON8!/b/dL8AAAAAAAAA&bo=BQNDAQAAAAADB2Y!&rf=viewer_4)
                - join算子既是宽依赖，又是窄依赖：  
                ![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/bQzroLgwSXzM12H16aPfN7AlDbUG234CU8.oM4OJXNk!/b/dFIBAAAAAAAA&bo=QANTAQAAAAADBzM!&rf=viewer_4)
            - Optionally, a Partitioner（分区器） for key-value RDDs (e.g. to say that the RDD is hash-partitioned)
            - Optionally, a list of preferred locations to compute each split on (e.g. block locations for an HDFS file)：**移动数据不如移动计算**，即数据在哪计算程序就在哪启动。
        - 创建RDD：
            - 由一个已存在的scala/java集合创建
                ```
                List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
                JavaRDD<Integer> distData = sc.parallelize(data);
                ```
            - 由外部存储系统的数据集创建：
                ```
                JavaRDD<String> distFile = sc.textFile("data.txt");
                ```
    - RDD的本质是对数据集的描述（只读的、可分区的分布式数据集），而不是数据本身
            - `List<E>`：包含少量数据的单机版集合
            - `RDD<T>`：分布式集合，数据量太大的话就可以分成几个区在几个节点上
    - RDD的关键特征：
        - RDD使用户能够显式地将计算结果保存在内存中，控制数据的划分，并使用更丰富的操作集合来处理
        - 记录数据的变换而不是数据本身，保证容错性（lineage）：RDD采用数据应用变换，若部分数据丢失，RDD拥有足够的信息得知这部分数据是如何计算的，可通过重新计算来得到丢失的数据（对窄依赖算子恢复速度很快，但对于宽依赖算子计算出的数据恢复速度较慢，所以Spark一般是对宽依赖算子计算得到的数据做缓存备份）  
        ![image](https://wx3.sinaimg.cn/mw690/006CX93ply1g0ege67f80j30hz09gweu.jpg)  
        lineage用于记录RDD的DAG关系
        - 懒操作：延迟计算，action的时候才操作
        - 瞬时性：用时才产生，用完即释放
    - 每个RDD都是包含了数据分区（partition）的集合（类似于MapReduce，相同key的value到同一个分区中去），每个partition是不可分割的  
    ![image](https://wx4.sinaimg.cn/mw690/006CX93ply1g0egjgbsnrj30jo0av7ba.jpg)  
        - 每个partition的计算就是一个task，task是调度的基本单位
        - 遵循数据局部性原则，使得数据传输代价最小
            - 如果一个任务需要的数据在某个节点的内存中，这个任务就会被分配至那个节点。此时的调度是指由Spark Driver决定计算partition的task应该分配到哪个executor上。如果此task失败，Spark Driver会重新分配task
            - 需要的数据在某个节点的文件系统中，就分配至那个节点
        - 如果task依赖的上层partition数据失效了，会先将其依赖的partition计算任务再重算一遍
        - 可以指定保存一个RDD的数据至节点的cache中，如果内存不够，会释放一部分长期不用的数据或溢出到磁盘
    - 从RDD DAG中划分出stage  
    ![image](https://wx4.sinaimg.cn/mw690/006CX93ply1g0egpyspv9j30j80b3aim.jpg)
- Spark action算子：分两类
    - Transformations：主要是用来做数据处理的变换操作的，是一种懒操作
        - map
        - filter
        - flatMap：将RDD的一个元素拆分成一个或多个元素
        - sample
        - groupByKey
        - reduceByKey
        - union
        - join
        - cogroup
        - crossProduct
        - mapValues
        - sort
        - partitionBy
    - Actions：真正执行Transformations算子的操作和最终操作
        - count
        - collect
        - reduce
        - lookup
        - foreach：遍历RDD中的每个元素
        - save