package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import static sha.NetworkBench.ClientState.WriteState.*;
import static sha.NetworkBench.ServerState.ReadState.*;
import static sha.Utils.*;

public class NetworkBench
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        String settings = "serverSettings.json";
        if(args[0].equals("client")) {
            settings = "clientSettings.json";
        }
        try {
            NetworkBench obj = new NetworkBench();
            try {
                s = readJsonFromClasspath(settings, Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
//            obj.test();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static class Settings {
        public String mode = "";
        public int bf = 0;
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
        if(s.mode.equals("server")) {
            Server server = new Server();
            //new Thread(server).start();
            server.run();
        } else {
//            Thread.sleep(1000);
//            new Thread(new NioClient(1)).start();
            new NioClient(1).run();

        }
    }




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
                sc.register(selector, SelectionKey.OP_WRITE|SelectionKey.OP_CONNECT|SelectionKey.OP_READ);
                sc.connect(new InetSocketAddress(s.ip, 8081));
            }
            int count = 0;
            long startTime = System.currentTimeMillis();
            outer:while(true) {
                boolean closing = false;
                count++;
                if(count%100==0) {
                    if(System.currentTimeMillis() - startTime > 30000) {

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
                    if(clientState!=null) {
                        clientState.startIteration();
                    }
                    if(clientState!=null) {
                        clientState.startIteration();
                    }

                    SocketChannel channel = (SocketChannel)key.channel();
                    if(key.isConnectable()) {
                        channel.finishConnect();
                        clientState = new ClientState(key);
                        key.attach(clientState);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }

                    else if(key.isWritable()) {
                        clientState.write2(channel);
                    } else if (key.isReadable()) {
//                        clientState.read(channel);
                    }
                    if(clientState!=null) {
                        clientState.finishIteration();
                    }
//                    int delay = 10;
//                    int sum = 0;
//                    for(int i=0; i<delay; i++) {
//                        for(int j=0; j<1000; j++) {
//                            sum+=j;
//                        }
//                    }
//                    if(sum==Integer.MAX_VALUE) {
//                        l("hi");
//                    }

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
        SelectionKey key;
        ByteBuffer magic = ByteBuffer.wrap("daphne".getBytes());
        ByteBuffer lenBf = ByteBuffer.allocateDirect(4);
        ByteBuffer data = ByteBuffer.allocateDirect(s.bf);
        ByteBuffer[] ar = {data};

        ByteBuffer response = ByteBuffer.allocateDirect(4);
        int written = 0;
        boolean readable = false;
        boolean eof = false;

        public ClientState(SelectionKey key) {
            this.key = key;
            lenBf.putInt(s.bf);
        }

        public enum WriteState {
            BEGIN_MAGIC, WRITING_MAGIC, BEGIN_LEN, WRITING_LEN, BEGIN_DATA, WRITING_DATA, READ_EOF
        }
        WriteState state = BEGIN_MAGIC;
        public void startIteration() {
            readable = false;
        }
        public void finishIteration() throws IOException {
//            log.debug("{}", readable);
//            key.interestOps(key.interestOps()|SelectionKey.OP_READ);
//
//            if(readable) {
//            } else {
//                key.interestOps(key.interestOps()&~SelectionKey.OP_READ);
//            }
            if(eof) {
                key.channel().close();
                key.attach(null);
                key.cancel();
            }

        }

        public void write2(SocketChannel socketChannel) throws IOException {
            if(data.remaining()==0) {
                magic.clear();
                lenBf.clear();
                data.clear();
                t.count(s.bf);
            }
            socketChannel.write(ar);
        }

        public void write(SocketChannel channel) throws IOException {
            if(state==BEGIN_MAGIC) {
                magic.limit(magic.capacity());
                magic.rewind();
                state = WRITING_MAGIC;
            } if (state == WRITING_MAGIC) {
                channel.write(magic);
                if(magic.remaining() == 0) {
                    state = BEGIN_LEN;
                }
            } if (state == BEGIN_LEN) {
                lenBf.limit(lenBf.capacity());
                lenBf.rewind();
                state = WRITING_LEN;
            } if (state == WRITING_LEN) {
                channel.write(lenBf);
                if(lenBf.remaining()==0) {
                    state = BEGIN_DATA;
                }
            } if (state == BEGIN_DATA) {
                data.limit(data.capacity());
                data.rewind();
                state = WRITING_DATA;
            } if (state == WRITING_DATA) {
                channel.write(data);
                if(data.remaining()==0) {
                    written++;
                    readable = true;
                    state = BEGIN_MAGIC;
                }
            }



        }
        public void read(SocketChannel channel) throws IOException {
            if(written<=0) {
                eof = true;
                log.error("wasnt expecting a response");
            }
            int ret = channel.read(response);
            eof = ret == -1;
            if(response.remaining()==0) {
                response.flip();
                int r = response.getInt();
                if(r!=s.bf) {
                    throw new IOException("should have got "+s.bf+" but got "+r);
                }
                response.clear();
                written--;
                readable = written > 0;
            }
        }
    }


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
                    if(serverState!=null) {
                        serverState.startIteration();
                    }
                    if(key.isAcceptable()) {
                        log.debug("acceptable {}", count);
                        ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                        SocketChannel socketChannel = channel.accept();
                        socketChannel.configureBlocking(false);
                        SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ);
                        serverState = new ServerState(newKey);
                        newKey.attach(serverState);
                        newKey.interestOps(SelectionKey.OP_READ);

                        log.debug("accepted a connection.");
                    } else if(key.isReadable()) {

                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        serverState.read2(socketChannel);
                    } else if(key.isWritable()) {

//                        SocketChannel socketChannel = (SocketChannel) key.channel();
//                        serverState.write(socketChannel);
                    }
                    if(serverState!=null) {
                        serverState.finishIteration();
                    }
                    iterator.remove();

                }
            }
//            log.debug("server: bytes:{}, sum:{}", bytes, sum);

        }
    }
    static Utils.Timer t = new Utils.Timer(NetworkBench.class);

    public static class ServerState {
        SelectionKey key;
        int c = "daphne".getBytes().length;
        ByteBuffer magicConstant = ByteBuffer.wrap("daphne".getBytes());
        ByteBuffer magic = ByteBuffer.allocateDirect(c);
        ByteBuffer lenBf = ByteBuffer.allocateDirect(4);
        ByteBuffer data = ByteBuffer.allocateDirect(1024*1024*1024);
        ArrayDeque<ByteBuffer> responses = new ArrayDeque<>();
        boolean writable = false;
        int len = 0;
        ByteBuffer[] ar = { data};

        public ServerState(SelectionKey key) {
            this.key = key;
        }

        public enum ReadState {
            MAGIC, LEN, DATA, EOF
        }
        ReadState state = MAGIC;
        public void startIteration() {
            writable = false;
        }
        public void finishIteration() throws IOException {
//            log.debug("{}", writable);
//            if(writable) {
//                key.interestOps(SelectionKey.OP_WRITE|SelectionKey.OP_READ);
//            } else {
//                key.interestOps(SelectionKey.OP_READ);
//            }
            if(state == EOF) {
                key.channel().close();
                key.cancel();
            }
        }

        public void write(SocketChannel channel) throws IOException {
//            l("came here");
            ByteBuffer[] srcs = responses.toArray(new ByteBuffer[0]);
            channel.write(srcs);
            while(responses.size() > 0) {
                if(responses.peekFirst().remaining() == 0) {
                    responses.removeFirst();
                }
            }
            if(responses.size() > 0) {
                writable = true;
            }
        }

        public void read2(SocketChannel socketChannel) throws IOException {
            if(data.remaining()==0) {
                magic.clear();
                lenBf.clear();
                data.clear();
            }
            long ret = socketChannel.read(ar);
            if(ret == -1) {
                socketChannel.close();
                key.cancel();
                key.attach(null);
            } else {
                t.count(ret);
            }

        }
        public void read(SocketChannel channel) throws IOException {
            if (state==MAGIC) {
                int ret = channel.read(magic);
                if (ret == -1) {
                    state = EOF;
                    return;
                }
                if(magic.remaining() == 0) {
                    magic.clear();
                    if(!magicConstant.equals(magic)) {
                        log.error("invalid magic header found");
                        state = EOF;
                        return;
                    }
                    state = LEN;
                    lenBf.clear();
                }
            } else if(state == LEN) {
                int ret = channel.read(lenBf);
                if (ret == -1) {
                    state = EOF;
                    return;
                }
                if(lenBf.remaining()==0) {
                    lenBf.rewind();
                    len = lenBf.asIntBuffer().get();
                    state = DATA;
                    data .clear();

                }

            } else if (state==DATA) {
                int ret = channel.read(data);
                if (ret == -1) {
                    state = EOF;
                    return;
                }

                if(data.remaining()==0) {
//                    ByteBuffer bf = ByteBuffer.allocateDirect(4).putInt(len);
//                    bf.flip();
//                    responses.addLast(bf);
                    writable = true;
                    state = MAGIC;
                    magic.clear();
                }
            }
        }
    }
}
