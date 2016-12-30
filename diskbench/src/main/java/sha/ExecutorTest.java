package sha;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static sha.Utils.readJsonFromClasspath;

public class ExecutorTest
{
//    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    final Utils.Timer sha = new Utils.Timer("sha");
//    final Utils.LatencyTimer lt = new Utils.LatencyTimer("lt");

    public static void main( String[] args ) {
        try {
            ExecutorTest obj = new ExecutorTest();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
//                log.warn("settings.json not found on classpath");
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
        } catch (Exception e) {
//            log.error("", e);
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
    public AtomicInteger cs = new AtomicInteger(0);
    Wrap wrap = new Wrap();
    private void go() throws Exception {
//        sha.die();
//        lt.die();
        int n = 1000;
        final ArrayBlockingQueue<Wrap> abq = new ArrayBlockingQueue<>(1);
        for (int i = 0; i < n; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("taker");

                    try {
                        while (true) {
                            abq.take();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
//                long last = System.nanoTime();

                @Override
                public void run() {
                    int count = 0;
                    Thread.currentThread().setName("taker");
                    while(true) {
                        try {
                            abq.put(wrap);
                            sha.count();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();

        }

    }

    public static class Wrap {
        public int count = 0;

    }
}
