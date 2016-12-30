package pranav;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class Eschaton
{

    public static void main( String[] args ) {
        Eschaton obj = new Eschaton();
        obj.go2();
    }


    /**
     * All teh code from here:
     */

    public void go2() {
        Raw raw = readJson("/Users/sharath.g/Downloads/chart.json", Raw.class);
//        System.out.println(raw.asteroids.size());
        ArrayList<As> as = raw.asteroids;
        int lcm = 1;
        for (As aa : as) {
            lcm = lcm*aa.t_per_asteroid_cycle / gcd(lcm, aa.t_per_asteroid_cycle);
        }
        
        int maxV = getMax(as.size()) + 5;
        int[][][] memo = new int[as.size()+10][maxV*2][lcm];
        int[][][] path = new int[as.size()+10][maxV*2][lcm];
        memo[0][180][0] = 1;
        LinkedList<Integer> q = new LinkedList<>();
        q.add(0);q.add(0);q.add(0);
        int aa[] = {-1, 0, 1};
        int ret = Integer.MAX_VALUE;
        int count = 0;

        int fp=0, fv=0, fs=0, fa = 0;
        while(q.size() > 0) {
            count++;
            if(count % 1000000 ==0) {
                System.out.println("number of nodes visited: "+count);
            }
            int p = q.removeFirst(), v = q.removeFirst(), s = q.removeFirst();
            int t = memo[p][v+maxV][s];
            for (int a : aa) {
                int vv = v + a;
                int pp = p + vv;
                int ss = s + 1;
                // death by hitting eschaton itself
                if (pp < 0) continue;
                // death by blast radius
                if(raw.t_per_blast_move*pp < t+1) {
                    continue;
                }
                //escaped eschaton
                if (pp > as.size()) {
                    if(ret > t) {
                        ret = t;
                        fp = p; fv = v; fs = s; fa = a;
                    }
                    continue;
                }
                // not on escaton itself
                if (pp > 0) {
                    As ass = as.get(pp - 1);
                    // death by hitting asteroid
                    if ((ass.offset + ss) % ass.t_per_asteroid_cycle == 0) {
                        continue;
                    }
                }
                ss %= lcm;
                if (memo[pp][vv + 180][ss] > 0) {
                    // found a loop
                    continue;
                }

                memo[pp][vv+maxV][ss] = t + 1;
                path[pp][vv+maxV][ss] = a;
                q.add(pp);q.add(vv);q.add(ss);
            }
        }

        //reconstruct the required accelarations
        List<Integer> result = new ArrayList<>();
        result.add(fa);
        while(fp!=0 || fv!=0||fs!=0) {
            int a = path[fp][fv+maxV][fs];
            result.add(a);
            fp -= fv;
            fv -= a;
            fs-=1; fs+=lcm; fs%=lcm;
        }
        Collections.reverse(result);
        System.out.println("printing the course plan:");
        System.out.println(result);

        System.out.println("minimum time is "+ ret);

    }
    public static <T> T readJson(String path, Class<T> claz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(Files.readAllBytes(Paths.get(path)), claz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getMax(int n) {
        int r = 0;
        while(n>=0) {
            n-=r;
            r++;
        }
        return r;
    }

    public int gcd(int a, int b) {
        if(b==0) return a;
        return gcd(b, a%b);
    }

    static class Raw {
        public ArrayList<As> asteroids = new ArrayList<>();
        public int t_per_blast_move = 0;
    }
    static class As {
        public int t_per_asteroid_cycle;
        public int offset;
    }


}
