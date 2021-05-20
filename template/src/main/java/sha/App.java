package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static sha.Utils.dumps;
import static sha.Utils.readJsonFromClasspath;

public class App 
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    public static void main( String[] args ) {
        try {
            App obj = getObj(args);
            obj.go();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * this method can be conveniently used in test cases to get the object under test
     */
    public static App getObj(String[] args) {
        App obj = new App();


        String path = "settings.json";

        try {
            if(args.length>0) {
                path = args[0];
            }
            s = readJsonFromClasspath(path, Settings.class);
        } catch (Exception e) {
            s = new Settings();
        }
        // log.info("Using settings:{}", dumps(s));

        return obj;
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
