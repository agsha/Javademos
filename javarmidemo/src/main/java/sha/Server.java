package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static sha.Utils.prettifyJson;
import static sha.Utils.readJsonFromClasspath;

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
            log.info("Using settings:{}", prettifyJson(s));
            obj.server();
        } catch (Exception e) {
            log.error("foo", e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }

        @Override
        public String toString() {
            return "";
        }
    }


    /**
     * All teh code from here:
     */
    HelloImpl hi;
    private void server() throws Exception {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(5000);
//                    System.out.println("unexporting object");
//                    final boolean b = UnicastRemoteObject.unexportObject(hi, false);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (NoSuchObjectException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

        Registry registry = LocateRegistry.createRegistry(19838);
        hi = new HelloImpl();
        Hello stub = (Hello) UnicastRemoteObject.exportObject(hi, 0);
        registry.rebind(Hello.class.getName(), stub);


        registry.bind("hello", hi);

    }

}
