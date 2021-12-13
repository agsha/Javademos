package sha;


import com.google.common.cache.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import static sha.Utils.readJsonFromClasspath;

@Slf4j
public class Guava
{
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Guava obj = new Guava();
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
    private void go() throws InterruptedException {
        Utils.Timer t = new Utils.Timer("expiry");
        Utils.Timer t1 = new Utils.Timer("put");
        RemovalListener<Long, MyMessage> removalListener = new RemovalListener<Long, MyMessage>() {
            public void onRemoval(RemovalNotification<Long, MyMessage> removal) {
                RemovalCause cause = removal.getCause();
                log.error("fooo {}", cause);
                if(removal.getKey() < 0) {
                }
                t.count();
            }
        };

        Cache<Long, MyMessage> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .removalListener(removalListener)
                .build();

        long l = 0;
        while (true) {
            cache.put(l++, new MyMessage());
            if((l & 1023) == 0) {
                t1.count(1024);
            }
            if(l==100) {
                break;
            }
        }
        cache.asMap().remove(20L);
        Thread.sleep(5000);
    }

    class MyMessage {
        public long id;
        public long timestamp;
        public CompletionHandler<ByteBuf, Void> callback;
    }
}
