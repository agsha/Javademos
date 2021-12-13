package sha.digisign;


import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import sha.Utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import static sha.Utils.readJsonFromClasspath;

@Slf4j
public class GenSig
{
    private static Settings s;

    public static void main( String[] args ) {
        try {
            GenSig obj = new GenSig();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go2();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void go2() throws Exception{
        System.out.println(DateTime.now().getMillis());
        System.out.println(DateTime.now().toDate());
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
    private void go() throws NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException, IOException, NoSuchProviderException, InvalidKeyException, SignatureException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(512, random);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();
        byte[] data = new Utils.RandomString().random(1024).getBytes();
        Utils.Timer t = new Utils.Timer("");
            Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
            dsa.initSign(priv);
        while(true) {
            dsa.update(data);
            byte[] realSig = dsa.sign();
            t.count();
        }
//        System.out.println(Arrays.toString(realSig));

    }

}
