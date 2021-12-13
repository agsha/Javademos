package io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import static sha.Utils.*;

public class SyncIoBidi
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            SyncIoBidi obj = new SyncIoBidi();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", dumps(s));
            obj.go();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }


    /**
     * All teh code from here:
     */
    Timer trpt;
    LatencyTimer lt;
//    Recorder recorder = new Recorder(SyncIo.class.getSimpleName());
//    Timer trpt = new Timer(recorder, "serverTrpt");
//    LatencyTimer lt = new LatencyTimer(recorder, "serverLat");


    public void go() throws Exception {
//        int cpus = Runtime.getRuntime().availableProcessors() / 2 ; // because we will be launching in pairs
//        List<Integer> threads = new ArrayList<>();
//        for(int i=1; i<=cpus; i++) {
//            threads.add(i);
//        }
//        for(int i=2*cpus; i<=2048; i*=2) {
//            threads.add(i);
//        }
//        int count = 0;
//        outer:for (Integer thread : threads) {
//            for(long size=10240; size<=10240; size*=2) {
//                for(long pending=1024; pending<=1024; pending*=4) {
//
//                    if(thread*size > 1024*1024*1024) {
//                        continue;
//                    }
//                    System.out.println();
//                    System.out.println();
//                    System.out.println("starting new run for thread="+thread+" size="+size);
//                    recorder.startRun(new MyRun(thread, (int)size, pending));
//                    oneTest(thread, (int)size, (int)size, pending);
//                    count++;
//                    recorder.finish();
//
//                }
//            }
//        }


        trpt = new Timer( "serverTrpt");
        lt = new LatencyTimer( "serverLat");

        oneTest(1, 1024, 128, 10240);

    }


    public void oneTest(int pairs, final int clientSize, int serverSize, final long pending) throws Exception {
        final int port = 10081;

        for (int i=0; i<pairs; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        goServer(port+ finalI, clientSize, serverSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        Thread.sleep(1000);
        for (int i=0; i<pairs; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        goClientWriter(port+ finalI, clientSize, serverSize, pending);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }


    private void goServer(int port, final int clientSize, final int serverSize) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress("127.0.0.1", port));
        log.debug("server started listening on port {}", port);
        final SocketChannel socketChannel = ssc.accept();
        final LinkedBlockingQueue<Long> abq = new LinkedBlockingQueue<>();
        ByteBuffer bf = ByteBuffer.allocate(clientSize);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer sb = ByteBuffer.allocate(serverSize);
                while (true) {
                    try {
                        busySpin();
                        sb.clear();
                        sb.putLong(0, abq.take());
                        while(sb.remaining()>0) {
                            socketChannel.write(sb);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void busySpin() {
                int a = 0;
                int count = 0;
                for(int i=0; i<10; i++) {
                    for(int j=0; j<clientSize; j++) {
                        count += bf.get(j);
                        a++;
                    }
                }
                if(count==Integer.MAX_VALUE) {
                    log.debug("unlucky");
                }
//                log.debug("spinning for {}", a);
            }
        }).start();
        while(true) {
            while(bf.remaining() > 0) {
                int ret = socketChannel.read(bf);
            }
            bf.clear();
            Long x = bf.getLong(0);
            abq.put(x);
        }
    }

    private void goClientWriter(int port, final int clientSize, final int serverSize, long pend) throws Exception {
        ByteBuffer bf = ByteBuffer.allocate(clientSize);
        final Semaphore pending = new Semaphore((int)pend);
        final SocketChannel socketChannel = SocketChannel.open();
        log.debug("trying connect on {} ", port);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", port));
        log.debug("finished connect");

        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer sb = ByteBuffer.allocate(serverSize);
                while (true) {
                    try {
                        while(sb.remaining() > 0) {
                            socketChannel.read(sb);
                        }
                        pending.release();
                        sb.clear();
                        lt.count(System.nanoTime()-sb.getLong(0));
                        trpt.count(clientSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        while(true) {
            pending.acquire();
            while(bf.remaining() > 0) {
                socketChannel.write(bf);
            }
            bf.clear();
            bf.putLong(0, System.nanoTime());
        }
    }

    public static class MyRun implements Serializable {
        public int threads = 0;
        public int size = 0;
        public long pending = 0;

        public MyRun(int threads, int size, long pending) {
            this.threads = threads;
            this.size = size;
            this.pending = pending;
        }

        // for objectmapper
        public MyRun() {
        }
    }

}
