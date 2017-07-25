package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static sha.Utils.readJsonFromClasspath;


public class AbqSkewed
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            AbqSkewed obj = new AbqSkewed();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
//                log.warn("settings.json not found on classpath");
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
        } catch (Exception e) {
//            log.error("", e);
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
    final Recorder recorder = new Recorder(AbqSkewed.class.getSimpleName());
    final Utils.Timer sha = new Utils.Timer(recorder, "sha");
    final Utils.LatencyTimer lt = new Utils.LatencyTimer("lt");

    final long time = 1000_000;


    private void go() throws Exception {
        List<Integer> runs = new ArrayList<>();
        int cpus = Runtime.getRuntime().availableProcessors();
        for(int i=0; i<cpus; i++) {
            runs.add(i+1);
        }
        for(int i=2*cpus; i<=2048; i*=2) {
            runs.add(i);
        }

        int c = 0;
        for (Integer threads : runs) {
            threads = 1000;
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("starting run for producers:1 consumers"+threads);
            recorder.startRun(new MyRun(1, threads));
            doTest(threads, 1);
//
//            System.out.println();
//            System.out.println();
//            System.out.println();
//            System.out.println("starting run for producers:1 consumers"+threads);
//            recorder.startRun(new MyRun(threads, 1));
//            doTest(threads, 1);
            recorder.finish();
            return;

        }


        recorder.finish();




    }

    public static class MyRun implements Serializable {
        public int producers = 0;
        public int consumers = 0;

        public MyRun(int producers, int consumers) {
            this.producers = producers;
            this.consumers = consumers;
        }

        // for objectmapper
        public MyRun() {
        }
    }
    public Wrap wrap = new Wrap();
    private static final int sz = 409600;
    public void doTest(int producers, int consumers) throws InterruptedException {
//        sha.die();
//        lt.die();

        final CyclicBarrier barrier = new CyclicBarrier(producers+consumers);
        final ArrayBlockingQueue<Wrap> abq = new ArrayBlockingQueue<>(sz);
        long last = System.nanoTime();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < consumers; i++) {
            ts.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    long kk = 0;

                    Thread.currentThread().setName("taker");
                    try {
                        long count = 0;
                        long mask = (1<<12) - 1;
                        long last = System.nanoTime();
                        barrier.await();
                        while (true) {
                            count++;
                            abq.take();
                            long now = System.nanoTime();
                            if(now-last > 500_000) {
                                lt.count(kk);
                                kk = 0;
                            }
                            last = now;
                            kk++;
//                            lt.count(abq.size());
                            if((count & mask) == 0) {
                                sha.count(mask);
                                if(Thread.interrupted()) {
                                    return;
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        log.debug("returning due to interrupt");
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        for (int i = 0; i < producers; i++) {
            ts.add(new Thread(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("taker");
                    try {
                        long count = 0;
                        long mask = 1<<12 - 1;
                        barrier.await();
                        while (true) {
                            count++;
                            wrap.count += count;
                            abq.put(wrap);
//                            lt.count(abq.size());
                            if((count & mask) == 0 ) {
                                if(Thread.interrupted()) {
                                    return;
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        log.debug("returning normally");
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                }
            }));

        }

        ts.forEach(Thread::start);
        Thread.sleep(time);
        ts.forEach(Thread::interrupt);
        for (Thread t : ts) {
            t.join();
        }

    }
    public static class Wrap {
        public int count = 0;

    }
}
