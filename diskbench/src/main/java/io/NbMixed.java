package io;

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

import static sha.Utils.*;

public class NbMixed
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    private Selector selector;

    public static void main( String[] args ) {
        try {
            NbMixed obj = new NbMixed();
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
        selector = Selector.open();
        int num = 100;
        for (int i = 0; i < num; i++) {
            registerReader(8081+i);

        }
        Thread.sleep(1000);
        for (int i = 0; i < num; i++) {
            registerWriter(8081+i);

        }

        eventLoop();
    }

    public Timer timer = new Timer("trpt");
    public LatencyTimer lt = new LatencyTimer("lt");

    public void eventLoop() throws Exception {
        final ArrayBlockingQueue<Runnable> abq = new ArrayBlockingQueue<>(1000);
        int nThreads = 4;
        for (int i = 0; i < nThreads; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        try {
                            abq.take().run();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        while(true) {
            int numSelects = selector.select();
            if (numSelects == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                final SelectionKey key = iterator.next();
                key.interestOps(0);
                abq.put(new Runnable() {
                    @Override
                    public void run() {
                        State state = (State)key.attachment();
                        if(key.isValid() && key.isWritable() && state.type==Type.WRITER) {
                            if(state.bf.remaining()==0) {
                                state.bf.clear();
                                state.bf.putLong(0, System.nanoTime());
                            }
                            SocketChannel channel = (SocketChannel) key.channel();
                            try {
                                channel.write(state.bf);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if(key.isValid() && key.isReadable() && state.type==Type.READER) {
                            if(state.bf.remaining()==0) {
                                state.bf.clear();
                                lt.count(System.nanoTime() - state.bf.getLong(0));
                                timer.count();
                            }
                            SocketChannel channel = (SocketChannel) key.channel();
                            try {
                                channel.read(state.bf);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        key.interestOps(state.interestOps);
                        selector.wakeup();
                    }
                });
                iterator.remove();
            }
        }

    }

    public void registerWriter(int port) throws Exception{
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", port));

        // start the non blocking
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_WRITE, new State(Type.WRITER, SelectionKey.OP_WRITE));
    }

    public void registerReader(final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocketChannel ssc = ServerSocketChannel.open();
                    ssc.bind(new InetSocketAddress(port));
                    SocketChannel socketChannel = ssc.accept();
                    log.debug("accepted the conn");
                    // start the non-blocking

                    socketChannel.configureBlocking(false);

                    socketChannel.register(selector, SelectionKey.OP_READ, new State(Type.READER, SelectionKey.OP_READ));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private static class State {
        ByteBuffer bf = ByteBuffer.allocate(64);
        Type type;
        int interestOps;

        public State(Type type, int interestOps) {
            this.type = type;
            this.interestOps = interestOps;
        }
    }
    private enum Type {WRITER, READER}



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
