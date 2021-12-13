package sha;


import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static sha.Utils.readJsonFromClasspath;

public class KConsumer {
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    Utils.Timer timer = new Utils.Timer("meter");
    public static void main(String[] args) {
        try {
            KConsumer obj = new KConsumer();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.print();
            obj.shutdown();
            obj.go();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void print() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                   if(close) {
                       break;
                   }
                    log.info("total polls:{} zero_polls:{}", count, zero_poll);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        log.error("", e);
                    }
                }
            }
        }).start();
    }

    volatile boolean close = false;
    private void shutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("shutting down.......");
                close = true;

                for (Consumer consumer : consumerList) {
                    consumer.wakeup();
                }

                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        log.error("", e);
                    }
                }

            }
        }));
    }

    public List<Consumer> consumerList = Collections.synchronizedList(new ArrayList<>());
    public List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
    private void go() {
        for (int i = 0; i < s.threads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        doWork();
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
    }

    public static class Settings {
        public int threads = 10;
        public String topic = "dart.fkint.cp.ca_common.RequestEvent";
        public int mod = 10000;
        // required for jackson
        public Settings() {
        }
    }
    volatile int count = 0;
    volatile int zero_poll = 0;


    /**
     * All teh code from here:
     */
    private void doWork() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "10.33.78.119:9092");
                props.put("group.id", "sharath.test.1");
//
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "sharath.test");
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
//                100);
        props.put("max.partition.fetch.bytes", 5000000);


        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumerList.add(consumer);
        consumer.subscribe(Arrays.asList(s.topic));
        System.out.println("Subscribed to topic " + s.topic);

        int timeout = 3;
        while (true) {
            count++;
            if(close) {
                log.info("shutting down consumer");
                consumer.close();
                return;
            }
            try {
                ConsumerRecords<String, String> records = consumer.poll(timeout*1000);
                timer.count(records.count());
                if(records.count() == 0) {
                    zero_poll++;
                }

            } catch (WakeupException e) {
                log.info("wakeup exception..........");
                log.info("shutting down consumer");
                consumer.close();
                return;

            }
        }
    }
}
