package com.nwnu.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * @author gaol
 * @Date: 2018/10/13 15:36
 * @Description
 * @mark 客户端去操作hdfs时，是有一个用户身份的。默认情况下，hdfs客户端api会从jvm中获取一个参数来作为自己的用户身份：-DHADOOP_USER_NAME=root
 */
public class HdfsClientDemo {
    FileSystem fs = null;
    Configuration conf = null;

    @Before
    public void init() throws IOException, URISyntaxException, InterruptedException {
        //注意：关于Block Size、Replication、默认文件系统等参数的设置，不仅可以在客户端由conf对象来设置，也可以在hdfs集群中的xml文件中配置
        conf = new Configuration();
        /**
         * 第一种方法：通过设定jvm参数来设置用户身份
         */
        //2、只有将要操作的文件系统设置为hdfs时，才可以去操作hdfs集群
//        conf.set("fs.defaultFS","hdfs://hadoopmaster:9000");
        //FileSystem为抽象类。1、这样就获取到了一个操作文件系统的客户端实例对象，但这样获取到的是本地linux系统的客户端对象
//        fs = FileSystem.get(conf);

        /**
         * 第二种方法：直接给hdfs api传入用户身份参数
         */
        conf.set("fs.defaultFS","hdfs://hadoopmaster:9000");

        //参数优先级： 1)、客户端代码中设置的值 2)、classpath下的用户自定义配置文件 3)、然后是服务器的默认配置
//        conf.set("dfs.replication","3"); //
        //获取到了一个操作文件系统的客户端实例对象
        fs = FileSystem.get(new URI("hdfs://hadoopmaster:9000"),conf,"root");
    }

    @Test
    public void uploadFile() throws IOException, InterruptedException {
//        Thread.sleep(50000);
        fs.copyFromLocalFile(new Path("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs.iml"),new Path("/win*Client/hdfs.iml.bak3"));
        fs.close();
    }

    @Test
    public void downloadFile() throws IOException {
        fs.copyToLocalFile(new Path("/case应用.sh"),new Path("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs_files"));

        //如果本地windows系统没有配支持hdfs的环境，下载文件时将“useRawLocalFileSystem=true”
//        fs.copyToLocalFile(false,new Path("/case应用.sh"),new Path("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs_files"),
//                            true);
        fs.close();
    }

    @Test
    public void getConf() {
        Iterator<Entry<String, String>> itr = conf.iterator();
        while(itr.hasNext()) {
            Entry<String, String> entry = itr.next();
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    @Test
    public void mkdirs() throws IOException {
        System.out.println(fs.mkdirs(new Path("/test_mkdir/aaa/bbb")));
        fs.close();
    }

    @Test
    public void deletedirs() throws IOException {
        //删除文件夹 ，如果是非空文件夹，参数2必须给值true（表示是否递归删除）
        System.out.println(fs.delete(new Path("/test_mkdir/aaa"),true));
        fs.close();
    }

    /**
     * 递归查询hdfs目录
     * @throws IOException
     */
    @Test
    public void ls() throws IOException {
        //此处返回的是一个remote迭代器，而不是java集合，原因就在于hdfs集群上的万亿级的文件信息如果都存储在集合中，那客户端内存就崩了。迭代器是每迭代一次都向服务器取一次
        //是从hdfs的数据池中通过其提供的迭代器取的
        RemoteIterator<LocatedFileStatus> remoteItr = fs.listFiles(new Path("/"), true);
        while (remoteItr.hasNext()) {
            LocatedFileStatus locatedFileStatus = remoteItr.next();
//            if (locatedFileStatus.getPath().equals(new Path("hdfs://hadoopmaster:9000/jdk*8u181*linux*i586.tar.gz"))) {
//                System.out.println(locatedFileStatus.getPermission() + " " + locatedFileStatus.getOwner() + " " + locatedFileStatus.getGroup() + " " +
//                        getPrintSize(locatedFileStatus.getLen()) + "\t" + locatedFileStatus.getReplication() + "\t" +
//                        getPrintSize(locatedFileStatus.getBlockSize()) + "\t" + locatedFileStatus.getPath());
//                continue;
//            }
            System.out.println(locatedFileStatus.getPermission() + " " + locatedFileStatus.getOwner() + " " + locatedFileStatus.getGroup() + " " +
                    getPrintSize(locatedFileStatus.getLen()) + "\t\t" + locatedFileStatus.getReplication() + "\t" +
                    getPrintSize(locatedFileStatus.getBlockSize()) + "\t" + locatedFileStatus.getPath());

            //打印hdfs上一个文件的块相关信息
            BlockLocation[] blockLocations = locatedFileStatus.getBlockLocations();
            for (BlockLocation bl : blockLocations) {
                System.out.println("块拓扑路径：" + Arrays.toString(bl.getTopologyPaths()));
                System.out.println("块偏移量：" + bl.getOffset());
                System.out.println("块大小：" + bl.getLength());
                System.out.println("块所在DataNode：" + Arrays.toString(bl.getHosts()));
                System.out.println("------------------------------------------------------------------------------");
            }
            System.out.println("********************************************************************************************");
        }
        fs.close();
    }

    /**
     * 查询hdfs的一级目录
     * @throws IOException
     */
    @Test
    public void ls2() throws IOException {
        FileStatus[] fileStatusArr = fs.listStatus(new Path("/"));
        for (FileStatus fileStatus : fileStatusArr) {
            System.out.println(fileStatus.getPath().getName() + '\t' + (fileStatus.isFile() ? '*' : 'd'));
        }
        fs.close();
    }

    /**
     * Byte转换为KB、MB、GB
     * 参考CSDN的程序：https://blog.csdn.net/yongh701/article/details/45769547
     * @param size
     * @return
     */
    public  String getPrintSize(long size) {
        //如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }
        //如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        //因为还没有到达要使用另一个单位的时候
        //接下去以此类推
        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "GB";
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Configuration conf = new Configuration();
        //2、只有将要操作的文件系统设置为hdfs时，才可以去操作hdfs集群
        conf.set("fs.defaultFS","hdfs://hadoopmaster:9000");
        //FileSystem为抽象类。1、这样就获取到了一个操作文件系统的客户端实例对象，但这样获取到的是本地linux系统的客户端对象
        FileSystem fs = FileSystem.get(conf);

//        Thread.sleep(50000);
        fs.copyFromLocalFile(new Path("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs.iml"),new Path("/win*Client"));
        fs.close();
    }
}
