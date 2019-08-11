- linux系统安装
- linux常用命令
- linux软件安装（yum）
- linux网络配置
- shell脚本

### 1、linux系统安装
#### 1、linux安装过程注意事项
- 要进行哪种类型的安装？——使用所有空间：整个硬盘都给当前的系统用，这种方案有一个默认分区。勾选上[查看并修改分区布局(V)]
    - 硬盘标识：第一块硬盘“sda”、第二块硬盘“sdb”、第三块硬盘“sdc”……
    - 分区标识：第一个分区“sda1”（boot分区）、第二个分区“sda2”（物理分区，将物理分区再进行逻辑分区，可动态调整逻辑分区大小）
    - 格式化：将分区表写入磁盘
- 创建一个系统普通用户：root是默认管理员用户
#### 2、linux目录结构
- linux中，有一个“/boot”分区，其余硬盘、U盘、光驱等都挂载在“/”下某个文件夹下。
- “/”下目录作用：
    - bin：命令，普通的功能可执行的程序
    - sbin：系统管理程序
    - boot：系统启动引导相关文件。例如“/boot/grub/”下为启动菜单相关文件。
    - dev：硬件资源
    - etc：系统或程序的配置文件。“/etc/sysconfig/networking-scripts”下配置网卡相关信息；“/etc/sysconfig/network”中可配置主机名
    - home：所有普通用户的目录，root用户目录在“/root”下
    - lib：linux中开源软件的公共库文件
    - media：将光驱、U盘等挂载到该目录下
    - mnt：临时挂载目录
    - usr：系统用户公共软件目录
    - temp：临时数据目录，其中数据可随时删掉
#### 3、linux网络配置
- 局域网工作机制：
    - IP地址
    - 子网掩码：“ip地址 & 子网掩码 = 网段地址”
    - 网关：网络总出口
    - DNS：域名解析服务
        - 1、从本地hosts文件中寻找“域名<->ip地址”映射信息，如果存在相应的映射关系则发出对该ip地址的请求
        - 2、如果本地hosts中没有该域名，则向外部的DNS服务器查询
        - 如果要上外网，则DNS地址写上网关地址即可，网关自己知道该向谁请求域名
- vmware虚拟机linux网络配置：
    - NAT模式：
        - 1、虚拟机网络连接方式选择为NAT（自定义Vmnet8 NAT）
        - 2、在“/etc/sysconfig/network-scripts/ifcfg-eth0”文件中：
            - a.配置ip、子网掩码、网关、外网DNS
                - 网关ip是由vmware虚拟出来的路由器的地址（例如“GATEWAY=192.168.157.2”）
                - 虚拟linux的ip应和网关地址在同一网段（例如“IPADDR=192.168.157.100”），子网掩码：“NETMASK=255.255.255.0”
                - “DNS1=192.168.157.2”
            - b.“BOOTPROTO=static”
            - c.“ONBOOT=yes” 
        - 3、重启网络服务：“service network restart”
        - 4-1、在“/etc/sysconfig/network”文件中修改主机名：`HOSTNAME=hadoop01`
        - 4-2、在“/etc/hosts”文件中配置局域网DNS：
            ```
            192.168.157.99 hadoopmaster
            192.168.157.100 hadoop01
            192.168.157.101 hadoop02
            ```
        - 5、在宿主机windows中配置VMware Network Adapter VMnet8：
            ```
            ip：192.168.157.1
            子网掩码：255.255.255.0
            ```
        - 6、确保宿主机windows中“VMware NAT Service”服务是已启动状态的。
        - 7、出现问题：
            - 主机可ping通虚拟机，但是虚拟机ping不通主机的情况：  
                参考解决方案：点击vmware软件“虚拟网络编辑器-->NAT设置-->域名服务器(DNS)”，查看一下DNS设置了没有，把【自动检测】前面的勾取消，在下面输入主机的DNS。  
            - 虚拟机ping外网时，一直卡主没反应：  
                检查是不是把主机网络连接的共享打开了，取消勾选“允许其他网络用户通过此计算机的Internet连接来连接”。
        - centos服务器同步网络时间命令：`ntpdate cn.pool.ntp.org`
    - 桥接模式：
    - Host-only模式
- linux防火墙配置
    - iptables的结构：iptables中有4张表，分别是filter、nat、mangle和raw，每张表中都包含了各自不同的链。
    - 命令：
        - 查看防火墙状态：`service iptables status`或iptables -L -n --line-numbers
        - 启动/关闭防火墙：`service iptables start/stop`
        - 查看防火墙是否开机启动：`chkconfig iptables --list`
        - 设置防火墙开机启动/不启动：`chkconfig iptables on/off`
        - 开启8080端口：`iptables -I INPUT -p tcp --dport 8080 -j ACCEPT`
### 2、linux常用命令  
参考文档`E:\hadoop视频\day01\课程中所用命令.txt`
### 3、linux下安装软件
- 安装jdk1.8.0_181
    - 在“/root/”目录下执行命令：`mkdir apps`
    - 解压安装包：`tar -zxvf jdk-8u181-linux-i586.tar.gz -C apps/`
    - 修改CentOS6.3环境变量：
        - `vim /etc/profile`
        - 在该文件最后添加：
            ```
            export JAVA_HOME=/root/apps/jdk1.8.0_181
	        export PATH=$PATH:$JAVA_HOME/bin
            ```  
    - 重新加载环境变量：`source /etc/profile`  
    【注意】当命令未加入到linux环境变量中时，要执行命令需要给命令加上一个路径，例如在“/root/apps/jdk1.8.0_181/bin”下执行
	“java -version”时需要：“./java -version”。
- CentOS6.3中RPM方式安装mysql5.7.10
    - 1、查看系统中已安装的mysql组件并卸载：
        ```
        rpm -qa | grep mysql
        rpm -e mysql-libs-5.1.61-4.el6.i686 --nodeps
        ```  
        如果不删除已安装的mysql组件，后续安装时会出现rpm包冲突的问题。
    - 2、安装依赖numactl：
        ```
        yum -y install numactl
        ```
    - 3、在官网“https://downloads.mysql.com/archives/community/” 下载mysql的rpm安装包，然后安装mysql RPM包，安装顺序不能乱（这4个包之间存在依赖关系）：
        ```
        rpm -ivh mysql-community-common-5.7.10-1.el6.i686.rpm
        rpm -ivh mysql-community-libs-5.7.10-1.el6.i686.rpm
        rpm -ivh mysql-community-client-5.7.10-1.el6.i686.rpm
        rpm -ivh mysql-community-server-5.7.10-1.el6.i686.rpm
        ```  
        【注意】：如果将“/usr”单独分了区，安装以上4个包时可能会出现诸如“installing package mysql-community-server-5.7.10-1.el6.i686 needs 58MB on the /usr filesystem”的提示，这种情况是因为“/usr”的存储空间不够了导致的。解决办法是给“/usr”目录扩容（对linux了解不深的话，很难！！），或只能将mysql安装在另一台机器上了。然而“指定安装目录”的做法是行不通的，因为并不是所有RPM包能被重定位的（mysql的rpm安装包就是这样），参考博客：[把RPM包安装到指定的目录](http://joerong666.iteye.com/blog/818994)。
    - 4、配置mysql数据库文件目录：  
        - 删除“/var/lib/mysql/”目录下的所有文件，新建一个“data”文件夹
        - 打开“vim /etc/my.cnf”文件，在“[mysqld]”一节下添加：`datadir=/var/lib/mysql/data`，保存并退出文件  
    
    【以下内容参考博客：[centos下RPM安装mysql5.7.13](https://www.cnblogs.com/xiaodangshan/p/7230111.html)】
    - 5、初始化mysqld服务：  
        为了保证数据库目录与文件的所有者为 mysql 登陆用户，如果你是以 root 身份运行 mysql 服务，需要执行下面的命令初始化：
            ```
            mysqld --initialize --user=mysql
            ```  
        此时查看“/var/lib/mysql/”目录下的data文件夹属性如下：  
            ```
            drwxr-x--x. 5 mysql mysql 4096 8月  29 19:38 data
            ```  
        另外`--initialize`选项默认以“安全”模式来初始化，则会为root用户生成一个密码并将该密码标记为过期，登陆后你需要设置一个新的密码;而使用`--initialize-insecure`命令则不使用安全模式，不会为root用户生成一个密码。使用`--initialize`初始化时会生成一个root账户密码，密码在“vim /var/log/mysqld.log”文件的最后一行：“A temporary password is generated for root@localhost:RrE6:a#*6dtr”，其中“RrE6:a#*6dtr”就是密码。
    - 6、启动mysqld服务：`service mysqld start`
    - 7、使用`mysql -uroot -pRrE6:a#*6dtr`命令登录mysql，然后修改root的密码为“root”：
        
        ```
            msql>alter user 'root'@'localhost' identified by 'root';
    
        　　mysql>use mysql;
        
        　　msyql>update user set user.Host='%' where user.User='root';
        
        　　mysql>flush privileges;
        
        　　mysql>quit
        ```
    - 8、使用`mysql -uroot -proot`重新登录验证：
        ```
        mysql> select version();
        ```
    - 9、下次登录mysql时，需要执行命令`service mysqld start`
### 4、Vmware的使用
- 克隆一台linux：
    - 虚拟机 `$\rightarrow$` 管理 `$\rightarrow$` 克隆 `$\rightarrow$` 创建完整克隆 `$\rightarrow$` 选择克隆机的工作目录
    - 解决克隆后eth0不见的问题：
        - 直接修改“/etc/sysconfig/network-script/ifcfg-eth0”：删掉UUID、HWADDR
        - 配置静态地址
        - 执行命令：`rm -rf /etc/udev/rules.d/70-persistent-net.rules`
        - reboot
### 5、shell脚本
- shell中的变量：分为系统变量、用户自定义变量，可用`set`命令查看当前系统中的所有变量
    - 定义变量：
        - `x=1`，等号两侧不能有空格
        - 双引号/单引号的区别：双引号仅将空格及转义字符脱意，单引号会将所有特殊字符脱意：
            ```
            [root@hadoopmaster ~]# str="hello world\t\n"
            [root@hadoopmaster ~]# echo $str
            hello world\t\n
            [root@hadoopmaster ~]# str1='$str'
            $str
            ```
        - 声明静态变量，不可撤销：`readonly B=2`
        - 声明全局变量：`export JAVA_HOME=/root/apps/jdk1.8.0_181`，全局变量不仅仅是在当前bash起作用，而是在所有的bash中起作用。
    - 撤销变量：`unset str1`
    - 命令的输出结果赋给变量：
        ```
        [root@hadoopmaster ~]# statistics=`ls`
        [root@hadoopmaster ~]# echo $statistics
        anaconda-ks.cfg apps install.log.gz install.log.syslog rootshells softwarepackages
        ```
    - shell内置的特殊变量：
        - `$?`：上一条命令的退出状态。若这个变量的值为0，则表示此命令正确执行；若这个变量的值非0，则表示此命令执行不正确
        - `$0`：当前脚本的名称
        - `$n`：获取第n个位置的脚本输入参数
        - `$*`和`$@`：获取脚本输入参数列表，可用循环遍历取出每个参数
- shell运算符：shell中仅支持整数运算，不支持浮点数运算。shell运算符在编写脚本时并不常用！！
- 流程控制：
    - if判断：
        - `[ condition ]`判断条件中，若condition不为空，则为true；若condition为空，则为false。例如`[  ]`返回1。
        - 判断语句：`[ condition ] && echo OK || echo notok`，条件满足打印“ok”，条件不满足打印“notok”。
        - 常用判断条件：
            - 字符串判断：`=`
            - 整数值判断：`-lt`、`-le`、`-eq`、`-gt`、`-ge`、`-ne`
            - 文件类型判断
    - case语句
    - for循环/while循环
- 自定义函数：
    - 必须在调用函数之前声明函数
    - 函数的返回值只能通过`$?`系统变量获得，可以显式加`return`返回，如果不加，将以最后一条命令的运行结果作为返回值。`return`后跟数值n(0-255)
- 脚本调试
    - `sh -vx shell脚本名称`
    - 或者在脚本中增加`set -x`
### 6、高级文本处理命令
- cut、sort、wc：参考文档`E:\hadoop视频\day02-shell\day02\sort-cut-wc详解.txt`
- sed：参考文档`E:\hadoop视频\day02-shell\day02\sed详解.txt`
- awk：参考文档`E:\hadoop视频\day02-shell\day02\awk详解.txt`

