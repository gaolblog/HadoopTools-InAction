package com.nwnu.hadoop.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;


/**
 * @author gaol
 * @Date: 2018/9/11 14:23
 * @Description
 */
public class SimpleZkClient {
    //zookeeper服务器的地址
    private static final String connectString = "hadoopmaster:2181,hadoop01:2181,hadoop02:2181";
    //超时时间2s
    private static final int sessionTimeOut = 2000;

    ZooKeeper zkClient = null;

    @Before
    public void init() throws IOException {
        //当客户端被创建的时候，其实有两个线程connect和listener，connect用于向zookeeper发出请求、上传数据、获取数据；
        //listener监听端口，一旦zookeeper发现事件变化，就会RPC listener监听的端口，从而listener会回调process()方法
        zkClient = new ZooKeeper(connectString, sessionTimeOut, new Watcher() {

            //当zookeeper发现节点变化时，会通知本客户端程序，而本客户端程序就会调用process()方法做一些事件响应逻辑
            @Override
            public void process(WatchedEvent event) {
                //客户端收到zookeeper的事件通知后的回调函数（应该是事件处理逻辑）
                System.out.println(event.getType() + " >>>>> " + event.getPath());
                //process()方法做的就是继续监听根结点
                try {
//                    System.out.println(zkClient);
                    zkClient.getChildren("/",true);
//                    List<String> children = zkClient.getChildren("/",true);
//                    for (String child : children) {
//                        System.out.print(child + " ");
//                    }
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 数据的增删改查
     */
    //创建数据节点到zookeeper中
    @Test
    public void testCreate() throws KeeperException, InterruptedException {
        //上传的数据可以是任何类型，但都要转成byte
        String nodeCreated = zkClient.create("/ideatest/idea02","222222".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    @Test
    //判断子节点是否存在
    public void testExists() throws KeeperException, InterruptedException {
        Stat stat = zkClient.exists("/idea",false);
        System.out.println(zkClient);
        System.out.println(stat == null? "not exist":"exist");
    }

    //获取子节点
    @Test
    public void getChildren() throws KeeperException, InterruptedException {
       //获取数据，并注册了一个监听器
        List<String> children = zkClient.getChildren("/",true);
        System.out.println(children);
//        for (String child : children) {
//            System.out.print(child); /*不知道为什么，此处不能用print()方法打印出来*/
//        }
        //逻辑结束了，但是方法的线程并未结束
        //listener线程在zookeeper中被设计为守护线程，这样当主线程退出时，守护线程也会退出，否则listener线程就阻塞了主线程
        Thread.sleep(Long.MAX_VALUE);
//        System.out.println("获取子节点逻辑结束");
    }

    //获取znode的数据
    @Test
    public void getData() throws KeeperException, InterruptedException {
        byte[] data = zkClient.getData("/ideatest/idea01",false,null);
        System.out.println(new String(data));
    }

    //删除znode
    @Test
    public void deleteZnode() throws KeeperException, InterruptedException {
        //参数2：指定要删除的版本，-1表示删除所有版本
        zkClient.delete("/ideatest/idea02",-1);
    }

    //修改znode数据
    @Test
    public void setData() throws KeeperException, InterruptedException {

        System.out.println("修改前:" + new String(zkClient.getData("/ideatest/idea01",false,null)));
        zkClient.setData("/ideatest/idea01","zookeeper".getBytes(),-1);
        System.out.println("修改后:" + new String(zkClient.getData("/ideatest/idea01",false,null)));
    }
}
