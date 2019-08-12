package com.nwnu.hadoop.mapreduce.rjoin;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/11/6 0:04
 * @Description
 */
public class ReduceJoin {
    static class ReduceJoinMapper extends Mapper<LongWritable, Text, Text, InfoBean> {
        InfoBean bean = new InfoBean();
        Text k = new Text();
        /**
         * Called once for each key/value pair in the input split. Most applications
         * should override this, but the default is the identity function.
         *
         * @param key
         * @param value
         * @param context
         */
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            //将一行文本内容转换为String
            String line = value.toString();

//            InputSplit inputSplit = context.getInputSplit();
            FileSplit inputSplit = (FileSplit) context.getInputSplit();
            //map task获取读取的文件切片所在文件的名称
            String fileName = inputSplit.getPath().getName();
            String pid = "";

            if (fileName.startsWith("order")) {
                String[] fields = line.split(",");
                pid = fields[2];
                //order_id  date    product_id  product_amount  product_name    category_id     product_price   flag
                bean.set(fields[0],fields[1],pid,Integer.parseInt(fields[3]),"","",0,"0");

            } else {
                String[] fields = line.split(",");
                pid = fields[0];
                //order_id  date    product_id  product_amount  product_name    category_id     product_price   flag
                bean.set("","",pid,0,fields[1],fields[2],Float.parseFloat(fields[3]),"1");
            }
            k.set(pid);
            context.write(k,bean);
        }
    }

    static class ReduceJoinReducer extends Reducer<Text, InfoBean, InfoBean, NullWritable> {
        /**
         * This method is called once for each key. Most applications will define
         * their reduce class by overriding this method. The default implementation
         * is an identity function.
         *
         * @param pid
         * @param beans
         * @param context
         */
        //相同product_id的订单bean和产品信息beans都在同一组
        @Override
        protected void reduce(Text pid, Iterable<InfoBean> beans, Context context) throws IOException, InterruptedException {
            for (InfoBean bean : beans) {

            }
        }
    }
}
