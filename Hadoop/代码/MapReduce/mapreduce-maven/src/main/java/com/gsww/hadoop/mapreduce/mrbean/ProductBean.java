package com.gsww.hadoop.mapreduce.mrbean;

import lombok.Data;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author gaol
 * @Date: 2019/2/13 15:20
 * @Description
 */
@Data
public class ProductBean implements Writable {

    private String product_id;
    private String product_name;
    private float product_price;
    private String category_id;

    public ProductBean() {
    }

    /**
     * setter
     * @param product_id
     * @param product_name
     * @param product_price
     * @param category_id
     */
    public void setProductBean(String product_id, String product_name, float product_price, String category_id) {
        this.product_id = product_id;
        this.product_name = product_name;
        this.product_price = product_price;
        this.category_id = category_id;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(product_id);
        out.writeUTF(product_name);
        out.writeFloat(product_price);
        out.writeUTF(category_id);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.product_id = in.readUTF();
        this.product_name = in.readUTF();
        this.product_price = in.readFloat();
        this.category_id = in.readUTF();
    }

    @Override
    public String toString() {
        return product_name + '\t' + product_price + '\t' + category_id;
    }
}
