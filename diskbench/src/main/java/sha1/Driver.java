package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.sun.tools.doclint.Entity.ne;
import static sha.Utils.*;

public class Driver
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Driver obj = new Driver();
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new Server().go();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Thread.sleep(5);
        new Client().go();
    }

}
