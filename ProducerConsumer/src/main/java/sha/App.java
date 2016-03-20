package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static sha.Utils.*;

public class App
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            App obj = new App();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", prettifyJson(s));
            obj.go();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static class Settings {
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
    private void go() throws Exception {
        BlockingQueue<Integer> q = new ArrayBlockingQueue<>(10000000);
        Timer t = new Timer("mytimer");
        t.enabled.set(false);
        for(int i=0; i<30; i++) {
            Thread th = new Thread(new Consumer(q, t));
            th.setDaemon(true);
            th.start();

        }
        t.enabled.set(true);
        for(long i=0; i<10000000000L; i++) {
            q.put(random(0, 101));
        }
    }

    public static class Consumer implements Runnable{

        BlockingQueue<Integer> q;
        private Utils.Timer t;

        public Consumer(BlockingQueue<Integer> q, Utils.Timer t) {
            this.q = q;
            this.t = t;
        }

        public void run() {
            while(true) {
                try {
                    q.take();
                    t.count();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
