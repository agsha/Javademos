package sha;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import static sha.Utils.readJsonFromClasspath;

public class LogSpeed
{
    private static final Logger log = Logger.getLogger(LogSpeed.class);

    private static Settings s;

    public static void main( String[] args ) {
        log.error("hiiiiiiiiiiiiiiiiiiiii");
        try {
            LogSpeed obj = new LogSpeed();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go(Integer.parseInt(args[0]));
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
    private void go(int threads) throws Exception {
        final AtomicLong logs = new AtomicLong(0);
        for(int i=0; i<threads; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long mask = (1<<11) - 1;
                    long count = 0;
                    while(true) {
                        count++;
                        if((count & mask) == 0) {
                            logs.addAndGet(mask+1L);
                        }
                        log.error("fooooooooooooooooooooooooooooooooooooooooooooo" +
                                "ooooooooooooooooooooo");
                        logs.incrementAndGet();
                    }
                }
            }).start();
        }
        Path path = Paths.get("logging.log");
        long last = 0;
        long lastSize = 0;
        while(true) {
            long sizeNow = 0;
            try {
                sizeNow = Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String ssize = format((sizeNow-lastSize)/2);
            lastSize = sizeNow;
            long now = logs.get();
            String slogs = format(now);
            long rate = (now-last)/2;
            String srate = format(rate);
            last = now;
            System.out.println(String.format("number of logs printed:%s rate:%s file_size_rate:%s", slogs, srate, ssize));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    String format(long x) {
        String s = ""+x;
        StringBuilder ss = new StringBuilder();
        for (int i = 0; i <s.length() ; i++) {
            if(i%3==0 && i>0) {
                ss.append("_");
            }
            ss.append(s.charAt(s.length()-i-1));

        }
//                return ss.reverse().toString();
        return ss.reverse().toString();
    }
}
