package nio2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import static sha.Utils.*;

public class AsyncNio2
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            AsyncNio2 obj = new AsyncNio2();
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
    private void go() throws Exception {
        AsynchronousServerSocketChannel server =
                AsynchronousServerSocketChannel.open().bind(new InetSocketAddress("localhost", 8081));
        AsynchronousSocketChannel serverChannel = server.accept().get();
        Thread.sleep(2000);
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        client.connect(new InetSocketAddress("localhost", 8081)).get();

        final ByteBuffer clientBuffer = ByteBuffer.allocate(64);
        Semaphore pending = new Semaphore(4);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                AsynchronousSocketChannel channel = null;
//                ByteBuffer sb = ByteBuffer.allocate(64);
//                try {
//                    channel = acceptFuture.get();
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }
//                while(true) {
//                    try {
//                        channel.read(sb).get();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    }
//                    if(sb.remaining()==0) {
//                        ByteBuffer ack = ByteBuffer.allocate(64);
//                        ack.putLong(0, sb.getLong(0));
//                        channel.write(ack);
//                    }
//
//                }
//            }
//        }).start();
//        AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        client.connect(server.getLocalAddress()).get();
        ByteBuffer cb = ByteBuffer.allocate(64);
    }

}
