/**
 * Created by sharath.g on 15/06/17.
 */
public class TestSignal {
    public static void main(String[] args) throws Exception {
        TestSignal ts = new TestSignal();
        ts.go();
    }

    private void go() throws Exception {
        Thread t1 = new Thread(){
            public void run()
            {
                try {
                    sleeeep();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                workkkk();
            }
        };
        Thread t3 = new Thread() {
            @Override
            public void run() {
                try {
                    both();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        t1.start();
        t2.start();
        t3.start();



    }


    public void sleeeep() throws InterruptedException {
        while(true) {
            Thread.sleep(1);
        }
    }


    public void both() throws InterruptedException {
        while(true) {
            long now = System.nanoTime();
            while(true) {
                long t = System.nanoTime();
                if(t - now > 1000_000) {
                    break;
                }
            }
            Thread.sleep(1);
        }
    }

    public void workkkk() {
        int count = 0;
        while(true) {
            count++;
        }
    }
}
