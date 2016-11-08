package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sha.Utils;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

import static sha.NetPerf.serverIp;
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
        ByteBuffer data = ByteBuffer.allocateDirect(1024*16);
        ByteBuffer ack = ByteBuffer.allocateDirect(1);
        Utils.Timer clientTimer = new Utils.Timer("clientTimer");
        clientTimer.die();

        Utils.Timer clientAck = new Utils.Timer("clientAck");
//        clientAck.die();
        long pending = 0;

        Utils.LatencyTimer clientEndToEnd = new Utils.LatencyTimer("clientE2E", 0, 100, 2000);






        Selector selector = Selector.open();
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_CONNECT|SelectionKey.OP_WRITE|SelectionKey.OP_READ);
        sc.connect(new InetSocketAddress("127.0.0.1", 8081));
        long timestamp = Long.MAX_VALUE;

        while(true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();
                SocketChannel channel = (SocketChannel)key.channel();
                if(key.isValid() && key.isConnectable()) {
                    channel.finishConnect();
                    log.debug("finished connect");
//                    channel.setOption(StandardSocketOptions.SO_SNDBUF, clientSend);
                        channel.socket().setTcpNoDelay(true);

                }
                if(key.isValid() && key.isReadable()) {
                    int ret = channel.read(ack);
                    if(ret > 0) {
                        ack.clear();
                        clientAck.count();
                        pending--;
                        clientEndToEnd.count(System.nanoTime()-timestamp);

                    }
                }
                if(pending ==0 && key.isValid() && key.isWritable()) {
                    if(data.remaining()==0) {
                        data.clear();
                        pending++;
                        timestamp = System.nanoTime();
                    }
                    int ret = channel.write(data);
                    clientTimer.count(ret);
                }
                iterator.remove();

            }
        }

    }

}
