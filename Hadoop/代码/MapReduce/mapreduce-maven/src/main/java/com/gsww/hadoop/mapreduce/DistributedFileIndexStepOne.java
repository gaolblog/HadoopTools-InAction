package com.gsww.hadoop.mapreduce;

import com.gsww.hadoop.mapreduce.MapJoin.MapJoinMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author gaol
 * @Date: 2019/2/15 14:34
 * @Description
 */
public class DistributedFileIndexStepOne {
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
        //伪分布式文件系统
        conf.set("fs.defaultFS","hdfs://localhost:9000");
        //配置文件系统为本地文件系统
//        conf.set("fs.defaultFS","file:///");

        //指定MapReduce程序分布式/伪分布式运行在yarn上
        conf.set("mapreduce.framework.name","yarn");
//        conf.set("mapreduce.framework.name","local");

        //指定yarn的resource manager的地址
//        conf.set("yarn.resourcemanager.hostname","master");

        // 添加Hadoop配置文件
        /*conf.addResource("hadoop/hdfs-site.xml");
        conf.addResource("hadoop/core-site.xml");
        conf.addResource("hadoop/mapred-site.xml");
        conf.addResource("hadoop/yarn-site.xml");*/

        // 添加伪分布式配置文件
        conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/hdfs-site.xml");
        conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/core-site.xml");
        conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/mapred-site.xml");
        conf.addResource("D:/JAVA/IdeaProjects/hadoop/win7-hadoop/hadoop-2.6.0/etc/hadoop/yarn-site.xml");

        // 如果要从windows系统中运行这个job提交客户端的程序，则需要加这个跨平台提交的参数。
        // 在windows下如果没有这句代码会报错 "/bin/bash: line 0: fg: no job control"，去网上搜答案很多都说是linux和windows环境不同导致的一般都是修改YarnRunner.java，但是其实添加了这行代码就可以了。
//        conf.set("mapreduce.app-submission.cross-platform","true");

        /**
         * 以下两句代码解决伪分布式运行时异常：
         * org.apache.hadoop.yarn.exceptions.InvalidResourceRequestException:
         *      Invalid resource request, requested memory < 0, or requested memory > max configured, requestedMemory=-1, maxMemory=8192
         */
        conf.set("mapreduce.map.memory.mb","1024");
        conf.set("mapreduce.reduce.memory.mb","1024");

        //指定jar包位置
        conf.set("mapreduce.job.jar", "D:/JAVA/IdeaProjects/hadoop/mapreduce-maven/out/artifacts/mapreduce_maven_jar/mapreduce-maven.jar");
    }

    static class DistributedFileIndexStepOneMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        Text kOut = new Text();
        IntWritable vOut = new IntWritable(1);
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            //将一行文本内容转换为字符串
            String line = value.toString();
            //文本字符串按空格切割为字符串数组
            String[] words = line.split(" ");
            //获取文本所在的文件信息
            FileSplit inputSplit = (FileSplit) context.getInputSplit();
            String fileName = inputSplit.getPath().getName();

            for (String word : words) {
                //给Mapper输出的键设值
                kOut.set(word + "-->" + fileName);
                context.write(kOut,vOut);

                /**
                 * <hello-->a.txt,1>
                 *
                 * hello    a.txt-->3,b.txt-->2,c.txt-->2
                 */
            }
        }
    }

    static class DistributedFileIndexStepOneReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        IntWritable vOut = new IntWritable();
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int count = 0;
            for (IntWritable value : values) {
                count += value.get();
            }

            //给Reducer的输出值设值
            vOut.set(count);
            context.write(key,vOut);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, URISyntaxException {
        //创建业务Job，Job里至少有一些默认参数
        Job job = Job.getInstance(conf);

        //1-1、指定本业务job要使用的Mapper/Reducer业务类
        job.setMapperClass(DistributedFileIndexStepOneMapper.class);
        job.setReducerClass(DistributedFileIndexStepOneReducer.class);

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
//        job.setMapOutputKeyClass(Text.class);
//        job.setMapOutputValueClass(NullWritable.class);

        //1-3、指定（Reducer）最终输出数据的k/v类型（若Mapper和Reducer的输出类型一样，只写以下两句即可，可以不必写setMapOutputKeyClass和setMapOutputValueClass）
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        //1-4、指定Reduce Task数量（如果指定了自定义分区，则Reduce Task的数量应>=分区数量；Reduce Task数量为1，则会忽略分区）
//        job.setNumReduceTasks(6);
//        job.setNumReduceTasks(0);
        job.setNumReduceTasks(1);


        //2-1、指定job的原始数据输入目录
        FileInputFormat.setInputPaths(job,new Path(args[0]));
        //2-2、指定job的输出结果目录
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        //3、指定自定义的数据分区器
//        job.setPartitionerClass(ProvincePartitioner.class);

        /**
         * 缓存一个文件到所有的Map Task运行节点的工作目录
         */
        //1).缓存jar包到所有的Map Task运行节点的classpath中
//        job.addArchiveToClassPath(archive);
        //2).缓存普通文件到所有的Map Task运行节点的classpath中
//        job.addFileToClassPath(file);
        //3).缓存压缩文件到所有的Map Task运行节点的工作目录
//        job.addCacheArchive(uri);
        //4).缓存普通文件到所有的Map Task运行节点的工作目录
        /**
         * Diagnostics: File file:/D:/JAVA/IdeaProjects/hadoop-file/mapreduce/mapjoin/t_product.txt
         * java.io.FileNotFoundException: File file:/D:/JAVA/IdeaProjects/hadoop-file/mapreduce/mapjoin/t_product.txt does not exist
         * "file:/D:/JAVA/IdeaProjects……" 是将job提交到本地MapReduce程序运行模拟器时才能够加载到指定文件的情况。若文件路径指定的是job客户端本地（windows），
         * 而提交job是到Yarn集群运行，那么就会报以上“xxx does not exist”的异常；
         * 若文件路径指定的是集群上的节点，由于不知道DistributedCache组件在哪台机器上，所以就需要给集群上的每台机器都指定相同的文件所在路径及文件。
         */
//        job.addCacheFile(new URI("file:/D:/JAVA/IdeaProjects/hadoop-file/mapreduce/mapjoin/cache/t_product.txt"));
//        job.addCacheFile(new URI("hdfs:///tmp/gaol/reducejoin/input/t_product.txt"));
//        job.addCacheFile(new URI("hdfs:///hadoop/mapreduce/mapjoin/cache/t_product.txt"));


        //5、判断HDFS上output文件夹是否存在，如果存在则删除
        Path path = new Path(args[1]);// 取第1个表示输出目录参数（第0个参数是输入目录）
        FileSystem fs = path.getFileSystem(conf);// 根据path找到这个文件
        if (fs.exists(path)) {
            fs.delete(path, true);// true的意思是，就算output有东西，也一带删除
        }

        //6、job.submit()：和yarn通信，然后将当前所在路径的jar包、以及WordCountDriver类中main()方法定义的相关参数xml文件（job.xml）赋值给yarn去运行。
        //但是如果用job.submit()方法，客户端这边程序运行结束，但并不知道集群上jar包程序运行的情况，所以使用job.waitForCompletion(true)阻塞住，
        // 一直等待集群的返回结果，然后自行退出（其实客户端程序退出与否都无所谓，只要提交到yarn了，集群上就运行程序了）
        boolean res = job.waitForCompletion(true);
        System.exit(res?0:1);
    }
}
