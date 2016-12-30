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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static sha.Utils.*;

public class NbEventloopPool
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            NbEventloopPool obj = new NbEventloopPool();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", dumps(s));
            obj.go();
        } catch (Exception e) {
            log.error("", e);
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
        int poolSize = 4;
        int conns = 100;
        final List<Selector> selectorList = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            final Selector selector = Selector.open();
            selectorList.add(selector);
        }
        for (int i = 0; i < conns; i++) {
            registerReader(8081+i, selectorList.get(i%selectorList.size()));
        }
        Thread.sleep(1000);
        for (int i = 0; i < conns; i++) {
            registerWriter(8081+i, selectorList.get((i)%selectorList.size()));
        }
        for (int i = 0; i < selectorList.size(); i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        eventLoop(selectorList.get(finalI));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    public Timer timer = new Timer("trpt");
    public LatencyTimer lt = new LatencyTimer("lt");

    public void eventLoop(Selector selector) throws Exception {
        while(true) {
            int numSelects = selector.select();
            if (numSelects == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                final SelectionKey key = iterator.next();
                State state = (State)key.attachment();
                if(key.isValid() && key.isWritable() && state.type== Type.WRITER) {
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
                if(key.isValid() && key.isReadable() && state.type== Type.READER) {
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
                iterator.remove();
            }
        }

    }

    public void registerWriter(int port, Selector selector) throws Exception{
        SocketChannel socketChannel = SocketChannel.open();
        log.debug("trying to connect to port: {}", port);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", port));

        // start the non blocking
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_WRITE, new State(Type.WRITER, SelectionKey.OP_WRITE));
        log.debug("fin registering writer");

        selector.wakeup();
    }

    public void registerReader(final int port, final Selector selector) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocketChannel ssc = ServerSocketChannel.open();
                    ssc.bind(new InetSocketAddress(port));
                    SocketChannel socketChannel = ssc.accept();
                    // start the non-blocking

                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ, new State(Type.READER, SelectionKey.OP_READ));

                    selector.wakeup();
                    log.debug("connected on {}", port);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private static class State {
        ByteBuffer bf = ByteBuffer.allocate(16*1024);
        Type type;
        int interestOps;

        public State(Type type, int interestOps) {
            this.type = type;
            this.interestOps = interestOps;
        }
    }
    private enum Type {WRITER, READER}

}
