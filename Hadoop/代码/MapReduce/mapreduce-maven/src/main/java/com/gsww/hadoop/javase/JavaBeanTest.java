package com.gsww.hadoop.javase;

import com.gsww.hadoop.mapreduce.mrbean.BusinessBean;
import com.gsww.hadoop.mapreduce.mrbean.FlowBean;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gaol
 * @Date: 2019/1/29 16:45
 * @Description
 */
public class JavaBeanTest {
//    static BusinessBean orderBean = new BusinessBean();

    public static void main(String[] args) {
//        testFlowBean();
//        testBusinessBean();
    }

    public static void testBusinessBean() {
        List<BusinessBean> originBeanList = new ArrayList<>();
        List<BusinessBean> orderBeanList = new ArrayList<>();

        BusinessBean bean1 = new BusinessBean();
        bean1.setBusinessBean("1001","20190212","p001","小米8",3078.9f,"C01",2,"0");
        originBeanList.add(bean1);

        BusinessBean bean2 = new BusinessBean();
        bean2.setBusinessBean("1001","20190212","p002","小米8s",3478.9f,"C02",1,"0");
        originBeanList.add(bean2);

        try {
            for (BusinessBean bean : originBeanList) {
                BusinessBean orderBean = new BusinessBean();
                BeanUtils.copyProperties(orderBean,bean);
                orderBeanList.add(orderBean);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        System.out.println(orderBeanList);
    }

    public static void testFlowBean() {
        List<FlowBean> beanList = new ArrayList<FlowBean>();
        FlowBean flowBean = new FlowBean();

        flowBean.setFlows(1,2);
        beanList.add(flowBean);

        flowBean.setFlows(10,20);
        beanList.add(flowBean);

        flowBean.setFlows(100,200);
        beanList.add(flowBean);

        System.out.println(beanList);
    }
}
