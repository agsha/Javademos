package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import static sha.NetworkBench.ServerState.State.*;
import static sha.Utils.*;

public class NetworkBench
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
//        args = new String[]{"client"};
//        args = new String[]{"server"};
        String settings = "serverSettings.json";
        if(args[0].equals("client")) {
            settings = "clientSettings.json";
        }
        try {
            NetworkBench obj = new NetworkBench();
            try {
                s = readJsonFromClasspath(settings, Settings.class);
                s.bf = bf;
            } catch (Exception e) {
                log.error("settings.json not found on classpath", e);
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
//            obj.test();
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public static class Settings {
        public String mode = "";
        public int bf = 1024;
        public String ip = "127.0.0.1";
        // required for jackson
        public Settings() {
        }
    }

    void test() {
        ByteBuffer b1 = ByteBuffer.wrap(new String("hello").getBytes());
        ByteBuffer b2 = ByteBuffer.wrap(new String("hello").getBytes());
        log.debug("{}", b1.equals(b2));
    }
    private void go() throws Exception {
        Server server = new Server();
        new Thread(server).start();
        Thread.sleep(2000);
        new Thread(new NioClient(1)).start();

//        if(s.mode.equals("server")) {
//            Server server = new Server();
//            new Thread(server).start();
////            server.run();
//        } else {
//            Thread.sleep(1000);
//            new Thread(new NioClient(1)).start();
////            new NioClient(1).run();
//
//        }
    }

    static int clientSend = 1024;
    static int clientRec = 256;
    static boolean noDelay = true;
    static int bf = 1024;



    public static class NioClient implements Runnable {
        private static final Logger log = LogManager.getLogger();
        private int numClients;


        public NioClient(int numClients) {
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

            Selector selector = Selector.open();

            for(int i=0; i<numClients;i++) {
                SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_CONNECT|SelectionKey.OP_READ|SelectionKey.OP_WRITE);
                sc.connect(new InetSocketAddress(s.ip, 8081));
            }
            int count = 0;
            long startTime = System.currentTimeMillis();
            outer:while(true) {
                boolean closing = false;
                count++;
                if(count%100==0) {
                    if(System.currentTimeMillis() - startTime > 30000000) {

                        closing = true;
                    }

                }
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    if(closing) {
                        key.channel().close();
                        key.attach(null);
                        key.cancel();
                        iterator.remove();
                        continue;
                    }
                    ClientState clientState = (ClientState) key.attachment();

                    SocketChannel channel = (SocketChannel)key.channel();
                    if(key.isConnectable()) {
                        channel.finishConnect();
                        clientState = new ClientState(key);
                        key.attach(clientState);
                        if(clientRec > 0) {
                            channel.setOption(StandardSocketOptions.SO_RCVBUF, clientRec);
                        }
                        if(clientSend > 0) {
                            channel.setOption(StandardSocketOptions.SO_SNDBUF, clientSend);
                        }
                        channel.socket().setTcpNoDelay(noDelay);

                        log.debug("tcp_nodelay:{}, rcvbuf:{} sndbuf:{}", channel.getOption(StandardSocketOptions.TCP_NODELAY), channel.getOption(StandardSocketOptions.SO_RCVBUF), channel.getOption(StandardSocketOptions.SO_SNDBUF));
                    }
                    clientState.write(channel);
                    if (key.isReadable()) {
                        clientState.read(channel);
                    }

                    iterator.remove();
                }
                if(closing) {
                    selector.close();
                    break;
                }
            }
        }
    }
//    private static final int size = 1024;
    public static class ClientState {
    LatencyTimer lt = new LatencyTimer("endToEnd");
    Utils.Timer tp = new Utils.Timer("clientThrougput");


    SelectionKey key;
        ByteBuffer magic = ByteBuffer.wrap("daphne".getBytes());
        ByteBuffer lenBf = ByteBuffer.allocateDirect(4);
        ByteBuffer timeStamp = ByteBuffer.allocateDirect(8);
        ByteBuffer data = ByteBuffer.allocateDirect(s.bf);
        ByteBuffer[] ar = {magic, lenBf, timeStamp, data};

        ByteBuffer responseHeader = ByteBuffer.allocateDirect("daphne".getBytes().length);
        ByteBuffer responseTimestamp = ByteBuffer.allocateDirect(8);
        ByteBuffer[] respAr = {responseHeader, responseTimestamp};

        int pending = 0;
        public ClientState(SelectionKey key) {
            this.key = key;
            lenBf.putInt(0, s.bf);
        }


        public void write(SocketChannel socketChannel) throws IOException {


            if(data.remaining()==0) {
                magic.clear();
                lenBf.clear();
                data.clear();
                timeStamp.putLong(0, System.nanoTime());
                timeStamp.clear();
                pending++;
            }
            if(pending<1000000) {

                long ret = socketChannel.write(ar);
                tp.count(ret);
            }
        }

        public void read(SocketChannel channel) throws IOException {
            channel.read(respAr);
            if(responseTimestamp.remaining()==0) {
                responseHeader.clear();
                if(!responseHeader.equals(magicConstant)) {
                    throw new IOException("magic header not found");
                }
                lt.count(System.nanoTime()-responseTimestamp.getLong(0));
                responseTimestamp.clear();
                responseHeader.clear();
                pending--;
            }
        }
    }

    static LatencyTimer dataTx = null;
    static LatencyTimer qTime = null;
    static LatencyTimer sendTimer = null;
    static LatencyTimer firstByteTime = null;
    public static class Server implements Runnable {
        private final Selector selector;
        private final ServerSocket socket;
        ServerSocketChannel ssc;


        @Override
        public void run() {
            try {
                throwingRun();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Server() {

            try {
                dataTx = new Utils.LatencyTimer("dataTx");
                qTime = new Utils.LatencyTimer("qTime");
                sendTimer = new Utils.LatencyTimer("sendTime");
                firstByteTime = new Utils.LatencyTimer("firstByteTime");

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

        public void throwingRun() throws Exception {
            int count = 0;
            while(true) {
                count++;
                int numSelects = selector.select();
                if(numSelects==0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    ServerState serverState = (ServerState) key.attachment();
                    SelectableChannel selectableChannel = key.channel();
                    if(key.isAcceptable()) {
                        log.debug("acceptable {}", count);
                        ServerSocketChannel channel = (ServerSocketChannel) selectableChannel;
                        SocketChannel socketChannel = channel.accept();
                        socketChannel.configureBlocking(false);
                        SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ);
                        serverState = new ServerState(newKey);
                        newKey.attach(serverState);
                        if(clientRec > 0) {
                            socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, clientRec);
                        }
                        if(clientSend > 0) {
                            socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, clientSend);
                        }
                        socketChannel.socket().setTcpNoDelay(noDelay);
                        log.debug("tcp_nodelay:{}, rcvbuf:{} sndbuf:{}", socketChannel.getOption(StandardSocketOptions.TCP_NODELAY), socketChannel.getOption(StandardSocketOptions.SO_RCVBUF), socketChannel.getOption(StandardSocketOptions.SO_SNDBUF));

                        log.debug("accepted a connection.");
                    }
                    if(key.isReadable()) {

                        SocketChannel socketChannel = (SocketChannel) selectableChannel;
                        serverState.read(socketChannel);
                    }
                    if(selectableChannel instanceof SocketChannel) {
                        SocketChannel socketChannel = (SocketChannel) selectableChannel;
                        serverState.write(socketChannel);
                    }

                    iterator.remove();

                }
            }
//            log.debug("server: bytes:{}, sum:{}", bytes, sum);

        }
    }
    static ByteBuffer magicConstant = ByteBuffer.wrap("daphne".getBytes());

    public static class ServerState {
        SelectionKey key;
        ByteBuffer magic = ByteBuffer.allocateDirect(magicConstant.capacity());
        ByteBuffer lenBf = ByteBuffer.allocateDirect(4);
        ByteBuffer timeStamp = ByteBuffer.allocateDirect(8);
        ByteBuffer data = ByteBuffer.allocateDirect(1024*1024*16);
        ByteBuffer view = null;
        ArrayDeque<ByteBuffer> responses = new ArrayDeque<>();
        ByteBuffer[] ar = { magic, lenBf, timeStamp};
        int toRead = 0;
        enum State {H, D};
        State state = H;

        //response fields
        Deque<Long> t1 = new ArrayDeque<>();
        Deque<Long> t2 = new ArrayDeque<>();
        ByteBuffer b0 = ByteBuffer.wrap("daphne".getBytes());
        ByteBuffer b1 = ByteBuffer.allocateDirect(8);
        ByteBuffer respAr[] = {b0, b1};


        public ServerState(SelectionKey key) {
            this.key = key;
            b1.position(b1.capacity()); // make b1 full so write triggered
        }

        long dataTxStartTime = 0;
        long rec = 0;

        int pending = 0;

        public void read(SocketChannel socketChannel) throws IOException {
            long ret;
            if(state == H) {
                ret = socketChannel.read(ar);
            } else {
                ret = socketChannel.read(view);
            }

            if(ret == -1) {
                socketChannel.close();
                key.cancel();
                key.attach(null);
                return;
            }

            if(state == H && timeStamp.remaining() == 0) {
                state = D;
                int bp = magic.position();
                int br = magic.remaining();
                magic.clear();
                toRead = lenBf.getInt(0);
                if(!magic.equals(magicConstant)) {
                    log.debug("yoooooo position:{} remaining:{} bp:{} br:{} rec:{} toRead:{}", magic.position(), magic.remaining(), bp, br, rec, toRead);

                    byte[] b = new byte[magic.capacity()];
                    magic.get(b);
                    magic.clear();
                    log.debug("yoooooo string is {}, contents is {}", new String(b), Arrays.toString(b));

                    throw new IOException("invalid header recieved");
                } else {
                    rec++;
                }

                data.position(data.capacity() - toRead);
                if(data.remaining()!=toRead) {
                    throw new IOException("misaligned");
                }
                view = data.slice();
                dataTxStartTime = System.nanoTime();
                firstByteTime.count(dataTxStartTime - timeStamp.getLong(0));
            }
            if(state==D && view.remaining()==0) {
                state = H;
                magic.clear();
                lenBf.clear();
                timeStamp.clear();
                data.clear();
                dataTx.count(System.nanoTime() - dataTxStartTime);
                long t = timeStamp.getLong(0);
                t1.addLast(t);
                t2.addLast(System.nanoTime());
            }
        }
        long sendTime = 0;
        public void write(SocketChannel channel) throws IOException {
            if((b1.remaining()==0) && t1.size()>0) {
                b0.clear(); b1.clear();
                b1.putLong(0, t1.removeFirst());

                qTime.count(System.nanoTime()-t2.removeFirst());

                long now = System.nanoTime();
                sendTimer.count(now - sendTime);
                sendTime = now;
            }
            if(b1.remaining()>0) {
                channel.write(respAr);
            }
        }
    }
}
