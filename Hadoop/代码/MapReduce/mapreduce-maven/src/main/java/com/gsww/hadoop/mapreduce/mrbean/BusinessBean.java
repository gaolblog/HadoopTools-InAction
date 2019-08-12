package com.gsww.hadoop.mapreduce.mrbean;

import lombok.Data;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author gaol
 * @Date: 2019/2/12 9:58
 * @Description
 */
@Data
public class BusinessBean implements Writable {

    private String order_id;
    private String date;
    private String product_id;
    private String product_name;
    private float product_price;
    private String category_id;
    private int product_amount;

    /**
     * flag=0表示订单信息bean
     * flag=1表示产品信息bean
     */
    private String flag;
    /**
     * 空参构造
     */
    public BusinessBean() {
    }

    /**
     * setter方法
     * @param order_id
     * @param date
     * @param product_id
     * @param product_name
     * @param product_price
     * @param category_id
     * @param product_amount
     */
    public void setBusinessBean(String order_id, String date, String product_id, String product_name, float product_price, String category_id, int product_amount, String flag) {
        this.order_id = order_id;
        this.date = date;
        this.product_id = product_id;
        this.product_name = product_name;
        this.product_price = product_price;
        this.category_id = category_id;
        this.product_amount = product_amount;
        this.flag = flag;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(order_id);
        out.writeUTF(date);
        out.writeUTF(product_id);
        out.writeUTF(product_name);
        out.writeFloat(product_price);
        out.writeUTF(category_id);
        out.writeInt(product_amount);
        out.writeUTF(flag);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.order_id = in.readUTF();
        this.date = in.readUTF();
        this.product_id = in.readUTF();
        this.product_name = in.readUTF();
        this.product_price = in.readFloat();
        this.category_id = in.readUTF();
        this.product_amount = in.readInt();
        this.flag = in.readUTF();
    }

    @Override
    public String toString() {
        return order_id + '\t' + date + '\t' + product_id + '\t' + product_name + '\t' + product_price + '\t' + category_id + '\t' + product_amount;
    }
}
