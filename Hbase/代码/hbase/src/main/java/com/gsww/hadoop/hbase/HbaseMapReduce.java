package com.gsww.hadoop.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gaol
 * @Date: 2019/1/8 9:41
 * @Description
 */
public class HbaseMapReduce {
    /**
     * Hadoop和Hbase配置
     */
    static Configuration conf = null;
    static Connection connection = null;
    static Table table = null;
    static {
        //要么在windows系统配置“HADOOP_HOME”环境变量后重启电脑，要么做如下设置
        System.setProperty("hadoop.home.dir", "D:\\JAVA\\IdeaProjects\\hadoop\\win7-hadoop\\hadoop-2.6.0");

        // 指定日志文件位置，必须使用绝对路径，可以直接使用hadoop配置文件中的log4j.properties，也可单独建立
        PropertyConfigurator.configure("D:\\JAVA\\IdeaProjects\\hadoop\\hbase\\src\\main\\resources\\hadoop\\log4j.properties");

        conf = new Configuration();
        //指定mr程序分布式运行在yarn上
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

        //Hbase集群设置
//        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum","utility,master,worker1,worker2,worker3");
        conf.set("hbase.zookeeper.property.clientPort","2181");

        conf.set("mapreduce.job.jar", "D:\\JAVA\\IdeaProjects\\hadoop\\hbase\\target\\hbase-1.0-SNAPSHOT.jar");
    }

    /**
     * Hbase用户表信息
     */
    public static final String inputTableName = "word_test";
    public static final String colf = "content";
    public static final String col = "info";
    public static final String outputTableName = "wordcount_test";

    /**
     * TableMapper<Text, IntWritable>
     *     Text：输出的key类型
     *     IntWritable：输出的value类型
     */
    static class HbaseMapper extends TableMapper<Text, IntWritable> {
        @Override
        /**
         * map(ImmutableBytesWritable key, Result value, Context context)
         *      输入类型：key ——rowkey   value ——Hbase表中一条rowkey记录Result
         *
         */
        protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
            Text word = new Text();
            IntWritable one = new IntWritable(1);

            //获取一个cell中的数据
            String cell = Bytes.toString(value.getValue(Bytes.toBytes(colf),Bytes.toBytes(col)));
            //将一个cell中的数据按空格分割
            String[] words = cell.split(" ");
            //遍历一个cell中的单词，做Map统计
            for (String w : words) {
                word.set(w);
                context.write(word, one);
            }
        }
    }

    /**
     * TableReducer<Text, IntWritable, ImmutableBytesWritable>
     *     Text：输入的key类型
     *     IntWritable：输入的value类型
     *     ImmutableBytesWritable：输出的key类型——rowkey类型
     */
    static class HbaseReducer extends TableReducer<Text, IntWritable, ImmutableBytesWritable> {
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            //Reduce聚合
            int count = 0;

            for (IntWritable value : values) {
                count += value.get();
            }

            //创建数据封装类，设置单词为rowkey
            Put put = new Put(Bytes.toBytes(key.toString()));
            //封装要put到指定rowkey的数据
            put.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col),Bytes.toBytes(count));
            //将Reducer聚合结果写到Hbase，实际上调用了“table.put()”方法
            context.write(new ImmutableBytesWritable(Bytes.toBytes(key.toString())), put);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //创建Hbase表并添加数据，MapReduce结果输出表
        initTable();
        //创建MapReduce job任务
        Job job = Job.getInstance(conf, "HbaseMapReduce");
        job.setJarByClass(HbaseMapReduce.class);

//        job.setJar("D:\\JAVA\\IdeaProjects\\hadoop\\hbase\\out\\artifacts\\hbase_jar\\hbase.jar");
        //创建全表扫描封装类
        Scan scan = new Scan();
        //指定查询获取Hbase表中某一列的数据
        scan.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col));

        //参数列表：要查询获取数据的表名、查询方式（可加过滤器）、Mapper类、Mapper的输出key、Mapper的输出value、Job
        TableMapReduceUtil.initTableMapperJob(inputTableName, scan, HbaseMapper.class,Text.class, IntWritable.class, job);
        //参数列表：要写MapReduce结果数据的表名、Reducer类、Job
        TableMapReduceUtil.initTableReducerJob(outputTableName, HbaseReducer.class, job);

        System.exit(job.waitForCompletion(true)?0:1);
    }

    public static void initTable() {
        /**
         * 创建Hbase表的管理类
         */
        HBaseAdmin hBaseAdmin = null;
        try {
            hBaseAdmin = new HBaseAdmin(conf);

            /**
             * 如果表已存在就删除
             */
            if (hBaseAdmin.tableExists(inputTableName) || hBaseAdmin.tableExists(outputTableName)) {
                hBaseAdmin.disableTable(inputTableName);
                hBaseAdmin.deleteTable(inputTableName);
                hBaseAdmin.disableTable(outputTableName);
                hBaseAdmin.deleteTable(outputTableName);
            }

            /**
             * Hbase用户表创建：“word_test”表和“wordcount_test”表
             */
            //创建表的描述类
            HTableDescriptor inputTableDesc = new HTableDescriptor(inputTableName);
            //创建列族的描述类
            HColumnDescriptor inputTableFamily = new HColumnDescriptor(colf);
            //将列族添加到表中
            inputTableDesc.addFamily(inputTableFamily);
            //创建表
            hBaseAdmin.createTable(inputTableDesc);

            HTableDescriptor outputTableDesc = new HTableDescriptor(outputTableName);
            HColumnDescriptor outputTableFamily = new HColumnDescriptor(colf);
            outputTableDesc.addFamily(outputTableFamily);
            hBaseAdmin.createTable(outputTableDesc);

            /**
             * 向“word_test”表中一次性插入多条数据
             */
            //通过工厂类创建1个连接（ConnectionFactory为连接池模式）
            connection = ConnectionFactory.createConnection(conf);
            //获得表的连接。“user”为表名
            table = connection.getTable(TableName.valueOf(inputTableName));

            List<Put> putList = new ArrayList<Put>();
            //创建数据封装类。Put()的参数就是rowkey，类型为字节数组
            Put put1 = new Put(Bytes.toBytes("w1"));
            put1.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col),Bytes.toBytes("The Apache Hadoop software library is a framework"));

            Put put2 = new Put(Bytes.toBytes("w2"));
            put2.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col),Bytes.toBytes("The common utilities that support the other Hadoop modules"));

            Put put3 = new Put(Bytes.toBytes("w3"));
            put3.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col),Bytes.toBytes("Hadoop by reading the documentation.The Apache Hadoop project develops open-source software for reliable, scalable, distributed computing."));

            Put put4 = new Put(Bytes.toBytes("w4"));
            put4.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col),Bytes.toBytes("Hadoop from the release page.Apache Hadoop 2.9.2 is a point release in the 2.x.y release line, building upon the previous stable release 2.9.1."));

            Put put5 = new Put(Bytes.toBytes("w5"));
            put5.addColumn(Bytes.toBytes(colf),Bytes.toBytes(col),Bytes.toBytes("Hadoop on the mailing list.spark mapreduce"));

            //添加数据
            putList.add(put1);
            putList.add(put2);
            putList.add(put3);
            putList.add(put4);
            putList.add(put5);
            table.put(putList);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
