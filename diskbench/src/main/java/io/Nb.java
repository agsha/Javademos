package io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import static sha.Utils.*;

public class Nb
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Nb obj = new Nb();
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
        int pairs = 800;
        for (int i = 0; i < pairs; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        server(8081+ finalI);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
        Thread.sleep(1000);
        for (int i = 0; i < pairs; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        client(8081+ finalI);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    public Timer timer = new Timer("trpt");
    public LatencyTimer lt = new LatencyTimer("lt");
    public void client(int port) throws Exception{
        ByteBuffer data = ByteBuffer.allocate(64);
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", port));

        // start the non blocking
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_WRITE);

        while(true) {
            int numSelects = selector.select();
            if (numSelects == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if(key.isValid() && key.isWritable()) {
                    socketChannel.write(data);
                    if(data.remaining()==0) {
                        data.clear();
                        data.putLong(0, System.nanoTime());
                    }
                }
                iterator.remove();
            }
        }



    }
    public void server(int port) throws Exception{
//        timer.die();
//        lt.die();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(port));
        SocketChannel socketChannel = ssc.accept();
        log.debug("accepted the conn");
        // start the non-blocking
        ByteBuffer data = ByteBuffer.allocate(64);

        socketChannel.configureBlocking(false);

        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);
        while(true) {
            int numSelects = selector.select();
            if (numSelects == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if(key.isValid() && key.isReadable()) {
                    socketChannel.read(data);
                    if(data.remaining()==0) {
                        timer.count();
                        lt.count(System.nanoTime()-data.getLong(0));
                        data.clear();
                    }
                }
                iterator.remove();
            }
        }

    }

}
