package sha;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static sha.Utils.readJsonFromClasspath;

@Slf4j
public class MyClass
{
    private static Settings s;

    public static void main( String[] args ) {
        try {
            MyClass obj = new MyClass();
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

    public List<List<String>> foo;

    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        MyFoo foo = new MyFoo("hello");
        ObjectMapper mapper = new ObjectMapper();
        log.debug(mapper.writeValueAsString(foo));

        MyFoo myFoo = mapper.readValue("{\"str\":\"hello\"}", MyFoo.class);


    }

    static class MyFoo {
        @JsonProperty private String str;

        public MyFoo(String str) {
            this.str = str;
        }

        public MyFoo() {
        }
    }
}
