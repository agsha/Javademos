package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static sha.Utils.prettifyJson;
import static sha.Utils.readJsonFromClasspath;

/**
 * Created by sharath.g on 03/04/16.
 */
public class Brackets {
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Brackets obj = new Brackets();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
            log.info("Using settings:{}", prettifyJson(s));
            obj.go();
        } catch (Exception e) {
            log.error("err", e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }

        @Override
        public String toString() {
            return "";
        }
    }


    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        log.debug(solve("())"));
    }

    public long solve(String s) {
        int n = s.length();
        long[][] memo = new long[n+1][n+1];
        for(int i=0; i+1<n; i++) {
            String ss = s.substring(i, i+2);
            memo[2][i] = ss.equals("()") || ss.equals("[]") ? 1 : 0;
        }

        for(int len=3; len<=n; len++) {
            for(int left=0, right=left+len-1; right<n; left++, right++) {
                memo[len][left] += memo[len-1][left+1];
                for(int k=left+1; k<=right; k++) {
                    if(!( (s.charAt(left)=='(' && s.charAt(k)==')') || (s.charAt(left)=='[' && s.charAt(k)==']'))) {
                        continue;
                    }
                    long a = memo[k-left-1][left+1]+1;
                    // from k+1 to left
                    long b = 1;
                    if(k<right) {
                        b = memo[right-k][k+1]+1;
                    }
                    memo[len][left] += a*b;
                }
            }
        }
        //log.debug(Arrays.deepToString(memo));
        return memo[n][0];

    }

}
