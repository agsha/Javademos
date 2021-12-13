package sha;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static sha.Utils.readJsonFromClasspath;

@Slf4j
public class ObjectMapperOptional
{
    private static Settings s;

    public static void main( String[] args ) {
        try {
            ObjectMapperOptional obj = new ObjectMapperOptional();
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
    private void go() throws JsonProcessingException {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        log.info("{}", m.writerWithDefaultPrettyPrinter().writeValueAsString(new Foo()));
    }

    public static class Foo {
        public Optional<String> oStr = Optional.of("hiiiiiiiiii");
    }
}
