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
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sharath.g on 08/07/15.
 * Opens a simgle connection and then the client tries to send 1GB data to server.
 * took 4 seconds to send 1 GB data.
 *
 * Optimal buffer size seems to be around 1<<17 bytes irrespective of amount of data
 * to be sent.
 *
 * With the optimal buffer size, time taken is roughly proportional to amount of data sent.
 */
public class NioSingleConnHugeData {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        NioSingleConnHugeData not = new NioSingleConnHugeData();
        not.go();
    }

    private void go() throws Exception {
        stats();
    }

    public void stats() throws Exception{
        for(int i=1; i<=1; i++) {
            bf = ByteBuffer.allocate(1<<i);
            Ret ret = startServer(i);
            log.debug("buf_power: {}, reads: {}, time(ms){}, bytes:{}", i, ret.reads, ret.nanoTime/1e6, ret.bytes);
        }
    }
    public void startClient(final int twoPower) {
        final ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.execute(new Runnable() {
            public void run() {
                try {
                    NioClient nc = new NioClient();
                    nc.asynchronousSend(1<<twoPower, 1<<30);
                    executorService.shutdown();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }
    private Ret startServer(int twoPower) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ServerSocket socket = ssc.socket();
        socket.bind(new InetSocketAddress(10488));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        //startClient(twoPower);
        Map<Integer, Integer> map = new TreeMap<>();
        int reads = 0;
        long now = 0;
        long bytes = 0;

        String op = "";
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
                    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    log.debug("accepted a connection.");
                    now = -System.nanoTime();
                } else if(key.isReadable()) {
                    reads++;
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    int ret = socketChannel.read(bf);
                    bf.flip();
                    if(ret == -1) {
                        iterator.remove();
                        key.channel().close();
                        //Files.write(Paths.get("syslogOp"), op.getBytes(), StandardOpenOption.CREATE);
                        op = "";
                        continue;
                    }
                    bytes+=bf.remaining();
                    op =  new String(bf.array());
                    if(op.contains("\n")) {
                        System.out.println();
                    }
                    System.out.print(op);
//                    if(!map.containsKey(bf.remaining())) {
//                        map.put(bf.remaining(), 1);
//                    } else {
//                        map.put(bf.remaining(), map.get(bf.remaining())+1);
//                    }
                    bf.clear();
                }
                iterator.remove();
            }
        }
//        log.debug("op:{}", op);
//        Files.write(Paths.get("syslogOp"), op.getBytes(), StandardOpenOption.CREATE);
//        ListMultimap<Integer, Integer> inverse = Multimaps.invertFrom(Multimaps.forMap(map),
//                ArrayListMultimap.<Integer, Integer>create());
//        //log.debug("straight map: {}", map);
//        //log.debug("{}", inverse);
//        selector.close();
//        ssc.close();
//        socket.close();
//        return new Ret(reads, now+System.nanoTime(), bytes);

    }

    public static class Ret {
        public long reads;
        public long nanoTime;
        public long bytes;

        public Ret(long reads, long nanoTime, long bytes) {
            this.reads = reads;
            this.nanoTime = nanoTime;
            this.bytes = bytes;
        }
    }

    ByteBuffer bf = ByteBuffer.allocate(2<<8 );

    public static class NioClient {
        private static final Logger log = LogManager.getLogger();

        public static void main(String[] args) throws Exception {
            NioClient not = new NioClient();
            not.go();
        }

        private void go() throws Exception {
            asynchronousSend(1<<10, 1<<25);
        }

        public void asynchronousSend(int bufferSize, int total) throws Exception {
            Random random = new Random();

            byte[] array = new byte[bufferSize];
            ByteBuffer bf = ByteBuffer.wrap(array);
            SocketChannel sc = SocketChannel.open();
            Selector selector = Selector.open();
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_WRITE|SelectionKey.OP_CONNECT);
            sc.connect(new InetSocketAddress(8081));
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
                    //log.debug("got an event");
                    else if(selectionKey.isWritable()) {
                        if(bytesWritten==bf.capacity()) {
                            random.nextBytes(array);
                            bytesWritten=0;
                            bf.clear();
                        }
                        int writtenNow = channel.write(bf);
                        bytesWritten+=writtenNow;
                        total-=writtenNow;
                        if(total<=0) {
                            break outer;
                        }
                    }
                    iterator.remove();
                }
            }
            selector.close();
            sc.close();
        }

        private void synchronousSend() throws Exception {
            Socket echoSocket = new Socket("localhost", 8081);
            String s = "sharath is great";
            int len  = s.length();
            OutputStream os = echoSocket.getOutputStream();
            long time = -System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                os.write(s.charAt(i%len));
            }
            //log.debug("{}", time+System.currentTimeMillis());
            log.debug("done transmitting");
            echoSocket.close();
        }
    }
}
