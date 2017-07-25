/* IMPORTANT: Multiple classes and nested static classes are supported */

/*
 * uncomment this if you want to read input.
 */
//imports for BufferedReader

import java.io.File;
import java.util.Scanner;

//import for Scanner and other utility classes

class TestClass3 {
    public static void main(String args[] ) throws Exception {
        /*
         * Read input from stdin and provide input before running
         * Use either of these methods for input
         */


        //Scanner
//        Scanner s = new Scanner(System.in);
        Scanner s = new Scanner(new File("/Users/sharath.g/Downloads/test1"));

        long now = System.currentTimeMillis();
        int N = s.nextInt();
        int Q = s.nextInt();
        long[] nums = new long[N];
        for(int i=0; i<N; i++) {
            nums[i] = s.nextInt();
        }

        long mod = (1000_000_000+7);

        long[] fibs = new long[N+5];
        fibs[1] = 1; fibs[2] = 1;
        for(int i=3; i<fibs.length; i++) {
            long f = fibs[i-1] + fibs[i-2];
            f %= mod;
            fibs[i] = f;
        }

//        System.out.println(Arrays.toString(fibs));
        int[] left = new int[Q];
        int[] right = new int[Q];

        for(int i=0; i<Q; i++) {
            left[i] = s.nextInt();
            right[i] = s.nextInt();
        }

        int batch = (int) Math.sqrt(N);
        long[] batchGcds = new long[N]; //doesnt matter :)

        for(int i=0; i<N; i+=batch) {
            batchGcds[i] = nums[i];
            for(int j=i; j<i+batch && j<N; j++) { // the last batch may have fewer than batch elements: be careful!
                batchGcds[i] = gcd(batchGcds[i], nums[j]);
            }
        }
//        System.out.println(Arrays.toString(batchGcds));
        for(int tcase=0; tcase<Q; tcase++) {
//            System.out.println("new test case================");
            int l = left[tcase] - 1;
            int r = right[tcase] - 1;
//            System.out.println("l:"+l +" r:"+r);
            long gcd = nums[l];
            int start = l;
            while(start<=r) {
                if(start%batch==0) {
                    break;
                }
                gcd = gcd(gcd, nums[start]);
                start++;
            }
//            System.out.println("finished first part at:"+start +" gcd is:"+gcd);
            while(start + batch - 1 <= r) {
                gcd = gcd(gcd, batchGcds[start]);
                start += batch;
            }
//            System.out.println("finished second part at:"+start +" gcd is:"+gcd);

            while(start <= r) {
                gcd = gcd(gcd, nums[start]);
                start++;
            }
//            System.out.println("finished third part at:"+start +" gcd is:"+gcd);

            System.out.println(fibonacci((int)(gcd-1)));
        }
        System.out.println("time taken:"+(System.currentTimeMillis() - now)/1000);


    }

    static int mod = 1000_000_000 + 7;
    // 0 based index: f0 = 1, f1 = 1
    private static int fibonacci(int n) {
        long[][] powerMatrix = pow(new long[][]{{1, 1}, {1, 0}}, n);
        long[][] fib = mul(powerMatrix, new long[][]{{1}, {1}});
//        System.out.println(Arrays.deepToString(fib));
        return (int)(fib[1][0]%mod);
    }

    private static long[][] pow(long[][] matrix, int n) {
        if(n==0) return new long[][]{{1, 0}, {0, 1}};
        if(n%2==0) {
            long[][] temp = pow(matrix, n/2);
            return mul(temp, temp);
        }
        return mul(pow(matrix, n-1), matrix);
    }

    static long[][] mul(long[][]a, long[][]b) {
        int n = a.length, k = a[0].length, m = b[0].length;
        long[][] c = new long[n][m];
        for(int i=0; i<n; i++) {
            for(int j=0; j<m; j++) {
                for(int kk=0; kk<k; kk++) {
                    c[i][j] += a[i][kk]*b[kk][j];
                    c[i][j] %= mod;
                }
            }
        }
        return c;
    }

    private static long fib(long n) {
        System.out.println("fid is "+n);
        int count = 1;
        int a = 1, b = 1;
        long mod = (1000_000_000+7);
        while(count<n) {
            int c = a+b;
            a = b;
            b = c;
            count++;
            a%=mod;
            b%=mod;
        }
        return a%mod;
    }

    private static long gcd(long a, long b) {
        while(b>0) {
            long temp = a%b;
            a = b;
            b = temp;
        }
        return a;
    }
}
