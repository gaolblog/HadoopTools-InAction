package com.nwnu.hadoop.mapreduce.flowsum;

import lombok.Data;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/10/24 15:10
 * @Description
 */
@Data
public class FlowBean implements Writable{
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

    //反序列化时需要反射调用空参构造方法，所以显式定义一个
    public FlowBean() {
    }

    public FlowBean(long upFlow, long downFlow) {
        this.upFlow = upFlow;
        this.downFlow = downFlow;
        this.sumFlow = upFlow + downFlow;
    }

    /**
     * 序列化方法
     * @param dataOutput    输出流
     * @throws IOException
     */
    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(upFlow);
        dataOutput.writeLong(downFlow);
        dataOutput.writeLong(sumFlow);
    }

    /**
     * 反序列化方法
     * @param dataInput
     * @throws IOException
     */
    //那边通过反射机制将这个类反射为一个对象，然后通过反序列化将对象里面的值给射进去
    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.upFlow = dataInput.readLong();
        this.downFlow = dataInput.readLong();
        this.sumFlow = dataInput.readLong();
    }

    @Override
    public String toString() {
        return upFlow + "\t" + downFlow + "\t" + sumFlow;
    }
}
