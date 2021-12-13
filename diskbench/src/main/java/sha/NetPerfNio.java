package sha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class NetPerfNio implements ServerRmi, ClientRmi, GatewayRmi
{
    private static final Logger log = LogManager.getLogger();

    public NetPerfNio() throws UnknownHostException {
    }


    public static void main( String[] args ) throws Exception{
        NetPerfNio obj = new NetPerfNio();
//        obj.test();
//        obj.go(args);
        obj.genTestCase();
//        doTest();

    }


    private void test() throws Exception {
//        ClientRmi clientRmi = (ClientRmi) LocateRegistry.getRegistry("10.32.10.252", 5002).lookup("ClientRmi");
//        log.debug("{}", clientRmi.clientResult());

    }

    static void doTest() throws Exception {
        new Thread(() -> {
            try {
                NetPerfNio server = new NetPerfNio();
                server.go("type server time 10 serverReal 127.0.0.1:8000".split("\\s+"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        Thread.sleep(1000);


        int clients = 2;
        String a = "";

        for(int i=0; i<clients; i++) {
            int finalI = i;
            a+="127.0.0.1:"+(i+5002);
            if(i<clients-1) {
                a+=",";
            }
            new Thread(() -> {
                try {
                    NetPerfNio client1 = new NetPerfNio();
                    client1.go(String.format("type client serverReal 127.0.0.1:8000 clientRmiPort %d", (finalI +5002)).split("\\s+"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        log.debug("{}", a);

        Thread.sleep(1000);

        NetPerfNio gateway = new NetPerfNio();
        gateway.go(String.format("type gateway serverRmi 127.0.0.1:5001 clientRmis %s", a).split("\\s+"));

    }

    private static class IpPort {
        public String ip;
        public int port;

        public IpPort(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    private String type;
    private IpPort serverRmi;
    private IpPort serverReal;
    private IpPort[] clientRmis;
    private long timeMs = 10000;
    private int gatewayRmiPort = 5000, serverRmiPort = 5001, clientRmiPort = 5002;
    private String ipAddr = Inet4Address.getLocalHost().getHostAddress();

    public void go(String[] args) throws Exception {
        if (args.length==0) {
            throw new RuntimeException("Usage: java NetPerfNio type <client|gateway|server> clientRmis <ip:port>[,<ip:port>] serverRmi <ip:port> serverReal <ip:port> time <secs[default=10]> gatewayRmiPort <default:5000> serverRmiPort <default:5001> clientRmiPort<default:5002> ipAddr <ipaddress for rmi <optional)");
        }
        int argIndex = 0;
        while(argIndex < args.length) {
            String arg = args[argIndex];
            if(arg.equals("type")) {
                type = args[++argIndex];
                argIndex++;
            } else if(arg.equals("serverReal")) {
                serverReal =  parseIpPort(args[++argIndex], 8000)[0];
                argIndex++;
            }  else if(arg.equals("serverRmi")) {
                serverRmi = parseIpPort(args[++argIndex], serverRmiPort)[0];
                argIndex++;
            } else if(arg.equals("clientRmis")) {
                clientRmis = parseIpPort(args[++argIndex], clientRmiPort);
                argIndex++;
            } else if(arg.equals("time")) {
                timeMs = Integer.parseInt(args[++argIndex])*1000;
                argIndex++;
            } else if(arg.equals("gatewayRmiPort")) {
                gatewayRmiPort = Integer.parseInt(args[++argIndex]);
                argIndex++;
            } else if(arg.equals("serverRmiPort")) {
                serverRmiPort = Integer.parseInt(args[++argIndex]);
                argIndex++;
            } else if(arg.equals("clientRmiPort")) {
                clientRmiPort = Integer.parseInt(args[++argIndex]);
                argIndex++;
            } else if(arg.equals("ipAddr")) {
                ipAddr = (args[++argIndex]);
                argIndex++;
            } else {
                throw new RuntimeException("unknown arg: "+args[argIndex] );
            }
        }


        System.setProperty("java.rmi.server.hostname", ipAddr);

        if(type.equals("server")) {
            server();
        } else if(type.equals("client")) {
            client();
        } else if(type.equals("gateway")) {
            gateway();
        }
    }

    private void gateway() throws IOException, InterruptedException, AlreadyBoundException, NotBoundException {
        GatewayRmi stub = (GatewayRmi) UnicastRemoteObject.exportObject(this, 0);
        Registry registry = LocateRegistry.createRegistry(gatewayRmiPort);
        registry.bind("GatewayRmi", stub);
        log.info("registered gateway rmi");

        ServerRmi server = (ServerRmi) LocateRegistry.getRegistry(serverRmi.ip, serverRmi.port).lookup("ServerRmi");
        ClientRmi[] clientRmiStubs = new ClientRmi[clientRmis.length];
        for (int i = 0; i < clientRmis.length; i++) {
            IpPort c = clientRmis[i];
            clientRmiStubs[i] = (ClientRmi) LocateRegistry.getRegistry(c.ip, c.port).lookup("ClientRmi");
        }

        ObjectMapper m = new ObjectMapper();
        Path p = Paths.get(System.getProperty("user.home"), "java_netperf_tests/niotests");
        ArrayNode root = (ArrayNode) m.readValue(Files.readAllBytes(p), JsonNode.class);
        for (JsonNode n : root) {
            ObjectNode node = (ObjectNode) n;
            if (node.get("completed").asBoolean()) {
                continue;
            }
            log.info("gateway starting a test case");
            String testcase = m.writeValueAsString(node);
            server.serverInit(testcase, clientRmiStubs.length);

            List<ThrowingThread> ts = new ArrayList<>();
            for (ClientRmi c : clientRmiStubs) {
                ts.add(new ThrowingThread(() -> {
                    try {
                        c.clientInit(testcase);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            try {
                exec(ts);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            server.serverWaitForClientConnect();

            server.serverStartNioLoops();

            ts = new ArrayList<>();
            for (ClientRmi c : clientRmiStubs) {
                ts.add(new ThrowingThread(() -> {
                    try {
                        c.clientStart();
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            try {
                exec(ts);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }



            String result = server.serverResult(testcase);
            log.info("all clients returned");
            node.setAll(m.readValue(result, ObjectNode.class));
            ArrayNode arn = m.createArrayNode();
            for (ClientRmi rmi : clientRmiStubs) {
                String r = rmi.clientResult();
                arn.add(r);
            }
            node.set("clientRtts", arn);

            Files.write(p, m.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes());

            // just cool down for two seconds
            Thread.sleep(5000);
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();
        }

    }

    private void client() throws RemoteException, InterruptedException, AlreadyBoundException  {
        ClientRmi stub = (ClientRmi)UnicastRemoteObject.exportObject(this, 0);
        Registry registry =  LocateRegistry.createRegistry(clientRmiPort    );
        registry.bind("ClientRmi", stub);
        log.info("registered client rmi");

    }

    private volatile List<ThrowingThread> clientThreads;
    private volatile List<ClientState> clientStates;
    @Override
    public void clientInit(String testcaseJson) throws RemoteException {
        ObjectMapper m = new ObjectMapper();
        ObjectNode node = null;
        try {
            node = m.readValue(testcaseJson, ObjectNode.class);
        } catch (IOException e) {
            throw new RemoteException("error parsing json", e);
        }

        int connections = node.get("connectionsPerClient").asInt();
        clientThreads = Collections.synchronizedList(new ArrayList<>());
        clientStates = Collections.synchronizedList(new ArrayList<>());

        for(int i=0; i<connections; i++) {
            ObjectNode finalNode = node;
            int msg = node.get("msg").asInt();
            String tt = node.get("tt").asText();

            ByteBuffer bf = ByteBuffer.allocate(msg);
            SocketChannel socketChannel;
            try {
                socketChannel = SocketChannel.open();
                log.info("trying to connect on {}:{}", serverReal.ip, serverReal.port);
                socketChannel.connect(new InetSocketAddress(serverReal.ip, serverReal.port));
                log.info("{}/{} th connection established on on {}:{}", i, connections, serverReal.ip, serverReal.port);
            } catch (IOException e) {
                throw new RemoteException("", e);
            }

            final ClientState state = new ClientState();
            clientStates.add(state);
            if(tt.equals("TCP_STREAM")) {
                clientThreads.add(new ThrowingThread(() -> {
                    try {
                        singleConnectionTcpStream(finalNode, socketChannel, bf, state);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            } else {
                clientThreads.add(new ThrowingThread(() -> {
                    try {
                        singleConnectionTcpRr(finalNode, socketChannel, bf, state);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
        }
    }

    @Override
    public String clientResult() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        sb.append("bytes,durationMillis,p50,p75Nanos,p90Nanos,p95Nanos,p99Nanos,p99.9Nanos,maxNanos,count");

        for (ClientState state : clientStates) {
            Pair pair = toStat(state.rttLat.snap(), "");
            sb.append(String.format("\n%d,%d,%s", state.bytes, state.durationMillis, pair.s2));
        }
        return sb.toString();
    }

    private void singleConnectionTcpStream(ObjectNode node, SocketChannel socketChannel, ByteBuffer bf, ClientState state) throws IOException {
        long start = System.nanoTime();
        log.debug("client started yo! {}", System.nanoTime());
        while(true) {
            try {
                state.bytes += socketChannel.write(bf);
            } catch(IOException e) {
                state.durationMillis = (System.nanoTime() - start)/1000_000L;
                log.debug("client ended {} durationMillis:{}", System.nanoTime(), (System.nanoTime() - start)/1000_000L );
                socketChannel.close();
                break;
            }
            bf.clear();
        }
    }
//    AtomicInteger count = new AtomicInteger();

    private void singleConnectionTcpRr(ObjectNode node, SocketChannel socketChannel, ByteBuffer bf, ClientState state) throws IOException {
        long start = System.nanoTime();
        outer: while(true) {
            while(bf.hasRemaining()) {
                try {
                    state.bytes += socketChannel.write(bf);
                } catch (IOException e) {
                    state.durationMillis = (System.nanoTime() - start) / 1000_000L;
                    socketChannel.close();
                    log.info("closing on client side write :");
                    break outer;
                }
            }
            bf.clear();

            while(bf.hasRemaining()) {
                try {
                    long now = socketChannel.read(bf);
                    if(now == -1) {
                        state.durationMillis = (System.nanoTime() - start) / 1000_000L;
                        socketChannel.close();
                        log.info("closing on client side read -1: ");

                        break outer;
                    }
                } catch (IOException e) {
                    state.durationMillis = (System.nanoTime() - start) / 1000_000L;
                    socketChannel.close();
                    log.info("closing on client side read exception: ");

                    break outer;
                }
            }
            bf.clear();
            state.rttLat.count();
        }
    }

    @Override
    public void clientStart() throws RemoteException {
        log.info("starting all clients");
        try {
            exec(clientThreads);
        } catch (Throwable throwable) {
            throw new RemoteException("", throwable);
        }
    }

    private void server() throws RemoteException, InterruptedException, AlreadyBoundException {
        log.info("starting server");
        ServerRmi stub = (ServerRmi)UnicastRemoteObject.exportObject(this, 0);
        Registry registry =  LocateRegistry.createRegistry(serverRmiPort    );
        registry.bind("ServerRmi", stub);
        log.info("registered server rmi");
    }

    private List<ThrowingThread> loops = new ArrayList<>();
    private List<ServerNioLoop> serverNioLoops = new ArrayList<>();
    private List<State> states = new ArrayList<>();
    private ThrowingThread listeningThread;
    private volatile boolean isRunning = false;
    private CountDownLatch clientLatch;
    volatile boolean exitListen = false;

    @Override
    public void serverInit(String testcaseJson, int numClients) throws RemoteException{
        if(isRunning) {
            throw new RemoteException("already some test is running");
        }
        isRunning = true;
        exitListen = false;
        ObjectMapper m = new ObjectMapper();
        ObjectNode node = null;
        try {
            node = m.readValue(testcaseJson, ObjectNode.class);
        } catch (IOException e) {
            throw new RemoteException("error parsing json", e);
        }

        int connectionsPerClient = node.get("connectionsPerClient").asInt();
        int nioloops = node.get("nioloops").asInt();
        int msg = node.get("msg").asInt();
        if(node.get("time")!=null) {
            timeMs = node.get("time").asLong();
        }

        TestType tt;
        if(node.get("tt").asText().equals("TCP_STREAM")) {
            tt = TestType.TCP_STREAM;
        } else {
            tt = TestType.TCP_RR;
        }

        loops = Collections.synchronizedList(new ArrayList<>());
        log.info("resetting connections");
        states = Collections.synchronizedList(new ArrayList<>());
        serverNioLoops = Collections.synchronizedList(new ArrayList<>());
        ArrayList<Selector> selectors = new ArrayList<>();

        for(int i=0; i<nioloops; i++) {
            Selector selector = null;
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new RemoteException("", e);
            }

            selectors.add(selector);
            ServerNioLoop serverNioLoop = new ServerNioLoop(node, selector, timeMs);
            serverNioLoops.add(serverNioLoop);
            loops.add(new ThrowingThread(serverNioLoop));
        }


        CountDownLatch latch = new CountDownLatch(1);
        int tot = numClients*connectionsPerClient;
        clientLatch = new CountDownLatch(numClients*connectionsPerClient);
        listeningThread = new ThrowingThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocketChannel ssc = ServerSocketChannel.open();
                    ssc.configureBlocking(false);
                    Selector selector = Selector.open();
                    ServerSocket ss = ssc.socket();

                    ss.bind(new InetSocketAddress(serverReal.port), 10000);
                    ssc.register(selector, SelectionKey.OP_ACCEPT);
                    long start = System.nanoTime();
                    int currentSelector = 0;
                    log.info("server started listening on {}", serverReal.port);
                    latch.countDown();
                    int c = 0;

                    while (true) {
                        if (exitListen) {
                            log.info("exiting listen loop");
                            for (SelectionKey key : selector.keys()) {
                                key.channel().close();
                                key.cancel();
                            }
                            selector.close();
                            ss.close();
                            ssc.close();
                            break;
                        }
                        selector.select(1000L);
                        Set<SelectionKey> sk = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = sk.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            if (key.isValid() && key.isAcceptable()) {
                                ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                                SocketChannel socketChannel = channel.accept();
                                socketChannel.configureBlocking(false);
                                currentSelector %= selectors.size();
                                log.info("assigning client {}/{} to selector:{}", c++, tot, currentSelector);
                                State state = new State(ByteBuffer.allocateDirect(msg), System.nanoTime(), socketChannel.getRemoteAddress().toString());
                                states.add(state);
                                int interest = SelectionKey.OP_READ;
                                socketChannel.register(selectors.get(currentSelector++), interest, state);
                                clientLatch.countDown();
                            }
                            iterator.remove();
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException("", e);
                }
            }
        });
        listeningThread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RemoteException("", e);
        }
    }

    @Override
    public void serverState() throws RemoteException{
        for (ThrowingThread loop : loops) {
            if(loop.myUEH.e!=null) {
                throw new RemoteException("", loop.myUEH.e);
            }
        }
    }

    @Override
    public void serverWaitForClientConnect() throws RemoteException {
        try {
            clientLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }
    }

    @Override
    public void serverStartNioLoops() throws RemoteException {
        for (ThrowingThread loop : loops) {
            loop.start();
        }
    }

    static class ServerNioLoop implements Runnable {
        // input
        ObjectNode node;
        Selector selector;
        private long timeMs;

        //output
        LatencyTimerThreadUnsafe selectLat = new LatencyTimerThreadUnsafe();
        LatencyTimerThreadUnsafe readLat = new LatencyTimerThreadUnsafe();
        LatencyTimerThreadUnsafe writeLat = new LatencyTimerThreadUnsafe();
        LatencyTimerThreadUnsafe totalLat = new LatencyTimerThreadUnsafe();

        public ServerNioLoop(ObjectNode node, Selector selector, long timeMs) {
            this.node = node;
            this.selector = selector;
            this.timeMs = timeMs;
        }

        @Override
        public void run() {
            try {
                TestType tt;
                if(node.get("tt").asText().equals("TCP_STREAM")) {
                    tt = TestType.TCP_STREAM;
                } else {
                    tt = TestType.TCP_RR;
                }

                long start = System.nanoTime();
                long a = start;
                outer:
                while (true) {
                    long b = 0;
//                    if(b-a < 0) {
//                        System.out.println("fooooooooooo");
//                    }
                    long temp = System.nanoTime();

                    totalLat.count(temp - a);
                    a = temp;
                    selector.select(1000L);
                    b = System.nanoTime();

                    selectLat.count(b-a);
                    if (a - start > timeMs * 1000L * 1000L) {
                        log.info("exiting recieve loop");
                        int k=0;
                        for (SelectionKey key : selector.keys()) {
                            key.channel().close();
                            key.cancel();
                            k++;
                            Thread.sleep(20);
                        }
                        log.info("closed {} connections from server", k);
                        selector.close();
                        break;
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if(tt==TestType.TCP_STREAM) {
                            doTcpStream(key, b);
                        } else {
                            doTcpRr(key, b);
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                }
                throw new RuntimeException(e);
            }
        }

        private void doTcpRr(SelectionKey key, long b) throws IOException {
            SocketChannel ch = (SocketChannel) key.channel();
            State state = (State) key.attachment();
            if(state.reading && key.isReadable()) {
                readAndUpdateLatencies(state, ch, b);
            }
            maybeFlipState(state);

            if(!state.reading && (!state.writeInterest || key.isWritable()) ) {
                writeAndUpdateLatencies(state, ch, b, key);
            }
            maybeFlipState(state);
        }


        private void doTcpStream(SelectionKey key, long b) throws IOException {
            SocketChannel ch = (SocketChannel) key.channel();
            State state = (State) key.attachment();
            maybeFlipState(state);
            readAndUpdateLatencies(state, ch, b);
        }

        private void readAndUpdateLatencies(State state, SocketChannel ch, long b) throws IOException {
            while (true) {
                int readNow = 0;
                long c = System.nanoTime();
                readNow = ch.read(state.bf);
                long d = System.nanoTime();
                readLat.count(d-c);

                if (readNow == -1) {
                    throw new RuntimeException("why did the client close the stream?");
                }

                state.bytes += readNow;
                if (readNow == 0 || !state.bf.hasRemaining()) {
                    break;
                }
            }
        }

        private void writeAndUpdateLatencies(State state, SocketChannel ch, long b, SelectionKey key) throws IOException {
            if(state.writeInterest) {
                // reset the write interest
                key.interestOps(SelectionKey.OP_READ);
                state.writeInterest = false;
            }
            while (true) {
                int writeNow = 0;
                long c = System.nanoTime();
                writeNow = ch.write(state.bf);
                long d = System.nanoTime();
                writeLat.count(d-c);
                if(!state.bf.hasRemaining()) {
                    break;
                }
                // rare condition, in this case, reenable the write interest
                if (writeNow == 0) {
                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    state.writeInterest = true;
                    break;
                }
            }
        }

        private void maybeFlipState(State state) {
            if(!state.bf.hasRemaining()) {
                state.bf.clear();
                state.reading = !state.reading;
            }
        }
    }


    public String
    serverResult(String testcaseJson) throws RemoteException {
        if(!isRunning) {
            throw new RemoteException("Nothing is running");
        }
        ObjectMapper m = new ObjectMapper();
        ObjectNode node = null;
        try {
            node = m.readValue(testcaseJson, ObjectNode.class);
        } catch (IOException e) {
            throw new RemoteException("error parsing json", e);
        }

        for (ThrowingThread loop : loops) {
            try {
                loop.join();
            } catch (InterruptedException e) {
                throw new RemoteException("", e);
            }
            if(loop.myUEH.e!=null) {
                throw new RemoteException("", loop.myUEH.e);
            }
        }

        exitListen = true;

        try {
            listeningThread.join();
        } catch (InterruptedException e) {
            throw new RemoteException("", e);
        }
        if(listeningThread.myUEH.e!=null) {
            throw new RemoteException("", listeningThread.myUEH.e);
        }


        ArrayNode arn = m.createArrayNode();
        for (ServerNioLoop loop : serverNioLoops) {
            String s1 = "", s2 = "";
            Pair p = toStat(loop.selectLat.snap(), "selectLat_");
            s1 += p.s1+","; s2+=p.s2+",";

            p = toStat(loop.readLat.snap(), "readLat_");
            s1 += p.s1+","; s2+=p.s2+",";

            p = toStat(loop.writeLat.snap(), "writeLat_");
            s1 += p.s1+","; s2+=p.s2+",";

            p = toStat(loop.totalLat.snap(), "totalLat_");
            s1 += p.s1; s2+=p.s2;

            arn.add(s1+"\n"+s2);
        }

        node.set("serverNioLoops", arn);


//        if(states.size() != 2*node.get("connectionsPerClient").asInt()) {
//            throw new RuntimeException(String.format("states.size()=%d but expected 2*%d", states.size(), node.get("connectionsPerClient").asInt()));
//        }
        arn = m.createArrayNode();

        for (State state : states) {
            String s1 = "clientIp,durationMillis,bytes";
            String s2 = String.format("%s,%d,%d", state.clientIp, (System.nanoTime()-state.startNanos)/1000000L, state.bytes);
            arn.add(s1+"\n"+s2);
        }

        node.set("clients", arn);
//        node.put("completed", true);

        try {
            return m.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RemoteException("error while writing", e);
        } finally {
            isRunning = false;
        }
    }


    /////////////////////////////ALL CRAP HERE//////////////////////////////////

    enum TestType {
        TCP_STREAM, TCP_RR
    }

    class Pair {
        String s1;
        String s2;
        public Pair(String s1, String s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
    }

    private Pair toStat(LatRet latRet, String prefix) {
        DecimalFormat df = new DecimalFormat("###.##");

        String s1 = String.format("%sp50,%sp75Nanos,%sp90Nanos,%sp95Nanos,%sp99Nanos,%sp99.9Nanos,%smaxNanos,%scount", prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix);
        String s2 = String.format("%s,%s,%s,%s,%s,%s,%d,%d", df.format(latRet.nanos[1]), df.format(latRet.nanos[2]), df.format(latRet.nanos[3]), df.format(latRet.nanos[4]), df.format(latRet.nanos[5]), df.format(latRet.nanos[6]), latRet.maxNanos, latRet.total);
        return new Pair(s1, s2);

    }
    private void cleanupAfterTestCase(Selector selector, ServerSocketChannel ssc, ServerSocket ss, List<SelectionKey> selectionKeys, List<Selector> selectors) throws RemoteException {
        for (SelectionKey selectionKey : selectionKeys) {
            if(selectionKey!=null) {
                selectionKey.cancel();
                try {
                    selectionKey.channel().close();
                } catch (IOException e) {
                    throw new RemoteException("", e);
                }
            }
        }

        for (Selector selector1 : selectors) {
            if(selector1!=null) {
                try {
                    selector1.close();
                } catch (IOException e) {
                    throw new RemoteException("", e);
                }
            }
        }

        try {
            if(ssc!=null) {
                ssc.close();
            }
            if(selector!=null) {
                selector.close();
            }
        } catch (IOException e) {
            throw new RemoteException("", e);
        }
    }


    class NioLoopRet {
        long bytes;
        LatencyTimerThreadUnsafe lt = new LatencyTimerThreadUnsafe();
    }
    private class State {
        // common to both tcp_stream and tcp_rr
        public ByteBuffer bf;
        String clientIp = "";
        long startNanos=0;
        long bytes = 0;

        //used by tcp_rr
        boolean reading = true;
        boolean writeInterest = false;

        public State(ByteBuffer bf, long startNanos, String clientIp) {
            this.bf = bf;
            this.startNanos = startNanos;
            this.clientIp = clientIp;
        }
    }

    private class ClientState {
        long bytes, durationMillis;
        LatencyTimerThreadUnsafe rttLat = new LatencyTimerThreadUnsafe();
    }

    class ThrowingThread extends Thread {
        MyUEH myUEH;
        public ThrowingThread(Runnable target) {
            super(target);
            myUEH = new MyUEH();
            this.setUncaughtExceptionHandler(myUEH);
        }
    }

    class MyUEH implements Thread.UncaughtExceptionHandler {
        public Thread th;
        public Throwable e;
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            th = t;
            this.e = e;
        }
    }
    private void exec(List<ThrowingThread> ts) throws Throwable {
        for (ThrowingThread t : ts) {
            t.start();
        }
        for (ThrowingThread t : ts) {
            t.join();
        }

        for (ThrowingThread t : ts) {
            if(t.myUEH.e != null) {
                throw t.myUEH.e;
            }
        }
    }

    public IpPort[] parseIpPort(String str, int defaultPort) {
        String[] split = str.split(",");
        IpPort[] ret = new IpPort[split.length];

        for (int i=0; i<split.length; i++) {
            String s1 = split[i];
            if(s1.contains(":")) {
                String[] xx = s1.split(":");
                ret[i] = new IpPort(xx[0], Integer.parseInt(xx[1]));
            } else {
                ret[i] = new IpPort(s1, defaultPort);
            }
        }
        return ret;
    }

    void genTestCase() throws JsonProcessingException {
        int clients = 2;
//        int[] connectionsPerClients = {1000/clients, 1200/clients, 1400/clients, 1800/clients, 2500/clients, 5000/clients};
        int[] connectionsPerClients = {4/clients};
        int[] msg = {1024, 1024*4};
        int[] nioloops = {5, 10};
        String[] tt = {"TCP_RR", "TCP_STREAM"};
        ObjectMapper m = new ObjectMapper();
        ArrayNode arrayNode = m.createArrayNode();
        for (int nioloop : nioloops) {
            for (int msgg : msg) {
                for (int connectionsPerClient : connectionsPerClients) {
                        for (String ttt : tt) {

                            JsonNode node = m.createObjectNode()
                                .put("connectionsPerClient", connectionsPerClient)
                                .put("msg", msgg)
                                .put("tt", ttt)
                                .put("nioloops", nioloop)
                                .put("completed", false);
                        arrayNode.add(node);

                    }

                }
            }
        }
        System.out.println(m.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode));
    }

    private static class LatencyTimerThreadUnsafe {
        private static char noname = 'A';
        private final String name;
        public long[] bins = new long[4000];
        public long maxNanos;
        public long lastCount = 0;

        double[] pTiles = new double[]{1, 50, 75, 90, 95, 99, 99.9};




        public LatencyTimerThreadUnsafe() {
            this( ""+noname);
            noname += 1;
        }

        public LatencyTimerThreadUnsafe(Utils.LatencyTimer.LatPrinter p) {
            this("noname");
        }

        public LatencyTimerThreadUnsafe( String name) {
            this.name = name;
//            pTiles = new double[100];
//            for(int i=0; i<100; i++) {
//                pTiles[i] = i+1;
//            }
//            pTiles[99] = 99.9;
            reset();
        }


        public void count(long latencyNanos) {
            int index = 0;
            maxNanos = (Math.max(maxNanos, latencyNanos));
            while(latencyNanos >= 1000) {
                latencyNanos /= 1000;
                index+=1000;
            }


            bins[(int) Math.min(index + latencyNanos, bins.length-1)]++;
        }

        public void count() {
            long now = System.nanoTime();
            if(lastCount > 0) {
                count(now - lastCount);
            }
            lastCount = (now);
        }

        public void reset() {
            for(int i=0; i<bins.length; i++) {
                bins[i] = 0;
            }
            maxNanos = 0;
        }


        public LatRet snap() {
            long[] mybins = bins;
            long mytotal = 0;
            for(int i=0; i<bins.length; i++) {
                mytotal+=mybins[i];
            }
            long myMaxNanos = maxNanos;

            double[] nanos = new double[pTiles.length];
            int index = 0;
            long cumulative = 0;
            for(int i=0; i<pTiles.length; i++) {
                long max = (long)((mytotal*pTiles[i])/100.0);
                while(index < mybins.length && mybins[index] + cumulative <  max) {
                    cumulative+=mybins[index];
                    index++;
                }
//                log.debug("ptile:{} index:{}", pTiles[i], index);

                long mul = 1;
                int temp = index;
                while(temp >= 1000) {
                    temp -= 1000;
                    mul *= 1000;
                }
                nanos[i] = (temp+1)*mul;
            }
            reset();
            return new LatRet(mytotal, myMaxNanos, nanos, pTiles);
        }

        public static class LatDefaultPrinter implements Utils.LatencyTimer.LatPrinter {
            @Override
            public void log(String name, Utils.LatencyTimer.LatRet ret) {
                log.debug("{}, {}",name, ret);
            }
        }
    }

    private static class LatRet implements Serializable {
        public double[] nanos;
        public double[] pTiles;
        public long total;
        public long maxNanos;
        public long snapTimeMillis;

        @Override
        public String toString() {
            if(total==0) {
                return "No data points";
            }
            DecimalFormat df = new DecimalFormat("###.##");
            String s = String.format("max:%s", timeFormat(maxNanos, df));
            for(int i = nanos.length-1; i>=0; i--) {
                s+=pTiles[i]+"%:"+timeFormat(nanos[i], df);
            }
            return s;
        }

        public static String timeFormat(double t, DecimalFormat df) {
            if(t<1000) {
                return df.format(t)+"ns ";
            } else if (t<1000_000) {
                return df.format(t/1000)+"us ";
            } else if (t<1000_000_000){
                return df.format(t/1000_000)+"ms ";
            } else {
                return df.format(t/1000_000_000)+"s ";
            }
        }
        //for objectmapper
        public LatRet(){}
        public LatRet(long total, long maxNanos, double[] nanos, double[] pTiles) {
            this.nanos = nanos;
            this.pTiles = pTiles;
            this.total = total;
            this.maxNanos = maxNanos;
            this.snapTimeMillis = System.currentTimeMillis();
        }
    }
}

interface ServerRmi extends Remote {
    void serverInit(String testcaseJson, int numClients) throws RemoteException;
    void serverWaitForClientConnect() throws RemoteException;
    String serverResult(String testcaseJson) throws RemoteException;
    void serverStartNioLoops() throws RemoteException;
    void serverState() throws RemoteException;
}

interface ClientRmi extends Remote {
    void clientInit(String testcase) throws RemoteException;
    void clientStart() throws RemoteException;
    String clientResult() throws RemoteException;
}
interface GatewayRmi extends Remote {

}
