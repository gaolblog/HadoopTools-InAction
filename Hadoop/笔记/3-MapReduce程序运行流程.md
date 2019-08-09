### 1、mapreduce工程工作目录
- mapreduce工程所在目录：`/root/apps/hadoop-2.6.4/share/hadoop/mapreduce`
- 运行mapreduce示例程序：`hadoop jar hadoop-mapreduce-examples-2.6.4.jar wordcount /wordcount/input /wordcount/output`
    - 运行成功的前提：hdfs集群的`/wordcount`目录下没有`output`文件夹
    - 运行成功的标志：
        ```
        18/10/08 18:09:16 INFO mapreduce.Job:  map 100% reduce 0%
        18/10/08 18:09:22 INFO mapreduce.Job:  map 100% reduce 100%
        18/10/08 18:09:23 INFO mapreduce.Job: Job job_1538989251002_0002 completed successfully
        18/10/08 18:09:23 INFO mapreduce.Job: Counters: 51
        ```
    - 注意：运行完mapreduce程序后，会在hdfs集群的根目录下生成一个`tmp`临时文件夹。
- mapreduce程序相对于hdfs来说，它是一个客户端。mapreduce程序从hdfs集群上读取数据，然后进入自己的处理流程。
### 2、mapreduce核心思想
1. mapreduce是什么？  
==mapreduce是一个分布式计算的编程框架。mapreduce的**核心功能**是将++用户编写的业务逻辑代码++和++自带默认组件++整合成一个完整的分布式运算程序，并发地运行在一个hadoop集群上。==
### 3、mapreduce程序运行流程
- 示意图：
    - WordCount—mapreduce程序执行流程：  
    ![image](https://wx2.sinaimg.cn/mw690/006CX93ply1fwqm1be2toj31kw0l2thh.jpg)
    - mapreduce内部shuffle过程：  
    ![image](https://wx1.sinaimg.cn/mw690/006CX93ply1fwqm2lj6lcj31kw0b3aj4.jpg)
- 流程分析：
    - **map task**
        - ***map task输出的数据必须是实现了hadoop序列化框架Writable接口的类型，如果要输出用户自定义的一个对象，就必须实现Writable接口。***
        - map task是越多越好？还是多少个合适？（Map Task的并行度：Map Task的数量本质上就是文件切片的数量）
            - **一般而言，在一台机器上启动的map task的数量是和这台机器CPU的核数有关系的。假设CPU是12核的，就可以启动12个map task。**
            - map task的个数本质上就是将待处理的数据切成若干份。（**一个map task进程对应一个文件分片**）
            - map task的任务划分（**逻辑划分**）是相对于hdfs之外的，hdfs对它来说是透明的。所以yarn客户端可以将一个300M大小的文件划分为3个100M的文件分片（**文件逻辑分片的工作是由yarn客户端完成的**），每个map task处理一个分片；而这300M的文件在hdfs内部是按照0-128M、128M-256M、256M-300M三个block**物理划分**的。**所以，任务划分并不是绝对的，对于hdfs上的大文件而言：一个map task处理一个block大小的文件分片就最好；对于小文件而言：一个map task处理多个小文件的block就最好。**
            - 用户的mapreduce程序放在hadoop集群上运行时，首先会启动mapreduce application master（mrp master），但mrp master怎么知道启动几个map task来运行mapreduce程序呢？——就是根据yarn客户端提交的`job.split`的文件中规划的文件分片的数量来决定的。
            - 文件分片的划分是由yarn客户端完成的，怎么形成文件切片说明文件的？  
            `boolean res = job.waitForCompletion();`方法实现的。
                - 本地java程序新建了一个yarn客户端，并把`job.xml`、`job.split`、jar包提交给yarn的代码流程追踪：  
                1. `job.waitForCompletion()`启动job提交流程`submit()`
                2. `submit()`主要做了三件事：
                    - `setUseNewAPI()`：使用new API。
                    - `connect()`：和yarn建立连接，实质上就是由本地java程序生成了一个和yarn通信的客户端。
                        - 给Job类中的成员变量cluster赋值。cluster是一个对象，这个对象配置了conf参数、获取到了当前客户端的用户身份、创建了一个提交job任务的yarn客户端对象。*如果没有配置yarn集群的地址，mapreduce框架提供一个yarn模拟器，这个模拟器在本地运行*。
                    - `submitter.submitJobInternal()`：提交
                        - 检查计算结果输出路径
                        - 获取yarn给客户端的job提交路径，这个路径应在hdfs上。如果是在本地上，则路径类似：`file:/tmp/hadoop-gaol/mapred/staging/root713665138/.staging`
                        - 获取yarn集群给它一个jobId
                        - 设置这个job的提交路径：`jobStagingArea+jobId`
                        - 把job对象写成一个文件，copy到提交路径
                        - 文件切片（`FileInputFormat`实现类的`getSplits()`方法完成）。将文件切片信息（`job.split`）写到了job提交路径jobSubmitDir上
                        - 写jar包、job.xml到提交路径：`writeConf(conf, submitJobFile)`
                        - 删除提交路径下生成的临时文件：job.split、job.xml、job.splitmetainfo
                3. FileInputFormat中**文件切片大小的参数配置**：参看文档：`E:\hadoop视频\day08\day08\03_离线计算系统_第3天（MAPREDUCE详解）v.3.docx`
                    - `FileInputFormat` map task和客户端都会使用，客户端使用它做文件切片（`getSplits()`方法），map task使用它从文件切片中一行行地读取数据。
                4. 代码追踪涉及的几个java文件：
                    - `E:\hadoop视频\day06\day06\软件\hadoop-2.6.4-src\hadoop-2.6.4-src\hadoop-mapreduce-project\hadoop-mapreduce-client\hadoop-mapreduce-client-core\src\main\java\org\apache\hadoop\mapreduce\Job.java`
                    - `E:\hadoop视频\day06\day06\软件\hadoop-2.6.4-src\hadoop-2.6.4-src\hadoop-mapreduce-project\hadoop-mapreduce-client\hadoop-mapreduce-client-core\src\main\java\org\apache\hadoop\mapreduce\Cluster.java`
                    - `E:\hadoop视频\day06\day06\软件\hadoop-2.6.4-src\hadoop-2.6.4-src\hadoop-mapreduce-project\hadoop-mapreduce-client\hadoop-mapreduce-client-core\src\main\java\org\apache\hadoop\mapreduce\JobSubmitter.java`
                    - `E:\hadoop视频\day06\day06\软件\hadoop-2.6.4-src\hadoop-2.6.4-src\hadoop-mapreduce-project\hadoop-mapreduce-client\hadoop-mapreduce-client-core\src\main\java\org\apache\hadoop\mapreduce\lib\input\FileInputFormat.java`
        - 关于大量小文件的优化策略：
            - 默认情况下，`TextInputFormat`对任务的切片是按切片规划文件实现的，不管文件多小，都会被切成一个片交给一个map task处理。这样如果有大量的小文件就会产生大量的map task，从而使得效率低下。
            - 优化策略：最好的办法是在数据采集时或数据处理系统的预处理阶段，就将小文件**合并**成大文件，然后再上传到hdfs。
            - 补救策略：如果大量小文件已经存储在hdfs上了，就使用另一种`InputFormat`来做文件的切片：`CombineFileInputFormat`，它的切片逻辑是将多个小文件从逻辑上规划到一个切片中，这样多个小文件就可以用一个map task来处理。
    - **reduce task**
        - 如果用户未自定义reduce task的数目，默认只启动1个reduce task。此时map task也就不用将输出结果进行分区了，直接将输出结果全部交给reduce task就行了。分区号的默认实现：
            ```
            /** Partition keys by their {@link Object#hashCode()}. */
            @InterfaceAudience.Public
            @InterfaceStability.Stable
            public class HashPartitioner<K, V> extends Partitioner<K, V> {
            
              /** Use {@link Object#hashCode()} to partition. */
              public int getPartition(K key, V value,
                                      int numReduceTasks) {
                //例如：这种做法只能保证同一个单词到同一个reduce task，每个reduce task拿到的单词数量不一定均匀
                return (key.hashCode() & Integer.MAX_VALUE) % numReduceTasks;
              }
            
            }
            ```
### 4、mapreduce程序运行的模式
- Hadoop三种运行模式参考博客：
    - [hadoop本地模式和伪分布式模式](https://www.jianshu.com/p/43556336b945)
    - [Windows 7 64位系统上搭建Hadoop伪分布式环境（很详细）](https://blog.csdn.net/u013159040/article/details/81939662)
- 本地运行模式
    - 含义：yarn客户端将程序提交到windows7本地的yarn模拟器，是在本地jvm中用线程模拟了map task和reduce task。
    - 参数配置：
        - 在程序中配置：
            ```
            Configuration conf = new Configuration();
            //配置mapreduce程序在本地运行
            conf.set("mapreduce.framework.name","local");
            //以下一句不写
            //conf.set("yarn.resourcemanager.hostname",hostname);
            //配置文件系统为本地文件系统
            conf.set("fs.defaultFS","file:///");
            ```
        - 或者在配置文件中配置：
            - `mapred-default.xml`：
                ```
                <property>
                  <name>mapreduce.framework.name</name>
                  <value>local</value>
                  <description>The runtime framework for executing MapReduce jobs.
                  Can be one of local, classic or yarn.
                  </description>
                </property>
                ```
            - `core-default.xml`：
                ```
                <property>
                  <name>fs.defaultFS</name>
                  <value>file:///</value>
                  <description>The name of the default file system.  A URI whose
                  scheme and authority determine the FileSystem implementation.  The
                  uri's scheme determines the config property (fs.SCHEME.impl) naming
                  the FileSystem implementation class.  The uri's authority is used to
                  determine the host, port, etc. for a filesystem.</description>
                </property>
                ```
            - `yarn-default.xml`（这个配置文件不用管！列在此处主要是为了说明yarn的默认配置文件内容）
                ```
                <property>
                <description>The hostname of the RM.</description>
                <name>yarn.resourcemanager.hostname</name>
                <value>0.0.0.0</value>
              </property>    
              
              <property>
                <description>The address of the applications manager interface in the RM.</description>
                <name>yarn.resourcemanager.address</name>
                <value>${yarn.resourcemanager.hostname}:8032</value>
              </property>
                ```  
            - ==**注意：**== 配置文件默认配置的就是local yarn模拟器和fs，所以如果程序中未配置任何参数，直接使用配置文件的默认配置，mapreduce程序就可以跑在windows本地。
    - **注意：**
        - 由于IntelliJ IDEA没有Hadoop插件，所以若要将windows本地的MapReduce程序提交到远程服务器的Yarn集群上运行就需要做以下几件事情：
            - 编译适合windows系统版本的Hadoop软件
            - 从网上下载适合编译后的Hadoop版本的`hadoop.dll`和`winutils.exe`。将`hadoop.dll`分别复制到windows版的Hadoop的bin目录下、`C:\Windows\System32`目录下；将`winutils.exe`复制到`C:\Windows\System32`目录下
            - 新建`HADOOP_HOME`环境变量，将其添加到系统环境变量中：`%HADOOP_HOME%\bin\`，然后重启电脑
            - 代码中做如下参数配置：
                ```
                static Configuration conf;
                static {
                    //要么在windows系统配置“HADOOP_HOME”环境变量后重启电脑，要么做如下设置
                //        System.setProperty("hadoop.home.dir", "D:\\JAVA\\IdeaProjects\\hadoop\\win7-hadoop\\hadoop-2.6.0");
                
                        // 指定日志文件位置，必须使用绝对路径，可以直接使用hadoop配置文件中的log4j.properties，也可单独建立
                        PropertyConfigurator.configure("D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/src/main/resources/hadoop/log4j.properties");
                
                        conf = new Configuration();
                
                        //配置文件系统为HDFS
                        conf.set("fs.defaultFS","hdfs://master:8020");
                
                        //指定MapReduce程序分布式运行在yarn上
                        conf.set("mapreduce.framework.name","yarn");
                
                        //指定yarn的resource manager的地址
                        conf.set("yarn.resourcemanager.hostname","master");
                
                        // 添加Hadoop配置文件（在resources目录下新建hadoop目录，然后将这4个配置文件复制到此目录）
                        conf.addResource("hadoop/hdfs-site.xml");
                        conf.addResource("hadoop/core-site.xml");
                        conf.addResource("hadoop/mapred-site.xml");
                        conf.addResource("hadoop/yarn-site.xml");
                
                        // 如果要从windows系统中运行这个job提交客户端的程序，则需要加这个跨平台提交的参数。
                        // 在windows下如果没有这句代码会报错 "/bin/bash: line 0: fg: no job control"，去网上搜答案很多都说是linux和windows环境不同导致的一般都是修改YarnRunner.java，但是其实添加了这行代码就可以了。
                        conf.set("mapreduce.app-submission.cross-platform","true");
                
                        //指定jar包位置
                        conf.set("mapreduce.job.jar", "D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/out/artifacts/mapreduce_maven_jar/mapreduce-maven.jar");
                    }
                ```
            - IntelliJ IDEA中对于project的配置参考博客：[配置IDEA开发环境向远程集群提交MapReduce应用](https://blog.csdn.net/m0_37367424/article/details/84031045)
        - IntelliJ IDEA中做配置使MapReduce程序在windows本地运行（local运行方式）：
            - 需要有windows版本的Hadoop软件
            - windows系统`C:\Windows\System32`目录和Hadoop的bin目录中不能有`hadoop.dll`
            - 代码中做如下参数配置：
                ```
                static Configuration conf;
                    static {
                        //要么在windows系统配置“HADOOP_HOME”环境变量后重启电脑，要么做如下设置
                //        System.setProperty("hadoop.home.dir", "D:\\JAVA\\IdeaProjects\\hadoop\\win7-hadoop\\hadoop-2.6.0");
                
                        // 指定日志文件位置，必须使用绝对路径，可以直接使用hadoop配置文件中的log4j.properties，也可单独建立
                        PropertyConfigurator.configure("D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/src/main/resources/hadoop/log4j.properties");
                
                        conf = new Configuration();
                
                        //配置文件系统为本地文件系统
                        conf.set("fs.defaultFS","file:///");
                        
                        //指定MapReduce程序分布式运行在JVM本地进程
                        conf.set("mapreduce.framework.name","local");
                
                        //指定jar包位置
                        conf.set("mapreduce.job.jar", "D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/out/artifacts/mapreduce_maven_jar/mapreduce-maven.jar");
                    }
                ```
            - 此时运行MapReduce程序出现如下异常信息：
                ```
                Exception in thread "main" java.lang.UnsatisfiedLinkError: org.apache.hadoop.io.nativeio.NativeIO$Windows.access0(Ljava/lang/String;I)Z
            	at org.apache.hadoop.io.nativeio.NativeIO$Windows.access0(Native Method)
            	at org.apache.hadoop.io.nativeio.NativeIO$Windows.access(NativeIO.java:609)
            	at org.apache.hadoop.fs.FileUtil.canRead(FileUtil.java:991)
            	at org.apache.hadoop.util.DiskChecker.checkAccessByFileMethods(DiskChecker.java:187)
            	at org.apache.hadoop.util.DiskChecker.checkDirAccess(DiskChecker.java:174)
            	at org.apache.hadoop.util.DiskChecker.checkDir(DiskChecker.java:108)
            	at org.apache.hadoop.fs.LocalDirAllocator$AllocatorPerContext.confChanged(LocalDirAllocator.java:314)
            	at org.apache.hadoop.fs.LocalDirAllocator$AllocatorPerContext.getLocalPathForWrite(LocalDirAllocator.java:377)
            	at org.apache.hadoop.fs.LocalDirAllocator.getLocalPathForWrite(LocalDirAllocator.java:151)
            	at org.apache.hadoop.fs.LocalDirAllocator.getLocalPathForWrite(LocalDirAllocator.java:132)
            	at org.apache.hadoop.fs.LocalDirAllocator.getLocalPathForWrite(LocalDirAllocator.java:116)
            	at org.apache.hadoop.mapred.LocalDistributedCacheManager.setup(LocalDistributedCacheManager.java:126)
            	at org.apache.hadoop.mapred.LocalJobRunner$Job.<init>(LocalJobRunner.java:171)
            	at org.apache.hadoop.mapred.LocalJobRunner.submitJob(LocalJobRunner.java:764)
            	at org.apache.hadoop.mapreduce.JobSubmitter.submitJobInternal(JobSubmitter.java:244)
            	at org.apache.hadoop.mapreduce.Job$10.run(Job.java:1307)
            	at org.apache.hadoop.mapreduce.Job$10.run(Job.java:1304)
            	at java.security.AccessController.doPrivileged(Native Method)
            	at javax.security.auth.Subject.doAs(Subject.java:415)
            	at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1924)
            	at org.apache.hadoop.mapreduce.Job.submit(Job.java:1304)
            	at org.apache.hadoop.mapreduce.Job.waitForCompletion(Job.java:1325)
            	at com.gsww.hadoop.mapreduce.MapJoin.main(MapJoin.java:202)
                ```  
                解决方法：在工程的java目录下新建package：`org.apache.hadoop.io.nativeio`，将Hadoop源码中的`org.apache.hadoop.io.nativeio`下的`NativeIO.java`文件复制到新建的package下。打开`NativeIO.java`文件找到第557行，将`return access0(path, desiredAccess.accessRight());`修改为`return true;`。
- 集群运行模式
    - 含义：把程序提交到yarn集群中去运行。
    - 参数配置：
        - 要想运行为集群模式，以下3个参数必须配置：
            ```
            Configuration conf = new Configuration();
            //指定mr程序分布式运行在yarn上
            conf.set("mapreduce.framework.name","yarn");
            //指定yarn的resource manager的地址
            conf.set("yarn.resourcemanager.hostname","hadoopmaster");
            //指定分布式文件系统
            conf.set("fs.defaultFS","hdfs://hadoopmaster:9000");
            ```  
        - ==**注意：**==  
            - 如果mr程序配置在yarn上运行，那么文件系统fs也必须指定为hdfs，因为如果fs指定为`file:///`，其它机器上的map task是拿不到客户端本地的文件数据的。 
            - 因为在linux上使用`hadoop jar`命令运行jar包时，该命令已经将linux本地的hadoop安装目录下的配置文件、hadoop的环境参数依赖等都加入到了classpath中，所以即便在mr程序中不做上述3句代码的参数配置，也可以正常运行jar包程序。
            - 程序中不做上述3句代码的参数配置，但使用`java -jar`命令来运行Runnable jar包程序，这就要求把`core-site.xml`、`hdfs-site.xml`、`mapred-site.xml.template`和`yarn-site.xml`这4个xml文件（已经配置好的，或直接从hadoop集群下载到windows上）加入到java工程的src目录下（即手动加入到该工程的classpath下），然后打成Runnable JAR在linux上运行。但是此时的问题是：`job.setJarByClass(WordCountDriver.class)`并不能让YarnRunner找到本java工程所在的Runnable jar包，所以需要显式设置Runnable JAR的位置：`job.setJar("/root/hadoop/hadoop.jars/wordcount.jar")`。
    - 在windows本地运行程序（客户端运行在IDEA中），将jar包提交到yarn集群上去运行：
        - 要么在程序中配置上述3句参数代码，要么把`core-site.xml`、`hdfs-site.xml`、`mapred-site.xml.template`和`yarn-site.xml`这4个xml文件（配置好的）copy到java工程的src目录下；
        - 设置本工程在windows上的Runnable JAR的位置：`job.setJar("D:/JAVA/IdeaProjects/hadoop/mapreduce/out/artifacts/mapreduce_jar/eclipse_runnable_wordcount.jar")`
        - 将`mapreduce-lib`下的`hadoop-mapreduce-client-jobclient-2.6.4.jar`中的`org.apache.hadoop.mapred`包中的`YARNRunner`类用新建在src下的`org.apache.hadoop.mapred`包中的`YARNRunner`类进行替换：主要是把**windows下的程序启动命令中的符号（%）修改为linux下的符号（$）**。修改后的`YARNRunner`类中的内容参看：`D:\JAVA\IdeaProjects\hadoop\mapreduce\src\org\apache\hadoop\mapred\YARNRunner.java`
        - 记得在IDEA中设置`VM options`：`-DHADOOP_USER_NAME=root`
        - 这样在windows本地就可以将程序提交到yarn集群上去运行，也可以在windows本地远程调试程序。
- 伪分布式运行
    - 含义：所谓伪分布式运行是指在一台机器上用不同的Java进程模拟Hadoop集群中的各个角色（NameNode、DataNode、SecondaryNameNode、YarnRunner、MRAppMaster）
    - 运行条件（以windows系统为例）：
        - 编译好的Hadoop软件
        - Hadoop的bin目录中要有`hadoop.dll`，`C:\Windows\System32`目录要有`winutils.exe`
        - 和本地运行模式一样，修改`NativeIO.java`文件第557行
        - 代码中做如下配置：
            ```
            static Configuration conf;
                static {
                    //要么在windows系统配置“HADOOP_HOME”环境变量后重启电脑，要么做如下设置
            //        System.setProperty("hadoop.home.dir", "D:\\JAVA\\IdeaProjects\\hadoop\\win7-hadoop\\hadoop-2.6.0");
            
                    // 指定日志文件位置，必须使用绝对路径，可以直接使用hadoop配置文件中的log4j.properties，也可单独建立
                    PropertyConfigurator.configure("D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/src/main/resources/hadoop/log4j.properties");
            
                    conf = new Configuration();
            
                    //伪分布式文件系统
                    conf.set("fs.defaultFS","hdfs://localhost:9000");
            
                    //指定MapReduce程序分布式/伪分布式运行在yarn上
                    conf.set("mapreduce.framework.name","yarn");
            
                    //指定yarn的resource manager的地址
            //        conf.set("yarn.resourcemanager.hostname","master");
            
                    // 添加伪分布式配置文件
                    conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/hdfs-site.xml");
                    conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/core-site.xml");
                    conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/mapred-site.xml");
                    conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/yarn-site.xml");
            
                    // 如果要从windows系统中运行这个job提交客户端的程序，则需要加这个跨平台提交的参数。
                    // 在windows下如果没有这句代码会报错 "/bin/bash: line 0: fg: no job control"，去网上搜答案很多都说是linux和windows环境不同导致的一般都是修改YarnRunner.java，但是其实添加了这行代码就可以了。
            //        conf.set("mapreduce.app-submission.cross-platform","true");
            
                    /**
                     * 以下两句代码解决伪分布式运行时异常：
                     * org.apache.hadoop.yarn.exceptions.InvalidResourceRequestException:
                     *      Invalid resource request, requested memory < 0, or requested memory > max configured, requestedMemory=-1, maxMemory=8192
                     */
                    conf.set("mapreduce.map.memory.mb","1024");
                    conf.set("mapreduce.reduce.memory.mb","1024");
            
                    //指定jar包位置
                    conf.set("mapreduce.job.jar", "D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/out/artifacts/mapreduce_maven_jar/mapreduce-maven.jar");
                }
            ```
        - 设置虚拟机参数：`-DHADOOP_USER_NAME=windows用户名称`，例如：`-DHADOOP_USER_NAME=gaol`
### 5、mapreduce程序运行时问题解决
- hadoop集群上运行eclipse打的普通jar包（不包含第三方依赖jar包）：
    - 命令：
        ```
        hadoop jar eclipse_wordcount.jar  com.nwnu.hadoop.mapreduce.wordcount.WordCountDriver /wordcount/input /wordcount/output
        ```  
        实质上该命令与下面命令的效果是一样的：
        ```
        java -cp [hadoop工程中的所有classpath列在此处] eclipse_wordcount.jar  com.nwnu.hadoop.mapreduce.wordcount.WordCountDriver /wordcount/input /wordcount/output
        ```  
        只不过`hadoop jar`命令是将classpath帮我们自动设置好了！
    - 说明：在hadoop上运行普通jar包时，代码中并没有加：
        ```
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        ```  
        但是程序可以正常运行。
- 将wordcount程序用eclipse打成Runnable JAR在hadoop集群上跑（用的命令：`java -jar eclipse_runnable_wordcount.jar /wordcount/input /wordcount/output`）时报的异常：
    - `java.io.IOException: No FileSystem for scheme: hdfs`，解决方法：在设置hadoop配置的时候，加上如下代码：
        ```
        Configuration conf = new Configuration();
        //要加的代码：
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        ```
    - 继续用命令``java -jar eclipse_runnable_wordcount.jar /wordcount/input /wordcount/output``运行jar包时报异常：
        ```
        Exception in thread "main" java.lang.reflect.InvocationTargetException
        ......
        Caused by: org.apache.hadoop.mapreduce.lib.input.InvalidInputException: Input path does not exist: file:/wordcount/input
        ```  
        异常解决方法：使用如下命令运行jar包即可：  
            ```
            java -jar eclipse_runnable_wordcount.jar hdfs://hadoopmaster:9000/wordcount/input hdfs://hadoopmaster:9000/wordcount/output
            ```
- Mapper类中的map()方法和Reducer类中的reduce()方法中`System.out.println(要打印的内容)`打印出来的内容在哪看？  
`System.out.println(要打印的内容)`中要打印的内容会被执行map task或reduce task的container打印在container的目录下。
    - 首先在`http://hadoopmaster:8088`查看执行map task和reduce task的Node在哪台机器上；
    - 在对应机器上进入`Attempt ID`标识的目录，例如：`/root/apps/hadoop-2.6.4/logs/userlogs/application_1540718744088_0004`，在该目录下查找map task或reduce task生成的`stdout`日志即可。