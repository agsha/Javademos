package io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void go() throws Exception {
        final int port = 8081;
        int pairs = 100;
        for ( int i=0; i<pairs; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        goServer(port+ finalI);
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
                        goClientWriter(port+ finalI);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    Timer trpt = new Timer("serverTrpt");
    LatencyTimer lt = new LatencyTimer("serverLat");

    private void goServer(int port) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress("127.0.0.1", port));
        SocketChannel socketChannel = ssc.accept();
        ByteBuffer bf = ByteBuffer.allocate(64);


        while(true) {
            socketChannel.read(bf);
            if(bf.remaining()==0) {
                bf.clear();
                lt.count(System.nanoTime() - bf.getLong(0));
                trpt.count();
            }


        }
    }


    AtomicInteger pending = new AtomicInteger(0);
    private void goClientWriter(int port) throws Exception {
        ByteBuffer bf = ByteBuffer.allocate(64);
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("127.0.0.1", port));
        Timer trpt = new Timer("clientTrpt");
        trpt.die();
        while(true) {
            socketChannel.write(bf);
            if(bf.remaining()==0) {
                pending.incrementAndGet();
                bf.clear();
                bf.putLong(0, System.nanoTime());
                trpt.count();
            }

        }


    }

}
