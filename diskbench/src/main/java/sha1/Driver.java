package sha1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import static sha.Utils.*;

public class Driver
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    public static long pending = -1;

    public static void main( String[] args ) {
        try {
            Driver obj = new Driver();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", dumps(s));
            obj.go(args);
//            obj.testArrayList();
        } catch (Exception e) {
            log.error("error", e);
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
     * @param args
     */
    private void go(String[] args) throws Exception {
        if(args.length==0) {
            args = new String[]{"client", "server", "pending", "1000000"};
        }
        List<String> argsList = Arrays.asList(args);
        int ind = argsList.indexOf("pending");
        if(ind>=0) {
            pending = Long.parseLong(argsList.get(ind+1));
        }
        if(argsList.contains("server")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new Server().go();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            Thread.sleep(1000);
        }
        if(argsList.contains("client")) {
//            Runnable r = new Runnable() {
//                @Override
//                public void run() {
//                    while(true) {
//                        wasteTime();
//                    }
//                }
//            };
//            for(int i=0; i<0; i++) {
//                new Thread(r).start();
//            }
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        new DiskWriter().go();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            }).start();
            new Client().go();

        }

    }

    public void wasteTime() {

        int sum = 0;
        for(int i=0; i<100_000_000; i++) {
            sum+=i;
        }
        if(sum==Integer.MAX_VALUE) {
            throw new RuntimeException("unlucky! ");
        }
//        try {
//            Thread.sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

}
