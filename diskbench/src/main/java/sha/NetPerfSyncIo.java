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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static sha.Utils.readJsonFromClasspath;

public class NetPerfSyncIo
{
    private static final Logger log = LogManager.getLogger();
    public static Settings s;
    public static String[] clientIps = "127.0.0.1".split(" ");

    public static void main( String[] args ) throws Exception{
        if (args.length==0) {
//            args = new String[]{"client", "server", "gentestcase"};
            args = new String[]{"gentestcase"};
        }



        String settings = "serverSettings.json";
//        if(args[0].equals("client")) {
//            settings = "clientSettings.json";
//        }
        NetPerfSyncIo obj = new NetPerfSyncIo();
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
            server.go();
        }

        if(Arrays.asList(args).indexOf("client") >=0 ) {
            Client client = new Client();
            client.go();
        }
        if(Arrays.asList(args).indexOf("gentestcase") >=0 ) {
            obj.genTestCase();
        }

    }

    private void genTestCase() throws JsonProcessingException {
        int[] threads = {1, 5, 10, 20, 30, 40};
        int[] msg = {1, 8, 64, 512, 1024, 2048, 4096, 8192, 32768};
        String[] tt = {"TCP_STREAM", "TCP_RR"};
        ObjectMapper m = new ObjectMapper();
        ArrayNode arrayNode = m.createArrayNode();
        for (String ttt : tt) {

            for (int msgg : msg) {
                for (int thread : threads) {

                    JsonNode node = m.createObjectNode()
                            .put("threads", thread)
                            .put("msg", msgg)
                            .put("tt", ttt)
                            .put("completed", false)
                            .set("netperf_op", m.createArrayNode());
                    arrayNode.add(node);
                }
            }
        }
        System.out.println(m.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode));
    }

    private void ggenTestCase() throws JsonProcessingException {
        int[] threads = {1};
        int[] msg = {1};
        String[] tt = {"TCP_STREAM"};
        ObjectMapper m = new ObjectMapper();
        ArrayNode arrayNode = m.createArrayNode();
        for (int thread : threads) {
            for (int msgg : msg) {
                for (String ttt : tt) {
                    JsonNode node = m.createObjectNode()
                            .put("threads", thread)
                            .put("msg", msgg)
                            .put("tt", ttt)
                            .put("completed", false)
                            .set("netperf_op", m.createArrayNode());
                    arrayNode.add(node);
                }
            }
        }
        System.out.println(m.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode));
    }

    public static class Settings {
        public String serverIp = "10.33.217.242";
        public int time = 20_000;
        // required for jackson
        public Settings() {
        }
    }


    public interface ServerRmi extends Remote {
        public void startListening(int threads, int buffer, String tt) throws RemoteException;

        void waitToFinish() throws RemoteException;

        void shutdown() throws RemoteException;
    }

    private static class Server implements ServerRmi {
        List<Thread> list = Collections.synchronizedList(new ArrayList<>());


        public void go() throws RemoteException, InterruptedException, AlreadyBoundException {
            ServerRmi stub = (ServerRmi)UnicastRemoteObject.exportObject(this, 0);
            Registry registry =  LocateRegistry.createRegistry(5000    );
            registry.bind("ServerRmi", stub);
            log.info("registered");
        }

        @Override
        public void startListening(int threads, int msg, String tt) throws RemoteException {
            list.clear();
            int port = 6000;
            for(int i=0; i<threads; i++) {
                int finalI = i;
                list.add(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(tt.equals("TCP_STREAM")) {
                            listenOnOneTcpStream(msg, port + finalI);
                        } else {
                            listenOnOneTcpRR(msg, port+finalI);
                        }
                    }
                }));
            }
            for (Thread thread : list) {
                thread.start();
            }
        }

        public void listenOnOneTcpRR(int msg, int port) {
            ServerSocketChannel ssc = null;
            try {
                ssc = ServerSocketChannel.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                ssc.bind(new InetSocketAddress(port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            SocketChannel socketChannel = null;
            try {
                socketChannel = ssc.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ByteBuffer bf = ByteBuffer.allocateDirect(msg);

            int read = 0;
            int written = 0;
            int readNow = 0;
            while(true) {
                read = 0;
                bf.clear();
                while(read < msg) {
                    try {
                        readNow = socketChannel.read(bf);
                        if(readNow == -1) {
                            closeServerSocket(socketChannel, ssc);
                            return;
                        }
                    } catch(IOException e) {
                        closeServerSocket(socketChannel, ssc);
                        return;
                    }
                    read += readNow;
                }

                bf.clear();
                try {
                    written = socketChannel.write(bf);
                    if(written != msg) {
                        closeServerSocket(socketChannel, ssc);
                        throw new RuntimeException(String.format("written:%d not equal to msg:%d", written, msg));
                    }
                } catch (IOException e) {
                    closeServerSocket(socketChannel, ssc);
                    return;
                }
            }
        }

        public void listenOnOneTcpStream(int msg, int port) {
            ServerSocketChannel ssc = null;
            try {
                ssc = ServerSocketChannel.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                ssc.bind(new InetSocketAddress(port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            SocketChannel socketChannel = null;
            try {
                socketChannel = ssc.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ByteBuffer bf = ByteBuffer.allocateDirect(msg);
            int read = 0;
            while(true) {
                try {
                    read = socketChannel.read(bf);
                } catch(IOException e) {
                    closeServerSocket(socketChannel, ssc);
                    return;
                }

                bf.clear();
                if(read == -1) {
                    closeServerSocket(socketChannel, ssc);
                    return;
                }
            }
        }

        @Override
        public void waitToFinish() throws RemoteException {
            for (Thread thread : list) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void shutdown() throws RemoteException {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            }).start();
        }
    }

    private static class Client {
        private ServerRmi server;
        ObjectMapper m = new ObjectMapper();
        Path p  = Paths.get(System.getProperty("user.home"), "java_netperf_tests/tests");

        public void go() throws IOException, NotBoundException, InterruptedException {
            this.server = (ServerRmi)LocateRegistry.getRegistry(s.serverIp, 5000).lookup("ServerRmi");
            log.debug("serverrmi on {}:{}", s.serverIp);
            ArrayNode root = (ArrayNode)m.readValue(Files.readAllBytes(p), JsonNode.class);
            List<Thread> list = new ArrayList<>();
            List<ClientRet> outputs = new ArrayList<>();

            for (JsonNode n : root) {
                ObjectNode node = (ObjectNode)n;
                if(node.get("completed").asBoolean()) {
                    continue;
                }
                String tt = node.get("tt").asText();
                int threads = node.get("threads").asInt();
                final int msg = node.get("msg").asInt();
                // just rest for two seconds
                Thread.sleep(2000);
                server.startListening(threads, msg, tt);
                Thread.sleep(2000); // let it start listening properly

                //start the freakin test

                //from port 6000 onwards
                list.clear();
                outputs.clear();
                for(int i=0; i<threads; i++) {
                    final int finalI = i;
                    list.add(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                if (tt.equals("TCP_STREAM")) {
                                    outputs.add(singleConnectionTcpStream(msg, 6000 + finalI));
                                } else {
                                    outputs.add(singleConnectionTcpRR(msg, 6000 + finalI));
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }));
                }

                for (Thread thread : list) {
                    thread.start();
                }

                for (Thread thread : list) {
                    thread.join();
                }

                //wait for server threads to die
                server.waitToFinish();
                ArrayNode ar = m.createArrayNode();
                for(int i=0; i<outputs.size(); i++) {
                    ClientRet cr = outputs.get(i);//1, 50, 75, 90, 95, 99, 99.9
                    String s1 = "threads,msg_size,trpt,trpt_units,duration,duration_units,p50,p75Nanos,p90Nanos,p95Nanos,p99Nanos,p99.9Nanos,maxNanos,count";
                    String s2 = String.format("%d,%d,%f,%s,%f,%s,%f,%f,%f,%f,%f,%f,%d,%d", threads, msg, cr.trpt, "bytes/sec", cr.secs, "seconds", cr.latRet.nanos[1], cr.latRet.nanos[2], cr.latRet.nanos[3], cr.latRet.nanos[4], cr.latRet.nanos[5], cr.latRet.nanos[6], cr.latRet.maxNanos, cr.latRet.total);
                    ar.add(s1+"\n"+s2);
                }
                node.set("netperf_op", ar);
                node.put("completed", true);
                Files.write(p, m.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes());
            }
            server.shutdown();
        }

        public ClientRet singleConnectionTcpRR(int msg, int port) throws InterruptedException {
            log.debug("came hereeeeeeeeee");
            ByteBuffer bf = ByteBuffer.allocate(msg);

            long written = 0;
            int writtenNow = 0;
            int read = 0;
            int readNow = 0;
            SocketChannel socketChannel = null;
            try {
                socketChannel = SocketChannel.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                log.debug("connecting to {}:{}", s.serverIp, port);
                socketChannel.connect(new InetSocketAddress(s.serverIp, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Thread.sleep(1000); // wait for server to enter the recieve loop
            long count = 0;
            long mask = (1<<12) - 1;
            LatencyTimerThreadUnsafe lat = new LatencyTimerThreadUnsafe();
            long start = System.currentTimeMillis();
            while(true) {

                bf.clear();
                try {
                    writtenNow = socketChannel.write(bf);
                } catch(IOException e) {
                    closeSocket(socketChannel);
                    double secs = (System.currentTimeMillis() - start)/1000.0;
                    return new ClientRet(((double)written)/secs, secs, lat.snap());
                }
                if(writtenNow != msg) {
                    closeSocket(socketChannel);
                    throw new RuntimeException(String.format("written:%d is not equal to msg_size:%d", written, msg));
                }
                written += writtenNow;

                read = 0;
                bf.clear();
                while(read < msg) {
                    try {
                        readNow = socketChannel.read(bf);
                        if(readNow == -1) {
                            closeSocket(socketChannel);
                            double secs = (System.currentTimeMillis() - start)/1000.0;
                            return new ClientRet(((double)written)/secs, secs, lat.snap());
                        }
                        read += readNow;
                    } catch (IOException e) {
                        closeSocket(socketChannel);
                        double secs = (System.currentTimeMillis() - start)/1000.0;
                        return new ClientRet(((double)written)/secs, secs, lat.snap());
                    }
                }
                lat.count();
                count++;

                if((count & mask) == 0 ) {
//                    log.debug("count {}", count);
                    long now = System.currentTimeMillis();
                    if(now - start > NetPerfSyncIo.s.time) {
                        closeSocket(socketChannel);
                        double secs = (System.currentTimeMillis() - start)/1000.0;

                        return new ClientRet(((double)written)/secs, secs, lat.snap());
                    }
                }
            }
        }
        public ClientRet singleConnectionTcpStream(int msg, int port) throws InterruptedException  {
            ByteBuffer bf = ByteBuffer.allocate(msg);
            long written = 0;
            SocketChannel socketChannel = null;
            try {
                socketChannel = SocketChannel.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                log.debug("connecting to {}:{}", s.serverIp, port);
                socketChannel.connect(new InetSocketAddress(s.serverIp, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Thread.sleep(1000); // wait for server to enter the recieve loop
            long count = 0;
            long mask = (1<<12) - 1;
            LatencyTimerThreadUnsafe lat = new LatencyTimerThreadUnsafe();
            long start = System.currentTimeMillis();
            long last = start ;
            long lastWritten = 0;
            while(true) {
                try {
                    written += socketChannel.write(bf);
                } catch(IOException e) {
                    closeSocket(socketChannel);
                    double secs = (System.currentTimeMillis() - start)/1000.0;
                    return new ClientRet(((double)written)/secs, secs, lat.snap());
                }
                lat.count();
                bf.clear();
                count++;
                if((count & mask) == 0 ) {
//                    log.debug("count {}", count);
                    long now = System.currentTimeMillis();
                    if(now - last > 2000) {
//                        System.out.println((int)((written-lastWritten)*8.0/(1000.0*(now-last))));
                        lastWritten = written;
                        last = now;
                    }
                    if(now - start > NetPerfSyncIo.s.time) {
                        closeSocket(socketChannel);
                        closeSocket(socketChannel);
                        double secs = (System.currentTimeMillis() - start)/1000.0;

                        return new ClientRet(((double)written)/secs, secs, lat.snap());
                    }
                }
            }
        }
        static class ClientRet {
            public double trpt;
            public double secs;
            public LatRet latRet;

            public ClientRet(double trpt, double secs, LatRet latRet) {
                this.trpt = trpt;
                this.secs = secs;
                this.latRet = latRet;
            }
        }
    }
    public  static void closeSocket(SocketChannel sc) {
        try {
            sc.close();
        } catch (IOException e1) {
            log.error("error while closing socket", e1);
        }

    }

    public  static void closeServerSocket(SocketChannel sc, ServerSocketChannel ssc) {
        try {
            sc.close();
        } catch (IOException e1) {
            log.error("error while closing socket", e1);
        }
        try {
            ssc.close();
        } catch (IOException e1) {
            log.error("error while closing server socket", e1);
        }

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
