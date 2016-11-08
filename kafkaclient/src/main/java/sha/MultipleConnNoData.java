package sha;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sharath.g on 08/07/15.
 * Tries to open as many connections as possible and measures how long
 * opening a connection takes place.
 *
 * Results:
 * around 4300 connections made and then it throws too
 * many open files exception.
 * First ~100 connections in < 1ms, total average 6 ms
 *
 */
public class MultipleConnNoData {
    private static final Logger log = LogManager.getLogger();


    public static void main(String[] args) throws Exception {
        MultipleConnNoData not = new MultipleConnNoData();
        not.go();
    }

    private void go() throws Exception {
        startserver();
    }

    private void startserver() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ServerSocket socket = ssc.socket();
        socket.bind(new InetSocketAddress(8081));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        int numConns = 0;

        startClient();

        List<Double> times = new ArrayList<>();
        long realstart = System.nanoTime();
        long total = 0;
        long start = System.nanoTime();
        outer:while(true) {
            int numSelects = selector.select();
            if(numSelects==0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = channel.accept();
                    if(socketChannel == null) continue ;
                    socketChannel.configureBlocking(false);
                    log.debug("accepted {} connections.", ++numConns);
                    long now = System.nanoTime();
                    times.add((now-start)/1e6);
                    total+=(now-start);
                    if(times.size()==4000) {
                        log.debug("times array is {}. sum of times in array:{}, total time:{}", times, total/1e6, (now - realstart)/1e6);
                    }
                    start = now;
                }
                iterator.remove();
            }

        }

    }


    public void startClient() {
        final ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.execute(new Runnable() {
            public void run() {
                try {
                    MultipleConnClient mc = new MultipleConnClient();
                    mc.asynchronousConnect();
                    executorService.shutdown();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }
    public static class MultipleConnClient {
        private static final Logger log = LogManager.getLogger();

        public static void main(String[] args) throws Exception {
            MultipleConnClient not = new MultipleConnClient();
            not.go();
        }

        private void go() throws Exception {
            asynchronousConnect();
        }

        public void asynchronousConnect() throws Exception {
            Selector selector = Selector.open();
            newConn(selector);
            outer:while(true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    SocketChannel channel = (SocketChannel)selectionKey.channel();
                    if(selectionKey.isConnectable()) {
                        channel.finishConnect();
                        newConn(selector);
                    }
                    iterator.remove();
                }
            }
        }

        public void newConn(Selector selector) throws Exception {
            log.debug("client creating new connection");
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_CONNECT);
            sc.connect(new InetSocketAddress(8081));
        }
    }
}
