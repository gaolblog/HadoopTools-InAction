### 1、Spark体系架构
- Spark软件栈
    - 示意图：  
    ![image](https://wx3.sinaimg.cn/mw690/006CX93ply1g0qvxdmectj30lt08uta3.jpg)
    - Spark基础功能：
        - Spark Core是Spark中最基础核心的功能。Spark Core中的核心功能：
            - SparkContext：  
            Driver Application的执行与输出都是通过SparkContext来完成的，在正式提交Application之前，首先要初始化SparkContext。==SparkContext隐藏了网络通信、分布式部署、消息通信、存储能力、计算能力、缓存、测量系统、文件服务、Web服务等内容==，应用程序开发者只需要使用SparkContext提供的API完成功能开发。  
            SparkContext内置的**DAGScheduler**负责创建job，将DAG中的RDD划分为不同的Stage，提交Stage等功能。  
            SparkContext内置的**TaskScheduler**负责资源的申请、任务的提交及请求集群对任务的调度等工作。  
            *SparkContext的角色类似于一个Boss，DAGScheduler相当于Boss的大脑，负责任务的规划工作；而TaskScheduler相当于Boss的秘书，负责将DAGScheduler的任务规划派发给具体的执行机构去实现。*
        - 存储体系：
            - Spark提供多种数据源获取方式：HDFS、HBase、关系型数据库、云产品等。
            - Spark优先考虑使用各节点的内存作为存储（BlockManager集群），当内存不足时才会考虑使用磁盘，这极大地减少了磁盘I/O，提升了任务执行效率，使得Spark适用于实时计算、流式计算等场景。此外，Spark还提供了以内存为中心的高容错分布式文件系统Alluxio供用户选择，Alluxio能够为Saprk提供可靠的内存级文件共享服务。
        - 计算引擎：  
        Spark计算引擎由SparkContext中的DAGScheduler、RDD及具体节点上的Executor负责执行的Map和Reduce任务组成。DAGScheduler和RDD虽然位于SparkContext内部，但是在任务正式提交与执行前将job中的RDD组织成有向无环图（DAG）、并对Stage进行划分决定了任务执行阶段任务的数量、迭代计算、shuffle等过程。
        - 部署模式：6种部署模式：Local、Standalone、Yarn、Mesos、Kubernetes、Cloud
    - Spark扩展功能：
        - SparkSQL：Spark增加了对SQL及Hive的支持。  
        SparkSQL的大致过程：首先使用SQL语法解析器将SQL转换为语法树，并且使用规则执行器将一系列规则应用到语法树，最终生成物理执行计划并执行的过程。其中，规则包括语法分析器和优化器。
        - SparkStreaming：SparkStreaming支持Kafka、Flume、Twitter、MQTT、ZeroMQ、Kinesis和简单的TCP套接字等多种数据输入流。输入流接收器负责接入数据，是接入数据流的接口规范。DStream是SparkStreaming中所有数据流的抽象，DStream可以被组织成DStreamGraph。DStream本质上是由一系列连续的RDD组成。**Storm是来一条数据处理一次，SparkStreaming是一次性处理一小段时间内的所有数据。**
        - SparkGraphX：GraphX是Spark提供的分布式图计算框架。GraphX主要遵循整体同步并行计算模式（BulkSynchronous Parallell，简称BSP）下的pregel模型实现。GraphX目前已经封装了最短路径、网页排名、连接组件、三角关系统计等算法的实现。
        - SparkMLlib：MLlib目前已经提供了基础统计、分类、回归、决策树、随机森林、朴素贝叶斯、保序回归、协同过滤、聚类、维数缩减特征提取与转型、频繁模式挖掘、预言模型标记语言、管道等多种数理统计、概率论、数据挖掘方面的算法。
- Spark核心概念
    - 