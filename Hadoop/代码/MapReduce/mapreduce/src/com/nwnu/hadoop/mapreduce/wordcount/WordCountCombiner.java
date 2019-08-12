package com.nwnu.hadoop.mapreduce.wordcount;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/10/30 10:55
 * @Description
 */
public class WordCountCombiner extends Reducer<Text, IntWritable,Text,IntWritable> {
    /**
     * This method is called once for each key. Most applications will define
     * their reduce class by overriding this method. The default implementation
     * is an identity function.
     *
     * @param key
     * @param values
     * @param context
     */
    @Override
    protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        int count = 0;

        for (IntWritable value : values) {
            count += value.get();
        }
        //输出了一个单词词频的统计结果
        context.write(key,new IntWritable(count));
    }
}
