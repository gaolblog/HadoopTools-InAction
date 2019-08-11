### 0、hdfs思想
- hdfs的作用：hdfs由于读写数据都很慢，所以其不是用来做类似网盘的存储功能的，它是用来支撑上层的分布式计算的！因为hdfs将文件切块、切块……，然后放在集群上，并发程序就可以处理本节点上的数据，提高了处理速度。（**分治思想**）
### 1、hdfs基础shell操作
- hdfs集群的工作目录
    - name node存放数据的工作目录：`/root/hadoop/hadoop-2.4.1/tmp/dfs/data/current/BP-31070050-192.168.157.99-1537810209829/current/finalized`
    - data node存放数据的工作目录：`/root/hadoop/hadoop-2.4.1/tmp/dfs/data/current/BP-31070050-192.168.157.99-1537810209829/current/finalized`
- hdfs shell操作命令
    - 常用命令：
        - 查看hdfs集群的状态：`hdfs dfsadmin -report`
        - 查看hdfs集群根目录下的所有文件：`hadoop fs -ls /`
        - 将linux本地文件传输到hdfs集群根目录：`hadoop fs -put file /` 或`hdfs dfs -put file /`
        - 从hdfs集群的根目录下载文件到linux本地：`hadoop fs -get /file linux本地目录`
        - 查看hdfs集群根目录下的文件的内容：`hadoop fs -cat /file`
        - 创建文件夹input：`hadoop fs -mkdir -p /wordcount/input`
    - 显示所有hdfs命令：`hadoop fs`
    - 常用命令参考手册：`E:\hadoop视频\day06\day06\02_离线计算系统_第2天（HDFS详解）.docx`（红色字体为自己标注）
    - 查看hadoop版本信息和位数：
        - 查看版本信息：`hadoop version`
        - 查看hadoop集群的位数：
            - `cd $HADOOP_HOME/lib/native`
            - `file  libhadoop.so.1.0.0`
- hdfs文件分块过程：
    - name node的作用就是：客户端在向hdfs集群写入文件时，name node会决定将此文件写入到哪几台机器中，当客户端从集群下载文件时，它又会告诉客户端依次从哪些机器下载文件的block。name node起到了“记账”的作用。
    - hdfs将文件按照128M的单位大小进行分块（可能是一个或多个块），然后将分成的块按照设定的需存储的文件副本的数量存储在多台linux机器的本地工作目录`/root/hadoop/hadoop-2.4.1/tmp/dfs/data/current/BP-31070050-192.168.157.99-1537810209829/current/finalized/subdir0/subdir0`中。
    - 从hdfs集群获取文件到某台linux机器的本地时，实际上是hdfs这个框架给客户端先后传输了文件块，然后再拼接成原来的文件。
### 2、hdfs写、读数据流程
- 写数据流程示意图（原图：`D:\JAVA\IdeaProjects\hadoop-youdaoNote\hdfs\hdfs写数据流程示意图.png`）
    - 示意图：  
    ![hdfs写数据流程示意图](https://wx4.sinaimg.cn/mw690/006CX93ply1fw7v0h473mj315h0iv0xd.jpg)  
    - **注意：** 
        - 当客户端第一次上传失败时，name node会给客户端重新分配几台data node让其重传，连续*三次*尝试重传失败就会抛异常。此时只能手动重新上传文件。
        - hdfs目前仅支持文件内容的append操作，但不能对文件内容进行修改。
- 读数据流程示意图（原图：`D:\JAVA\IdeaProjects\hadoop-youdaoNote\hdfs\hdfs读数据流程示意图.png`）  
![hdfs读数据流程示意图](https://wx1.sinaimg.cn/mw690/006CX93ply1fwcskbl1ofj30v90gegno.jpg)
### 3、NameNode管理元数据的机制
- 一步步解决NameNode管理元数据时引出来的问题：
    - 客户端写数据到hdfs集群时，NameNode应该把元数据记录在哪里？内存？磁盘上的文件？……
        - 当hdfs集群中存储的文件数量巨大时，相应地元数据也很大，如果把元数据信息存储在磁盘上，则NameNode在查询客户端请求的元数据时会很慢，所以hdfs将元数据信息存在NameNode的**内存中**
    - 把元数据存储在内存中，但万一NameNode的内存挂了呢？（这个时候虽然DataNode上的数据没有丢失，但是客户端获取不到元数据信息也就取不到DataNode上的数据）
        - 解决思路：把内存中的元数据信息定时地Dump（即序列化元数据对象）到磁盘文件（fsimage）中。但是这种思路的问题在于：假设定时时长为1小时，那么这1小时Dump出来的元数据是和前1小时Dump出来的元数据有差异的，但假如这1小时Dump数据时NameNode这台机器挂了呢？那么这前后1小时有差异的元数据信息也就不存在了！
        - 解决上述思路中的弊端：将初次运行hdfs集群的元数据信息从内存中Dump到fsimage中，往后的元数据更新操作都记录在edits日志中，如果NameNode的内存挂了，就可以依靠fsimage和edits日志文件，通过算法将元数据信息恢复到内存中。但是随着更新的元数据越来越多，edits日志文件也会越来越多，在重启集群时，将旧的fsimage加载到内存（这个很快），并且利用edits重新计算所有的元数据信息并合并fsimage，这就要把很多的edits读入到内存中进行算法运算，这样也很耗内存，恢复元数据的速度也很慢。所以此时Secondary NameNode就派上了用场：Secondary NameNode会定时请求NameNode是否需要合并（checkpoint）fsimage和edits，或者当edits达到一定数据量时，NameNode会通知Secondary NameNode进行合并，这样就可避免NameNode自己合并时对其内存的大负荷。万一NameNode的内存挂了，就可以将Secondary NameNode新合并过来的fsimage加载到内存中，很快地恢复出元数据信息。
    - NameNode管理元数据的机制示意图：  
    ![image](https://wx3.sinaimg.cn/mw690/006CX93ply1fwdbs7shd0j314w0i3ad9.jpg)
- 一个结论：hdfs集群不适合存储大量的小文件。例如一个200M的文件如果被分成20个10M的小文件，则需要20个block的元数据存储，而如果分成128M+72M，则只需要2个block的元数据存储单位。
- 思考问题：
    - NameNode如果宕机，hdfs服务还能不能正常提供？
        - 不能！因为Secondary NameNode和NameNode的角色是不可相互替换的，Secondary NameNode上虽然有fsimage元数据信息，但是其没有提供查询/更新元数据的功能。
    - 如果NameNode的硬盘损坏，元数据信息是否还能恢复？
        - 绝大部分元数据都可以被恢复。可以直接把Secondary NameNode的工作目录copy到NameNode的新磁盘上，这样就可以恢复出元数据信息了。
        - 一般而言，是将edits日志文件配置在NameNode的多个磁盘上（即配置NameNode的工作目录在多块磁盘上），做好备份，这样即便有一块磁盘损坏，也可根据其它磁盘上的edits日志恢复出完整的元数据信息。
            - 配置hdfs集群NameNode的元数据目录在不同的磁盘上（即`hdfs-site.xml`配置文件中的`dfs.namenode.name.dir`属性，默认值是`${hadoop.tmp.dir}/dfs/name`）：
                ```
                # 首先使用 fdisk -lh 命令查看硬盘使用情况
                
                # 分别在“/root”及“/disk1”目录下创建“hadoop”文件夹
                
                # vim /root/apps/hadoop-2.6.4/etc/hadoop/hdfs-site.xml
                
                # 添加如下属性配置：
                <property>
                    <name>dfs.name.dir</name>
                    <value>/root/hadoop/name1,/disk1/hadoop/name2</value>
                </property>
                
                # 要想使上述配置生效，需要停止hdfs集群后，重新format NameNode。在格式化NameNode之前需要做好如下工作：
                # 1、备份好hdfs集群上DataNode的数据
                # 2、在格式化前必须清除所有DataNode的“dfs/name”下的内容，否则在启动hadoop时子节点的守护进程会启动失败
                ```
    - 配置NameNode工作目录参数时，有没有要注意的点？
        - 将NameNode的工作目录配置在多个磁盘中
        - DataNode也可以配置多个磁盘：这样在多个客户端同时向hdfs集群写数据时，DataNode就可以并发的将不同客户端的数据写入到不同的磁盘上。但是与NameNode配置多块磁盘不同：DataNode是把多块磁盘当做同一个数据空间，而NameNode是将日志文件备份在不同的磁盘上。
        - DataNode配置多块磁盘（扩容配置）：
            ```
            vim /root/apps/hadoop-2.6.4/etc/hadoop/hdfs-site.xml
            # 添加如下属性配置：
                <property>
                    <name>dfs.data.dir</name>
                    <value>目录路径</value>
                </property>
            ```
    - **注意：** 
        - NameNode内存的标配一般是128G，如果hdfs集群中一台NameNode的内存不够用怎么办？—可以再加一台NameNode，即所谓联邦机制。
        - 客户端查询/下载hdfs集群上的文件内容并不会涉及到NameNode元数据的更新，当客户端上传文件、更改文件目录、新建文件夹等操作时就会更新NameNode中的元数据。对NameNode而言，只有将客户端的操作记录在内存和日志时，才会告诉客户端其操作是成功的，让其进行下一个操作，而文件上传失败并不会记录在日志中，只是让客户端重传。
        - NameNode工作时只会查询内存中的元数据信息，而不会管磁盘上的edits日志和fsimage镜像文件，即便将这两个文件删除了，NameNode也可以继续工作。
### 4、DataNode工作机制
- DataNode工作机制示意图：  
![image](https://wx2.sinaimg.cn/mw690/006CX93ply1fwdnslequwj30jh08ndgm.jpg)
- 集群容量不够，怎么扩容？  
[hadoop动态增加节点](https://blog.csdn.net/dxl342/article/details/52957672)
- 如果有一些datanode宕机，该怎么办？ 
    - datanode宕机原因无非是进程死亡、网络故障（namenode配置的检查心跳时间太短）引起的
    - [datanode宕机后的初步梳理](https://blog.csdn.net/xiaoyutongxue6/article/details/79810605)
- datanode明明已启动，但是集群中的可用datanode列表中就是没有，怎么办？
    - DataNode的配置文件中，指定NameNode的URL不对
    - NameNode重新格式化了，但是之前的datanode中，data目录下有着之前NameNode的标志，所以不能加入这个集群中
- hdfs的一些问题集锦：  
[HDFS的一些关键问题](https://my.oschina.net/liufukin/blog/796524)
### 5、使用hdfs提供的java API操作hdfs文件系统
- 在windows中写程序生成一个hdfs的客户端去操作linux上的hdfs文件系统，*首先需要的就是windows系统本身要有支持hdfs的环境*（**这个winutil.exe主要是用于模拟linux下的目录环境的**），如果没有这个环境，则在java程序中配置的关于hdfs的参数是不起作用的，会报空指针异常。
    - 解决方法：
        - (1)下载`hadoop.dll`、`winutils.exe`文件放到IDEA所连接的hadoop的bin目录下（win系统里边：`D:\JAVA\IdeaProjects\hadoop\win7-hadoop\hadoop-2.6.4\bin`）。**注意：** *不同win系统下的`hadoop.dll`、`winutils.exe`文件是不一样的，不能通用*
        - (2)win系统中C盘下的system32目录放一份
        - (3)win系统环境变量的PATH里边加一下(1)中的bin目录
    - 或者：如果本地windows系统没有配支持hdfs的环境，下载文件时将`“useRawLocalFileSystem=true”`，即：
        ```
        fs.copyToLocalFile(false,new Path("/case应用.sh"),newPath("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs_files"),true);
        ```
- java程序写一个客户端去操作hdfs集群：
    - java程序客户端对hdfs集群进行配置：
        - hdfs客户端默认加载的是`hdfs-lib/hadoop-hdfs-2.6.4.jar/hdfs-default.xml`中的配置
        - 如果java工程定义了配置文件，例如`D:\JAVA\IdeaProjects\hadoop\hdfs\src\hdfs-site.xml`，则以该文件中的配置为准。**注意：** java程工程下给出的xml配置文件必须放在工程目录的src目录下，否则不起作用。即**在工程classpath下给定相应的配置**
        - 如果java程序中定义了对hdfs集群的配置，例如`conf.set("dfs.replication","3");`，则前面两处的配置都不起作用，以java程序中定义的配置为准
