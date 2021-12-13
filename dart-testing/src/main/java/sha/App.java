package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static sha.Utils.readJsonFromClasspath;

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


    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        log.debug("Hello, world!");
    }

}
