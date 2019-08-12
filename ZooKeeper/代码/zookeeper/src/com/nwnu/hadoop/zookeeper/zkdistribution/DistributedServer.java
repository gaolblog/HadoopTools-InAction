package com.nwnu.hadoop.zookeeper.zkdistribution;

import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;

/**
 * @author gaol
 * @Date: 2018/9/17 10:47
 * @Description
 */
public class DistributedServer {

    //zookeeper服务器的地址
    private static final String connectString = "hadoopmaster:2181,hadoop01:2181,hadoop02:2181";
    //超时时间2s
    private static final int sessionTimeOut = 2000;

    private static final String parentNode = "/servers";

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
                System.out.println(event.getType() + " >>>>> " + event.getPath());
                //process()方法做的就是继续监听根结点
                try {
                    zk.getChildren("/",true);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 向zookeeper集群注册服务器信息
     * @param hostname
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void registerServer(String hostname) throws KeeperException, InterruptedException {
        String create = zk.create(parentNode + "/server", hostname.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println(hostname + " is online... " + create);
    }

    /**
     * 服务器端业务功能
     * @param hostname
     * @throws InterruptedException
     */
    public void handleBussines(String hostname) throws InterruptedException {
        System.out.println(hostname + " starts working...");
        Thread.sleep(Long.MAX_VALUE);
        //例如业务逻辑可以是一直监听外面的请求...
    }

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        //1、获取zookeeper连接
        DistributedServer server = new DistributedServer();
        server.getConnet();

        //2、利用zookeeper连接注册服务器信息
        server.registerServer(args[0]);

        //3、启动服务器端业务功能
        server.handleBussines(args[0]);
    }
}
