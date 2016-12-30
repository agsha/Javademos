package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static sha.Utils.*;

public class SharedSynchronized
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            SharedSynchronized obj = new SharedSynchronized();
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
    static HighChartTrpt hs;
    static Timer t;
    public static final Object lock = new Object();

    public List<Runner> runners = new ArrayList<>();

    private void go() throws Exception {
        hs = new HighChartTrpt(this.getClass().getSimpleName());
        t = new Timer(hs, "mytimer");

        List<Integer> runs = new ArrayList<>();
        int cpus = Runtime.getRuntime().availableProcessors();
        for(int i=0; i<cpus; i++) {
            runs.add(i+1);
        }
        for(int i=2*cpus; i<=2048; i*=2) {
            runs.add(i);
        }

        System.out.println("runs is "+runs.toString());

        for(int threads : runs) {
            System.out.println();
            System.out.println();
            System.out.println("new run with threads = "+threads);
            runners.clear();


            List<Thread> ts = new ArrayList<>(threads);
            hs.startRun(threads);
            for(int i=0; i<threads; i++) {
                Runner r = new Runner();
                runners.add(r);
                Thread tt = new Thread(r);
                ts.add(tt);
                tt.start();
            }
            for (Thread thread : ts) {
                thread.join();
            }

        }

        hs.finish();
    }

    public static class Runner implements Runnable{
        @Override
        public void run() {
            long last = System.nanoTime();
            final long mask = (1L << 15) - 1;
            long temp = 0;

            while(true) {
                synchronized (lock) {
                    temp++;
                }
                if((temp & mask) == 0) {
                    t.count(mask);
                    long now = System.nanoTime();
                    if(now - last >= 60_000_000_000L) {
                        return;
                    }
                }

            }

        }

    }

}
