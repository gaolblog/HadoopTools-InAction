package com.gsww.hadoop.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gaol
 * @Date: 2018/12/27 22:17
 * @Description
 */
public class HbaseTest {
    //Configuration实质上就是一个Map
    static Configuration config = null;
    private Connection connection = null;
    private Table table = null;

    /**
     * client连接Hbase集群的配置
     * @throws IOException
     */
    @Before
    public void init() throws IOException {
        config = HBaseConfiguration.create();
        //zookeeper的地址，首先hbase要找到zookeeper，找到之后通过socket通信建立连接
//        config.set("hbase.zookeeper.quorum","hadoopmaster,hadoop01,hadoop02");
        config.set("hbase.zookeeper.quorum","utility,master,worker1,worker2,worker3");
        //zookeeper客户端端口。zookeeper的另外2个端口：数据端口——2888，心跳端口——3888
        config.set("hbase.zookeeper.property.clientPort","2181");

        //通过工厂类创建1个连接（ConnectionFactory为连接池模式）
        connection = ConnectionFactory.createConnection(config);
        //获得表的连接。“user_test”为表名
        table = connection.getTable(TableName.valueOf("user_test"));

    }

    /**
     * 创建Hbase表
     * @throws IOException
     */
    @Test
    public void createTable() throws IOException {
        //创建表的管理类
        HBaseAdmin hBaseAdmin = new HBaseAdmin(config);

        //创建表的描述类
        TableName tableName = TableName.valueOf("user_test"); //表名称
        HTableDescriptor desc = new HTableDescriptor(tableName);

        //创建列族的描述类
        HColumnDescriptor family1 = new HColumnDescriptor("info1"); //列族1
        //将列族添加到表中
        desc.addFamily(family1);
        HColumnDescriptor family2 = new HColumnDescriptor("info2"); //列族2
        desc.addFamily(family2);

        //创建表
        hBaseAdmin.createTable(desc);
    }

    /**
     * 插入一条数据
     * @throws IOException
     */
    @Test
    public void insertData() throws IOException {
        /**
         * 向表中插入数据之前，先获得表连接
         */
        //通过工厂类创建1个连接（ConnectionFactory为连接池模式）
        connection = ConnectionFactory.createConnection(config);
        //获得表的连接。“user”为表名
        table = connection.getTable(TableName.valueOf("user_test"));

        //创建数据封装类。Put()的参数就是rowkey，类型为字节数组
        Put put = new Put(Bytes.toBytes("u2"));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("name"),Bytes.toBytes("Andy"));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("age"),Bytes.toBytes(23));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("sex"),Bytes.toBytes("男"));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("address"),Bytes.toBytes("Canada"));
        put.addColumn(Bytes.toBytes("info2"),Bytes.toBytes("math"),Bytes.toBytes("95"));
        put.addColumn(Bytes.toBytes("info2"),Bytes.toBytes("english"),Bytes.toBytes("89"));

        //添加数据
        table.put(put);
    }

    /**
     * 插入多条数据
     * @throws IOException
     */
    @Test
    public void insertDataSet() throws IOException {
        /**
         * 向表中插入数据之前，先获得表连接
         */
        //通过工厂类创建1个连接（ConnectionFactory为连接池模式）
        connection = ConnectionFactory.createConnection(config);
        //获得表的连接。“user”为表名
        table = connection.getTable(TableName.valueOf("user_test"));

        List<Put> putList = new ArrayList<Put>();
        //创建数据封装类。Put()的参数就是rowkey，类型为字节数组
        Put put = new Put(Bytes.toBytes("u1"));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("name"),Bytes.toBytes("Bob"));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("age"),Bytes.toBytes(26));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("sex"),Bytes.toBytes("男"));
        put.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("address"),Bytes.toBytes("China"));
        put.addColumn(Bytes.toBytes("info2"),Bytes.toBytes("score_math"),Bytes.toBytes("96.5"));
        put.addColumn(Bytes.toBytes("info2"),Bytes.toBytes("score_english"),Bytes.toBytes("88"));

        Put put1 = new Put(Bytes.toBytes("u2"));
        put1.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("name"),Bytes.toBytes("Andy"));
        put1.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("age"),Bytes.toBytes(25));
        put1.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("sex"),Bytes.toBytes("女"));
        put1.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("address"),Bytes.toBytes("American"));
        put1.addColumn(Bytes.toBytes("info2"),Bytes.toBytes("score_english"),Bytes.toBytes("90"));
        //添加数据
        putList.add(put);
        putList.add(put1);
        table.put(putList);
    }

    /**
     * 删除数据
     * 注意：如果加上时间戳，会删除指定时间戳的版本，不加则会删除所有版本。
     * @throws IOException
     */
    @Test
    public void deleteData() throws IOException {
        /**
         * 删除表数据之前，先获得表连接
         */
        //通过工厂类创建1个连接（ConnectionFactory为连接池模式）
        connection = ConnectionFactory.createConnection(config);
        //获得表的连接。“user”为表名
        table = connection.getTable(TableName.valueOf("user_test"));

        //创建删除数据封装类。删除数据时，指定要删除哪个rowkey的数据
        Delete delete = new Delete(Bytes.toBytes("u1"));
        //指定要删除哪个列族的哪个列的数据
//        delete.addColumn(Bytes.toBytes("info2"),Bytes.toBytes("score_data mining"));
        //指定要删除哪个列族
        delete.addFamily(Bytes.toBytes("info2"));//转换为二进制
        //删除操作
        table.delete(delete);
    }

    /**
     * 单条数据查询
     * @throws IOException
     */
    @Test
    public void queryData() throws IOException {
        /**
         * 查询之前，先获得表连接
         */
        //通过工厂类创建1个连接（ConnectionFactory为连接池模式）
        connection = ConnectionFactory.createConnection(config);
        //获得表的连接。“user_test”为表名
        table = connection.getTable(TableName.valueOf("user_test"));

        //创建封装查询条件的类。
        Get get = new Get(Bytes.toBytes("u2"));
        //查询一条rowkey记录
        Result res = table.get(get);
//        System.out.println(res);  //keyvalues={u1/info1:_0/1545895588842/Put/vlen=1/seqid=0, u1/info1:age/1545895475625/Put/vlen=1/seqid=0, u1/info1:name/1545895588842/Put/vlen=6/seqid=0, u1/info1:sex/1545895475625/Put/vlen=3/seqid=0}

        //查询一条rowkey下的某个列族下的特定列的cell值
        /**
         * 注意：用java API插入整数和在Hbase shell插入整型值所占内存大小是不一样的，
         * 所以java API用int读取Hbase中的整型值会报错：“java.lang.IllegalArgumentException: offset (0) + length (4) exceed the capacity of the array: 1”
         */
        int age = Bytes.toInt(res.getValue(Bytes.toBytes("info1"),Bytes.toBytes("age")));
        System.out.println(age);
    }

    /**
     * 全表扫描查询数据。当Hbase中数据量大时，不可以执行全表扫描操作。
     * @throws IOException
     */
    @Test
    public void scanTable() throws IOException {
        /*
         * 创建全表扫描封装类
         */
        Scan scan = new Scan();
        //设置全表扫描的起始rowkey和终止rowkey，是左闭右开区间：例如“[u2,u4)”
//        scan.setStartRow(Bytes.toBytes("u2"));
//        scan.setStopRow(Bytes.toBytes("u4"));

        //scan.addColumn(byte[] family,  @Nullable byte[] qualifier)用于设置要全表扫描的某个列族下的哪个列
//        scan.addColumn(Bytes.toBytes("info1"),Bytes.toBytes("age"));
        //ResultScanner为以Result为迭代泛型的迭代器，Result是单条查询结果数据
        ResultScanner resultScanner = table.getScanner(scan);
        for (Result result : resultScanner) {
            /**
             * 如果“scan.addColumn(byte[] family,  @Nullable byte[] qualifier)”中设置的是扫描age列，那么此处要获取name列的数据就全为null。
             * Hbase中每个列族下存储的就是一些HFile文件，设置了哪个列族就会扫描这个列族下的一些文件。
             */
            String name = Bytes.toString(result.getValue(Bytes.toBytes("info1"),Bytes.toBytes("name")));
            System.out.println(name);
        }
    }

    /**
     * 全表扫描过滤器（是将符合条件的记录过滤出来）分类：
     *  1、列值过滤器
     *  2、列名前缀过滤器
     *  3、多个列名前缀过滤器
     *  4、rowkey过滤器
     */

    /**
     * 全表扫描列值过滤器
     * @throws IOException
     */
    @Test
    public void scanTableByColumnValueFilter() throws IOException {
        //列值过滤器
        SingleColumnValueFilter scvf = new SingleColumnValueFilter(Bytes.toBytes("info1"),Bytes.toBytes("age"), CompareOp.EQUAL,Bytes.toBytes("25"));

        //创建全表扫描封装类
        Scan scan = new Scan();
        scan.setFilter(scvf);
        ResultScanner resultScanner = table.getScanner(scan);

        for (Result result : resultScanner) {
            System.out.println(result);
        }
    }

    /**
     * 全表扫描列名过滤器
     * @throws IOException
     */
    @Test
    public void scanTableByColumnPrefixFilter() throws IOException {
        //列名前缀过滤器
        ColumnPrefixFilter cpf = new ColumnPrefixFilter(Bytes.toBytes("address"));

        //创建全表扫描封装类
        Scan scan = new Scan();
        scan.setFilter(cpf);
        ResultScanner resultScanner = table.getScanner(scan);

        for (Result result : resultScanner) {
            System.out.println(result);
        }
    }

    /**
     * 全表扫描多列名前缀过滤器：多个列名前缀是“或”的关系
     * @throws IOException
     */
    @Test
    public void scanTableByMultipleColumnPrefixFilter() throws IOException {
        //多列名前缀 二维数组
        byte[][] prefixs = new byte[][]{Bytes.toBytes("address"),Bytes.toBytes("sex")};

        //创建全表扫描封装类
        Scan scan = new Scan();
        //多列名前缀过滤器
        scan.setFilter(new MultipleColumnPrefixFilter(prefixs));
        ResultScanner resultScanner = table.getScanner(scan);

        for (Result result : resultScanner) {
            System.out.println(result);
        }
    }

    /**
     * 通过给rowkey设置正则过滤条件进行高效查询
     * @throws IOException
     */
    @Test
    public void queryDataSetByRowFilter() throws IOException {
        //2、创建rowkey正则过滤器
        RowFilter rf = new RowFilter(CompareOp.EQUAL, new RegexStringComparator("^u1_"));

        //1、创建全表扫描封装类
        Scan scan = new Scan();
        //3、设置过滤器到Scan
        scan.setFilter(rf);

        //4、获取过滤扫描结果类（是一个迭代器）
        ResultScanner resultScanner = table.getScanner(scan);

        //5、迭代取出筛选出来的每条记录
        for (Result result : resultScanner) {
            System.out.println(result);
        }
    }

    /**
     * 通过过滤器列表添加多个过滤器（条件）刷选出符合复杂过滤条件的记录
     * 注意：当过滤器列表中过滤操作条件为“MUST_PASS_ALL”时，过滤器列表中的过滤器必须是不同的类型，
     *      如果过滤器是同一种类型，那意味着过滤条件既要满足A，又要满足B，例如如下示例代码中：要筛选出列名前缀既为“name”，又为“sex”的记录是不存在的！
     *      “MUST_PASS_ALL”操作是针对一条记录，筛选出满足多个条件的某条或某些记录。
     * @throws IOException
     */
    @Test
    public void queryDataSetByRowFilterList() throws IOException {
        //2-1、创建指定“MUST_PASS_ALL”（过滤器列表中所有过滤器的条件为“与”关系）或“MUST_PASS_ONE”（过滤器列表中所有过滤器的条件为“或”关系）操作的过滤器列表
        FilterList filters = new FilterList(Operator.MUST_PASS_ALL);
        //2-2、创建过滤器
        ColumnPrefixFilter nameColumnPrefixFilter = new ColumnPrefixFilter(Bytes.toBytes("name"));
        ColumnPrefixFilter sexColumnPrefixFilter = new ColumnPrefixFilter(Bytes.toBytes("sex"));
//        RowFilter rowFilter1 = new RowFilter(CompareOp.EQUAL, new RegexStringComparator("^u1_"));
//        RowFilter rowFilter2 = new RowFilter(CompareOp.EQUAL, new RegexStringComparator("^u2_"));

        //2-3、将过滤器添加到过滤器列表FilterList
        filters.addFilter(nameColumnPrefixFilter);
        filters.addFilter(sexColumnPrefixFilter);
//        filters.addFilter(rowFilter1);
//        filters.addFilter(rowFilter2);

        //1、创建全表扫描封装类
        Scan scan = new Scan();
        //3、设置过滤器列表到Scan
        scan.setFilter(filters);

        //4、获取过滤扫描结果类（是一个迭代器）
        ResultScanner resultScanner = table.getScanner(scan);

        //5、迭代取出筛选出来的每条记录
        for (Result result : resultScanner) {
            System.out.println(result);
        }
    }

    @Test
    public void deleteTable() throws IOException {
        HBaseAdmin hBaseAdmin = new HBaseAdmin(config);
        hBaseAdmin.disableTable("user");
        hBaseAdmin.deleteTable("user");
        //关闭表连接
        hBaseAdmin.close();
    }

    @After
    public void close() throws IOException {
        //将表连接归还连接池
        table.close();
        //关闭ConnectionFactory
        connection.close();
    }
}
