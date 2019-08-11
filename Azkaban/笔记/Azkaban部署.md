### 1、azkaban
- 什么是azkaban？
    - 工作流调度器。
- azkaban部署
    - 安装包准备：
        - Azkaban Web服务器：`azkaban-web-server-2.5.0.tar.gz`
        - Azkaban执行服务器：`azkaban-executor-server-2.5.0.tar.gz`
        - Azkaban脚本导入：`azkaban-sql-script-2.5.0.tar.gz`
        - MySQL：目前azkaban只支持mysql，需安装mysql服务器
    - 解压上述3个`*.tar.gz`到新建的`azkaban`文件夹
    - 启动mysqld服务`service mysqld start`，然后用`mysql -uroot -proot`登录到mysql数据库。在mysql中执行azkaban的建表sql脚本：`source /root/azkaban/azkaban-2.5.0/create-all-sql-2.5.0.sql`
    - azkaban的Web页面是需要通过https协议（加密安全协议）来访问的，所以需要在服务端生成https协议证书。使用`keytool -keystore keystore -alias jetty -genkey -keyalg RSA`命令生成证书文件keystore：
        - 密钥库口令此处设为：`azkaban123`
        - `CN=gaol, OU=nwnu, O=nwnu, L=Lanzhou, ST=Gansu, C=CN是否正确?`
        - `[否]`：`y`
        - `<jetty>` 的密钥口令：`azkaban123`
    - 将`keystore`copy到azkaban web服务器根目录中：`cp keystore azkaban-web-2.5.0/`
    - 集群所有机器统一时区：`cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime`
        - 如果`/usr/share/zoneinfo/Asia/Shanghai`时区文件不存在，使用`tzselect`命令生成
    - 集群所有机器统一时间：
        ```
        date -s "2018-11-01 15:01:30"
        hwclock -w //写到硬件
        ```
    - 修改web server的`azkaban.properties`文件：`vim /root/azkaban/azkaban-web-2.5.0/conf/azkaban.properties`，修改内容如下：
        ```
        #Azkaban Personalization Settings
        azkaban.name=AzkabanTest                           #服务器UI名称,用于服务器上方显示的名字
        azkaban.label=My Local Azkaban                               #描述
        azkaban.color=#FF3601                                                 #UI颜色
        azkaban.default.servlet.path=/index                         #
        web.resource.dir=web/                                                 #默认根web目录
        default.timezone.id=Asia/Shanghai                           #默认时区,已改为亚洲/上海 默认为美国
         
        #Azkaban UserManager class
        user.manager.class=azkaban.user.XmlUserManager   #用户权限管理默认类
        user.manager.xml.file=/root/azkaban/azkaban-web-2.5.0/conf/azkaban-users.xml              #用户配置
         
        #Loader for projects
        executor.global.properties=conf/global.properties    # global配置文件所在位置
        azkaban.project.dir=projects                                                #
         
        database.type=mysql                                                              #数据库类型
        mysql.port=3306                                                                       #端口号
        mysql.host=localhost                                                      #数据库连接IP
        mysql.database=azkaban                                                       #数据库实例名
        mysql.user=root                                                                 #数据库用户名
        mysql.password=root                                                          #数据库密码
        mysql.numconnections=100                                                  #最大连接数
         
        # Velocity dev mode
        velocity.dev.mode=false
        # Jetty服务器属性.
        jetty.maxThreads=25                                                               #最大线程数
        jetty.ssl.port=8443                                                                   #Jetty SSL端口
        jetty.port=8081                                                                         #Jetty端口
        jetty.keystore=/root/azkaban/azkaban-web-2.5.0/keystore                                                          #SSL文件名
        jetty.password=azkaban123                                                             #SSL文件密码
        jetty.keypassword=azkaban123                                                      #Jetty主密码 与 keystore文件相同
        jetty.truststore=/root/azkaban/azkaban-web-2.5.0/keystore                                                                #SSL文件名
        jetty.trustpassword=azkaban123                                                   # SSL文件密码
         
        # 执行服务器属性
        executor.port=12321                                                               #执行服务器端口
         
        # 邮件设置
        mail.sender=xxxxxxxx@163.com                                       #发送邮箱
        mail.host=smtp.163.com                                                       #发送邮箱smtp地址
        mail.user=xxxxxxxx                                       #发送邮件时显示的名称
        mail.password=**********                                                 #邮箱密码
        job.failure.email=xxxxxxxx@163.com                              #任务失败时发送邮件的地址
        job.success.email=xxxxxxxx@163.com                            #任务成功时发送邮件的地址
        lockdown.create.projects=false                                           #
        cache.directory=cache                                                            #缓存目录
        ```
    - 修改web server的用户配置文件`azkaban-users.xml`：`vim /root/azkaban/azkaban-web-2.5.0/conf/azkaban-users.xml`，在该文件加入：
        ```
        <user username="admin" password="admin" roles="admin,metrics" />
        ```
    - 修改azkaban执行服务器executor的配置文件：`vim /root/azkaban/azkaban-executor-2.5.0/conf/azkaban.properties`，修改为如下内容：
        ```
        #Azkaban
        default.timezone.id=Asia/Shanghai                                              #时区
         
        # Azkaban JobTypes 插件配置
        azkaban.jobtype.plugin.dir=plugins/jobtypes                   #jobtype 插件所在位置
         
        #Loader for projects
        executor.global.properties=/root/azkaban/azkaban-executor-2.5.0/conf/global.properties
        azkaban.project.dir=projects
         
        #数据库设置
        database.type=mysql                                                                       #数据库类型(目前只支持mysql)
        mysql.port=3306                                                                                #数据库端口号
        mysql.host=localhost                                                           #数据库IP地址
        mysql.database=azkaban                                                                #数据库实例名
        mysql.user=root                                                                       #数据库用户名
        mysql.password=root                                  #数据库密码
        mysql.numconnections=100                                                           #最大连接数
         
        # 执行服务器配置
        executor.maxThreads=50                                                                #最大线程数
        executor.port=12321                                                               #端口号(如修改,请与web服务中一致)
        executor.flow.threads=30                                                                #线程数
        ```
    - 启动azkaban web服务器：
        - 启动azkaban web服务器时报错：
            ```
            Invalid maximum heap size: -Xmx4G
            The specified size exceeds the maximum representable size.
            Could not create the Java virtual machine
            ```  
            解决方法：修改文件`azkaban/azkaban-web-2.5.0/bin/azkaban-web-start.sh`，将文件中的`AZKABAN_OPTS="-Xmx4G"`修改为 `AZKABAN_OPTS="-Xmx512M"`。设置的大小按照机器的存储而定，如果设置太大可能无法启动，设置太小会内存溢出。
        - 启动azkaban web服务器：
            - 启动命令：
                ```
                cd /root/azkaban/azkaban-web-2.5.0
                bin/azkaban-web-start.sh
                ```
            - 启动成功标志：
                ```
                2018/11/01 15:41:31.528 +0800 INFO [AzkabanWebServer] [Azkaban] Server running on ssl port 8443.
                ```
            - 登录azkaban web服务器：
                - 浏览器输入：`https://hadoopmaster:8443/`；用户/密码：`admin/admin`
                - 解决azkaban web界面“丑”的方法：
                    ```
                    vim /root/azkaban/azkaban-web-2.5.0/conf/azkaban.properties
                    修改：web.resource.dir=/root/azkaban/azkaban-web-2.5.0/web/
                    ```
        - 启动azkaban executor：
            - 启动命令：
                ```
                cd /root/azkaban/azkaban-executor-2.5.0
                bin/azkaban-executor-start.sh
                ```
            - 启动报错：
                ```
                [root@hadoopmaster azkaban-executor-2.5.0]# Error occurred during initialization of VM
                Could not reserve enough space for 3145728KB object heap
                ```  
                解决方法：修改`/root/azkaban/azkaban-executor-2.5.0/bin/azkaban-executor-start.sh`，将`AZKABAN_OPTS="-Xmx3G"`修改为 `AZKABAN_OPTS="-Xmx512M"`
            - 启动成功标志：
                ```
                2018/11/01 16:17:30.347 +0800 INFO [AzkabanExecutorServer] [Azkaban] Azkaban Executor Server started on port 12321
                ```
- azkaban实战：
    - azkaban内置的任务类型支持command、java（jar包）
            
