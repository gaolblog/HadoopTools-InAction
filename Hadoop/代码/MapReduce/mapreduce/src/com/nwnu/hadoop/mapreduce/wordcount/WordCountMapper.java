package com.nwnu.hadoop.mapreduce.wordcount;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/10/22 17:22
 * @Description
 * 泛型解释：
 *      KEYIN：默认情况下是mr框架所读到的【一行文本】的起始偏移量，Long类型，但是在hadoop中有自己的更精简的序列化接口，所以不直接用Long，而用LongWritable
 *      VALUEIN：默认情况下，是mr框架所读到的一行文本的内容，String类型，同上，用Text
 *      KEYOUT：是用户自定义逻辑处理完成之后要输出数据中的key，在此处是单词，String类型，同上，用Text
 *      VALUEOUT：是用户自定义逻辑处理完成之后输出数据中的value，在此处是单词次数，Integer类型，同上，用IntWritable
 */
//不管是对map task的输入数据 ，还是map task输出的处理后的数据，都有可能要经过网络传输，所以需要序列化（序列化对象里的数据即可，而不需要此对象的继承结构等冗余信息）
public class WordCountMapper extends Mapper<LongWritable,Text,Text,IntWritable> {
    /**
     * map阶段的业务逻辑就写在map()方法中。
     * Map Task会对每一行输入数据调用一次map()方法，但并不会立即将map()处理结果分发给Reduce Task，而是先将结果写到一个分区文件，
     * 不同的单词写到不同的区，最后将不同的区分发给不同的Reduce。
     * @param key
     * @param value
     * @param context   封装了MapReduce框架中复杂的输入输出过程
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        //将map task传给map()方法的一行文本内容转换为String类型
        String line = value.toString();
        //将一行文本内容按空格切割为单词数组
        String wordsArr[] = line.split(" ");
        //遍历一行文本内容中的单词
        for (String word : wordsArr) {
            //将单词作为key，次数1作为value，以便于后续的数据分发：根据单词分发，将相同单词分发到同一Reduce Task上
            context.write(new Text(word),new IntWritable(1));
        }
    }
}
