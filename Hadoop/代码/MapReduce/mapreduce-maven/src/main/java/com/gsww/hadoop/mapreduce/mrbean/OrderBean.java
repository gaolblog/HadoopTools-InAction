package com.gsww.hadoop.mapreduce.mrbean;

import lombok.Data;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author gaol
 * @Date: 2019/2/13 15:31
 * @Description
 */
@Data
public class OrderBean implements Writable {
    private String order_id;
    private String date;
    private String product_id;
    private int product_amount;

    public OrderBean() {
    }

    /**
     * setter
     * @param order_id
     * @param date
     * @param product_id
     * @param product_amount
     */
    public void setOrderBean(String order_id, String date, String product_id, int product_amount) {
        this.order_id = order_id;
        this.date = date;
        this.product_id = product_id;
        this.product_amount = product_amount;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(order_id);
        out.writeUTF(date);
        out.writeUTF(product_id);
        out.writeInt(product_amount);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.order_id = in.readUTF();
        this.date = in.readUTF();
        this.product_id = in.readUTF();
        this.product_amount = in.readInt();
    }

    @Override
    public String toString() {
        return order_id + '\t' + date + '\t' + product_id + '\t' +product_amount;
    }
}
