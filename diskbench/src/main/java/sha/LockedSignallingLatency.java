package sha;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.out;

public class LockedSignallingLatency
{
    private static final int ITERATIONS = 10 * 1000 * 1000;

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

        final long duration = System.nanoTime() - start;

        out.printf("duration %,d (ns)\n", duration);
        out.printf("%,d ns/op\n", duration / (ITERATIONS * 2L));
        out.printf("%,d ops/s\n", (ITERATIONS * 2L * 1000000000L) / duration);
    }

    public static final class SendRunner implements Runnable
    {
        public void run()
        {
            for (long i = 0; i < ITERATIONS; i++)
            {
                lock.lock();
                try
                {
                    sendValue = i;
                    sendCondition.signal();
                }
                finally
                {
                    lock.unlock();
                }

                lock.lock();
                try
                {
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

            }
        }
    }

    public static final class EchoRunner implements Runnable
    {
        public void run()
        {
            for (long i = 0; i < ITERATIONS; i++)
            {
                lock.lock();
                try
                {
                    while (sendValue != i)
                    {
                        sendCondition.await();
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

                lock.lock();
                try
                {
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
