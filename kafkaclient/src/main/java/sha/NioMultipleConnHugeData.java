package sha;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
 * 10 different client connect to 10 different server (one client per server)
 * and the server tries to send 1GB data to clients
 * as fast as possible
 *
 * Optimal buffer size seems to be around 1<<17 bytes irrespective of amount of data
 * to be sent.
 *
 * With the optimal buffer size, time taken is roughly proportional to amount of data sent.
 */
public class NioMultipleConnHugeData {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        NioMultipleConnHugeData not = new NioMultipleConnHugeData();
        not.go();
    }

    private void go() throws Exception {
        stats();
    }

    public void stats() throws Exception{
        Server server = new Server(1<<16);
        new Thread(server).start();
        Thread.sleep(1000);
        new Thread(new NioClient(1<<16, 10)).start();
    }


    public static class Server implements Runnable {
        private final Selector selector;
        private final ServerSocket socket;
        private final ByteBuffer bf;
        ServerSocketChannel ssc;
        private int batchsize;

        @Override
        public void run() {
            try {
                throwingRun();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Server(int batchsize) {
            this.batchsize = batchsize;
            bf = ByteBuffer.allocate(batchsize );

            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                selector = Selector.open();
                socket = ssc.socket();
                socket.bind(new InetSocketAddress(8081));
                ssc.register(selector, SelectionKey.OP_ACCEPT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        static int batch = 1000000000;

        public void throwingRun() throws Exception {
            Utils.Timer t = new Utils.Timer(getClass());
            t.reset();
            long last = 0;
            long bytes = 0;
            long sum = 0;
            outer:while(true) {
                int numSelects = selector.select();
                if(numSelects==0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if(key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                        SocketChannel socketChannel = channel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        log.debug("accepted a connection.");
                    } else if(key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        int ret = socketChannel.read(bf);
                        if(ret==-1) break outer;
                        if (ret > 0) {
                            bf.flip();
//                            log.debug("recieved:\n{}", Arrays.toString(Arrays.copyOfRange(bf.array(), 0, bf.limit())));
                            bytes += bf.remaining();
//                            while (bf.hasRemaining()){
//                                sum+=bf.get();
//                            }
                           t.count(bf.remaining());

                            bf.clear();
                        }
                    }
                    iterator.remove();

                }
            }
            log.debug("server: bytes:{}, sum:{}", bytes, sum);

        }
    }

    public static class NioClient implements Runnable {
        private static final Logger log = LogManager.getLogger();
        private int bufferSize;
        private int numClients;

        public NioClient(int bufferSize, int numClients) {
            this.bufferSize = bufferSize;
            this.numClients = numClients;
        }

        @Override
        public void run() {
            try {
                throwingRun();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void throwingRun() throws Exception {
            Random random = new Random();
            long sum = 0;
            long blocksToWrite = 100000000000L;

            Selector selector = Selector.open();

            for(int i=0; i<numClients;i++) {
                ByteBuffer bf = ByteBuffer.allocate(bufferSize);
                //to read mode
                bf.flip();
//                log.debug("pos:{}, lim{}, cap:{}", bf.position(), bf.limit(), bf.capacity());

                SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_WRITE|SelectionKey.OP_CONNECT, bf);
                sc.connect(new InetSocketAddress(8081));
            }
            int bytesWritten = 0;
            outer:while(true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    SocketChannel channel = (SocketChannel)selectionKey.channel();
                    if(selectionKey.isConnectable()) {
                        channel.finishConnect();
                    }
                    else if(selectionKey.isWritable()) {
                        ByteBuffer bf = (ByteBuffer) selectionKey.attachment();
                        if(bf.remaining()==0) {
                            if(blocksToWrite == 0) {
                                channel.close();
                                break outer;
                            }
                            blocksToWrite--;
                            bf.clear();
                            random.nextBytes(bf.array());
//                            log.debug("data sent:\n{}",Arrays.toString(bf.array()));
//                            for(int i=0; i<bf.array().length; i++) {
//                                sum+=bf.array()[i];
//                            }
                        }
                        int writtenNow = channel.write(bf);
                        bytesWritten+=writtenNow;
//                        log.debug("writtenNow:{}, total:{}", writtenNow, bytesWritten);
                    }
                    iterator.remove();
                }
            }
            log.debug("client: bytes:{}, sum:{}", bytesWritten, sum);

        }

    }
}
