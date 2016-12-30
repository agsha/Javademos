package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.RandomAccessFile;
import java.util.Arrays;

import static sha.Utils.*;

public class DiskWriter
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            DiskWriter obj = new DiskWriter();
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
    public void go() throws Exception {
        Timer diskBytes = new Timer("diskBytes");
        LatencyTimer diskWrLat = new LatencyTimer("diskWrLat");
        RandomAccessFile file = new RandomAccessFile("/grid/vdb/diskbench/logfile", "rw");
        long now = System.nanoTime();
        while(true) {
            byte[] b = new byte[16*1024];
            Arrays.fill(b, (byte)(-1));
            file.write(b);
            long xx = System.nanoTime();
            diskWrLat.count(xx-now);
            now = xx;
            diskBytes.count(b.length);

//            if(file.length() > 1024*1024*1024) {
//                file.setLength(0);
//            }
        }
    }

}
