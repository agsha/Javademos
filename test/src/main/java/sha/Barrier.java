package sha;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Phaser;

import static sha.Utils.readJsonFromClasspath;

@Slf4j
public class Barrier
{
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Barrier obj = new Barrier();
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

    private void go() {
        t1("ping");
        t2("pong");
    }


    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }

    final Phaser p = new Phaser(1);
    Utils.Timer t = new Utils.Timer("");
    Object o = new Object();
    ArrayBlockingQueue q = new ArrayBlockingQueue(1);
    /**
     * All teh code from here:
     */
    private void t1(final String msg) {
        new Thread(() -> {
            while (true) {
                try {
                    q.put(o);
                    q.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                t.count();
            }
        }).start();
    }

    private void t2(final String msg) {
        new Thread(() -> {
            while (true) {
                try {
                    q.take();
                    q.put(o);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}
