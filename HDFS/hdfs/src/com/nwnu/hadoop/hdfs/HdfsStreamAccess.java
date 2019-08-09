package com.nwnu.hadoop.hdfs;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author gaol
 * @Date: 2018/10/21 19:40
 * @Description 用流的方式操作hdfs上指定偏移量范围的数据
 */
public class HdfsStreamAccess {
    FileSystem fs = null;
    Configuration conf = null;

    @Before
    public void init() throws IOException, URISyntaxException, InterruptedException {
        //注意：关于Block Size、Replication、默认文件系统等参数的设置，不仅可以在客户端由conf对象来设置，也可以在hdfs集群中的xml文件中配置
        conf = new Configuration();

        /**
         * 第二种方法：直接给hdfs api传入用户身份参数
         */
        conf.set("fs.defaultFS","hdfs://hadoopmaster:9000");

        //参数优先级： 1)、客户端代码中设置的值 2)、classpath下的用户自定义配置文件 3)、然后是服务器的默认配置
        conf.set("dfs.replication","3");
        //获取到了一个操作文件系统的客户端实例对象
        fs = FileSystem.get(new URI("hdfs://hadoopmaster:9000"),conf,"root");

    }

    @Test
    public void uploadFileByStream() throws IOException {
        FSDataOutputStream fsDataOutputStream = fs.create(new Path("/win-Client/english-passage1"), true);
        FileInputStream fileInputStream = new FileInputStream("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs_files/English-Signs Your Children Are Ready to Use Technology.docx");

        IOUtils.copy(fileInputStream,fsDataOutputStream);
    }

    @Test
    public void downloadFileByStream() throws IOException {
        FSDataInputStream fsDataInputStream = fs.open(new Path("/win-Client/hdfs.iml.bak"));
        FileOutputStream fileOutputStream = new FileOutputStream("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs_files/hdfs.iml");

        IOUtils.copy(fsDataInputStream,fileOutputStream);
    }

    @Test
    public void randomAccessFileContextByStream() throws IOException {
        FSDataInputStream fsDataInputStream = fs.open(new Path("/win-Client/english-passage1"));
//        fsDataInputStream.seek(6);
        FileOutputStream fileOutputStream = new FileOutputStream("D:/JAVA/IdeaProjects/hadoop/hdfs/hdfs_files/english-passage1.part");

        //从文件中偏移量为6的位置开始读取36个字节的内容
        IOUtils.copyLarge(fsDataInputStream,fileOutputStream,6,36);
    }

    @Test
    public void catFileContextByStream() throws IOException {
        FSDataInputStream fsDataInputStream = fs.open(new Path("/win-Client/english-passage1"));

        //通过FSDataInputStream流的方式将文件内容打印到控制台
        IOUtils.copy(fsDataInputStream,System.out);
    }
}
