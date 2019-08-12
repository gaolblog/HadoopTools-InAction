package com.gsww.hadoop.mapreduce.mrbean;

import lombok.Data;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author gaol
 * @Date: 2019/1/15 15:07
 * @Description  自定义Map Task、Reduce Task传输数据的对象，需要实现Hadoop自带的序列化框架接口
 */

@Data
public class FlowBean implements WritableComparable<FlowBean> {
    /**
     * 上行流量
     */
    private long upFlow;
    /**
     * 下行流量
     */
    private long downFlow;
    /**
     * 总流量
     */
    private long sumFlow;

    /**
     * FlowBean的空参构造方法。
     */
    public FlowBean() {
    }

    /**
     * FlowBean的setters
     * @param upFlow
     * @param downFlow
     */
    public void setFlows(long upFlow, long downFlow) {
        this.upFlow = upFlow;
        this.downFlow = downFlow;
        this.sumFlow = upFlow + downFlow;
    }

    /**
     * 序列化（Hadoop的序列化框架很精简，只需要通过DataOutput流将要序列化的数据写出去即可）
     * @param dataOutput
     * @throws IOException
     */
    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(upFlow);
        dataOutput.writeLong(downFlow);
        dataOutput.writeLong(sumFlow);
    }

    /**
     * 反序列化（反序列化的一方通过反射机制将类反射为一个对象，然后通过此反序列化方法把对象的值给射进去）
     * @param dataInput
     * @throws IOException
     */
    @Override
    public void readFields(DataInput dataInput) throws IOException {
        upFlow = dataInput.readLong();
        downFlow = dataInput.readLong();
        sumFlow = dataInput.readLong();
    }

    /**
     * Reduce Task输出聚合结果到文本文件的一行内容
     * @return
     */
    @Override
    public String toString() {
        return upFlow + "\t" + downFlow + "\t" + sumFlow;
    }

    /**
     * 倒序比较的compareTo方法
     * @param o
     * @return
     */
    @Override
    public int compareTo(FlowBean o) {
        return this.sumFlow>o.getSumFlow()?-1:1;
    }
}
