package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sha.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import static sha.Utils.*;

public class Server
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Server obj = new Server();
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
        Utils.Timer serverTimer = new Utils.Timer("serverTimer");
        Utils.Timer serverAck = new Utils.Timer("serverAck");
        serverTimer.die();
        serverAck.die();

        ArrayBlockingQueue<Long> abq = new ArrayBlockingQueue<Long>(1000000);
        ByteBuffer data = ByteBuffer.allocate(64);
        ByteBuffer ack = ByteBuffer.allocate(64);
        long pending = 0;
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ServerSocket socket = ssc.socket();
        socket.bind(new InetSocketAddress(8081));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        long sum = 0;
        while(true) {
            int numSelects = selector.select();
            if(numSelects==0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                SelectableChannel selectableChannel = key.channel();
                if (key.isValid() && key.isAcceptable()) {
                    log.debug("acceptable");
                    ServerSocketChannel channel = (ServerSocketChannel) selectableChannel;
                    SocketChannel socketChannel = channel.accept();
                    socketChannel.configureBlocking(false);
                    SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
//                    socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, clientSend);
//                    socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 1024*1024*16);
//                    socketChannel.socket().setTcpNoDelay(true);

                }
                if(key.isValid() && key.isReadable()) {

                    SocketChannel socketChannel = (SocketChannel) selectableChannel;
                    int read = socketChannel.read(data);
                    if(read == -1) {
                        socketChannel.close();
                        key.cancel();
                        iterator.remove();
                        continue;
                    }
                    if(data.remaining()==0) {
                        if(abq.offer(data.getLong(0))) {
                            data.clear();
//                            data = ByteBuffer.allocate(64);
                            pending++;
                            serverTimer.count();
                        }
                    }
                }
                if(key.isValid() && key.isWritable() && pending>0) {
                    SocketChannel socketChannel = (SocketChannel) selectableChannel;
                    try {
                        socketChannel.write(ack);
                    } catch (IOException e) {
                        socketChannel.close();
                        key.cancel();
                        iterator.remove();
                        continue;
                    }
                    if(ack.remaining()==0) {
                        ack.clear();
//                        ack = ByteBuffer.allocate(64);
                        ack.putLong(0, abq.poll());
                        pending--;
                        serverAck.count();
                    }
                }
//                for(int i=0; i<10_000; i++) {
//                    sum+=i;
//                }
//                if(sum == Integer.MAX_VALUE) {
//                    throw new Exception("unlucky");
//                }

                iterator.remove();
            }
        }
    }

}
