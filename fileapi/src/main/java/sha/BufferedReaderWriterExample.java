package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static sha.Utils.readJsonFromClasspath;

public class BufferedReaderWriterExample
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            BufferedReaderWriterExample obj = new BufferedReaderWriterExample();
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
        write();
    }


    public void write() {
        Charset charset = Charset.defaultCharset();
        String s = "hello, world!";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/tmp/myfile"), charset)) {
            writer.write(s, 0, s.length());
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public void read() {
        Charset charset = Charset.defaultCharset();
        String s = "hello, world!";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/tmp/myfile"), charset)) {
            writer.write(s, 0, s.length());
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
}
