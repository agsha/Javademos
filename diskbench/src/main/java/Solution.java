import java.util.Scanner;

public class Solution {
    public static void main(String args[] ) throws Exception {
        Solution s = new Solution();
        s.go();

    }

    public void go() throws Exception {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt();
        for(int tcase=0; tcase<N; tcase++ ) {
            int n = sc.nextInt();
            if(!isPrime(n)) {
                System.out.println("false");
                continue;
            }


        }
    }

    public int digisum(int n) {
        int sum = 0;
        while(n>0) {
            sum+=(n%10);
            n/=10;
        }
        return sum;
    }

    public boolean isPrime(int n) {
        if(n<=1) return false;
        if(n==2) return true;
        if(n%2==0) return false;
        for(int x = 3; x*x <= n; x+=2) {
            if(n%x==0) return false;

        }
        return true;
    }
}
