package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static sha.NetPerf.ServerState.State.D;
import static sha.NetPerf.ServerState.State.H;
import static sha.Utils.LatencyTimer;
import static sha.Utils.readJsonFromClasspath;
import static sha.Utils.writeJson;

public class NetPerf
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    public static String serverIp = "127.0.0.1";
    public static String[] clientIps = "127.0.0.1".split(" ");

    public static void main( String[] args ) {
        if (args.length==0) {
            args = new String[]{"client", "server", "controller"};
        }



        String settings = "serverSettings.json";
        if(args[0].equals("client")) {
            settings = "clientSettings.json";
        }
        try {
            NetPerf obj = new NetPerf();
            try {
                s = readJsonFromClasspath(settings, Settings.class);
            } catch (Exception e) {
                log.error("settings.json not found on classpath", e);
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
//            obj.go();
//            obj.test();
            if(Arrays.asList(args).indexOf("server") >=0 ) {
                Server server = new Server();
                new Thread(server).start();
                ServerRmiImpl serverRmiImpl = new ServerRmiImpl(server);
                Registry registry =  LocateRegistry.createRegistry(5000);
                registry.bind("ServerRmi", serverRmiImpl);
                Thread.sleep(2000);
            }

            if(Arrays.asList(args).indexOf("client") >=0 ) {
                Registry registry =  LocateRegistry.createRegistry(5001);
                registry.bind("ClientRmi", new NioClient(1));
            }

            if(Arrays.asList(args).indexOf("controller") >=0 ) {
                Controller c = new Controller();
                new Thread(c).start();
            }


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





    public static class ServerReading implements Serializable{
        public List<LatencyTimer.LatRet> dataTx = new ArrayList<>(), qTime = new ArrayList<>(), sendTimer = new ArrayList<>(), firstByteTime = new ArrayList<>();
        public List<Utils.Timer.Ret> serverThroughput = new ArrayList<>();
        public void clear() {
            dataTx.clear();
            qTime.clear();
            sendTimer.clear();
            firstByteTime.clear();
            serverThroughput.clear();
        }

    }

    public static class ClientReading implements Serializable {

        public List<LatencyTimer.LatRet> latRet = new ArrayList<>();

    }
    public interface ServerRmi extends Remote {
        public void prepare(int bf, int clientSend, boolean waitPending) throws RemoteException;

        ServerReading snap() throws RemoteException;

        void shutdown() throws RemoteException;
    }
    public interface ClientRmi extends Remote {
        public ClientReading prepare(int bf, int clientSend, boolean waitPending) throws RemoteException;
    }
    public static class ServerRmiImpl extends UnicastRemoteObject implements  ServerRmi {
        public Server server;

        public ServerRmiImpl(Server server) throws RemoteException {
            this.server = server;
        }

        @Override
        public void prepare(int bf, int clientSend, boolean waitPending) {
            server.bf = bf;
            server.clientSend = clientSend;
            server.waitPending = waitPending;
            try {
                server.bq.put(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public ServerReading snap() {
            return server.snap();
        }

        @Override
        public void shutdown() {
            server.shutdown.set(true);

        }

    }

    public static class OverallReading {
        public int buffer;
        public int clientSend;
        public boolean wait;
        public List<ClientReading> clientReadings;
        public ServerReading serverReadings;

        public OverallReading(int buffer, int clientSend, boolean wait, List<ClientReading> clientReadings, ServerReading serverReadings) {
            this.buffer = buffer;
            this.clientSend = clientSend;
            this.wait = wait;
            this.clientReadings = clientReadings;
            this.serverReadings = serverReadings;
        }

        public OverallReading() {
        }
    }

    public static class Controller implements Runnable{

        private final ServerRmi serverRmi;
        private final List<ClientRmi> clientRmiList = new ArrayList<>();

        public Controller() throws RemoteException, NotBoundException {
            this.serverRmi = (ServerRmi)LocateRegistry.getRegistry(serverIp, 5000).lookup("ServerRmi");
            for(String ip : clientIps) {
                clientRmiList.add((ClientRmi)LocateRegistry.getRegistry(ip, 5001).lookup("ClientRmi"));
            }
        }

        @Override
        public void run() {
            ArrayList<OverallReading> list = new ArrayList();
            for (int b = 16*1024; b <= 32*1024; b*=2) {
                for(int clientSend=b*2; clientSend<=b*2; clientSend*=2) {
                    for(final boolean wait : new boolean[]{false, true}) {
                        log.debug("current run is bf:{} clientSend:{} wait:{}", b, clientSend, wait);

                        try {
                            serverRmi.prepare(b, clientSend, wait);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        log.debug("finished prep server");
                        final List<ClientReading> clientReadings = Collections.synchronizedList(new ArrayList<ClientReading>());
                        List<Thread> clientThreads = new ArrayList<>();
                        for (final ClientRmi clientRmi : clientRmiList) {
                            final int finalClientSend = clientSend;
                            final int finalB = b;
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        clientReadings.add(clientRmi.prepare(finalB, finalClientSend, wait));
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            t.start();
                            clientThreads.add(t);

                        }
                        for (Thread clientThread : clientThreads) {
                            try {
                                clientThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }


                        ServerReading serverReadings = null;
                        try {
                            serverReadings = serverRmi.snap();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        log.debug("finished snap server");

                        list.add(new OverallReading(b, clientSend, wait, clientReadings, serverReadings));
                        try {
                            Files.write(Paths.get(System.getProperty("user.home"), "netPerf"), writeJson(list).getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        log.debug("finished current run");
                    }
                }

            }
            try {
                serverRmi.shutdown();
            } catch (RemoteException e) {
                e.printStackTrace();
            }


        }
    }


    public static class NioClient extends UnicastRemoteObject implements ClientRmi {
        private static final Logger log = LogManager.getLogger();
        private int numClients;

        int bf, clientSend;
        boolean waitPending;
        LatencyTimer lt;


        public NioClient(int numClients) throws RemoteException {
            super();
            this.numClients = numClients;
        }
        @Override
        public ClientReading prepare(int bf, int clientSend, boolean waitPending) {
            this.bf = bf;
            this.clientSend = clientSend;
            this.waitPending = waitPending;
            final ClientReading reading = new ClientReading();
             this.lt = new LatencyTimer(new LatencyTimer.LatPrinter() {
                @Override
                public void log(String name, LatencyTimer.LatRet ret) {
                    if(ret.total == 0) return;
                    reading.latRet.add(ret);
                    log.debug("name:{}, {}",name, ret);
                }
            }, "endToEnd", 0, 20_000, 2000);
            lt.die();
            try {
                throwingRun();
                lt.die();
                return reading;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        public void throwingRun() throws Exception {

            Selector selector = Selector.open();

            for(int i=0; i<numClients;i++) {
                SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
//                sc.register(selector, SelectionKey.OP_CONNECT|SelectionKey.OP_READ|SelectionKey.OP_WRITE);
                sc.register(selector, SelectionKey.OP_CONNECT|SelectionKey.OP_WRITE);
                sc.connect(new InetSocketAddress(serverIp, 8081));
            }
            List<ClientState> clientStates = new ArrayList<>();
            int count = 0;
            long startTime = System.currentTimeMillis();
            boolean closing = false;
            int openConns = 0;
            outer:while(true) {

                count++;
                if(count%100000==0 && !closing && System.currentTimeMillis() - startTime > 30_000) {
                    closing = true;
                    for (ClientState clientState : clientStates) {
                        clientState.closing = true;
                    }

                }
                if(closing && openConns==0) {
                    return;
                }
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    ClientState clientState = (ClientState) key.attachment();

                    SocketChannel channel = (SocketChannel)key.channel();
                    if(key.isValid() && key.isConnectable()) {
                        channel.finishConnect();
                        clientState = new ClientState(key, lt, waitPending);
                        key.attach(clientState);
                        clientStates.add(clientState);
                        openConns++;
                        log.debug("incremented openconns: {}", openConns);

//                        if(clientRec > 0) {
//                            channel.setOption(StandardSocketOptions.SO_RCVBUF, clientRec);
//                        }
                        if(clientSend > 0) {
                            channel.setOption(StandardSocketOptions.SO_SNDBUF, clientSend);
                        }
//                        channel.socket().setTcpNoDelay(noDelay);

                        log.debug("tcp_nodelay:{}, rcvbuf:{} sndbuf:{}", channel.getOption(StandardSocketOptions.TCP_NODELAY), channel.getOption(StandardSocketOptions.SO_RCVBUF), channel.getOption(StandardSocketOptions.SO_SNDBUF));
                    }
                    if(key.isValid() && key.isWritable()) {
                        clientState.write(channel);
                    }
                    if (key.isValid()&&key.isReadable()) {
                        clientState.read(channel);
                    }
                    if(!key.isValid()) {
                        openConns--;
                        log.debug("decremented openconns: {}", openConns);
                    }

                    iterator.remove();
                }
            }
        }
    }
    public static void close(SelectionKey key) throws IOException {
        key.channel().close();
        key.attach(null);
        key.cancel();
    }
    public static void softClose(SelectionKey key) throws IOException {
        key.channel().close();
    }

    //    private static final int size = 1024;
    public static class ClientState {
//        Utils.Timer tp = new Utils.Timer("clientThrougput");
        SelectionKey key;
        private LatencyTimer lt;
        private boolean waitPending;
        ByteBuffer magic = ByteBuffer.wrap("daphne".getBytes());
        ByteBuffer lenBf = ByteBuffer.allocateDirect(4);
        ByteBuffer timeStamp = ByteBuffer.allocateDirect(8);
        ByteBuffer data = ByteBuffer.allocateDirect(s.bf);
        ByteBuffer[] ar = {magic, lenBf, timeStamp, data};

        ByteBuffer responseHeader = ByteBuffer.allocateDirect("daphne".getBytes().length);
        ByteBuffer responseTimestamp = ByteBuffer.allocateDirect(8);
        ByteBuffer[] respAr = {responseHeader, responseTimestamp};
        Utils.Timer clientTrpt = new Utils.Timer("clientTrpt");


        int pending = 1;
        int maxPending = 0;
        public boolean closing;

        public ClientState(SelectionKey key, LatencyTimer lt, boolean waitPending) {
            this.key = key;
            this.lt = lt;
            this.waitPending = waitPending;
            maxPending = waitPending?2:1000_1000;
            lenBf.putInt(0, s.bf);
        }


        public void write(SocketChannel socketChannel) throws IOException {


            if(data.remaining()==0) {
                if(closing) {
                    log.debug("client: not accepting write now.");
                    key.interestOps(key.interestOps()&~SelectionKey.OP_WRITE);
                    return;
                }
                magic.clear();
                lenBf.clear();
                data.clear();
                timeStamp.putLong(0, System.nanoTime());
                timeStamp.clear();
                pending++;
            }
//            if(pending<maxPending) {

                long ret = socketChannel.write(ar);
                clientTrpt.count(ret);
//            }
        }

        public void read(SocketChannel channel) throws IOException {
            log.debug("should never be called");
            long read = channel.read(respAr);
            if(responseTimestamp.remaining()==0) {
                responseHeader.clear();
                if(!responseHeader.equals(magicConstant)) {
                    throw new IOException("magic header not found");
                }
                lt.count(System.nanoTime()-responseTimestamp.getLong(0));
                responseTimestamp.clear();
                responseHeader.clear();
                pending--;
                if(closing &&pending%1000==0) {
                    log.debug("closing, waiting for pending: {}", pending);
                }
                if(pending == 0 && closing) {
                    close(key);
                    log.debug("client: closed key");
                }
            }
        }
    }
    static LatencyTimer dataTx = null;
    static LatencyTimer qTime = null;
    static LatencyTimer sendTimer = null;
    static LatencyTimer firstByteTime = null;
    static Utils.Timer serverThroughput = null;

    public static class Server implements Runnable {
        private final Selector selector;
        private final ServerSocket socket;
        AtomicBoolean shutdown = new AtomicBoolean(false);
        ServerSocketChannel ssc;
        public int bf, clientSend;
        public boolean waitPending;
        public BlockingQueue<Boolean> bq = new ArrayBlockingQueue<Boolean>(1);
        ServerReading serverReading = new ServerReading();
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
            List<SelectionKey> keys = new ArrayList<>();
            class Recorder implements LatencyTimer.LatPrinter {
                List<LatencyTimer.LatRet> list;

                public Recorder(List<LatencyTimer.LatRet> list) {
                    this.list = list;
                }

                @Override
                public void log(String name, LatencyTimer.LatRet ret) {
                    list.add(ret);
                    log.debug("name:{}, {}",name, ret);
                }
            }
            dataTx = new LatencyTimer(new Recorder(serverReading.dataTx), "dataTx", 0, 1000, 2000);
            qTime = new LatencyTimer(new Recorder(serverReading.qTime), "qTime", 0, 100, 2000);
            sendTimer = new LatencyTimer(new Recorder(serverReading.sendTimer), "sendTime", 0, 20_000, 2000);
            firstByteTime = new LatencyTimer(new Recorder(serverReading.firstByteTime), "firstByteTime", 0, 20_000, 2000);
            dataTx.die(); qTime.die(); sendTimer.die();firstByteTime.die();
            serverThroughput = new Utils.Timer(new Utils.Timer.Printer() {
                @Override
                public void log(String name, Utils.Timer.Ret ret) {
                    serverReading.serverThroughput.add(ret);
                    log.debug("name:{}, {}", name, ret);
                }
            }, "serverTrpt");


            while(true) {
                count++;
                if(count%1000==0) {
                    if(shutdown.get()) {
                        selector.close();
                        return;
                    }
                    if(bq.poll()!=null) {
                        serverReading.clear();
                        dataTx.reset();
                        qTime.reset();
                        sendTimer.reset();
                        firstByteTime.reset();
                        serverThroughput.reset();
                    }
                }
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
                    if(key.isValid() && key.isAcceptable()) {
                        log.debug("acceptable {}", count);
                        ServerSocketChannel channel = (ServerSocketChannel) selectableChannel;
                        SocketChannel socketChannel = channel.accept();
                        socketChannel.configureBlocking(false);
                        SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
                        keys.add(newKey);
                        serverState = new ServerState(newKey);
                        newKey.attach(serverState);
//                        if(clientRec > 0) {
//                            socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, clientRec);
//                        }
                        if(clientSend > 0) {
                            log.debug("setting rcvbuf on server to {}", clientSend);
                            socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, clientSend);
                        }
                        socketChannel.socket().setTcpNoDelay(true);
                        log.debug("tcp_nodelay:{}, rcvbuf:{} sndbuf:{}", socketChannel.getOption(StandardSocketOptions.TCP_NODELAY), socketChannel.getOption(StandardSocketOptions.SO_RCVBUF), socketChannel.getOption(StandardSocketOptions.SO_SNDBUF));

                        log.debug("accepted a connection.");
                    }
                    if(key.isValid() && key.isReadable()) {

                        SocketChannel socketChannel = (SocketChannel) selectableChannel;
                        serverState.read(socketChannel);
                    }
                    if(key.isValid() && key.isWritable()) {
                        SocketChannel socketChannel = (SocketChannel) selectableChannel;
                        serverState.write(socketChannel);
                    }

                    iterator.remove();

                }
            }
//            log.debug("server: bytes:{}, sum:{}", bytes, sum);

        }

        public ServerReading snap() {
            return serverReading;
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
        boolean closed = false;


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
                close(key);
                log.debug("server: closed key");
                return;
            } else {
                serverThroughput.count(ret);
            }

            if(state == H && timeStamp.remaining() == 0) {
                state = D;
                int bp = magic.position();
                int br = magic.remaining();
                magic.clear();
                toRead = lenBf.getInt(0);
                if(!magic.equals(magicConstant)) {
                    log.debug("yoooooo magic not found. position:{} remaining:{} bp:{} br:{} rec:{} toRead:{}", magic.position(), magic.remaining(), bp, br, rec, toRead);

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
//                t1.addLast(t);
//                t2.addLast(System.nanoTime());
            }
        }
        long sendTime = 0;
        public void write(SocketChannel channel) throws IOException {
            Long x = null;
            if((b1.remaining()==0) && t1.size()>0) {
                x = t1.pollFirst();
                if(x==null) {
                    if(closed) {
                        close(key);
                        log.debug("server finished writing responses, closing channel");
                    }
                    return;
                }
                b0.clear(); b1.clear();
                b1.putLong(0, x);

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
