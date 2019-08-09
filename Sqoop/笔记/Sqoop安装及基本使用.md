### 1、什么是sqoop
- 概念：sqoop是Apache的一款**Hadoop和关系数据库服务器之间传送数据**的数据迁移工具。
    - import：从structured datastores导入到HDFS、Hive或Hbase等
    - export：从Hadoop导出到structured datastores
- 工作原理：sqoop数据迁移实际上是将导入导出命令转换为MapReduce程序执行，主要是对MapReduce中InputFormat和OutputFormat的定制。
### 2、sqoop安装
- 前提条件：已配置好java环境，搭建好hadoop环境
- 官网下载sqoop：http://www.apache.org/dyn/closer.lua/sqoop/1.4.7
- 如果要在hive和关系型数据库之间导数据的话，sqoop要安装在有hive目录的节点上
- 修改sqoop配置文件：
    ```
    cd /root/apps/sqoop-1.4.7/conf
    mv sqoop-env-template.sh sqoop-env.sh
    vim sqoop-env.sh    // 修改如下内容
        #Set path to where bin/hadoop is available
        export HADOOP_COMMON_HOME=/root/apps/hadoop-2.6.4
     
        #Set path to where hadoop-*-core.jar is available
        export HADOOP_MAPRED_HOME=/root/apps/hadoop-2.6.4
     
        #set the path to where bin/hbase is available
        export HBASE_HOME=/root/apps/hbase-1.2.5
     
        #Set the path to where bin/hive is available
        export HIVE_HOME=/root/apps/hive-1.2.1
    ```
- 在sqoop的lib目录下加入mysql的驱动包：
    ```
    cp  $HIVE_HOME/lib/mysql-connector-java-5.1.28.jar   $SQOOP_HOME/lib/
    ```
- sqoop启动验证：
    ```
    cd /root/apps/sqoop-1.4.7/bin
    ./sqoop-version
    ```
### 3、数据导入
- mysql数据库表导入HDFS
    - 导入默认目录：`./sqoop-import --connect jdbc:mysql://localhost:3306/test --username root --password root --table emp --m 1`（默认是导入到了HDFS的`/user/root/表名`目录下）
    - 导入指定目录：`./sqoop-import --connect jdbc:mysql://localhost:3306/test --username root --password root --table emp --target-dir /user/sqoop_import/emp_target --m 1`
- mysql数据库表导入Hive
    - 命令：`./sqoop-import --connect jdbc:mysql://hadoop01:3306/test --username root --password root --table emp --hive-import --m 1`  
        `--m 1`表示运行1个Map Task（**sqoop中只有Map Task，没有Reduce task**）。
    - 执行以上语句若报错：
        ```
        ERROR manager.SqlManager: Error executing statement: java.sql.SQLException: Access denied for user 'root'@'hadoop01' (using password: YES)
        java.sql.SQLException: Access denied for user 'root'@'hadoop01' (using password: YES)
        
    	ERROR tool.ImportTool: Import failed: java.io.IOException: No columns to generate for ClassWriter
        ```  
        两步解决：
        - 优先使用：`./sqoop-import --connect jdbc:mysql://localhost:3306/test --username root --password root --table emp --hive-import --m 1`命令重新导入，若继续报错则尝试第二步
        - 登录mysql客户端，使用如下命令：
            ```
            GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION; --允许其他用户远程免密登录本地mysql
            flush privileges;--使生效
            ```
- 按需导入
    - sqoop导入命令中添加`where`条件进行筛选：
        ```
        ./sqoop-import --connect jdbc:mysql://localhost:3306/test --username root --password root --table emp --where 'mgr = 7698' --target-dir /user/sqoop_import/emp_target_where --m 1
        ```
    - sqoop导入命令中使用select语句查询导入：
        ```
        ./sqoop-import --connect jdbc:mysql://localhost:3306/test --username root --password root --query 'select * from emp where empno = 7839  and $CONDITIONS' --split-by empno --fields-terminated-by '\t' --target-dir /user/sqoop_import/emp_target_select --m 1
        ```  
        - `--query 'select * from emp where empno = 7839  and $CONDITIONS'`中的`$CONDITIONS`**必须在查询导入时加入到SQL语句中的where子句中**。`--query`是作用在关系数据库进行查询的，所以多复杂都可以。
        - `--split-by 关系数据库字段`只有在Map Task数目>=2个时才有作用，意思是按关系数据库字段将要导入的数据划分给多个Map Task执行。
        - `--fields-terminated-by 分隔符`可以指定导入到HDFS上的文件中的数据字段之间的分隔符。
    - 增量导入：
        ```
        ./sqoop-import --connect jdbc:mysql://localhost:3306/test --username root --password root --table emp --incremental append --check-column empno --last-value 7782 --target-dir /user/sqoop_import/emp_target_increment --m 1
        ```
        - `-check-column 关系数据库字段`表示以关系数据库中的哪个字段作为增量导入的参考字段
        - `--last-value 字段值`表示上一次增量导入结束时的最后一条记录的参考字段值，这一次导入就从这个值的往后一条记录开始导入
### 4、数据导出
- HDFS（Hive）数据导出到Mysql
    - 使用sqoop导出数据到关系型数据库时，必须先在关系数据库建好表
        ```
        create table if not exists t_students(id int,age int,name varchar(10));
        ```
    - sqoop导出命令：`./sqoop-export --connect jdbc:mysql://localhost:3306/test --username root --password root --table t_students --export-dir /user/hive/warehouse/hadoop.db/t_students/ --m 1`
        - 导出报错：
            ```
            19/01/11 10:04:21 WARN util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
            19/01/11 10:04:21 INFO Configuration.deprecation: mapred.jar is deprecated. Instead, use mapreduce.job.jar
            19/01/11 10:04:25 INFO Configuration.deprecation: mapred.reduce.tasks.speculative.execution is deprecated. Instead, use mapreduce.reduce.speculative
            19/01/11 10:04:25 INFO Configuration.deprecation: mapred.map.tasks.speculative.execution is deprecated. Instead, use mapreduce.map.speculative
            19/01/11 10:04:25 INFO Configuration.deprecation: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
            19/01/11 10:04:25 INFO client.RMProxy: Connecting to ResourceManager at hadoopmaster/192.168.157.99:8032
            19/01/11 10:04:41 INFO input.FileInputFormat: Total input paths to process : 1
            19/01/11 10:04:41 INFO input.FileInputFormat: Total input paths to process : 1
            19/01/11 10:04:41 INFO mapreduce.JobSubmitter: number of splits:4
            19/01/11 10:04:41 INFO Configuration.deprecation: mapred.map.tasks.speculative.execution is deprecated. Instead, use mapreduce.map.speculative
            19/01/11 10:04:42 INFO mapreduce.JobSubmitter: Submitting tokens for job: job_1547161230208_0007
            19/01/11 10:04:44 INFO impl.YarnClientImpl: Submitted application application_1547161230208_0007
            19/01/11 10:04:44 INFO mapreduce.Job: The url to track the job: http://hadoopmaster:8088/proxy/application_1547161230208_0007/
            19/01/11 10:04:44 INFO mapreduce.Job: Running job: job_1547161230208_0007
            19/01/11 10:05:14 INFO mapreduce.Job: Job job_1547161230208_0007 running in uber mode : false
            19/01/11 10:05:14 INFO mapreduce.Job:  map 0% reduce 0%
            19/01/11 10:06:32 INFO mapreduce.Job:  map 75% reduce 0%
            19/01/11 10:06:33 INFO mapreduce.Job:  map 100% reduce 0%
            19/01/11 10:06:36 INFO mapreduce.Job: Job job_1547161230208_0007 failed with state FAILED due to: Task failed task_1547161230208_0007_m_000003
            Job failed as tasks failed. failedMaps:1 failedReduces:0
            
            19/01/11 10:06:36 INFO mapreduce.Job: Counters: 12
            	Job Counters 
            		Failed map tasks=1
            		Killed map tasks=3
            		Launched map tasks=4
            		Data-local map tasks=4
            		Total time spent by all maps in occupied slots (ms)=292964
            		Total time spent by all reduces in occupied slots (ms)=0
            		Total time spent by all map tasks (ms)=292964
            		Total vcore-milliseconds taken by all map tasks=292964
            		Total megabyte-milliseconds taken by all map tasks=299995136
            	Map-Reduce Framework
            		CPU time spent (ms)=0
            		Physical memory (bytes) snapshot=0
            		Virtual memory (bytes) snapshot=0
            19/01/11 10:06:36 WARN mapreduce.Counters: Group FileSystemCounters is deprecated. Use org.apache.hadoop.mapreduce.FileSystemCounter instead
            19/01/11 10:06:36 INFO mapreduce.ExportJobBase: Transferred 0 bytes in 131.4267 seconds (0 bytes/sec)
            19/01/11 10:06:36 INFO mapreduce.ExportJobBase: Exported 0 records.
            19/01/11 10:06:36 ERROR mapreduce.ExportJobBase: Export job failed!
            19/01/11 10:06:36 ERROR tool.ExportTool: Error during export: 
            Export job failed!
            	at org.apache.sqoop.mapreduce.ExportJobBase.runExport(ExportJobBase.java:445)
            	at org.apache.sqoop.manager.SqlManager.exportTable(SqlManager.java:931)
            	at org.apache.sqoop.tool.ExportTool.exportTable(ExportTool.java:80)
            	at org.apache.sqoop.tool.ExportTool.run(ExportTool.java:99)
            	at org.apache.sqoop.Sqoop.run(Sqoop.java:147)
            	at org.apache.hadoop.util.ToolRunner.run(ToolRunner.java:70)
            	at org.apache.sqoop.Sqoop.runSqoop(Sqoop.java:183)
            	at org.apache.sqoop.Sqoop.runTool(Sqoop.java:234)
            	at org.apache.sqoop.Sqoop.runTool(Sqoop.java:243)
            	at org.apache.sqoop.Sqoop.main(Sqoop.java:252)

            ```
        - 由于导出命令中指定了Map Task的数量为1，所以先找到是哪个NodeManager在运行这个Map Task，然后在`/root/apps/hadoop-2.6.4/logs/userlogs`目录下按Map Task的运行时间找到最新运行的日志目录（例如`application_1547177505875_0002`），在该目录的下一级目录中找到`syslog`日志文件，查看日志内容如下：
            ```
            The last packet sent successfully to the server was 0 milliseconds ago. The driver has not received any packets from the server.
            	at org.apache.sqoop.mapreduce.ExportOutputFormat.getRecordWriter(ExportOutputFormat.java:79)
            	at org.apache.hadoop.mapred.MapTask$NewDirectOutputCollector.<init>(MapTask.java:644)
            	at org.apache.hadoop.mapred.MapTask.runNewMapper(MapTask.java:764)
            	at org.apache.hadoop.mapred.MapTask.run(MapTask.java:341)
            	at org.apache.hadoop.mapred.YarnChild$2.run(YarnChild.java:163)
            	at java.security.AccessController.doPrivileged(Native Method)
            	at javax.security.auth.Subject.doAs(Subject.java:422)
            	at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1656)
            	at org.apache.hadoop.mapred.YarnChild.main(YarnChild.java:158)
            Caused by: com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure
            ```  
            网上说是*数据库服务等待连接超时的问题*，解决方案是：修改mysql的`my.cnf`（位于`/etc/my.cnf`）文件内容：
            ```
            [mysqld]
            wait_timeout = 86400    //由默认的28800改大点。默认设置查看命令：“mysql> show global variables like 'wait_timeout';”
            ```  
            **然后直接重启服务器！** （重启mysql服务，仍然不能导出数据）