package sha;

import com.lmax.disruptor.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static com.lmax.disruptor.RingBuffer.*;
import static sha.Utils.dumps;
import static sha.Utils.readJsonFromClasspath;

public class MultiProdMultiCons
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            MultiProdMultiCons obj = new MultiProdMultiCons();
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
    CyclicBarrier barrier;
    RingBuffer<MyData> ringBuffer;
    Recorder recorder = new Recorder(MultiProdMultiCons.class.getSimpleName());
    Utils.Timer trpt = new Utils.Timer("serverTrpt");

    public void go() throws Exception {
        oneTest(1, 1, 1024*4);
    }

    private void oneTest(int producers, int consumers, int size) throws Exception {
        synchronized (this) {
            barrier = new CyclicBarrier(producers + consumers + 1);
            ringBuffer =
                    createMultiProducer(MyData.EVENT_FACTORY, size, new YieldingWaitStrategy());
//                    createSingleProducer(MyData.EVENT_FACTORY, size, new YieldingWaitStrategy());
        }
        List<Thread> threads = new ArrayList<>();
        for(int i=0; i<producers; i++) {
            Thread t  = new Thread(() -> {
                try {
                    produce();
                } catch (BrokenBarrierException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }
        for(int i=0; i<consumers; i++) {
            Thread t  = new Thread(() -> {
                try {
                    consume();
                } catch (BrokenBarrierException | InterruptedException | TimeoutException | AlertException e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }

        barrier.await();
        Thread.sleep(30_000);
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private void consume() throws BrokenBarrierException, InterruptedException, TimeoutException, AlertException {
        long count = 0;
        long mask = (1<<12) - 1;
        final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
        ringBuffer.addGatingSequences(sequence);
        long nextSequence = sequence.get() + 1L;

        barrier.await();
        while(true) {
            final long availableSequence = sequenceBarrier.waitFor(nextSequence);

            while (nextSequence <= availableSequence)
            {
                ringBuffer.get(nextSequence);
                nextSequence++;
            }

            sequence.set(availableSequence);

            count++;
            if((count & mask) == 0 ) {
                if(Thread.interrupted()) {
                    return;
                }
            }
        }
    }

    private void produce() throws BrokenBarrierException, InterruptedException {
        long count = 0;
        long mask = (1<<12) - 1;
        barrier.await();



        while(true) {
            long next = ringBuffer.next();
            ringBuffer.get(next).val = count;
            ringBuffer.publish(next);

            count++;
            if((count & mask) == 0 ) {
                trpt.count(mask);
                if(Thread.interrupted()) {
                    return;
                }
            }
        }
    }


    static class MyData {
        public long val;
        public static final EventFactory<MyData> EVENT_FACTORY = MyData::new;
    }

}
