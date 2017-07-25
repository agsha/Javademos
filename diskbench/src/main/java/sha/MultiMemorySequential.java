package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static sha.Utils.*;

public class MultiMemorySequential
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            MultiMemorySequential obj = new MultiMemorySequential();
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
    static Timer t;
    public static final Object lock = new Object();
    public int time = 30_000;
    public static final int[] workingSet = new int[1024*1024*1024];
    public static CyclicBarrier barrier;

    public List<Runner> runners = new ArrayList<>();

    private void go() throws Exception {
        final Recorder recorder = new Recorder(MultiMemorySequential.class.getSimpleName());
        t = new Timer(recorder, "mytimer");

        int cpus = Runtime.getRuntime().availableProcessors();
        List<Integer> threads = new ArrayList<>();
        for(int i=1; i<=cpus; i++) {
            threads.add(i);
        }
        for(int i=2*cpus; i<=2048; i*=2) {
            threads.add(i);
        }

        // 16 ints = 64 bytes = 1 cache line
        int count = 0;
        outer:for(int mem=16; mem<=16*1024*1024; mem*=4) {
            for(int thread : threads) {
                if(thread*mem > workingSet.length) break;
                System.out.println();
                System.out.println();
                System.out.println("new run with threads:"+thread+" and mem:"+mem);

                final List<Thread> ts = new ArrayList<>(thread);
                int start = 0;
                for(int i=0; i<thread; i++) {
                    ts.add(new Thread(new Runner(start, mem)));
                    start+=mem;
                }
                synchronized (lock) {
                    barrier = new CyclicBarrier(thread+1); // 1 for me
                }
                ts.forEach(Thread::start);
                recorder.startRun(new MyRun(thread, mem));
                barrier.await();
                Thread.sleep(time);
                ts.forEach(Thread::interrupt);
                for (Thread t : ts) {
                    t.join();
                }

                count++;
//                if(count==2) {
//                    break outer;
//                }

            }
        }

        recorder.finish();
    }

    public static class Runner implements Runnable{
        public int start=0, sz=0;

        public Runner(int start, int sz) {
            this.start = start;
            this.sz = sz;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("taker");
            for(int i=start; i<start+sz-1; i++) {
                workingSet[i] = i+1;
            }
            workingSet[start+sz-1] = start; //loopback to beginning
//            sattoloPermute(start, sz);
            try {
                long count = 0;
                int index = start;
                long mask = (1<<12) - 1;
//                System.out.println(Long.toBinaryString(mask));
                barrier.await();
                while (true) {
                    index = workingSet[index];
                    index = workingSet[index];
                    index = workingSet[index];
                    index = workingSet[index];
                    index = workingSet[index];
                    index = workingSet[index];
                    index = workingSet[index];
                    index = workingSet[index];
                    count+=8;

                    if((count & mask) == 0 ) {
                        t.count(mask);
                        if(Thread.interrupted()) {
                            return;
                        }
                        if(index == Integer.MAX_VALUE) {
                            System.out.println("unlucky");
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.debug("returning normally");
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

    }
    public static void sattoloPermute(int start, int len) {
        for (int i = len; i > 1; i--) {
            // choose index uniformly in [0, i-1)
            int r = (int) (Math.random() * (i-1));
            int swap = workingSet[start+r];
            workingSet[start+r] = workingSet[start+i-1];
            workingSet[start+i-1] = swap;
        }
    }

    public static class MyRun implements Serializable {
        public int threads = 0;
        public int workingSet = 0;

        public MyRun(int threads, int workingSet) {
            this.threads = threads;
            this.workingSet = workingSet;
        }

        // for objectmapper
        public MyRun() {
        }
    }

}
