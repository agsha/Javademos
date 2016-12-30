package pranav;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static sha.Utils.*;

public class App 
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            App obj = new App();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", dumps(s));
            obj.go2();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }


    /**
     * All teh code from here:
     */
    static class Raw {
        public ArrayList<As> asteroids = new ArrayList<>();
        public int t_per_blast_move = 0;
    }
    static class As {
        public int t_per_asteroid_cycle;
        public int offset;
    }

    public void go2() throws Exception {
        Raw raw = readJson("/Users/sharath.g/Downloads/chart.json", Raw.class);
//        l(raw.asteroids.size());
        ArrayList<As> as = raw.asteroids;
        int lcm = 60;
        int[][][] memo = new int[as.size()+10][180*2+4][60];
        int[][][] path = new int[as.size()+10][180*2+4][60];
        memo[0][180][0] = 1;
        LinkedList<Integer> q = new LinkedList<>();
        q.add(0);q.add(0);q.add(0);
        int aa[] = {-1, 0, 1};
        int ret = Integer.MAX_VALUE;
        int count = 0;

        int fp=0, fv=0, fs=0, fa = 0;
        while(q.size() > 0) {
            count++;
//            l(count);
            if(count % 1000000 ==0) {
                l(count);
            }
            int p = q.removeFirst(), v = q.removeFirst(), s = q.removeFirst();
            int t = memo[p][v+180][s];
            for (int a : aa) {
                int vv = v + a;
                int pp = p + vv;
                int ss = s + 1;
//                l(pp, vv, ss);
                // death by hitting eschaton itself
                if (pp < 0) continue;
                // death by blast radius
                if(2*pp < t+1) {
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
                ss %= 60;
                if (memo[pp][vv + 180][ss] > 0) {
                    // found a loop
                    continue;
                }

                memo[pp][vv+180][ss] = t + 1;
                path[pp][vv+180][ss] = a;
                q.add(pp);q.add(vv);q.add(ss);
            }
        }
        List<Integer> result = new ArrayList<>();
        result.add(fa);
        while(fp!=0 || fv!=0||fs!=0) {
            int a = path[fp][fv+180][fs];
            result.add(a);
            fp -= fv;
            fv -= a;
            fs-=1; fs+=60; fs%=60;
        }
        Collections.reverse(result);
        l(result);

        l("minimum time is ", ret);

    }
    private void go() throws Exception {
        ObjectMapper m = new ObjectMapper();
        Raw raw = readJson("/Users/sharath.g/Downloads/pranav.json", Raw.class);
//        l(raw.asteroids.size());
        ArrayList<As> as = raw.asteroids;
        int lcm = 60;
//        for (As aa : as) {
//            lcm = lcm*aa.t_per_asteroid_cycle / gcd(lcm, aa.t_per_asteroid_cycle);
//        }
//        l(lcm);
        // state is (p, v, s)
        l(as.size());
        int sz = 12662;
        int[][][] memo = new int[as.size()][180*2+4][60];
        List<Integer> dfs = new ArrayList<>(as.size() * 182*2 * 60);
        int count = 0;
        int aa[] = {-1, 0, 1};
        int[] ret = new int[3];
        // v = 180 is 0
        dfs.add(encode(0, 180, 0));

        int debug = 0;

        while(count<dfs.size()) {
            decode(dfs.get(count++), ret);
            int p = ret[0], v = ret[1] - 180, s = ret[2];
            for (int a : aa) {
                int vv = v + a;
                int pp = p + v;
                int ss = s + 1;

                // death by hitting eschaton itself
                if(pp<0) continue;
                //escaped eschaton
                if(pp>as.size()) continue;
                // not on escaton itself
                if(pp > 0) {
                    As ass = as.get(pp - 1);
                    // death by hitting asteroid
                    if(( ass.offset + ss ) % ass.t_per_asteroid_cycle == 0) {
                        continue;
                    }
                }
                // blast radius will be taken care of during the actual dfs

                // check if already visited this state
                ss %= 60;
                try {
                    if (memo[pp][vv + 180][ss] == 0) {
                        if(debug++ % 1000000 == 0) {
                            l(debug);
                        }
                        memo[pp][vv + 180][ss] = 1;
                        dfs.add(encode(pp, vv+180, ss));
                    }
                } catch (Exception e) {
                    l(pp, vv+180, ss);
                    System.exit(0);
                }
            }
        }

        l(dfs.size());




//        int[][][] dfs = new int[raw.asteroids.size()][180][20]
    }

    public int encode(int p, int v, int s) {
        return (p<<16) | (v<<6) | (s);
    }

    public void decode(int v, int[]ret) {
        ret[0] = v>>16;
        ret[1] = v>>6 & ((1<<10) - 1);
        ret[2] = v & ((1<<6) - 1);
    }

    public int gcd(int a, int b) {
        if(b==0) return a;
        return gcd(b, a%b);
    }



}
