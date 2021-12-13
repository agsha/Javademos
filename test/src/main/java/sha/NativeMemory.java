package sha;


import java.nio.ByteBuffer;
import java.util.Scanner;


public class NativeMemory
{

    public static void main( String[] args ) throws InterruptedException {
        NativeMemory obj = new NativeMemory();

        obj.go();

    }



    /**
     * All teh code from here:
     */
    private void go() throws InterruptedException {
        System.out.println("starting the program. type a number");
        Scanner sc = new Scanner(System.in);
        sc.nextInt();
        ByteBuffer bf = ByteBuffer.allocateDirect(1024*1024*1024 + 512*1024*1024);
        while (bf.remaining() > 20) {
            bf.putLong(0xcafebabe);
        }
        System.out.println("finished allocating the direct bytebuffer");
        Thread.sleep(1000000);
    }

}
