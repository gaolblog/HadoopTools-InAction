package com.nwnu.hadoop.mapreduce.flowsum;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;

import java.util.HashMap;

/**
 * @author gaol
 * @Date: 2018/10/29 14:51
 * @Description
 * <KEY, VALUE> 对应的是FlowCountMapper输出的<Text,FlowBean>
 */
public class ProvincePartitioner extends Partitioner<Text,FlowBean> {
    public static HashMap<String,Integer> provinceDict = new HashMap<>();
    static {
        provinceDict.put("135",0);
        provinceDict.put("136",1);
        provinceDict.put("137",2);
        provinceDict.put("138",3);
        provinceDict.put("139",4);
    }
    /**
     * Get the partition number for a given key (hence record) given the total
     * number of partitions i.e. number of reduce-tasks for the job.
     *
     * <p>Typically a hash function on a all or a subset of the key.</p>
     *
     * @param phoneNumber          the key to be partioned.
     * @param flowBean      the entry value.
     * @param numPartitions the total number of partitions.
     * @return the partition number for the <code>key</code>.
     */
    @Override
    public int getPartition(Text phoneNumber, FlowBean flowBean, int numPartitions) {
        String phoneNumberPrefix = phoneNumber.toString().substring(0,3);
        Integer provinceId = provinceDict.get(phoneNumberPrefix);
        return provinceId == null?5:provinceId;
    }
}
