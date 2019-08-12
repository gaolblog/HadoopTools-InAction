package com.gsww.hadoop.mapreduce;

import com.gsww.hadoop.mapreduce.WordCount.WordCountMapper;
import com.gsww.hadoop.mapreduce.WordCount.WordCountReducer;
import com.gsww.hadoop.mapreduce.mrbean.FlowBean;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author gaol
 * @Date: 2019/1/15 14:35
 * @Description
 */
public class FlowSum {
    /**
     * CDH Hadoop集群配置
     */
    static Configuration conf;
    static {
        //要么在windows系统配置“HADOOP_HOME”环境变量后重启电脑，要么做如下设置
//        System.setProperty("hadoop.home.dir", "D:\\JAVA\\IdeaProjects\\hadoop\\win7-hadoop\\hadoop-2.6.0");

        // 指定日志文件位置，必须使用绝对路径，可以直接使用hadoop配置文件中的log4j.properties，也可单独建立
        PropertyConfigurator.configure("D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/src/main/resources/hadoop/log4j.properties");

        conf = new Configuration();

        //配置文件系统为HDFS
//        conf.set("fs.defaultFS","hdfs://master:8020");
        //指定MapReduce程序分布式运行在yarn上
        conf.set("mapreduce.framework.name","yarn");
        //指定yarn的resource manager的地址
        conf.set("yarn.resourcemanager.hostname","utility");

        // 添加Hadoop配置文件
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

    /**
     * MapReduce的新API中，Partitioner是abstract class，而不是interface
     */
    static class ProvincePartitioner extends Partitioner<Text, FlowBean> {
        /**
         * 手机号码归属地按省份分区字典
         */
        static Map<String,Integer> provinceDict = new HashMap<String,Integer>();
        static {
            provinceDict.put("135",0);
            provinceDict.put("136",1);
            provinceDict.put("137",2);
            provinceDict.put("138",3);
            provinceDict.put("139",4);
        }

        /**
         * 对每一个手机号码的<Text,FlowBean>都需要调用一次getPartition方法，所以分区字典表应提前加载到内存中，然后去访问；
         * 如果访问外部字典（例如数据库中的），那么大数据量的手机统计信息则对于数据库的压力就会较大，速度也很慢
         * @param phoneNumber
         * @param flowBean
         * @param numPartitions
         * @return  provinceId：整型的分区号
         */
        @Override
        public int getPartition(Text phoneNumber, FlowBean flowBean, int numPartitions) {
            String phoneNumberPrefix = phoneNumber.toString().substring(0,3);
            Integer provinceId = provinceDict.get(phoneNumberPrefix);
            return provinceId == null?5:provinceId;
        }
    }

    static class FlowSumMapper extends Mapper<LongWritable, Text, Text, FlowBean> {
        Text phoneNum = new Text();
        FlowBean flowBean = new FlowBean();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            //1、将一行文本内容转换为字符串
            String line = value.toString();
            //2、按结构化数据字段间的分隔符切分字符串为字符串数组
            String[] flowArr = line.split("\t");
            //3、设置Map Task的输出key-value
            phoneNum.set(flowArr[1]);
            flowBean.setFlows(Long.parseLong(flowArr[flowArr.length-3]),Long.parseLong(flowArr[flowArr.length-2]));

            /**
             * 每调用一次map方法是把同一个对象的每次set的不同值序列化到了【文件】，
             * 所以reduce端每次拿到的是同一个对象的不同值，然后再反序列成一个对象，获取对象中的值
             */
            context.write(phoneNum, flowBean);
        }
    }

    static class FlowSumReducer extends Reducer<Text, FlowBean, Text, FlowBean> {
        FlowBean flowBean = new FlowBean();

        @Override
        protected void reduce(Text key, Iterable<FlowBean> values, Context context) throws IOException, InterruptedException {
            //1、reduce()方法对key相同的一组key-value对做聚合
            long upFlows = 0;
            long downFlows = 0;
            for (FlowBean fb : values) {
                upFlows += fb.getUpFlow();
                downFlows += fb.getDownFlow();
            }
            //2、设置Reduce Task的输出的key-value
            flowBean.setFlows(upFlows, downFlows);

            context.write(key, flowBean);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //创建业务Job，Job里至少有一些默认参数
        Job job = Job.getInstance(conf);

        //1-1、指定本业务job要使用的Mapper/Reducer业务类
        job.setMapperClass(FlowSumMapper.class);
        job.setReducerClass(FlowSumReducer.class);

        /**
         * 3-3、指定combiner组件。
         * 但是combiner并不是随便可以使用的，需要看业务逻辑。如果使用不当，就会使得业务逻辑改变。
         * 注意：由于Combiner和Reducer做的实际上是同一件事情（只是两者面向的对象不同），所以直接将“CombinerClass”设置为“Reducer.class”是可以的
         */
        job.setCombinerClass(FlowSumReducer.class);
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
        job.setMapOutputValueClass(FlowBean.class);

        //1-3、指定（Reducer）最终输出数据的k/v类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        //1-4、指定Reduce Task数量（如果指定了自定义分区，则Reduce Task的数量应>=分区数量；Reduce Task数量为1，则会忽略分区）
//        job.setNumReduceTasks(6);
        job.setNumReduceTasks(1);

        //2-1、指定job的原始数据输入目录
        FileInputFormat.setInputPaths(job,new Path(args[0]));
        //2-2、指定job的输出结果目录
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        //3-1、指定自定义的数据分区器
//        job.setPartitionerClass(ProvincePartitioner.class);

        //3-2、Mapper输出键值对按键排序

        //4、判断HDFS上output文件夹是否存在，如果存在则删除
        Path path = new Path(args[1]);// 取第1个表示输出目录参数（第0个参数是输入目录）
        FileSystem fs = path.getFileSystem(conf);// 根据path找到这个文件
        if (fs.exists(path)) {
            fs.delete(path, true);// true的意思是，就算output有东西，也一带删除
        }

        //5、job.submit()：和yarn通信，然后将当前所在路径的jar包、以及WordCountDriver类中main()方法定义的相关参数xml文件（job.xml）赋值给yarn去运行。
        //但是如果用job.submit()方法，客户端这边程序运行结束，但并不知道集群上jar包程序运行的情况，所以使用job.waitForCompletion(true)阻塞住，
        // 一直等待集群的返回结果，然后自行退出（其实客户端程序退出与否都无所谓，只要提交到yarn了，集群上就运行程序了）
        boolean res = job.waitForCompletion(true);
        System.exit(res?0:1);
    }
}
