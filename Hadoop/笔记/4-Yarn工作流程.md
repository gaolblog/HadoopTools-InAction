### yarn
- yarn是什么：Yarn是一个资源调度平台，负责为运算程序提供服务器运算资源（类似docker的虚拟运算容器（cpu+ram））。相当于一个==分布式的操作系统平台==，而mapreduce、spark、storm等运算程序则相当于运行于操作系统之上的应用程序。
- yarn的特点：YARN只负责程序运行所需资源的分配、回收等调度任务，与应用程序的内部工作机制完全无关（**即运算程序和YARN是解耦的**）。所以YARN已经成为通用的资源调度平台，许多的运算框架都可以借助YARN来进行资源管理，例如：MapReduce、Spark、Storm、TEZ、Flink等。
- mapreduce程序在yarn集群上的运行流程示意图：  
![image](https://wx4.sinaimg.cn/mw690/006CX93ply1fwqly3c82bj31kw0yzwqx.jpg)