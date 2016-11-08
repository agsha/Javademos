package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.SystemClock;

import java.io.File;
import java.util.Map;

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
    private void go() throws Exception {
        long sum = 0;
        long tot = 1000_000/16;
        long start = System.nanoTime();
        for(long count = 0; count<tot; count++) {
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
            sum+=System.nanoTime();
        }
        long duration = (System.nanoTime()-start);
        log.debug("sum:{}, time_per_call_nano:{}", sum, duration/(tot*32));
    }

}
