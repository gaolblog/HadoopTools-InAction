### 0、Spark官网简介
- Spark核心组件：Spark Core、Spark SQL、Spark Streaming、MLlib、GraphX
    - 所有模块都是基于Spark Core的，每个模块的程序最终都会转转为Spark Core程序执行
    - 可以在Spark Core的基础上扩展开发新模块
- Spark的特点：
    - Speed
    - Ease of Use
    - Generality
    - Runs Everywhere
### 1、Spark集群搭建
参考博客：[spark-2.3.0和Hadoop2.6.5完全分布式安装和部署——分布式集群(参考记录）](https://blog.csdn.net/zhangvalue/article/details/80653313)
- local模式：直接将官网下载的压缩包`spark-2.4.0-bin-hadoop2.6.tgz`解压即可使用
- 集群模式
    - 在一台节点上解压`spark-2.4.0-bin-hadoop2.6.tgz`后，修改`/root/apps/spark-2.4.0/conf`目录下的两个文件：
        - `spark-env.sh`文件中新增如下内容：
            ```
            export JAVA_HOME=/root/apps/jdk1.8.0_181
            export SPARK_MASTER_HOST=hadoopmaster
            export SPARK_MASTER_PORT=7077
            ```
        - `slaves`中的内容：
            ```
            # A Spark Worker will be started on each of the machines listed below.
            hadoopmaster
            hadoop01
            hadoop02
            ```
    - 将解压并修改后的spark目录scp到从节点的同一目录：`scp -r spark-2.4.0 从节点主机名:$PWD`
    - 修改主从节点的环境变量并使之生效：
        ```
        export SPARK_HOME=/root/apps/spark-2.4.0
        export PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
        ```
### 2、Spark程序运行模式
- 参考博客：[Spark2.x学习笔记：4、Spark程序架构与运行模式](https://blog.csdn.net/chengyuqiang/article/details/77858086)
- 本地模式
    - `./run-example SparkPi 10 --utility local[2]`：2表示用2个进程并发模拟执行（或启动2个executor）
    - local模式的Spark context Web UI只能通过 **==http://主机域名:4040==** 看到（在哪台机器上提交程序或运行交互式spark-shell程序，那么主机域名就是那台机器的域名）
    - local模式下spark-shell中运行wordcount：  
    words_test.txt中的内容：  
    ```
    hadoop java storm flink hive
    spark c apache hive hbase kafka
    mapreduce tez yarn tez hdfs
    flink hadoop spark spark yarn
    sqoop flume mahout streaming tez mlib
    hdfs mesos kafka tachyon hbase oozie
    python php web yarn kafka storm
    ```  
    spark-shell：  
    ```
    scala> sc.textFile("/root/hadoop_testfiles/words_test.txt") //加载本地文件的RDD
    2019-02-26 11:01:46 WARN  SizeEstimator:66 - Failed to check whether UseCompressedOops is set; assuming yes
    res0: org.apache.spark.rdd.RDD[String] = /root/hadoop_testfiles/words_test.txt MapPartitionsRDD[1] at textFile at <console>:25
    
    scala> res0.flatMap(line => line.split(" "))
    .map((_,1)).reduceByKey(_+_)    //reduceByKey是先分组后聚合
    .sortBy(_._2,false) //第一个“_”表示一个tuple
    ```  
    结果：  
    ```
    Array[(String, Int)] = Array((yarn,3), (kafka,3), (tez,3), (spark,3), (hive,2), (flink,2), (hadoop,2), (hdfs,2), (storm,2), (hbase,2), (mlib,1), (tachyon,1), (php,1), (oozie,1), (python,1), (mapreduce,1), (apache,1), (web,1), (java,1), (streaming,1), (sqoop,1), (mahout,1), (flume,1), (mesos,1), (c,1))
    ```
- 集群模式
    - Spark Standalone：Spark集群可以不依赖于Yarn集群而单独运行，此时Spark独享计算资源
        - 运行示例程序进行验证：**只有连接了Spark集群的spark-shell或spark-submit才可以在 ==http://主节点域名:8080/== 中看到**，此时在哪台机器上提交程序或运行spark-shell程序，那么可通过 **==http://主机域名:4040==** 查看SparkContext内容
            - spark-submit提交示例程序：
                - 首先启动Spark集群：`/root/apps/spark-2.4.0/sbin/start-all.sh`
                - 运行计算`$\pi$`的例子：
                    ```
                    spark-submit \
                    --class org.apache.spark.examples.SparkPi \
                    --master spark://hadoopmaster:7077 \
                    --driver-memory 512m \
                    --executor-memory 512m \
                    --total-executor-cores 2 \
                    /root/apps/spark-2.4.0/examples/jars/spark-examples_2.11-2.4.0.jar \
                    100
                    ```
            - 启动连接了Spark集群的spark-shell：
                ```
                spark-shell \
                --master spark://hadoopmaster:7077 \
                --executor-memory 512m \
                --total-executor-cores 2
                ```
        - 自己写的`WordCountJava7`提交到Spark StandAlone集群运行：
            - java7 wordcount程序：
                ```
                package com.ww.hadoop.spark;

                import org.apache.spark.SparkConf;
                import org.apache.spark.api.java.JavaPairRDD;
                import org.apache.spark.api.java.JavaRDD;
                import org.apache.spark.api.java.JavaSparkContext;
                import org.apache.spark.api.java.function.*;
                import scala.Tuple2;
                
                import java.util.Arrays;
                import java.util.Iterator;
                import java.util.List;
                
                /**
                 * @author gaol
                 * @Date: 2019/2/26 21:03
                 * @Description
                 */
                public class WordCountJava7 {
                    public static void main(String[] args) {
                        /**
                         * 1、获取Spark编程入口
                         */
                        SparkConf conf = new SparkConf().setAppName("WordCountJava7");
                        JavaSparkContext sc = new JavaSparkContext(conf);
                
                        /**
                         * 2、通过Spark编程入口加载数据
                         */
                        JavaRDD<String> lines = sc.textFile(args[0]);
                
                        /**
                         * 3、针对加载数据的RDD做各种数据处理的逻辑
                         */
                        //3-1、将RDD中的每个元素拆分为一个或多个元素，构成新的RDD，这个新RDD包含的就是被拆分后的所有新元素
                        JavaRDD<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
                            @Override
                            public Iterator<String> call(String line) throws Exception {
                                return Arrays.asList(line.split(" ")).iterator();
                            }
                        });
                        
                        //3-2、将words这个RDD中的每个元素映射为“(word,1)”元组的形式
                        JavaPairRDD<String, Integer> wordPairs = words.mapToPair(new PairFunction<String, String, Integer>() {
                            @Override
                            public Tuple2<String, Integer> call(String word) throws Exception {
                                return new Tuple2<>(word, 1);
                            }
                        });
                
                        //3-3、将wordPairs RDD中的每个(word,1)元组做聚合
                        JavaPairRDD<String, Integer> wordcount = wordPairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
                            @Override
                            public Integer call(Integer v1, Integer v2) throws Exception {
                                return v1 + v2;
                            }
                        });
                
                        //3-4-<1>、对wordcount RDD中的每个元组元素按key排序
                        JavaPairRDD<String, Integer> sortedWordcountByKey = wordcount.sortByKey();
                
                        //3-4-<2>、对wordcount RDD中的每个元组元素按value降序排序
                        JavaPairRDD<Integer, String> swappedWordcount = wordcount.mapToPair(new PairFunction<Tuple2<String, Integer>, Integer, String>() {
                            @Override
                            public Tuple2<Integer, String> call(Tuple2<String, Integer> wordcountTuple2) throws Exception {
                                return new Tuple2<>(wordcountTuple2._2, wordcountTuple2._1);
                            }
                        });
                    
                        JavaPairRDD<Integer, String> sortedSwappedWordcount = swappedWordcount.sortByKey(false);
                        
                        JavaPairRDD<String, Integer> sortedWordcountByVaule = sortedSwappedWordcount.mapToPair(new PairFunction<Tuple2<Integer, String>, String, Integer>() {
                            @Override
                            public Tuple2<String, Integer> call(Tuple2<Integer, String> sortedSwappedTuple2) throws Exception {
                                return new Tuple2<>(sortedSwappedTuple2._2, sortedSwappedTuple2._1);
                            }
                        });
                        
                        /**
                         * 4、对逻辑处理的最终结果算子，触发执行
                         */
                        List<Tuple2<String, Integer>> wordcountCollect = sortedWordcountByVaule.collect();
                        for (Tuple2<String, Integer> tuple : wordcountCollect) {
                            System.out.println(tuple);
                        }
                
                        /**
                         * 5、关闭Spark编程入口
                         */
                        sc.close();
                    }
                }
                ```
            - spark-submit命令：
                ```
                spark-submit \
                --class com.ww.hadoop.spark.WordCountJava7 \
                --master spark://hadoopmaster:7077 \
                --driver-memory 512m \
                --executor-memory 512m \
                --total-executor-cores 1 \
                /root/hadoop_jars/spark/spark2.4.0-1.0-SNAPSHOT.jar \     //程序jar包所在位置 
                hdfs:///spark/wordcount/input      //main方法的args[0]实参，如果是集群模式运行程序，此处就应是从HDFS上读取文件
                ```
            - spark-submit提交jar包到集群时报错：`java.net.UnknownHostException: spark`，解决方法：
                - Spark集群的每个节点都要复制：把hadoop安装目录下的`core-site.xml`和`hdfs-site.xml`复制到spark的conf目录中，然后修改`spark-defaults.conf`文件内容，增加新复制的这两个文件的位置：
                    ```
                    spark.files     file:///root/apps/spark-2.4.0/conf/core-site.xml,file:///root/apps/spark-2.4.0/conf/hdfs-site.xml
                    ```
                - 检查是不是误写了`hdfs:///spark/wordcount/input`
    - Spark on Yarn：在Yarn集群上运行Spark程序，与Mapreduce、Tez等计算框架共享计算资源
        - yarn-cluster模式：适用于生产环境，中间运行时信息会输出到日志。*大数据量一般使用的都是yarn-cluster模式*
        - yarn-client模式：适合于交互与调试，Spark程序的中间运行结果会实时输出到终端。*对于小数据集可使用这种模式*
        - 两种模式的区别：
            - Spark中，Node Manager上的Application Master称之为Driver，container中跑的是executor（jvm进程）
            - yarn-client模式下，Yarn上的Application Master就在提交Spark任务的Client所在的机器上。而不像yarn-cluster模式，Application Master在Resource Manager分配的机器上
- `spark-submit`命令参数解读：
    - 提交java7版本的wordcount到Spark Standalone集群：
        ```
        spark-submit --class com.ww.hadoop.spark.WordCountJava7 \
        --master spark://hadoopmaster:7077 \
        --driver-memory 512m \
        --executor-memory 512m \
        --total-executor-cores 1 \
        --conf spark.default.parallelism=1 
        /root/hadoop_jars/spark/spark2.4.0-1.0-SNAPSHOT.jar \
        hdfs:///spark/wordcount/input hdfs:///spark/wordcount/output
        ```
    - Spark集群中executor相当于线程池，可以使用一个CPU core的计算资源。executor的数目取决于`spark-submit`时设置的两个参数：`--total-executor-cores NUM`和`--executor-cores NUM`。参考博客：[Spark学习笔记之-Spark-Standalone下driver和executor分配](https://blog.csdn.net/dandykang/article/details/48525467)
    - `--conf spark.default.parallelism=NUM`参数用于设置每个executor可以执行的task数目，即并行度。task数目的设定决定最后结果文件的数目。
    - `--executor-memory MEM`的大小不能比节点的总内存大。如果当前节点的某个应用程序已经占用了部分内存，又来了一个新的应用程序，但新应用程序的executor-memory比该节点剩余的内存要大，那么这个新的应用程序只能等待其它应用程序运行结束释放内存后，它才可以运行，所以此时Cpu core资源也压根不会分配给它。
### 3、Spark作业与Mapreduce作业之间的区别
- Mapreduce中的task是多进程并发执行，Spark中的task是多线程并发执行
    - 多进程：方便控制资源，独享进程空间。但是消耗更多的启动时间，不适合低延时作业
    - 多线程：
        - 一个节点上，一个task就是一个线程，所有的任务在一个进程中共享内存，所以多线程模式适合内存密集型任务
        - 同节点上所有任务运行在JVM进程（executor）中，适合executor所占资源被多批任务调用的情况
- Mapreduce的缺陷：
    - MRv1的缺陷与MRv2的改进：
        - 可扩展性差：
            - JobTracker/TaskTracker：首先用户程序（JobClient）提交了一个job，job的信息会发送到Job Tracker，Job Tracker是Map-reduce框架的中心，它需要与集群中的机器定时通信heartbeat，需要管理哪些程序应该跑在哪些机器上，需要管理所有job失败、重启等操作。TaskTracker是Map-Reduce集群中每台机器都有的一个部分，它做的事情主要是监视自己所在机器的资源情况。
            - MRv1的缺陷：JobTracker既负责资源管理又负责任务调度，当集群繁忙时，JobTracker很容易称为瓶颈，最终导致可扩展性问题。
            - MRv2的改进：**基本思想就是将JobTracker两个主要的功能分离成单独的组件，这两个功能是资源管理和任务调度/监控。新的资源管理器全局管理所有应用程序计算资源的分配。每一个应用的ApplicationMaster负责相应的调度和协调。一个应用程序无非是一个单独的传统的MapReduce任务或者是一个DAG(有向无环图)任务。** *ResourceManager和每一台机器的阶段管理服务器能够管理用户在哪台机器上的进程并能对计算进行组织。*
        - 可用性差：MRv1没有高可用，MRv2出现了高可用
        - 资源利用率低
            - MRv1的缺陷：TaskTracker使用`slot`（`slot`代表计算资源）等量划分本节点上的资源。一个Task获取到一个slot后才有机会运行，Hadoop调度器负责将各个TaskTracker上的空闲slot分配给Task使用。**一些Task并不能充分利用slot，而其它Task也无法使用这些空闲的资源。** slot分为Map slot和Reduce slot两种，分别供MapTask和ReduceTask使用。有时会因为作业刚刚启动等原因导致MapTask很多，而ReduceTask任务还没有调度的情况，这是Reduce slot也会被闲置。
            - MRv2的改进：将Map slot和Reduce slot统一为container，container对MapTask和ReduceTask都能执行
        - 不能支持多种计算框架：MRv2将MapReduce编程模型和Yarn做了分离，这样MapReduce可以跑在其它的资源调度系统之上，Yarn上也可以跑其它的分布式计算框架
    - MRv2的缺陷：由于MapReduce对HDFS的频繁操作（包括计算结果持久化、数据备份、资源下载及shuffle等）导致磁盘I/O成为系统性能的瓶颈，因此只适用于离线数据处理或批处理，而不能支持迭代式、交互式、流式数据处理
- MapReduce编程模型与RDD的不同：MapReduce是把数据集中的一个元素（例如一行文本）抽象为一个<key-value>对，一个元素一个元素做运算的，直到处理完所有的数据元素；Spark是将整个数据集抽象为一个分布式数据集RDD做整体运算的。
- Spark相较于MapReduce的优势：
    - 技术上的优势：
        - 减少了磁盘IO：
            - 示意图：  
            ![image](https://wx2.sinaimg.cn/mw690/006CX93ply1g0f7mjuew4j30um0dxgmp.jpg) 
            - 解释：MapReduce程序一般是由多个job串联起来执行的，每个job都会将此次map-reduce的结果持久化到HDFS，此外mapper也是输出到HDFS，reducer再从HDFS读取。这样反复地读写磁盘就使得MapReduce模型计算速度慢；Spark中task构成的DAG图是对此次Spark应用程序处理数据流程的一个描述，上一个task计算完成后会把数据交给下一个task，当下一个task完成计算后，上一个task的数据就会从内存中清除。即就是说Spark始终在内存中计算，不会把中间结果落地到磁盘，这就加快了计算速度。此外如果一个task的计算结果被下一级的多个task所依赖，那么这个task的计算结果就会被持久化到内存中。而不是如图中所示，task3需要task2的数据时，要从task1计算到task2；task7需要task2的数据时，又得重新从task1计算到task2输出，不是这样的！
        - 避免重复计算：Spark可以把数据持久化到内存中以供其它的task使用，避免了重复计算。
        - 增加并行度：MapReduce中就分两种Task：MapTask和ReduceTask，每一个Task占一个JVM进程；而Spark中一个Task占一个线程，所以一个JVM进程可以启多个Task。
        - 可选的shuffle和排序
            - MapReduce的shuffle过程是由partitioner、combiner、sorter等组件构成的一个固定的shuffle套路，是为了解决计算通用型问题，可以通过增加某些组件来达到解决特定场景问题的目的；Spark提供了4种不同的shuffle策略，分别对应不同的应用场景。MapReduce的shuffle是“全”，Spark的shuffle是“专”。
            - MapReduce的shuffle过程中排序是必须的，而Spark中排序是可选的。
        - 灵活的内存管理策略：MapReduce的Task（进程）在启动之前就给其指定了固定大小的内存，如果超出指定的内存，直接OutOfMemory；Spark中可以对一个executor（进程）和其中的Task（线程）指定固定大小的内存，虽也有可能出现OutOfMemory的情况，但是Spark的Task线程除了能够使用JVM进程的内存外（堆内内存），还可以使用操作系统的内存（堆外内存）。一个executor中的所有Task共享分配给该executor的操作系统内存。  
        ![image](https://wx2.sinaimg.cn/mw690/006CX93ply1g0f9c017m8j30p90cx3yt.jpg)
    - 业务上的优势：Hadoop生态中需要MapReduce、Hive、Mahout等工具的共同使用才能搭建成一套业务系统来应对复杂的业务计算场景，而Spark生态中基于Spark Core的Spark SQL、Spark Streaming、MLlib等用一个平台就可以搭建解决复杂业务计算场景的系统
- Spark区别于Mapreduce的基础概念
    - Spark架构中的节点角色：Master、Worker
    - Application：spark-submit提交的程序  
    ![image](https://wx1.sinaimg.cn/mw690/006CX93ply1g0e6oqp4mdj30k70bgdfz.jpg)
    - ClusterManager：集群管理器，可以是Standalone Manager、Yarn或Mesos等。  
    ![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/m5iv6S5bHcgfmctvTMNhhkjanHOq8Uh9Z5o641sdae8!/b/dDEBAAAAAAAA&bo=UgJCAQAAAAADBzE!&rf=viewer_4)
    - Driver：*相当于Yarn上的Application Master的角色。Application Master向Resource Manager申请的是Spark Executor资源，Executor启动后，由Spark的Application Master向Executor分配Task，分配多少task、分配到哪个Executor，这些都由Application Master决定。* **Driver就是Application中的所有Task程序的主控程序，包含main方法的一个进程**。初始化SparkContext就是在初始化Driver。  
    ![image](https://wx4.sinaimg.cn/mw690/006CX93ply1g0roqtovsqj30gl07ojvd.jpg)
        - ActorSystem：底层进行网络通信的基础组件。ActorSystem生成actor用于和另一个节点上的actor通信。
        - BlockManager：决定读取数据的方式和写数据时数据的格式的集群组件。
        - BroadcastManager：相当于MapReduce中的DistributeCache（DistributeCache将数据或文件分发到了所有执行MapTask的节点上），负责将用户程序中所有要**广播**的值广播到所有节点上去。如下代码中，`reduceByKey()`中的函数逻辑会在每个Worker上执行，主控程序中的`a`的值就会被广播到每个Worker。
            ```
            val a = 10
            val wordcountRDD: RDD[(String, Int)] = linesRDD.flatMap(_.split(" ")).map((_,1)).reduceByKey((x: Int,y: Int) => x+y+a)
            ```
        - TaskScheduler
        - DAGScheduler
    - Executor：每个Spark executor作为一个Yarn容器运行（executor都是装载在container中运行的），每个Executor占一个JVM进程。Executor相当于是一个线程池，多线程管理Executor中的task（线程），线程池模型省去了像MapReduce那样频繁启停进程的开销。  
    ![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/PhkS1rtJFEiVtWSDRpoc1iUm6OGBs2.2AqNPG4kIWNY!/b/dLgAAAAAAAAA&bo=RwNTAQAAAAADBzQ!&rf=viewer_4)
    - Job：和MapReduce中的Job的概念不同。Spark中的Job划分依据是一个Application中是否要得到一个scala集合或对象的数据的时候调用了一个算子，按照该算子将Application划分成前后两个job。  
    ![image](https://wx1.sinaimg.cn/mw690/006CX93ply1g0qwmr8tqdj30lo08mabp.jpg)
    - Stage：Spark中一个Job会切分为多个Stage，各个Stage之间顺序执行。Stage的划分依据是是否有shuffle依赖，按照当前的shuffle算子将一个job划分为前后两个stage。
    - Task：Spark中最小的执行单元，如下图示，一个黄色框圈起来的就是一个Task：  
    ![image](https://wx3.sinaimg.cn/mw690/006CX93ply1g0qwuomfqyj30nq0a6wki.jpg)
    - Taskset：对应一组关联的相互之间没有shuffle依赖关系的task组成
    - deploy-mode
        - `--deploy-mode client`：Driver就在提交程序的节点上
        - `--deploy-mode cluster`：Driver在资源空闲的任意一个节点上
- Spark应用程序提交大致流程：  
![image](http://m.qpic.cn/psb?/V11uuMZ31LPowO/LqloBDL2ulC10AmlwJQzIO7CW5D0SST.hENJkEeeWTw!/b/dFMBAAAAAAAA&bo=VAIcAQAAAAADB2k!&rf=viewer_4)  
    - 首先SparkContext会向ClusterManager申请集群计算资源①，在这之前DAGScheduler会划分好task。TaskScheduler在有计算资源的情况下就会派发task给不同的Worker④
    - ClusterManager会访问Worker，检查是否有足够的资源②，如果有就会启动Executor，初始化task③
    - 所有Worker中的Executor执行成功后就会给Driver返回一个状态信息⑤