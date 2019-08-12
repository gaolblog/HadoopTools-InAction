package com.nwnu.hadoop.mapreduce.flowsum;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author gaol
 * @Date: 2018/10/26 0:12
 * @Description
 */
public class NumberFormatExceptionTest {
    public static void main(String[] args) {
        //1、创建读文件字符流
        FileReader fr = null;

        try {
            fr = new FileReader("D:\\JAVA\\IdeaProjects\\hadoop\\mapreduce\\flow.log");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //2、创建读文件字符流的缓冲区以提高读/写文件的效率
        BufferedReader bufr = new BufferedReader(fr);

        //3、按行读取指定的文件内容，并复制到另一文件
        try {
                String line = null;
//            while ((line = bufr.readLine())!= null) { //注意：按行读取文本内容时，不会读入换行符
//                System.out.println(line);
                line = bufr.readLine();
                String[] fields = line.split("\t");
                System.out.println(Arrays.toString(fields));
            System.out.println(Long.parseLong(fields[fields.length - 3]));
            System.out.println(Long.parseLong(fields[fields.length - 2]));
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
