package io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import static sha.Utils.*;

public class SyncIoBidiUnrelated
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            SyncIoBidiUnrelated obj = new SyncIoBidiUnrelated();
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
        int pairs = 4;

        for (int i=0; i<pairs; i++) {
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


    final Timer trpt = new Timer("trpt");
    final LatencyTimer lt = new LatencyTimer("lt");
    private void goServer(int port) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress("localhost", port));
        final SocketChannel socketChannel = ssc.accept();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer sb = ByteBuffer.allocate(64);
                while (true) {
                    try {
                        sb.clear();
                        sb.putLong(0, System.nanoTime());
                        while(sb.remaining()>0) {
                            socketChannel.write(sb);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        ByteBuffer bf = ByteBuffer.allocate(64);


        long now = System.nanoTime();
        while(true) {
            while(bf.remaining() > 0) {
                int ret = socketChannel.read(bf);
            }
            bf.clear();
            lt.count(System.nanoTime()-bf.getLong(0));
            trpt.count();
        }
    }

    private void goClientWriter(int port) throws Exception {
        ByteBuffer bf = ByteBuffer.allocate(64);
        final Semaphore pending = new Semaphore(100000000);
        final SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", port));
        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer sb = ByteBuffer.allocate(64);
                while (true) {
                    try {
                        while(sb.remaining() > 0) {
                            socketChannel.read(sb);
                        }
                        lt.count(System.nanoTime()-sb.getLong(0));
                        pending.release();
                        trpt.count();
                        sb.clear();
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
}
