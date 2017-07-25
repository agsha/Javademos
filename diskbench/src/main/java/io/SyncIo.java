package io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sha.Recorder;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static sha.Utils.*;

public class SyncIo
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            SyncIo obj = new SyncIo();
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
    Recorder recorder = new Recorder(SyncIo.class.getSimpleName());
    Timer trpt = new Timer(recorder, "serverTrpt");
//    AtomicInteger pending = new AtomicInteger(0);
    LatencyTimer lt = new LatencyTimer(recorder, "serverLat");
    CyclicBarrier barrier;

    public void go() throws Exception {
        int cpus = Runtime.getRuntime().availableProcessors();
        List<Integer> threads = new ArrayList<>();
        for(int i=1; i<=cpus; i++) {
            threads.add(i);
        }
        for(int i=2*cpus; i<=2048; i*=2) {
            threads.add(i);
        }
        int count = 0;
        outer:for (Integer thread : threads) {
            for(long size=64; size<=1024*1024; size*=2) {
                if(thread*size > 1024*1024*1024) {
                    continue;
                }
                System.out.println();
                System.out.println();
                System.out.println("starting new run for thread="+thread+" size="+size);
                recorder.startRun(new MyRun(thread, (int)size));
                oneTest(thread, (int)size);
                count++;
                recorder.finish();

//                if(count==2) {
//                    break outer;
//                }


            }
        }

    }
    public void oneTest(int threads, final int size) throws Exception {
        barrier = new CyclicBarrier(2*threads+1);
        List<Thread> listOfThreads = new ArrayList<>();
        final int port = 9081;
        for ( int i=0; i<threads; i++) {
            final int finalI = i;
            Thread thread = new Thread(() -> {
                try {
                    goServer(port + finalI, size);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listOfThreads.add(thread);
            thread.start();
        }
        Thread.sleep(1000);
        for (int i=0; i<threads; i++) {
            final int finalI = i;
            Thread thread = new Thread(() -> {
                try {
                    goClientWriter(port + finalI, size);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listOfThreads.add(thread);
            thread.start();
        }
        barrier.await();
        Thread.sleep(45_000);
        listOfThreads.forEach(Thread::interrupt);
        for (Thread t : listOfThreads) {
            t.join();
        }
        Thread.sleep(2000);
    }


    private void goServer(int port, int size) throws IOException, BrokenBarrierException, InterruptedException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress("127.0.0.1", port));
        SocketChannel socketChannel = ssc.accept();
        ByteBuffer bf = ByteBuffer.allocate(size);
        long count = 0;
        long mask = (1<<12) - 1;
        barrier.await();

        while(true) {
            try {
                int read = socketChannel.read(bf);
                if(read == -1) {
                    socketChannel.close();
                    ssc.close();
                    return;
                }
            } catch(IOException e) {
                socketChannel.close();
                ssc.close();
                return;
            }
            if(bf.remaining()==0) {
                bf.clear();
                lt.count(System.nanoTime() - bf.getLong(0));
                trpt.count();
            }
            count++;
            if((count & mask) == 0 ) {
                if(Thread.interrupted()) {
                    socketChannel.close();
                    ssc.close();

                    return;
                }
            }
        }
    }


    private void goClientWriter(int port, int size) throws Exception {
        ByteBuffer bf = ByteBuffer.allocate(size);
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("127.0.0.1", port));
        long count = 0;
        long mask = (1<<12) - 1;
        barrier.await();
        while(true) {
            try {
                socketChannel.write(bf);
            } catch(IOException e) {
                socketChannel.close();
                return;
            }
            if(bf.remaining()==0) {
//                pending.incrementAndGet();
                bf.clear();
                bf.putLong(0, System.nanoTime());
            }
            count++;
            if((count & mask) == 0 ) {
                if(Thread.interrupted()) {
                    socketChannel.close();

                    return;
                }
            }

        }
    }
    public static class MyRun implements Serializable {
        public int threads = 0;
        public int size = 0;

        public MyRun(int threads, int size) {
            this.threads = threads;
            this.size = size;
        }

        // for objectmapper
        public MyRun() {
        }
    }

}
