### 1、hadoop基本概念
- 什么是hadoop？（做什么用的）
- hadoop的来源
- hadoop生态圈
### 2、企业大数据处理的基本流程
- 网站点击流日志数据挖掘系统
- 推荐系统架构
### 3、hadoop集群搭建
- hadoop集群：hdfs集群 + yarn集群
    - hdfs集群：一个name node + 多个data node；yarn集群：一个resource manager + 多个data manager
    - name node和resource manager在企业生产环境中是独立部署在不同机器上的，但在实验环境下两者可部署在同一台机器上
    - hdfs和yarn集群并非都要部署，例如只部署hdfs集群只有文件存储功能
- hadoop官网资源
- 搭建hadoop集群：
    - 搭建之前的准备工作：
        - 集群机器网络配置
        - 为所有集群机器创建hadoop用户：（这里实验使用root账户）
            ```
            useradd hadoop
            passwd hadoop #hadoop用户的密码设为hadoop
            ```
        - 为hadoop用户配置sudo权限，这样hadoop用户就可以执行root命令了
        - 配置主机之间的ssh免密登录
        - 关闭集群机器的防火墙
    - hadoop集群搭建：
        - 下载所有apache开源软件历史版本的网址：http://archive.apache.org/
        - `hadoop-2.6.4.tar.gz`包下的目录结构：
            - bin：hadoop操作命令
            - etc：配置文件
            - include：C语言本地库的头文件
            - lib：本地库
            - libexec：
            - sbin：系统启动管理命令
            - share：jar包
        - hadoop集群配置：在一台主节点机器上进入到`/root/apps/hadoop-2.6.4/etc/hadoop`目录下
            - hadoop运行时环境变量配置（java）：
                ```
                vim hadoop-env.sh
                # The java implementation to use.
                export JAVA_HOME=/root/apps/jdk1.8.0_181
                ```
            - hadoop运行时参数配置（启动hadoop集群的最低要求配置）：需要配置`core-site.xml`、`hdfs-site.xml`、`mapred-site.xml.template`和`yarn-site.xml`4个文件中的参数
                - 配置`core-site.xml`：
                    ```
                    <!-- 指定hadoop所使用的文件系统的schema（URI），HDFS的老大（NameNode）的地址 -->
            		<property>
            			<name>fs.defaultFS</name>
            			<value>hdfs://hadoopmaster:9000</value>
            		</property>
            		
            		<!-- 指定hadoop运行时产生文件的存储目录 -->
            		<property>
            			<name>hadoop.tmp.dir</name>
            			<value>/root/hadoop/hadoop-2.4.1/tmp</value>
            		</property>
                    ```
                - 配置`hdfs-site.xml`：
                    ```
                    <!-- 指定HDFS存储副本的数量，默认为3 -->
            		<property>
            			<name>dfs.replication</name>
            			<value>2</value>
            		</property>
            		
            		<!-- 指定第二个NameNode的端口，第一个的是50070 -->
            		<property>
            			<name>dfs.secondary.http.address</name>
            			<value>hadoopmaster:50090</value>
            		</property>
                    ```
                - 配置`mapred-site.xml.template`，先把该文件改名：`mv mapred-site.xml.template mapred-site.xml`：
                    ```
                    <!-- 指定mr运行在yarn上，默认是运行在local上，而非分布式运行 -->
            		<property>
            			<name>mapreduce.framework.name</name>
            			<value>yarn</value>
            		</property>
                    ```
                - 配置`yarn-site.xml`：
                    ```
                    <!-- 指定YARN的老大（ResourceManager）的地址 -->
            		<property>
            			<name>yarn.resourcemanager.hostname</name>
            			<value>hadoopmaster</value>
            		</property>
            		
            		<!-- reducer获取数据的方式 -->
            		<property>
            			<name>yarn.nodemanager.aux-services</name>
            			<value>mapreduce_shuffle</value>
            		</property>
                    ```
        - 将配置好的`/root/apps/`目录下的`hadoop-2.6.4`目录复制到其它集群机器上（同时其它机器上也要有`/root/hadoop/hadoop-2.4.1/tmp`目录）：
            ```
            scp -r hadoop-2.6.4/ hadoop01:/root/apps
            scp -r hadoop-2.6.4/ hadoop02:/root/apps
            ```
        - 将hadoop添加到linux系统环境变量（所有的机器都配置）：
            ```
            vim /etc/profile
            # 在“/etc/profile”文件中添加以下两行内容：
            export HADOOP_HOME=/root/apps/hadoop-2.6.4
            export PATH=$PATH:$ZOOKEEPER_HOME:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
            # 使所有配置的机器都生效
            source /etc/profile 
            ```
        - 格式化namenode：
            - 在namenode节点上输入命令：`hdfs namenode -format`（**注意：该命令只是初始化了name node的工作目录，而data node的工作目录是在data node启动后自己初始化的**）
                - 所以，再次格式化name node时（**重新格式化意味着集群的数据会被全部删除，格式化前需考虑数据备份或转移问题**），data node持有的还是name node被格式化之前的身份，所以需要手动删除所有data node以及name node上的工作目录，才可以重新组建hdfs集群。
                - [Hadoop namenode重新格式化需注意问题](https://blog.csdn.net/gis_101/article/details/52821946)
            - 格式化hdfs成功的标志：输出日志信息`INFO common.Storage: Storage directory /root/hadoop/hadoop-2.4.1/tmp/dfs/name has been successfully formatted.`
        - 利用`/root/apps/hadoop-2.6.4/sbin`目录下提供的shell脚本启动hadoop集群：
            - 利用脚本启动的前提是在`/root/apps/hadoop-2.6.4/etc/hadoop`目录下的`slaves`文件中添加从节点的主机名：
                ```
                # 默认是localhost
                hadoop01
                hadoop02
                ```
            - hdfs集群启动：`start-dfs.sh`；yarn集群启动：`start-yarn.sh`
                - [hadoop三种启动方式](https://blog.csdn.net/jiao_zg/article/details/70763445)
                    - 每个守护线程逐一启动：`hadoop-daemon.sh start datanode`；`yarn-daemon.sh start nodemanager`
                - [HDFS集群启动过程详解](https://blog.csdn.net/amber_amber/article/details/38268407)
            - 验证启动是否成功：每台机器中输入命令`jps`
            - 关闭集群：`stop-all.sh`
        - HDFS和MapReduce的Web管理界面：
            - HDFS：http://192.168.157.99:50070
            - MapReduce：http://192.168.157.99:8088
### 4、hadoop集群相关问题
- hdfs动态扩容/下线
- hdfs文件副本：如果hdfs集群中只有一个data node，那么无论配置的xml中的副本数量是多少，此文件都只有一份；当配置的副本数>机器数时，hdfs集群实际存放的文件副本数就是机器数。这个配置的副本数只有在机器数>=配置的副本数时才起作用。
- 其它问题参看：“E:\hadoop视频\day07\day07\day06的问题总结.txt”
### 5、hadoop与云计算平台的区别
