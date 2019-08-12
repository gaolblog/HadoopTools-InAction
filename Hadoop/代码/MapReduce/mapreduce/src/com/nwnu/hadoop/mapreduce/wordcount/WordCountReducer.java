package com.nwnu.hadoop.mapreduce.wordcount;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/10/22 18:02
 * @Description
 * 泛型解释：
 *      KEYIN、VALUEIN 对应Mapper输出的KEYOUT,VALUEOUT类型
 *      KEYOUT（单词）, VALUEOUT（出现的总次数） 是自定义reducer逻辑处理结果的输出数据类型
 */
//Reducer实质上也是每一次处理一个键值对，直到把所有map task的同一分区上传过来的键值对都处理完
public class WordCountReducer extends Reducer<Text,IntWritable,Text,IntWritable>{
    //输入给reduce task的是键相同、值为1的键值对组，不同单词就有不同的多个组
    //键取的是一组键值对中，第1个键值对的键；值就是这1组所有键值对的值的迭代器

    /**
     * Reduce Task是对相同key的一组单词调用一次reduce()进行处理
     * @param key
     * @param values
     * @param context
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        int count = 0;

        for (IntWritable value : values) {
            System.out.println(value);
            count += value.get();
//            System.out.println(key.toString() + value.get());
        }
        //输出了【一个单词】词频的统计结果
        context.write(key,new IntWritable(count));
    }
}
