package com.nwnu.hadoop.mapreduce.wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.CombineTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/10/22 21:39
 * @Description WordCountDriver-main相当于一个yarn的客户端。用来配置任务处理的相关参数、jar包，最后提交给yarn。
 */
//启动类。主要就是要构造出job对象，然后将其形成job.xml文件、jar包提交给yarn
public class WordCountDriver {
    //首先启动main程序，然后将Mapper、Reducer程序（jar包）、参数描述（封装）成一个任务（job）提交给yarn
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        /**
         * Configuration作为Hadoop的一个基础功能承担着重要的责任，
         * 为Yarn、HDFS、MapReduce、NFS、调度器等提供参数的配置、配置文件的分布式传输(实现了Writable接口)等重要功能
         */
        Configuration conf = new Configuration();
//        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
//        conf.set("fs.hdfs.impl",org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());

        /**
         * 如下两行配置了mapreduce程序在本地运行（直接不配置“conf.set("yarn.resourcemanager.hostname",hostname)”），实际上不配置这两行，默认也是在本地跑程序的：
         * “mapred-default.xml”文件默认参数就是配置在本地：[mapreduce.framework.name:local]
         * “core-default.xml”：[fs.defaultFS:file:///]
         */
        //配置在本地yarn模拟器运行程序
//        conf.set("mapreduce.framework.name","local");
        //配置文件系统为本地“file:///”
//        conf.set("fs.defaultFS","file:///");

        //如下这样，配置的文件系统是hdfs，但是数据输入/输出路径配置的参数却是windows上的，所以报异常：not a valid DFS filename
        /**
         * Exception in thread "main" java.lang.IllegalArgumentException:
         * Pathname /D:/JAVA/IdeaProjects/hadoop/win7-hadoop/mapreduce-InOutput/output/wordcount from
         * hdfs://hadoopmaster:9000/D:/JAVA/IdeaProjects/hadoop/win7-hadoop/mapreduce-InOutput/output/wordcount is not a valid DFS filename.
         */
        conf.set("fs.defaultFS","hdfs://hadoopmaster:9000");

        /**
         * 以下两句代码是指定客户端通信的yarn的地址，但是在linux上运行该程序的jar时，由于在hadoop集群中配置了yarn的地址，所以会自动找到
         * */
        //指定mr运行在yarn上，默认是运行在local上，而非分布式运行
        conf.set("mapreduce.framework.name","yarn");
        //指定yarn的老大（ResourceManager）的地址
        conf.set("yarn.resourcemanager.hostname","hadoopmaster");

        //new Configuration()，job里至少有一些默认参数
        Job job = Job.getInstance(conf);

        //4、指定本程序的jar所在本地路径。
        // jar包中的main()方法一启动，就可以知道当前这个jar包所在的路径，然后将该jar提交给yarn
        //当然也可以写死，例如：job.setJar("/root/hadoop/hadoop.jars/wordcount.jar");但这样jar包就必须放在指定的这个目录下
//        job.setJarByClass(WordCountDriver.class); //会自动获得jar包所在的本地路径
        job.setJar("D:/JAVA/IdeaProjects/hadoop/mapreduce/out/artifacts/mapreduce_jar/eclipse_runnable_wordcount_win_test_sout.jar");

        //1-1、指定本业务job要使用的Mapper/Reducer业务类
        job.setMapperClass(WordCountMapper.class);
        job.setReducerClass(WordCountReducer.class);

        //指定combiner。但是combiner并不是随便可以使用的，需要看业务逻辑。如果使用不当，就会使得业务逻辑改变。
        //或job.setCombinerClass(WordCountReducer.class)
//        job.setCombinerClass(WordCountCombiner.class);

        /** 指定InputFormat的实现类，默认是TextInputFormat
         *  CombineTextInputFormat是将多个小文件合并到一个切片中，并不是合并成文件
         */
//        job.setInputFormatClass(CombineTextInputFormat.class);
        //设置合并成的切片的最大size为4M
//        CombineTextInputFormat.setMaxInputSplitSize(job,4194304);
        //设置合并成的切片的最小size为2M
//        CombineTextInputFormat.setMinInputSplitSize(job,2097152);

        //1-2、指定Mapper输出数据的k/v类型，防止MapReduce框架自动利用第三方工具进行序列化
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        //1-3、指定（Reducer）最终输出数据的k/v类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        //2-1、指定job的原始数据输入目录
        FileInputFormat.setInputPaths(job,new Path(args[0]));
        //2-2、指定job的输出结果目录
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        //5、job.submit()：和yarn通信，然后将当前所在路径的jar包、以及WordCountDriver类中main()方法定义的相关参数xml文件（job.xml）赋值给yarn去运行。
        //但是如果用job.submit()方法，客户端这边程序运行结束，但并不知道集群上jar包程序运行的情况，所以使用job.waitForCompletion(true)阻塞住，
        // 一直等待集群的返回结果，然后自行退出（其实客户端程序退出与否都无所谓，只要提交到yarn了，集群上就运行程序了）
        boolean res = job.waitForCompletion(true);
        System.exit(res?0:1);
    }
}
