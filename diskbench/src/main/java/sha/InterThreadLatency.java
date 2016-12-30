package sha;
public  class InterThreadLatency

{
    public static final long ITERATIONS = Long.MAX_VALUE;

    public static volatile long s1;
    public static volatile long s2;

    public static void main(final String[] args) throws InterruptedException {
        Thread.currentThread().setName("taker");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("taker");
                long value = s2;
                while (true)
                {
                    while (value == s1)
                    {
                        // busy spin
                    }
                    value = ++s2;
                }

            }
        });
        t.start();

        long start = System.nanoTime();

        long value = s1;
        long ops = 0;
        while (s1 < ITERATIONS)
        {
            while (s2 != value)
            {
                // busy spin
            }
            value = ++s1;
            if((value & 1024L) ==0) {
                long now = System.nanoTime();
                long duration = now - start;
                long opsNow = value - ops;
                if(duration > 1000_000_000) {
                    System.out.println("duration(s) = " + duration/1000_000_000);
                    System.out.println("ns per op = " + duration / (opsNow * 2));
                    System.out.println("op/sec = " +
                            (opsNow*2*1000_000_000) / duration);
                    System.out.println("s1 = " + s1 + ", s2 = " + s2);
                    start = now;
                    ops = value;

                }

            }


        }


    }

}
