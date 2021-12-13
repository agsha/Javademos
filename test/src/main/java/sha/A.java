package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static sha.Utils.readJsonFromClasspath;

public class A
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            A obj = new A();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go2();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void go2() throws IOException {
        Process sudo = new ProcessBuilder("python", "/home/sharath.g/sl.py").start();

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
    }

}
