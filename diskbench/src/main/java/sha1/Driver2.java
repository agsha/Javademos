package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static sha.Utils.*;

public class Driver2
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Driver2 obj = new Driver2();
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
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    server();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        Thread.sleep(1000);
        client();
    }
    private void server() throws Exception {
        ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(8899);
        while(true) {
            Socket s = ss.accept();
            InputStream is = s.getInputStream();
            while(true) {
                int x = is.read();
                System.out.println(x);
                if(x==-1) {
                    break;
                }
            }
            s.close();

        }
    }

    public void client() throws Exception {
        Socket s = SocketFactory.getDefault().createSocket("localhost", 8899);
        for (int i = 0; i < 4; i++) {
            s.getOutputStream().write('a');
            Thread.sleep(1000);
        }
        Thread.sleep(1000000);

    }
}
