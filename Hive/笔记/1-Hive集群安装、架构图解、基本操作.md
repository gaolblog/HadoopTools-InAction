### 1、hive安装配置
- 安装命令：
    ```
    tar -zxvf apache-hive-1.2.1-bin.tar.gz -C ./apps/
    mv apache-hive-1.2.1-bin hive-1.2.1
    ```
- 配置hive：
    - hive默认使用的元信息数据库是db，db是在当前hive运行目录下生成元信息数据库文件，所以当每次hive启动的目录不同时，上次建的库、表就看不到了。所以给hive配置一个独立的mysql数据库作为其元信息数据库：`vim hive-site.xml`
        ```
        <configuration>
        <!-- 配置mysql服务器主机名和端口 -->
        <property>
        <name>javax.jdo.option.ConnectionURL</name>
        <value>jdbc:mysql://localhost:3306/hive?createDatabaseIfNotExist=true</value>
        <description>JDBC connect string for a JDBC metastore</description>
        </property>
        
        <!-- 配置mysql驱动 -->
        <property>
        <name>javax.jdo.option.ConnectionDriverName</name>
        <value>com.mysql.jdbc.Driver</value>
        <description>Driver class name for a JDBC metastore</description>
        </property>
        
        <!-- mysql用户名 -->
        <property>
        <name>javax.jdo.option.ConnectionUserName</name>
        <value>root</value>
        <description>username to use against metastore database</description>
        </property>
        
        <!-- mysql登录密码 -->
        <property>
        <name>javax.jdo.option.ConnectionPassword</name>
        <value>root</value>
        <description>password to use against metastore database</description>
        </property>
        </configuration>
        ```
    - 把mysql驱动包`mysql-connector-java-5.1.28.jar`放到hive的classpath（`/root/apps/hive-1.2.1/lib`）下
    - 启动hdfs集群的前提下启动hive：
        ```
        cd /root/apps/hive-1.2.1
        bin/hive
        ```
    - 启动时报错：  
        ```
        [ERROR] Terminal initialization failed; falling back to unsupported
        java.lang.IncompatibleClassChangeError: Found class jline.Terminal, but interface was expected
        	at jline.TerminalFactory.create(TerminalFactory.java:101)
        	at jline.TerminalFactory.get(TerminalFactory.java:158)
        	at jline.console.ConsoleReader.<init>(ConsoleReader.java:229)
        	at jline.console.ConsoleReader.<init>(ConsoleReader.java:221)
        	at jline.console.ConsoleReader.<init>(ConsoleReader.java:209)
        	at org.apache.hadoop.hive.cli.CliDriver.setupConsoleReader(CliDriver.java:787)
        	at org.apache.hadoop.hive.cli.CliDriver.executeDriver(CliDriver.java:721)
        	at org.apache.hadoop.hive.cli.CliDriver.run(CliDriver.java:681)
        	at org.apache.hadoop.hive.cli.CliDriver.main(CliDriver.java:621)
        	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        	at java.lang.reflect.Method.invoke(Method.java:498)
        	at org.apache.hadoop.util.RunJar.run(RunJar.java:221)
        	at org.apache.hadoop.util.RunJar.main(RunJar.java:136)
        
        Exception in thread "main" java.lang.IncompatibleClassChangeError: Found class jline.Terminal, but interface was expected
        	at jline.console.ConsoleReader.<init>(ConsoleReader.java:230)
        	at jline.console.ConsoleReader.<init>(ConsoleReader.java:221)
        	at jline.console.ConsoleReader.<init>(ConsoleReader.java:209)
        	at org.apache.hadoop.hive.cli.CliDriver.setupConsoleReader(CliDriver.java:787)
        	at org.apache.hadoop.hive.cli.CliDriver.executeDriver(CliDriver.java:721)
        	at org.apache.hadoop.hive.cli.CliDriver.run(CliDriver.java:681)
        	at org.apache.hadoop.hive.cli.CliDriver.main(CliDriver.java:621)
        	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        	at java.lang.reflect.Method.invoke(Method.java:498)
        	at org.apache.hadoop.util.RunJar.run(RunJar.java:221)
        	at org.apache.hadoop.util.RunJar.main(RunJar.java:136)
        ```  
        上述错误是因为hive的`jline-2.12.jar`与hadoop的`/root/apps/hadoop-2.6.4/share/hadoop/yarn/lib`下的`jline-0.9.94.jar`冲突了。解决办法就是用hive的`jline-2.12.jar`替换hadoop的`jline-0.9.94.jar`。
- 在DataGrip中增加hive的驱动：
    - 将hive安装目录lib目录下的如下jar包复制到DataGrip的jdbc驱动文件夹下新建的`Hive`目录中（`C:\Users\gaol\.DataGrip2017.2\config\jdbc-drivers\Hive`）：
        ```
        commons-collections-3.2.2.jar
        commons-logging-1.1.3.jar
        hadoop-common-2.7.2.jar
        hive-cli.jar
        hive-common.jar
        hive-exec.jar
        hive-jdbc.jar
        hive-metastore.jar
        hive-service.jar
        httpclient-4.2.5.jar
        httpcore-4.2.5.jar
        libfb303-0.9.3.jar
        log4j-1.2.16.jar
        log4j-api-2.6.2.jar
        log4j-core-2.6.2.jar
        slf4j-api-1.7.5.jar
        slf4j-log4j12-1.7.5.jar
        ```
    - 在DataGrip中新建`Driver`并命名为`Hive`，添加上述这些jar包到`Additional`
    - 选择`Class`：`org.apache.hive.jdbc.HiveDriver`；选择`Dialect`：`SQL92`
    - 设置URL templates：
        ```
        Name        Template
        default     jdbc:hive2://{host}:{port}/{database}[;<;,{:identifier}={:param}>]
        ```
    - 新建DataSources：
        ```
        host：192.168.157.100   port：10000
        Database：hadoop    
        user：root  # 若没有启用任何hive认证，则user就是hive所在的linux主机用户
        password：  # 无
        ```
    - 在linux上启动hive为服务器模式：
        ```
        /root/apps/hive-1.2.1/bin
        ./hiveserver2
        ```
    - 至此，就可以使用DataGrip连接远程linux上的hive了，但是有一个bug：连上的hive库中的表并没有在DataGrip的视图中显示出来，但查询表中数据是正常显示的。
- hive自带的命令行远程连接工具：beeline（类似DataGrip的功能，只不过是命令行的）
    ```
    cd /root/apps/hive-1.2.1/bin
    ./beeline
    ```  
    进入beeline命令提示符后连接hive：
    ```
    Beeline version 1.2.1 by Apache Hive
    beeline> !connect jdbc:hive2://localhost:10000 # 连接hive的命令
    Connecting to jdbc:hive2://localhost:10000
    Enter username for jdbc:hive2://localhost:10000: root # 用户名、密码可以在“hive-site.xml”中配置，默认用户是启动hive的用户
    Enter password for jdbc:hive2://localhost:10000:      # 无，直接回车即可     
    Connected to: Apache Hive (version 1.2.1)
    Driver: Hive JDBC (version 1.2.1)
    Transaction isolation: TRANSACTION_REPEATABLE_READ
    ```  
    或者在hive的bin目录下新建一个如下shell脚本（`startbeeline.sh`），在启动beeline的同时就连接上：
    ```
    #!/bin/bash
    ./beeline -u jdbc:hive2://localhost:10000 -n root
    ```
### 2、hive架构
- 传统业务数据库和数据仓库的区别
    - 传统业务数据库：支持在线联机业务，但是可容纳的数据量有限（数据量大时查询速度很慢）。建表遵循三范式。
    - 数据仓库：按**主题**将传统业务数据库中的历史数据定期增量式存储，主要用作离线数据分析、报表统计。数据仓库表是**宽表**，建表模型有星型模型、雪花模型等。
- hive是什么？
    - hive是利用hadoop实现数据仓库的一个工具，**可以将==结构化的数据文件==映射为一张数据库表，并提供类SQL查询功能。**
    - hive工作机制：  
    ![image](https://wx3.sinaimg.cn/mw690/006CX93ply1fwyqtd6469j30s80c1jx3.jpg)
    - hive库、表本质上就是hdfs上的目录！
    - hive并不会做数据检查，能解释的它就解释，不能解释的就置NULL，多了的数据就丢弃。
### 3、hive基本操作
- hive建表操作：
    - 创建hive内部表
        ```
        create table if not exists t_mytable(sid string,sname string)
        row format delimited fields terminated by '\005'
        stored as textfile;
        ```
    - 创建hive外部表
        ```
        create external table if not exists t_ext_mytable(sid string,sname string)
          row format delimited fields terminated by '\t'
          stored as textfile
          location '/root/hadoop/hive/external/';
        ```  
        **注意：** 内部表中和外部表的区别：  
        1. hive外部表的location指的是外部表并没有存储在hive指定的hdfs目录下（例如：`/user/hive/warehouse/hadoop.db`），而是自定义的hdfs目录。
        2. drop内部表时会将元信息和hdfs上的文件数据一并删除，而drop外部表时仅会删除mysql中的元数据信息，不会删除hdfs上的文件数据。
    - 创建带分区的表
        ```
        create table if not exists t_part_mytable(sid string,sname string)
        partitioned by(country string)
        row format delimited fields terminated by ','
        stored as textfile;
        ```
    - 创建分桶表
        - 分桶表的创建  
            `students.txt`文件内容：
            ```
            1000,23,star
            1001,22,bob
            1002,26,Allen
            1003,20,smith
            1004,20,james
            1005,23,martin
            1006,24,Gary
            1007,25,William
            1008,27,Joseph
            1009,21,bob
            ```  
            创建一个普通表存储`students.txt`中的数据：  
            ```
            create table if not exists t_students(id int,age int,name string)
            row format delimited fields terminated by ','
            stored as textfile;
            
            load data local inpath '/root/hadoop_testfiles/students.txt' into table t_students;
            ```
            创建分桶表：  
            ```
            create table t_bkt_students(id int,age int,name string)
            clustered by (id)   -- 按照id字段分桶
            sorted by (age)    -- 桶内按照age字段排序
            into 4 buckets      -- 分成4个桶
            row format delimited fields terminated by ',';
            ```  
            开启分桶模式和reducer数量：
            ```
            set hive.enforce.bucketing = true; -- 开启分桶模式
            set mapreduce.job.reduces=4;       -- 分桶数目=reducer数目
            ```  
            准备分桶数量的数据插入到分桶表中：（**分桶表中的数据是从其它表查询出来，分桶、排序后（此时的select跑的是mr程序）插入到分桶表的，不是load进去的**）
            ```
            insert into table t_bkt_students
            select * from t_students distribute by (id) sort by (age); -- 将t_students表中的数据按id字段分区，每个分区中按age字段排序
                                                                       -- 之所以将t_students中的数据分成4个区，是因为设置了set mapreduce.job.reduces=4;
            ```  
            **注意：** 如果分桶字段和排序字段是同一个时，`select`中可用`cluster by(id)`来替代`distribute by(id) sort by(id)`。
        - *分桶表相关：*
            - `order by`会对输入做全局排序，因此只有一个reducer，会导致当输入规模较大时，需要较长的计算时间；`sort by`不是全局排序，其在数据进入reducer前完成排序。因此，如果用`sort by`进行排序，并且设置`mapred.reduce.tasks>1`，则`sort by`只保证每个reducer的输出有序，不保证全局有序。
            - `distribute by`根据`distribute by`指定的内容将数据分到不同的reducer。
            - `cluster by` 除了具有`distribute by`的功能外，还会对该字段进行排序。因此，常常认为`cluster by = distribute by + sort by`。
        - 分桶表的作用：提高join操作的效率  
        ```
        select a.id,a.name,b.addr from a join b on a.id = b.id;
        ```  
        如果a表和b表都是分桶表，而且分桶的字段是id字段。**做这个join操作时，是不需要做全表笛卡尔积的**。
        - 分桶表和分区表的区别  
            - 分桶表是将表中的数据真真切切的在hdfs上分成了几个文件（桶）；表的分区是将几个分区的文件放在了不同的文件夹下，分区字段是一个伪字段。
            - 对分区中的数据又可以分桶。
- hive删除表、清空表
    ```
    drop table 表名;
    truncate table 表名;
    ```
- 向hive表中导入数据（load方式分为overwrite（覆盖）或into（追加）方式）
    - 普通导入
        - `load data local inpath '/root/hadoop_testfiles/test_ext.csv' into table t_ext_mytable;`是将linux本地的格式化数据文件导入hive表
        - `load data inpath '/root/hadoop/hive/external/test.txt' into table t_hive;`是将hdfs上的格式化数据文件导入hive表
    - 导入时指定分区：  
        ```
        load data local inpath '/root/hadoop_testfiles/student_p1.data' into table t_part_mytable partition(country='CN'); # 前5条数据
        load data local inpath '/root/hadoop_testfiles/student_p2.data' into table t_part_mytable partition(country='UK'); # 后3条数据
        ```  
        结果：  
           
        ```
                                                            # 伪字段
        +---------------------+-----------------------+-------------------------+--+
        | t_part_mytable.sid  | t_part_mytable.sname  | t_part_mytable.country  |
        +---------------------+-----------------------+-------------------------+--+
        | stu2001             | Allen                 | CN                      |
        | stu2003             | marry                 | CN                      |
        | stu2006             | bob                   | CN                      |
        | stu2007             | mark                  | CN                      |
        | stu2010             | stiven                | CN                      |
        | stu1003             | pink                  | UK                      |
        | stu3008             | yellow                | UK                      |
        | stu8890             | black                 | UK                      |
        +---------------------+-----------------------+-------------------------+--+
        ```  
        **注意：** 这个伪字段只是用`select * from t_part_mytable;`查询出来时的显示，实际上在hdfs上的文件数据中并没有该字段。
    - insert语句：
        - 特点：
            - 与mysql的`insert`语句不同，hive的`insert`是将查询结果插入到（into）hive新表或覆盖到（overwrite）hive已有表中。
            - hive的`insert`不仅可以插入查询结果到hive表，还可以插入到本地（linux）文件或hdfs上的文件中。
        - 示例：
            - 将查询结果保存到一张新的hive表中：
            ```
            create table t_tmp as 
            select * from exer_student;
            ```
            - 将查询结果保存到一张已存在的hive表中：
            ```
            -- into是追加，overwrite是覆盖
            insert into/overwrite table t_tmp
            select * from exer_student;
            ```
            - 将查询结果保存到指定的文件目录（可以是linux本地目录，也可以是hdfs上的目录）
            ```
            -- 插入到linux本地目录
            insert overwrite local directory '/root/hadoop_testfiles/hive_output'
            select * from exer_student;
            ```  
            数据写入到文件系统时进行文本序列化，且每列用`^A`来分割，`\n`为换行符。用more命令查看时不容易看出分割符，可以使用：   
            `sed -e 's/\x01/|/g' filename`来查看。
            ```
            -- 插入到hdfs上的目录
            insert overwrite directory '/root/hadoop/hive/hive_output'
            select * from exer_student;
            ```
- hive表分区的增删改查
    - 增加分区：
        ```
        alter table t_part_mytable add partition (country='US') partition(country='AU');
        ```  
        **注意：** 给hive中的表新建分区后，要导入该分区下的数据，只能将数据文件上传到hdfs上（没法用`load`命令）：
        ```
        hadoop fs -put student_p3.data /user/hive/warehouse/hadoop.db/t_part_mytable/country=US
        ```
    - 删除分区：
        ```
        alter table t_part_mytable drop partition (country='US');
        ```  
        **注意：** 删除分区会将分区中的数据也删除！
    - 查看分区：
        ```
        show partitions t_part_mytable;
        ```
- hive中的inner join（join）、left join、right join、full outer join、left semi join
    - join：
        - 语法：`a join b on a.字段 = b.字段`
        - 实质：在求a表和b表的交集
    - left join：
        - 语法：`a left join b on a.字段 = b.字段`
        - 实质：a是数据的主控方，a中的记录全显示，b和a能关联上的就显示，关联不上的就置NULL
    - right join：
        - 语法：`a right join b on a.字段 = b.字段`
        - 实质：b是数据的主控方，b中的记录全显示，a和b能关联上的就显示，关联不上的就置NULL
    - full outer join：
        - 语法：`a full outer join b on a.字段 = b.字段`
        - 实质：相当于left join + right join，a是left join的数据主控方，b是right join的数据主控方
    - left semi join（效率要高于inner join）
        - 语法：`a left semi join b on a.字段 = b.字段`
        - 实质：a是数据的主控方，显示b和a关联上的a的那部分
- hive在0.13版本开始已经支持in和not in子查询（但是必须给主查询语句中要查询的表起个别名）。
    - 示例：  
    ```
    select a.id,a.name from t_join_a a where a.id in (select b.id from t_join_b b);
    ```  
    或  
    ```
    select * from t_join_a a left semi join t_join_b b on a.id = b.id;
    ```
### 4、hive函数
- hive内置函数：`E:\hadoop视频\day11\day11\hive常用函数参考手册.docx`
- 快速测试hive内置函数：
    ```
    -- 在hive中建一个dual表
    create table dual (content string);
    -- 将一个“只有一个空格的”文件（dual.data）load到dual表
    load data local inpath '/root/hadoop_testfiles/dual.data' into table dual;
    -- 例如，测试函数substr()
    select substr('hadoop',1,4) from dual;
    ```  
    测试结果如下：
    ```
    +-------+--+
    |  _c0  |
    +-------+--+
    | hado  |
    +-------+--+
    ```
- hive中自定义函数：
    - 先开发一个java类，继承UDF，并重载evaluate()方法。例如：
    ```
    package com.nwnu.hadoop.hive.udf;

    import org.apache.hadoop.hive.ql.exec.UDF;
    
    public class ToLowerCaseUdf extends UDF {
    	//重载evaluate()方法，该方法必须是public的
    	public String evaluate(String field) {
    		String result = field.toLowerCase();
    		return result;
    	}
    }
    ```
    - 将该java类打成jar包上传到服务器，将此jar包添加到hive的calsspath下：
    ```
    0: jdbc:hive2://localhost:10000> add JAR /root/hadoop_jars/toLowerCaseUdf.jar;
    ```
    - 创建临时函数与开发好的java class关联：
    ```
    0: jdbc:hive2://localhost:10000> create temporary function tolowercaseudf as 'com.nwnu.hadoop.hive.udf.ToLowerCaseUdf';
    ```
    - 测试UDF：
    ```
    0: jdbc:hive2://localhost:10000> select tolowercaseudf('AsDFGhbf') from dual;
    +-----------+--+
    |    _c0    |
    +-----------+--+
    | asdfghbf  |
    +-----------+--+
    1 row selected (0.371 seconds)
    ```