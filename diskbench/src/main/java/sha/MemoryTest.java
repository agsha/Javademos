package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

import static sha.Utils.*;

public class MemoryTest
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            MemoryTest obj = new MemoryTest();
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


    private void go() {
        for(int i=0; i<1; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        work();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    /**
     * All teh code from here:
     */
    private void work() throws Exception {
        int[] x = new int[1024*1024*64];
        Random r = new Random();
        for(int i=0; i<x.length; i++){
            x[i] = r.nextInt(x.length);
        }
        Timer t = new Timer(Thread.currentThread().getName());
        int sum = 0;
        int ind = 0;
        while(ind>=0) {
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;
//            sum+=x[ind];
//            ind++;

//            if(ind>=x.length) ind=0;
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
            ind = x[ind];
//            sum+=ind;
            t.count(10);
        }
        log.debug(sum);
    }

}
