package sha;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.out;

public class PureLocks
{
    private static final long ITERATIONS = Long.MAX_VALUE;

    private static final Lock lock = new ReentrantLock();
    private static final Condition sendCondition = lock.newCondition();
    private static final Condition echoCondition = lock.newCondition();

    private static long sendValue = -1L;
    private static long echoValue = -1L;

    public static void main(final String[] args)
            throws Exception
    {
        final Thread sendThread = new Thread(new SendRunner());
        final Thread echoThread = new Thread(new EchoRunner());

        final long start = System.nanoTime();

        echoThread.start();
        sendThread.start();

        sendThread.join();
        echoThread.join();

    }

    public static final class SendRunner implements Runnable
    {
        public void run()
        {
            Thread.currentThread().setName("taker");
            int mask = 2<<15;
            long ops = 0;
            long last = System.nanoTime();
            for (long i = 0; i < ITERATIONS; i++)
            {
                lock.lock();
                try
                {
                    sendValue = i;
                    sendCondition.signal();
                    while (echoValue != i)
                    {
                        echoCondition.await();
                    }
                }
                catch (final InterruptedException ex)
                {
                    break;
                }
                finally
                {
                    lock.unlock();
                }
                if((i&mask)==0) {
                    long now = System.nanoTime();
                    if(now-last >= 1000_000_000) {
                        long sec = (now-last)/1000_000_000;
                        out.printf("duration %,d (ns)\n", sec);
                        out.printf("%,d ns/op\n", (now-last) / ((i-ops)*2L));


                        out.printf("%,d ops/s\n", (i-ops) * 1000_000_000L * 2 /  (now-last) );
                        last = now;
                        ops = i;
                    }
                }

            }

        }
    }

    public static final class EchoRunner implements Runnable
    {
        public void run()
        {
            Thread.currentThread().setName("taker");

            for (long i = 0; i < ITERATIONS; i++)
            {
                lock.lock();
                try
                {
                    while (sendValue != i)
                    {
                        try {
                            sendCondition.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    echoValue = i;
                    echoCondition.signal();
                }
                finally
                {
                    lock.unlock();
                }
            }
        }
    }
}
