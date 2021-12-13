package sha;


import com.codahale.metrics.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

import static sha.Utils.readJsonFromClasspath;

public class My
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            My obj = new My();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }
    private final MetricRegistry metrics = new MetricRegistry();

    private final Counter pendingJobs = metrics.counter("foo");
    private final Histogram responseSizes = metrics.histogram("histo");
    private final Timer responses = metrics.timer("xxx");


    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(3, TimeUnit.SECONDS);


        int x = 0;
        while (true) {
            final Timer.Context context = responses.time();
            context.stop();
        }
//
//        Utils.LatencyTimer t = new Utils.LatencyTimer();
//
//        while (true) {
//            t.count();
//        }

    }

}
