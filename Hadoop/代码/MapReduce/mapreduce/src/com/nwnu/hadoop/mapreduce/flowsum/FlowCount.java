package com.nwnu.hadoop.mapreduce.flowsum;

import lombok.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * @author gaola
 * @Date: 2018/10/24 15:03
 * @Description
 */
public class FlowCount {
    static class FlowCountMapper extends Mapper<LongWritable,Text,Text,FlowBean> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            //将一行文本内容转换为String
            String line = value.toString();
            //切分字段
            String[] fields = line.split("\t");
            //取出手机号
            String phoneNumber = fields[1];


            //取出上行流量和下行流量
            long upFlow = Long.parseLong(fields[fields.length - 3]);
            long downFlow = Long.parseLong(fields[fields.length - 2]);

            //map task输出的数据必须是能够被序列化的
            context.write(new Text(phoneNumber),new FlowBean(upFlow,downFlow));
        }
    }

    static class FlowCountReducer extends Reducer<Text,FlowBean,Text,FlowBean> {
        @Override
        protected void reduce(Text phoneNum, Iterable<FlowBean> beans, Context context) throws IOException, InterruptedException {
            long upFlowSum = 0;
            long downFlowSum = 0;

            //遍历所有的bean，将同一手机号的上行流量、下行流量分别累加
            for (FlowBean bean : beans) {
                upFlowSum += bean.getUpFlow();
                downFlowSum += bean.getDownFlow();
            }
            context.write(new Text(phoneNum),new FlowBean(upFlowSum,downFlowSum));
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        /**
         * 此时conf中虽然有一些关于hdfs配置的默认参数，但是windows7上的这个java客户端仍然找不到hdfs集群在哪里的！
         * 所以在运行该jar包时指定了：“hdfs://hadoopmaster:9000/wordcount/input/flow.log hdfs://hadoopmaster:9000/wordcount/output_flowcount2”，
         * 或者debug时，配置了Program arguments：“hdfs://hadoopmaster:9000/wordcount/input/flow.log hdfs://hadoopmaster:9000/wordcount/output_flowcount2”
         */
        Configuration conf = new Configuration();

        //yarnd的服务器地址没有配置，所以job默认是提交到windows7的yarn模拟器的

        //new Configuration()，job里至少有一些默认参数
        Job job = Job.getInstance(conf);

        //指定本程序的jar所在本地路径。jar包中的main()方法一启动，就可以知道当前这个jar包所在的路径，然后将该jar提交给yarn
        //当然也可以写死，例如：job.setJar("/root/hadoop/hadoop.jars/wordcount.jar");
        job.setJarByClass(FlowCount.class);

        //指定本业务job要使用的Mapper/Reducer业务类
        job.setMapperClass(FlowCountMapper.class);
        job.setReducerClass(FlowCountReducer.class);

        //指定OutputCollector调用我们自定义的Partitioner
        job.setPartitionerClass(ProvincePartitioner.class);
        //指定reduce task的数目
        /**
         * 说明：
         *  ProvincePartitioner中的getPartition()方法定义的分区是6个，如果setNumReduceTasks()中定义的reduce task
         *  数目（设为x）是1个，则所有的<phoneNum,FlowBean>都会被写到1个文件中；
         *  若2=<x<6，则会报错：Error: java.io.IOException: Illegal partition for xxxxxxx；
         *  若x>6，则结果文件part-0000x会生成x-6个空文件。
         */
        job.setNumReduceTasks(6);

        //指定Mapper输出数据的k/v类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);

        //指定Reducer最终输出数据的k/v类型
        job.setOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);

        //指定job的原始数据输入目录
        FileInputFormat.setInputPaths(job,new Path(args[0]));
        //指定job的输出结果目录
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        //job.submit()
        boolean res = job.waitForCompletion(true);
        System.exit(res?0:1);
    }
}
