package sha;

import com.flipkart.specter.AdminApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static sha.Utils.prettifyJson;
import static sha.Utils.readJsonFromClasspath;

public class Client
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Client obj = new Client();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", prettifyJson(s));
            obj.client();
        } catch (Exception e) {
            log.error("error occured", e);
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
    private void client() throws Exception {
        Registry registry = LocateRegistry.getRegistry("10.32.71.122", 19838);

        AdminApi hello = (AdminApi) registry
                .lookup(AdminApi.class.getName());
//        hello.upgrade("http://10.47.4.220/repos/specter/138", "1.28032018014015");
        log.debug(hello.getCurrentVersion());
//        hello.exceptionExample();
    }

}
