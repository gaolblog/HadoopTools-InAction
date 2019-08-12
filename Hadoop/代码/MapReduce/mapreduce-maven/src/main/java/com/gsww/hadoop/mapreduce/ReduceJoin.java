package com.gsww.hadoop.mapreduce;

import com.gsww.hadoop.mapreduce.FlowSumSort.FlowSumSortMapper;
import com.gsww.hadoop.mapreduce.mrbean.BusinessBean;
import com.gsww.hadoop.mapreduce.mrbean.FlowBean;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gaol
 * @Date: 2019/2/12 9:51
 * @Description
 */
public class ReduceJoin {

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
        conf.set("yarn.resourcemanager.hostname","master");

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

    static class ReduceJoinMapper extends Mapper<LongWritable, Text, Text, BusinessBean> {

        BusinessBean businessBean = new BusinessBean();
        Text kOut = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            /**
             * 根据读到的切片获取切片所在的文件信息
             */
            //InputSplit强转为FileSplit
            FileSplit inputSplit = (FileSplit)context.getInputSplit();
            String fileName = inputSplit.getPath().getName();

            //首先将读到的一行文本内容转换为字符串
            String line = value.toString();
            //按文本中的间隔符切割字符串为数组
            String[] fields = line.split("\t");
            //product_id：订单表和产品表的关联字段
            String pid = "";

            if (fileName.contains("order")) {
                pid = fields[2];
                //设值
                businessBean.setBusinessBean(fields[0],fields[1], pid,
                        "", 0f,"", Integer.parseInt(fields[3]),"0");
            } else {
                pid = fields[0];
                //设值
                businessBean.setBusinessBean("","",pid,fields[1],
                        Float.parseFloat(fields[3]),fields[2],0,"1");
            }

            kOut.set(pid);
            context.write(kOut, businessBean);
        }
    }

    static class ReduceJoinReducer extends Reducer<Text, BusinessBean, BusinessBean, NullWritable> {

        BusinessBean productBean = new BusinessBean();

//        BusinessBean orderBean = new BusinessBean();

        /**
         * reduce方法处理的是同一产品的所有订单信息
         * @param pid
         * @param businessBeans
         * @param context
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        protected void reduce(Text pid, Iterable<BusinessBean> businessBeans, Context context) throws IOException, InterruptedException {

            List<BusinessBean> orderBeanList = new ArrayList<>();

            for (BusinessBean bean : businessBeans) {
                //if条件为真表示Reducer端获取到的是产品bean，否则就是订单bean
                if (bean.getFlag().equals("1")) {
                    try {
                        /**
                         * 每次虽是给同一对象设值，每调一次reduce方法是同一段内存的不同值写入到文件的
                         */
                        //属性copy
                        BeanUtils.copyProperties(productBean, bean);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } else {
//                    orderBean每次reduce都得新new出来[实际上orderBean设为成员变量时也可以出结果，不理解为什么？]
                    BusinessBean orderBean = new BusinessBean();
                    try {
                        //属性copy
                        BeanUtils.copyProperties(orderBean, bean);
                        orderBeanList.add(orderBean);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }

            /**
             * 拼接关联
             */
            for (BusinessBean orderBean : orderBeanList) {
                //给每一条订单信息bean搭上对应的商品信息
                orderBean.setProduct_name(productBean.getProduct_name());
                orderBean.setCategory_id(productBean.getCategory_id());
                orderBean.setProduct_price(productBean.getProduct_price());

                context.write(orderBean, NullWritable.get());
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //创建业务Job，Job里至少有一些默认参数
        Job job = Job.getInstance(conf);

        //1-1、指定本业务job要使用的Mapper/Reducer业务类
        job.setMapperClass(ReduceJoinMapper.class);
        job.setReducerClass(ReduceJoinReducer.class);

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
        job.setMapOutputValueClass(BusinessBean.class);

        //1-3、指定（Reducer）最终输出数据的k/v类型
        job.setOutputKeyClass(BusinessBean.class);
        job.setOutputValueClass(NullWritable.class);

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
