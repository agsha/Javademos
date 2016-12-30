package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sha.Utils;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

import static sha.Utils.dumps;
import static sha.Utils.readJsonFromClasspath;

public class Client
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Client obj = new Client();
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
        ByteBuffer data = ByteBuffer.allocate(64);
        ByteBuffer ack = ByteBuffer.allocate(64);
        Utils.Timer clientTimer = new Utils.Timer("clientTimer");
        clientTimer.die();

        Utils.Timer clientAck = new Utils.Timer("clientAck");
//        clientAck.die();
        long pending = 0;

        Utils.LatencyTimer clientEndToEnd = new Utils.LatencyTimer("clientE2E");






        Selector selector = Selector.open();
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_CONNECT|SelectionKey.OP_WRITE|SelectionKey.OP_READ);
        sc.connect(new InetSocketAddress("localhost", 8081));
//        sc.connect(new InetSocketAddress("10.33.57.199", 8081));
        long timestamp = Long.MAX_VALUE;
//        Utils.LatencyTimer t6 = new Utils.LatencyTimer("t6", 0, 50, 2000);
//        Utils.LatencyTimer t8 = new Utils.LatencyTimer("t8", 0, 50, 2000);
        long now = System.nanoTime();
        while(true) {
            int rr = selector.select();
            if(rr==0) continue;
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                SocketChannel channel = (SocketChannel)key.channel();

                if(key.isValid() && key.isConnectable()) {
                    channel.finishConnect();
                    log.debug("finished connect");
//                    channel.setOption(StandardSocketOptions.SO_SNDBUF, clientSend);
//                    channel.setOption(StandardSocketOptions.SO_RCVBUF, 1024*1024*16);
//                        channel.socket().setTcpNoDelay(true);
                }

                if(key.isValid() && key.isReadable()) {

                    channel.read(ack);

                    if(ack.remaining()==0) {
                        clientEndToEnd.count(System.nanoTime()-ack.getLong(0));

                        ack.clear();
                        clientAck.count();
                        pending--;

                    }
                }

                if( pending < Driver.pending && key.isValid() && key.isWritable()) {

//                    now = System.nanoTime();
                    int ret = channel.write(data);
//                    t8.count(System.nanoTime()-now);

                    if(data.remaining()==0) {
                        data.clear();
                        pending++;
                        clientTimer.count();
                        data.putLong(0, System.nanoTime());
                    }

                }
                int sum = 0;
                iterator.remove();

            }
        }

    }

}
