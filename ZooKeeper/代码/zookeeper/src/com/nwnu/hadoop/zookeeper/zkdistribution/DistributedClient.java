package com.nwnu.hadoop.zookeeper.zkdistribution;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gaol
 * @Date: 2018/9/17 11:33
 * @Description
 */
public class DistributedClient {

    //zookeeper服务器的地址
    private static final String connectString = "hadoopmaster:2181,hadoop01:2181,hadoop02:2181";
    //超时时间2s
    private static final int sessionTimeOut = 2000;

    private static final String parentNode = "/servers";

    private volatile List<String> serverList;

    ZooKeeper zk = null;

    /**
     * 创建到zookeeper集群的客户端连接
     * @throws IOException
     */
    public void getConnet() throws IOException {
        zk = new ZooKeeper(connectString, sessionTimeOut, new Watcher() {

            //当zookeeper发现节点变化时，会通知本客户端程序，而本客户端程序就会调用process()方法做一些事件响应逻辑
            @Override
            public void process(WatchedEvent event) {
                //客户端收到zookeeper的事件通知后的回调函数（应该是事件处理逻辑）
                //process()方法做的就是继续监听根结点
                try {
                    getServerList();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 获取服务器信息列表
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void getServerList() throws KeeperException, InterruptedException {
        //获取服务器子节点信息，并监听父节点
        List<String> children = zk.getChildren(parentNode,true);
        List<String> servers = new ArrayList<>();

        for (String child : children) {
            byte[] data = zk.getData(parentNode + "/" + child, false, null);
            servers.add(new String(data));
        }
        serverList = servers;
        System.out.println(serverList);
    }

    /**
     * 客户端业务功能
     */
    public void handleBussines() throws InterruptedException {
        System.out.println("client starts working...");
        Thread.sleep(Long.MAX_VALUE);
    }

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        //获取zookeeper连接
        DistributedClient client = new DistributedClient();
        client.getConnet();
        //获取servers的子节点信息并监听，从中获取服务器信息列表
        client.getServerList();
        //业务线程启动，获取服务器列表中的信息
        client.handleBussines();
    }
}
