package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static sha.Utils.prettifyJson;
import static sha.Utils.readJsonFromClasspath;

public class Insurance
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Insurance obj = new Insurance();
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

    private void go() throws Exception{
        compute(7);
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
    private List<Double> compute(double interest) throws Exception {
        List<Double> l = new ArrayList<>();
        double monthly = 1;
        double p = 0;
        double multiplier = interest / (100.0*4);
        double interestForYear = 0;
        for(int q=0; q<1000; q++) {
            if(q%3==0) {
                double interestForQuarter = p*multiplier + monthly*multiplier*(3.0/3 + 2.0/3 + 1.0/3);
                p += interestForQuarter + 3*monthly;

                interestForYear += interestForQuarter;
            }

            if (q % 12 == 0 ) {
                log.debug("{} {} {}", q/12, p, p/(q*monthly));
                l.add(p);
                interestForYear = 0;
            }
        }
        return l;
    }
}
