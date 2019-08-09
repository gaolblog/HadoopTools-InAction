### 1、hbase基础
- 什么是hbase：分布式列式存储数据库。Hadoop的三驾马车：HDFS、MapReduce、Hbase
- 特点：
    - 高可靠性（有备份）、高性能（机器多）、可伸缩（能够动态增删节点）、面向列
    - 能够**存储并处理**大型的数据
- 列式和行式数据库的区别
    - mysql实质上就是一个解析器，将用户的sql查询语句转换成io，然后从linux/windows的底层文件系统取到数据返回给用户
    - HDFS也是架设在linux文件系统之上的一个分布式文件系统软件，它的作用是和mysql同理的
    - 区别：
        - Hbase是**按列进行数据存储的**，为了在存储大量数据时不会出错，还做了列式分表
        - Hbase只能存储数据，不能做多表关联，也没有事务管理
- Hbase存储数据的基本流程
    - 基本流程：  
    由于HDFS只能存储文件而不能存储数据（试想如果用HDFS存储数据，那么NameNode就需要对每一条数据做索引。对于海量数据，NameNode的内存肯定是不够的，此外对针对每条数据做操作，也会将NameNode“累死”），所以就需要Habse来存储索引这些数据。一条一条数据先缓存在Hbase集群的从节点HregionServer中，当数据量达到128M时，Hbase就会将这些数据作为一个文件写入HDFS上，这样对于HDFS的NameNode而言，一大块数据的元数据就只有一条。  
    所以Hbase就相当于介于原始数据源和HDFS之间的一个缓存层。
    - Hbase集群中的各个角色：
        - 一个或者多个主节点Hmaster：并不负责Hbase集群**元数据**的存储，元数据是由Zookeeper来存储的。Hmaster只负责表的信息以及在从节点HregionServer挂掉后数据的迁移工作或从节点HregionServer上线后数据的均衡分配。
        - 多个从节点HregionServer：Hmaster并不和Hbase要存储的数据打交道，HregionServer负责数据的增删改查
        - client
### 2、hbase安装
- 下载兼容已有hadoop和jdk版本的hbase，兼容性检查参考：[学习 HBase，应该选择哪个版本？](https://blog.csdn.net/tzhuwb/article/details/81153323)  
此处实验hadoop版本：2.6.4，jdk版本：1.8.0_181
- 上传`hbase-2.0.3-bin.tar.gz`、解压
- 添加环境变量：`vim /etc/profile`，添加以下内容：
    ```
    export HBASE_HOME=/root/apps/hbase-2.0.3
    export PATH=$PATH:$HBASE_HOME/bin
    ```  
    使环境变量生效：`source /etc/profile`。  
    **在其它机器上也添加上述HBASE的环境变量。**
- 修改配置文件：修改`/root/apps/hbase-2.0.3/conf`目录下的`hbase-env.sh`、`hbase-site.xml`和`regionservers`
    - `hbase-env.sh`配置Hbase的运行环境相关，需要修改3处内容：
        ```
        # jdk安装目录
        export JAVA_HOME=/root/apps/jdk1.8.0_181
        
        # 据说此处应该配java的classpath，配置成如下这样，在有flume的机器上会报错
        export HBASE_CLASSPATH=${HADOOP_HOME}/etc/hadoop/ # hadoop配置文件的位置
        # export JAVA_CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
        
        # 并发GC配置（默认就是这样，无需配置）
        export HBASE_OPTS="$HBASE_OPTS -XX:+UseConcMarkSweepGC"
        
        # 如果使用独立安装的zookeeper，这个地方就是false
        export HBASE_MANAGES_ZK=false
        ```
    - `hbase-site.xml`Hbase核心配置：
        ```
        <configuration>
        <!-- hbasemaster的主机和端口 -->
        <property>
            <name>hbase.master</name>     
            <value>hadoopmaster:60010</value>
        </property>
     
        <!-- hbase存放数据目录，该目录会由Hbase自动创建，无须手动在hdfs上创建 -->
        <property>
            <name>hbase.rootdir</name>
            <value>hdfs://hadoopmaster:9000/user/hbase</value>
        </property>             
      
        <!-- 是否分布式运行，false即为单机 -->
        <property>
            <name>hbase.cluster.distributed</name>
            <value>true</value>
        </property>
  
        <!-- list of  zookooper -->
        <property>
            <name>hbase.zookeeper.quorum</name>
            <value>hadoopmaster,hadoop01,hadoop02</value>
        </property>                             
 
        <!--zookooper配置、日志等的存储位置 -->
        <property>
            <name>hbase.zookeeper.property.dataDir</name>
            <value>/root/hadoop/hbase/zookeeper</value>
        </property>
        </configuration>
        ```
    - `regionservers`配置从节点机器的域名：
        ```
        hadoop01
        hadoop02
        ```
- 启动Hbase
    - 启动命令：
        - 启动Hbase集群：`start-hbase.sh`
        - Hbase单个节点启动：`hbase-daemon.sh start regionserver/master`（动态增加节点）
    - 启动报错：
        - `java.lang.IncompatibleClassChangeError: Found class jline.Terminal, but interface was expected`
        - 解决方法：参考CSDN博客：[hive启动报错：Found class jline.Terminal, but interface was expected](https://blog.csdn.net/silentwolfyh/article/details/51568228)  
        要在哪台机器上启动Hbase，就把哪台机器上的`/root/apps/hadoop-2.6.4/share/hadoop/yarn/lib/jline-0.9.94.jar`替换为较新版本的`jline-2.12.jar`
    - 在启动Hbase之前，先启动hdfs集群，以及在每台机器上启动Zookeeper服务后（每台机器上使用命令`./zkServer.sh start`），才能在网页上查看到Hbase信息：`http://hadoopmaster:16010`
    - 问题解决：
        - Hbase集群启动后，有一个HRegionServer启动几秒后自动挂掉，查看日志可能是集群系统时间不同步造成的，解决方法：[CentOS设置系统时间与网络时间同步](https://blog.csdn.net/keith003/article/details/82019238)
        - `hbase shell`进入hbase命令行界面后，建表报错：`Error:hbase.PleaseHoldException: Master is initializing`，查看日志`/root/apps/hbase-2.0.3/logs/hbase-root-master-hadoopmaster.log`是因为`master.HMaster: hbase:meta,,1.1588230740 is NOT online`的问题，但是HMaster进程确实是启动的。***网上也查不到行之有效的方法，最后用Hbase自带的zookeeper后正常了！！***——尝试过的网上的方法（*都没用*，但是格式化namenode这个方法没试，但觉得应该和这个没关系，而是和自己装的zookeeper配置啥的有关系，或是hbase和zookeeper产生重复文件啥的：`https://blog.csdn.net/xugen12/article/details/47279147`。）：
            - 集群机器系统时间不同步
            - `/etc/hosts`文件中没有`127.0.0.1 localhost`
            - 删除zookeeper集群`/root/hadoop/zookeeper/data/version-2`
            - 先启动regionserver，在启动HMaster
        - hadoop版本是`hadoop 2.6.4`，zookeeper版本是`zookeeper-3.4.5`，hbase版本是`Hbase-2.0.3`/`Hbase-0.98.9`，事实证明这三个版本的软件是不兼容的：zookeeper版本与hbase版本不兼容导致一系列莫名其妙的的问题（最大的问题就是HMaster、HRegionServer闪退）！
- Hbase动态增删节点：
    - 增加：复制原子节点到新节点上，然后：`hbase-daemon.sh start regionserver`
    - 删除：杀死进程
- Hbase双主：
    - 在任意安装了Hbase的机器上启动HMaster：`local-master-backup.sh start 2`
### 3、Hbase数据模型
- 基本概念：
    - rowkey：不可重复，按字典序排列。如果对相同的rowkey添加两次，这两次的数据将进行合并。
    - 时间戳：cell保存着同一份数据的多个版本，版本通过时间戳来索引
    - 列族：列族下面可以有n个列，每一个列族是一个HFile文件，列是数据内容、可以动态增加
- Hbase命令：
    - show tables：`list`
    - 表描述：`describe '表名'`
    - 判断表是否存在：`exists '表名'`
    - 判断表是否被禁用：`is_enabled '表名'`、`is_disabled '表名'`
    - insert：`put 'table_name','rowkey','列族:列','value'`
    - update：相同的rowkey，对某一个列重新添加就是修改
    - 查询：
        - 全表扫描：`scan table_name`
        - 查看某个表某个列中所有数据：`scan '表名' , {COLUMNS=>'列族名:列名'}`
        - 查看记录rowkey下的所有数据：`get '表名' , 'rowKey'`
        - 获取某个列族：`get '表名','rowkey','列族'`
        - 获取某个列族的某个列：`get '表名','rowkey','列族：列'`
        - 表行数计数：`count '表名'`
    - 删除表记录：
        - 删除某列：`delete  '表名' ,'行名' , '列族：列'`
        - 删除整行：`deleteall '表名','rowkey'`
        - 清空表：`truncate '表名'`，实际上分两步：1、disable 表；2、truncate 表
    - drop table：先要屏蔽该表，才能对该表进行删除。分两步：1、`disable '表名'`；2、`drop '表名'`
### 4、Hbase基本原理
- Hbase写数据的速度要快于读数据的速度，但是其读数据的速度还是要比Mysql快
- 写数据原理
    - 大体流程：Hbase集群中的HRegionServer通过HDFS的客户端API将数据写入HDFS的DataNode
    - 向Hbase中写一条数据的流程：
        - Hbase Client向Hbase Shell所在的HRegionServer发送写请求`put`
        - 首先通过zookeeper定位应该将数据写到哪台HRegionServer上
            - 一台HRegionServer上几个核心的东西，HRegionServer是通过这些东西将数据写到了不同的地方：
                - HRegion：类 
                - HLog：类中的文件系统路径。
                - MemStore：类中的List或Map集合，所以是内存存储的数据
                - StoreFile：类中的String包含的路径，Input流。文件版的路径
                - HFile：文件版的路径
        - 找到具体的HRegionServer后，*是将数据的部分写到这张Hbase表的某个分区（HRegion，即将这部分数据封装在HRegion类中）里*（一张Hbase表的数据在多台机器上，而不是在一台机器上）
            - *表分区的原因是为了充分利用多台机器的IO性能，提高读写速度*
        - `put 'user_test','u1','info1:name','Andy'`这条put命令本身（包含数据）是写到HLog（write ahead log）中的，HLog是一种Append Log，只能从底层追加写，但是不能修改。
        - 数据写完HLog后，再将数据写入到内存MemStore，给Hbase Client返回消息。数据要先写到HLog，原因在于当这台机器的内存挂了后，HMaster就可以从HLog中获取数据记录完成数据的迁移。由于rowkey是按字典序排的，所以特定rowkey的记录会被插入到特定的HRegion中，所以Hbase写入数据的速度很快。
        - 当MemStore中的数据达到128M时，就将数据刷到StoreFile，StoreFile再将数据写入到HFile，最后通过HDFS的API将HFiles作为一个128M的数据块put到HDFS中。同时清除MemStore和HLog中的数据。但是flush到HDFS中的数据的管理权限还是归HRegionServer这台机器所有。
        - 数据合并拆分过程：
            - 数据块从HRegionServer flush到HDFS后，这块数据的管理权限还是这个HRegion。在这个HRegionServer上的数据的删除并不是真正的从HDFS中删除，而是有一个删除日志记录删除操作，所以在查询数据的版本时还可以查到最近的版本的数据。 
            - 当HDFS上这个HRegionServer的数据块数目达到两个时，HMaster就会将数据块加载到本地合并成一块（<256M，因为有删除操作），即按照操作日志HLog对两块数据进行增删操作。如果合并后的数据块<256M，就写回HDFS，让该HRegionServer继续管理；当合并的数据超过256M时，进行均衡的拆分，将拆分后的region分配给不同的HRegionserver管理（数据块越小，读取速度会越快）
            - **这样做的好处：** 由于数据块的合并和拆分都由HMaster完成，并未占HRegionServer的资源，且合并清理了垃圾数据，拆分使得数据块被均衡分配给不同的HRegionServer管理，提高了读取效率。
- HRegionServer架构
    - 原理图  
    ![image](https://wx3.sinaimg.cn/mw690/006CX93ply1fyup4uf9xlj30kc0cm449.jpg)
    - 解释：一张Hbase表被分成多个分区存储到不同的HRegionServer上，每个HRegionServer有N个HRegion，每个HRegion中的Store中的StoreFile达到一定阈值后，就会进行一次合并操作，对同一个key的修改合并到一起，形成一个大的StoreFile。当StoreFile的大小达到一定阈值后，又会对 StoreFile进行切分操作，等分为两个StoreFile。
- Hbase插入一条数据流程总结：
    - Hbase Client通过Zookeeper存储的HRegionServer寻址地址、`-ROOT-`表、`.META.`表找到要按rowkey写入数据的HRegionServer的HRegion（注意：一个HRegionServer上对于同一张表，可能有多个不同Region的HRegion。但是对于一个HRegionServer而言，所有的HRegion共享同一份HLog）
    - 往HRegion（实质上是一个java bean）的文件路径HLog写一条数据，往HRegion的内存MemStore中写一条数据
    - HRegion中的MemStore数据达到64M（默认是64M，但通常我们选择128M）时，就从内存flush到StoreFile，然后通过HDFS Client存储到HDFS上。当该HRegion管理的数据块在HDFS上被flush了4块（假设是4块，这应该是可配置参数）时，HMaster开始将这4块64M大小的数据块加载到其本地文件系统进行合并，合并后若数据块大小不大于256M，则将合并后的大数据块写回到HDFS交由之前的HRegion继续管理。……随着数据不断地被写入，假设该HRegion又新产生了3块64M大小的数据块，加上之前合并成的256M大小的数据块，那么HDFS上又有了4块数据块，所以此时HMaster又要开始合并这4块数据块了：合并后总数据块大小大于256M了，所以HMaster需要将这块大数据块均衡地拆分成两块，然后将两块新拆分的数据块分配给两个HRegionServer。这样HMaster只需修改Zookeeper中`.META.`表的Region信息（数据记录的起始行和终止行）及其ip即可。
    - 假设某台HRegionServer的内存挂了，那么这台HRegionServer上HRegion管理的HDFS上的数据块可通过修改Zookeeper存储的Table元数据重新将数据块的管理权限分配给其它好的HRegionServer上的HRegion。但问题是挂掉的HRegionServer上的HRegion的内存数据MemStore咋办？此时HLog就要起作用了：将HLog中不同用户表的数据进行拆分，例如`user`表的是一块，`test`表的是一块。将拆分的不同的数据块重新分配给HRegionServers的HRegion，只要HRegion将分到的HLog中的数据加载到内存，那么整个HBase集群存储的数据就又完整了。极端问题考虑：如果某台HRegionServer机器直接烧了呢？那意味着硬盘烧了，HLog也不存在了！其实HLog每隔一段时间（可配置参数）就会被同步到HDFS，但是如果在Hlog被同步到HDFS的间隔时间内，服务器的硬盘烧了，那么这种情况就无法恢复数据了！！
- `-ROOT-`表和`.META.`表
    - `.META.`表存储的是Hbase用户表的Regions信息和Region的存储ip。当`.META.`表越来越大时，也会被分区
    - `-ROOT-`表存储的是`.META.`表的存储ip
- Hbase读数据流程：
    - 通过Zookeeper的`-ROOT-`表和`.META.`表定位到存储待查询数据的HRegionServer
    - HRegionServer将内存和硬盘上的数据合并后返回给Hbase Client。例如`user_test`表的`rowkey='u1'`这条数据的值在第一次被put后，很长时间都没被修改过，所以一段时间后被flush到HDFS上了（MemStore可能被清除，HLog也可能已删除数据日志），但是近期修改了它的`info1:name`的值，所以在查询`u1`这条记录时需要从内存和硬盘上进行合并
    - Hbase会通过算法将经常查询的数据记录缓存在HRegionServer上
### 5、使用MapReduce操作Hbase上的数据