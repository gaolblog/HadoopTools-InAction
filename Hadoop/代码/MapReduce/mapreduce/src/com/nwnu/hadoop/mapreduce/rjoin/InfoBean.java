package com.nwnu.hadoop.mapreduce.rjoin;

import lombok.Data;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/11/6 0:10
 * @Description 将ValueOut封装成bean序列化出去
 */
@Data
public class InfoBean implements Writable {
    private String order_id;
    private String date;
    private String product_id;
    private int product_amount;
    private String product_name;
    private String category_id;
    private float product_price;
    //flag=0表示这个对象封装的是订单信息，flag=1表示封装的是产品信息
    private String flag;

    //反序列化时需要反射调用空参构造方法，所以显式定义一个
    public InfoBean() {
    }
    //有参构造器
    public void set(String order_id, String date, String product_id, int product_amount, String product_name, String category_id, float product_price, String flag) {
        this.order_id = order_id;
        this.date = date;
        this.product_id = product_id;
        this.product_amount = product_amount;
        this.product_name = product_name;
        this.category_id = category_id;
        this.product_price = product_price;
        this.flag = flag;
    }

    //把要输出的内容序列化出去
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(order_id);
        out.writeUTF(date);
        out.writeUTF(product_id);
        out.writeInt(product_amount);
        out.writeUTF(product_name);
        out.writeUTF(category_id);
        out.writeFloat(product_price);
        out.writeUTF(flag);
    }

    //反序列化进来
    @Override
    public void readFields(DataInput in) throws IOException {
        this.order_id = in.readUTF();
        this.date = in.readUTF();
        this.product_id = in.readUTF();
        this.product_amount = in.readInt();
        this.product_name = in.readUTF();
        this.category_id = in.readUTF();
        this.product_price = in.readFloat();
        this.flag = in.readUTF();
    }

    @Override
    public String toString() {
        return "order_id='" + order_id + '\'' +
                ", date='" + date + '\'' +
                ", product_id='" + product_id + '\'' +
                ", product_amount=" + product_amount +
                ", product_name='" + product_name + '\'' +
                ", category_id='" + category_id + '\'' +
                ", product_price=" + product_price +
                ", flag='" + flag;
    }
}
