/* IMPORTANT: Multiple classes and nested static classes are supported */


import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

class TestClass {
    private static int N, Q, mod = 1000_000_000 + 7;
    public static void main(String args[] ) throws Exception {
        TestClass tc = new TestClass();
//        tc.combo();
//        tc.oldGo();
//        tc.go();
//        tc.testfib();
        tc.testImpls();
//        tc.failingTest();
    }

    public void testfib() {
        int a = 1, b = 1;
        for(int i=2; i<1000_000_00; i++) {
            if(i%10000==0) {
                System.out.println(i);
            }
            int c = a+b;
            c%=mod;
            a = b;
            b = c;
            if(c!=fibonacci(i)) {
                throw new RuntimeException();
            }
        }

    }


    public void combo() throws Exception{
                        /*
         * Read input from stdin and provide input before running
         * Use either of these methods for input
         */


        //Scanner
        Scanner s = new Scanner(System.in);
//        Scanner s = new Scanner(new File("/Users/sharath.g/Downloads/test"));

        long now = System.currentTimeMillis();
        int N = s.nextInt();
        int Q = s.nextInt();
        int[] nums = new int[N];
        for(int i=0; i<N; i++) {
            nums[i] = s.nextInt();
        }



//        System.out.println(Arrays.toString(fibs));
        int[] left = new int[Q];
        int[] right = new int[Q];

        for(int i=0; i<Q; i++) {
            left[i] = s.nextInt();
            right[i] = s.nextInt();
        }

        int[] tree = new int[3*N];
        initialize(0, N-1, 1, tree, nums);
//        System.out.println(Arrays.toString(batchGcds));
        for(int tcase=0; tcase<Q; tcase++) {
//            System.out.println("new test case================");
            int l = left[tcase] - 1;
            int r = right[tcase] - 1;
            int gcd = rangeMinimumQuery(l, r, 1, 0, N-1, tree);
            System.out.println(fibonacci((int)(gcd-1)%mod));
        }
//        System.out.println("time taken:"+(System.currentTimeMillis() - now)/1000);

    }
    public void oldGo() throws Exception{
                /*
         * Read input from stdin and provide input before running
         * Use either of these methods for input
         */


        //Scanner
        Scanner s = new Scanner(System.in);
//        Scanner s = new Scanner(new File("/Users/sharath.g/Downloads/test"));

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
//        System.out.println("time taken:"+(System.currentTimeMillis() - now)/1000);
    }
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

    // segment tree or interval tree

    private static int rangeMinimumQuery(int from, int to, int node, int nodeLeft, int nodeRight, int[]tree) {
//        System.out.println("calling nodeleft "+nodeLeft+" noderight:"+nodeRight+" node:"+node);

        if(to < nodeLeft || from > nodeRight) return -1;
        if(from <= nodeLeft && nodeRight <= to) {
//            System.out.println("1 nodeleft "+nodeLeft+" noderight:"+nodeRight+" node:"+node+" val:"+tree[node]);
            return tree[node];
        }
        int mid = (nodeLeft+nodeRight)/2;
        int p1 = rangeMinimumQuery(from, to, 2*node, nodeLeft, mid, tree);
        int p2 = rangeMinimumQuery(from, to, 2*node + 1, mid+1, nodeRight, tree);
//        System.out.println("2 nodeleft "+nodeLeft+" noderight:"+nodeRight+" node:"+node+" mid: "+mid + " p1:"+p1+" p2:"+p2);

        if(p1==-1) {

            return p2;
        }
        if(p2==-1) {

            return p1;
        }

        int x =  gcd(p1, p2);
//        System.out.println("4 nodeleft "+nodeLeft+" noderight:"+nodeRight+" node:"+node+" val:"+x);
        return x;
    }

    private static void initialize(int from, int to, int index, int[]tree, int[]vals) {
//        System.out.println("calling from:"+from+" to:"+to+" index:"+index);
        if(from==to) {
//            System.out.println("setting from");

//            System.out.println("from:"+from+" to:"+to+" index:"+index+" = "+vals[from]);
            tree[index] = vals[from];

            return;
        }
        int mid = (to + from) / 2;
        initialize(from, mid, index*2, tree, vals);
        initialize(mid+1, to, index*2 + 1, tree, vals);
        tree[index] = gcd(tree[index*2], tree[index*2+1]);
//        System.out.println("from:"+from+" to:"+to+" index:"+index+" = "+tree[index]);

    }

//    private static int gcd(long a, long b) {
//        long k1 = a, k2=b;
//        while(b>0) {
//            long x = a%b;
//            a = b;
//            b = x;
//        }
////        System.out.println("gcd a:"+k1+" b:"+k2+" ="+a);
//
//        return (int)a;
//    }

    private static int gcd(long a, long b) {
        return (int)Math.min(a, b);
    }

    int[] batchGcds;
    int batch = 0;

    public void batchBuild(int[] nums) {
        int N = nums.length;
        batch = (int) Math.sqrt(nums.length);
        batchGcds = new int[N]; //doesnt matter :)

        for(int i=0; i<N; i+=batch) {
            batchGcds[i] = nums[i];
            for(int j=i; j<i+batch && j<N; j++) { // the last batch may have fewer than batch elements: be careful!
                batchGcds[i] = gcd(batchGcds[i], nums[j]);
            }
        }
    }

    public int batchQuery(int[]nums, int l, int r) {
        int gcd = nums[l];
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
        return gcd;
    }

    public void failingTest() {
        int[] nums = {-1516837964, 139843454, -1251420757, -1111639443, -745826926, -2102571199, -2125396415, 422414625, 651900712, -1252550944, 766258881, 1112232246, 945023913, -1905474184, -460965043, -599185429, -167881184, 1452380453, -1067449183, -111911909, 1649292743, -1165142327, -860178807, -1628680802, 986706841, -1932371443, 1380886930, 1254414063, -1834420955, 201032101, 1483318276, 791883022, 2085210276, 833819607, -868502002, 346970536, 1276018253, 1386886973, -1483135677, -1160072332};
        l(nums.length);
        int[] tree = new int[10 * nums.length];
        initialize(0, nums.length - 1, 1, tree, nums);

    }
    public void testImpls() {
        while(true) {
            System.out.println("starting new run");
            Random rand = new Random();
            int[] nums = new int[rand.nextInt(200_000)+1];
            int nn = nums.length;
            for (int i = 0; i < nn; i++) {
                nums[i] = rand.nextInt();
            }
            batchBuild(nums);
            int[] tree = new int[10 * nn];
            try {
                initialize(0, nn - 1, 1, tree, nums);
            } catch (Exception e) {
                l(nums);
            }
            int count = 0;

            while (true) {
                int r = rand.nextInt(nums.length);
                int l = rand.nextInt(r + 1);
                if (batchQuery(nums, l, r) != rangeMinimumQuery(l, r, 1, 0, nn - 1, tree)) {
                    throw new RuntimeException();
                }
                count++;
                if (count % 1000_000 == 0) {
                    l("yo", count);
                }
                if (count % 2_000_000 == 0) {
                    break;
                }


            }
        }



    }
    public void go() throws Exception {
                /*
         * Read input from stdin and provide input before running
         * Use either of these methods for input */


        //Scanner
        Scanner s = new Scanner(System.in);
//        Scanner s = new Scanner(new File("/Users/sharath.g/Downloads/test"));
        N = s.nextInt();
        Q = s.nextInt();
        int[] vals = new int[N];
        for(int i=0; i<N; i++) {
            vals[i] = fibonacci(s.nextInt()-1);
        }
//        System.out.println(Arrays.toString(vals));

        int[] tree = new int[3*N];
//        System.out.println("tree length "+tree.length);

        initialize(0, N-1, 1, tree, vals);
//        System.out.println(Arrays.toString(tree));

        for(int test=0; test<Q; test++) {
            int l = s.nextInt() - 1;
            int r = s.nextInt() - 1;
//            System.out.println(r);
            int gcd = rangeMinimumQuery(l, r, 1, 0, N-1, tree);
            System.out.println(gcd%mod);
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
