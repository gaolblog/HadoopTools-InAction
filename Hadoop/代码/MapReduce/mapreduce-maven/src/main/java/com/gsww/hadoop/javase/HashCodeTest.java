package com.gsww.hadoop.javase;

/**
 * @author gaol
 * @Date: 2019/1/28 15:10
 * @Description
 */
public class HashCodeTest {
    public static void main(String[] args) {
        String s1 = "hello";
        String s2 = "hellow";

        System.out.println(s1.hashCode());
        System.out.println(s2.hashCode() & Integer.MAX_VALUE);
    }
}
