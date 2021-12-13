package sha;


import lombok.extern.slf4j.Slf4j;

import static sha.Utils.readJsonFromClasspath;

@Slf4j
public class App 
{
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
    private void go() {
    }

}
