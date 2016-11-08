package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
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
public class MultipleConnPeriodicSmallData {
    private static final Logger log = LogManager.getLogger();


    public static void main(String[] args) throws Exception {
        MultipleConnPeriodicSmallData not = new MultipleConnPeriodicSmallData();
        not.go();
    }

    private void go() throws Exception {
        hello();
    }

    private void hello() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ServerSocket socket = ssc.socket();
        socket.bind(new InetSocketAddress(8081));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        int numConns = 0;

        startClient();

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
            long start = System.nanoTime();
            newConn(selector);
            int count = 0;
            List<SocketChannel> channels = new ArrayList<>();
            outer:while(true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    SocketChannel channel = (SocketChannel)selectionKey.channel();
                    if(selectionKey.isConnectable()) {
                        channel.finishConnect();
                        channels.add(channel);
                        newConn(selector);
                        count++;
                        if(count==4000) {
                            iterator.remove();
                            break outer;
                        }
                    }
                    iterator.remove();
                }
            }
            log.debug("finished initializing 4000 connections in time(ms):{}", (System.nanoTime()-start)/1e6);

            byte[] data = new byte[1<<15];
            Random random = new Random();
            random.nextBytes(data);
            for (SocketChannel channel : channels) {
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.flip();
                channel.register(selector, SelectionKey.OP_WRITE, bf);
            }

            outer:while(true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    ByteBuffer bf = (ByteBuffer)selectionKey.attachment();
                    SocketChannel channel = (SocketChannel)selectionKey.channel();
                    if(bf.hasRemaining()) {
                        channel.write(bf);
                    } else {
                        channel.close();
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
