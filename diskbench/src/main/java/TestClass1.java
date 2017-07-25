/* IMPORTANT: Multiple classes and nested static classes are supported */

/*
 * uncomment this if you want to read input.
//imports for BufferedReader
import java.io.BufferedReader;
import java.io.InputStreamReader;

//import for Scanner and other utility classes
import java.util.*;
*/

import java.util.*;

public class TestClass1 {
    public static void main(String args[] ) throws Exception {
        TestClass1 tc = new TestClass1();
        tc.go();
//        tc.tp();

    }

    public void tp() {
        boolean[] prime = new boolean[100];
        primefill(prime);
        for(int i=0; i<prime.length; i++) {
            if(prime[i])
                l(i);
        }

    }

    // sieve of eratosthenes
    public void primefill(boolean[] prime) {
        Arrays.fill(prime, true);
        for(int p = 2; p*p <=prime.length; p++)
        {
            // If prime[p] is not changed, then it is a prime
            if(prime[p])
            {
                // Update all multiples of p
                for(int i = p*2; i < prime.length; i += p)
                    prime[i] = false;
            }
        }


    }

    public void go() throws Exception {
        Scanner s = new Scanner(System.in);
//        Scanner s = new Scanner(new File("/Users/sharath.g/Downloads/test2"));


        int N = s.nextInt();
        int M = s.nextInt();
        boolean[] prime = new boolean[100_000+1];
        primefill(prime);
        boolean graph[][] = new boolean[N][M];


        for(int i=0; i<N; i++) {
            for (int j = 0; j < M; j++) {
                int num = s.nextInt();
                graph[i][j] = prime[num];
            }
        }

//        System.out.println(Arrays.deepToString(graph));
        int[][] paths = new int[N][M];
        int[][] x = new int[N][M];
        int[][] y = new int[N][M];
        x[N-1][M-1] = N+10;
        y[N-1][M-1] = M+10;
        if(graph[N-1][M-1]) {
            paths[N-1][M-1] = 1;
        }
        int mod = 1000_000_000 + 7;

        for(int i=N-1; i>=0; i--) {
            for(int j=M-1; j>=0; j--) {
                if(!graph[i][j]) {
                    continue;
                }
                boolean done = false;
                if(i<N-1 && j<M-1 && paths[i+1][j+1] > 0) {
                    paths[i][j] += paths[i+1][j+1];
                    paths[i][j] %= mod;
                    done = true;
                    x[i][j] = i+1;
                    y[i][j] = j+1;
                }

                if(i<N-1 && paths[i + 1][j] > 0) {
                    paths[i][j] += paths[i + 1][j];
                    paths[i][j] %= mod;

                    if(!done) {
                        done = true;
                        x[i][j] = i+1;
                        y[i][j] = j;
                    }
                }
                if(j<M-1 && paths[i][j + 1] > 0) {
                    paths[i][j] += paths[i][j + 1];
                    paths[i][j] %= mod;
                    if(!done) {
                        x[i][j] = i;
                        y[i][j] = j+1;

                    }
                }
            }
        }
        int ans = paths[0][0];
        paths = null; // garbage collect

        System.out.println(ans);
        if(ans==0) {
            return;
        }

        int xx = 0, yy = 0;
        while(xx<=N-1&&yy<=M-1) {
            String ss = (xx+1)+" "+(yy+1);
            System.out.println(ss);

            int a = x[xx][yy];
            int b = y[xx][yy];

            xx = a;
            yy = b;
        }

    }

    public static void l(Object... o) {
        String s = "";
        for (Object oo : o) {
            if (oo instanceof int[]) {
                s += Arrays.toString((int[]) oo) + " ";
                continue;
            }
            if (oo instanceof double[]) {
                s += Arrays.toString((double[]) oo) + " ";
                continue;
            }
            if (oo instanceof boolean[]) {
                s += Arrays.toString((boolean[]) oo) + " ";
                continue;
            }
            if (oo instanceof char[]) {
                s += Arrays.toString((char[]) oo) + " ";
                continue;
            }
            if (oo instanceof long[]) {
                s += Arrays.toString((long[]) oo) + " ";
                continue;
            }
            if (oo instanceof String[]) {
                s += Arrays.toString((String[]) oo) + " ";
                continue;
            }
            if (oo instanceof Object[]) {
                s += Arrays.deepToString((Object[]) oo) + " ";
                continue;
            }
            s += (oo.toString()) + " ";
        }
        System.out.println(s);
    }


}
