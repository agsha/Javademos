package mixed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static sha.Utils.*;

public class MixedFullyConcurrent
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    private Selector selector;

    public static void main( String[] args ) {
        try {

            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", dumps(s));
            for (int i = 0; i < 1; i++) {
                final int k = i;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new MixedFullyConcurrent().go(9091+k);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        } catch (Exception e) {
            log.error("sd", e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }
    public Phaser phaser = new Phaser();

    /**
     * All teh code from here:
     */
    LatencyTimer lt = new LatencyTimer("waitTime");

    Timer clientRx = new Timer("clientRx");

    private void go(int pp) throws Exception {
//        clientRx.die();
//        lt.die();
        Thread.currentThread().setName("main_thread");
        selector = Selector.open();
        int t = 1;
        int n = 6;
        final ArrayBlockingQueue<Runnable> abq = new ArrayBlockingQueue<>(100000);
        for (int i = 0; i < n; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            abq.take().run();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Worker_Thread_"+i).start();
        }

        for (int i = 0; i < t; i++) {
            registerServer(pp+i);
        }
        Thread.sleep(2000);
        for (int i = 0; i < t; i++) {
            registerClient(pp+i);
        }


        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
        int count = 0;
        phaser.register();
        while(true) {
            int numSelects = selector.select();
            if (numSelects == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();


            while (iterator.hasNext()) {
                final SelectionKey key = iterator.next();
                if (key.isValid() && key.isReadable()) {

                    phaser.register();
                    abq.put(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                process(key, Action.READ);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        }
                    });
//                    process(key, Action.READ);
//                    phaser.arriveAndDeregister();
                }
                if (key.isValid() && key.isWritable()) {
                    phaser.register();
                    abq.put(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                process(key, Action.WRITE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        }
                    });
//                    process(key, Action.WRITE);
//                    phaser.arriveAndDeregister();
                }
                iterator.remove();
            }
            phaser.arriveAndAwaitAdvance();
        }
    }

    public void registerClient(int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", port));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE, new Data(Daemon.CLIENT));
    }

    public void registerServer(final int port) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocketChannel ssc = null;
                try {
                    ssc = ServerSocketChannel.open();
                    ssc.bind(new InetSocketAddress("localhost", port));
                    SocketChannel socketChannel = ssc.accept();
                    socketChannel.configureBlocking(false);

                    socketChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE, new Data(Daemon.SERVER));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        Thread.sleep(2000);

    }
    public enum Action {
        READ, WRITE
    }

    public enum Daemon {
        CLIENT, SERVER
    }
    int PENDING = 10000000;


    public class Data {
        public Daemon d;
        public ByteBuffer sb = ByteBuffer.allocate(64);
        public ByteBuffer rb = ByteBuffer.allocate(64);
        AtomicInteger pending = new AtomicInteger(-1);
        ArrayBlockingQueue<Long> q = new ArrayBlockingQueue<Long>(10000);

        public Data(Daemon d) {
            this.d = d;
        }
    }

    void process(SelectionKey key, Action action) throws Exception {

        Data data = (Data)key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if(data.d == Daemon.CLIENT && action== Action.WRITE) {

            if(data.pending.get()>=PENDING) {
                return;
            }
            if(data.sb.remaining()==0) {
                data.pending.incrementAndGet();
                data.sb.clear();
                data.sb.putLong(0, System.nanoTime());
            }
            channel.write(data.sb);
        } else if(data.d== Daemon.CLIENT && action== Action.READ) {

            if(data.rb.remaining()==0) {
                clientRx.count();
                data.pending.decrementAndGet();
                data.rb.clear();
                lt.count(System.nanoTime()-data.rb.getLong(0));
            }
            int ret = channel.read(data.rb);
        } else if(data.d== Daemon.SERVER && action== Action.READ) {

            if(data.rb.remaining()==0) {
//                if(data.q.offer(data.rb.getLong(0))) {
//                    data.rb.clear();
//                }
                lt.count(System.nanoTime()-data.rb.getLong(0));

                data.rb.clear();

            }
            channel.read(data.rb);

        } else if(data.d== Daemon.SERVER && action== Action.WRITE) {

//            Long ts= data.q.poll();
//            if(ts==null) return;
            if(data.sb.remaining()==0) {
                data.sb.clear();
//                data.sb.putLong(0, ts);
                data.sb.putLong(0, System.nanoTime());
            }
            channel.write(data.sb);
        } else {
            log.debug("inknown operation");
        }


    }
}
