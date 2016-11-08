package sha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by sharath.g on 09/06/15.
 */
public class Utils {

    public static final RandomString rs = new RandomString();
    private static final Logger log = LogManager.getLogger();
    public static void main(String[] args) {
        //log.debug("{}", httpPostJson("http://posttestserver.com/post.php", "f u"));
        //log.debug("{}", httpPostJson("http://httpbin.org/post", "f u"));
    }

    public static Document readXml(String path) {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            return builder.parse(Files.newInputStream(Paths.get(path)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static Node evalXpathNode(Document doc, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (Node)xPath.compile(exp).evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static Node evalXpathNode(Node node, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (Node)xPath.compile(exp).evaluate(node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }


    public static NodeList evalXpathNodeList(Document doc, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (NodeList)xPath.compile(exp).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }


    public static NodeList evalXpathNodeList(Node node, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (NodeList)xPath.compile(exp).evaluate(node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void walk(String path, String pattern, final FileVisitor<Path> fv) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        try {
            Files.walkFileTree(Paths.get(path), new FileVisitor<Path>(){

                boolean ok(Path path) {
                    return matcher.matches(path);
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if(!ok(dir)) return FileVisitResult.CONTINUE;
                    return fv.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!ok(file)) return FileVisitResult.CONTINUE;
                    return fv.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if(!ok(file)) return FileVisitResult.CONTINUE;
                    return fv.visitFileFailed(file, exc);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if(!ok(dir)) return FileVisitResult.CONTINUE;
                    return fv.postVisitDirectory(dir, exc);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T> T readJson(String path, Class<T> claz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(Files.readAllBytes(Paths.get(path)), claz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readJson(String path, TypeReference tp) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(Files.readAllBytes(Paths.get(path)), tp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // returns a random int between min inclusive and max exclusive
    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }
    public static <T> T readJsonFromClasspath(String path, TypeReference tp) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(ClassLoader.getSystemResourceAsStream(path), tp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readJsonFromClasspath(String path, Class<T> claz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(ClassLoader.getSystemResourceAsStream(path), claz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJson(String path, Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String s = mapper.writeValueAsString(obj);
            Files.write(Paths.get(path), s.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readClasspathFile(String fileName) {
        try {
            return IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettifyJson(String json, ObjectMapper mapper) {
        try {
            Object o = mapper.readValue(json, Object.class);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettifyJson(String json) {
        return prettifyJson(json, new ObjectMapper());
    }

    public static String prettifyJson(Object o) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class RandomString {
        private SecureRandom random = new SecureRandom();
        public String random(int len) {
            return new BigInteger(len*8, random).toString(32);
        }
    }

    public static String prettifyJsonNode(JsonNode node) {
        return prettifyJson(node.toString());
    }
    public static String writeJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class LatencyTimer extends Thread {
        private final String name;
        private LatPrinter printer;
        // bin i (0 based) holds the count from [1000*(i) nanos, 1000*(i+1)nanos)
        // the last element holds the count from [1000*i to oo)
        public AtomicLong[]bins = new AtomicLong[2000];
        int[] pTiles = new int[]{90, 92, 94, 96, 98};


        public LatencyTimer(String name) {
            this(new LatDefaultPrinter(), name);
        }


        public LatencyTimer(Class name) {
            this(new LatDefaultPrinter(), name.getName());
        }

        public LatencyTimer(LatPrinter p) {
            this(p, "noname");
        }

        public LatencyTimer(LatPrinter p, String name) {
            this.name = name;
            this.printer = p;
            for(int i=0; i<bins.length; i++) {
                bins[i] = new AtomicLong(0);
            }
            setDaemon(true);
            start();
        }

        public void setPrinter(LatPrinter printer) {
            this.printer = printer;
        }

        AtomicLong total = new AtomicLong(0);
        public void count(long latencyNanos) {
            long latencyMicros = Math.min(latencyNanos/1000, bins.length-1);
            total.incrementAndGet();
            bins[(int)latencyMicros].incrementAndGet();
        }

        @Override
        public void run() {
            while(true) {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                printer.log(name, snap());
            }
        }



        public interface LatPrinter {
            void log(String name, LatRet ret);
        }


        public void doLog() {
            printer.log(name, snap());
        }

        private LatRet snap() {
            long[] mybins = new long[bins.length];
            for(int i=0; i<mybins.length; i++) {
                mybins[i] = bins[i].get();
            }
            long mytotal = total.get();

            double[] millis = new double[pTiles.length];

            int index = 0;
            int cumulative = 0;
            outer:for(int i=0; i<pTiles.length; i++) {
                long max = ((mytotal*pTiles[i])/100);
                while(mybins[index] + cumulative <= max) {
                    index++;
                    if(index>=mybins.length) break outer;
                    cumulative+=mybins[index];
                }
                millis[i] = (index+1)/1000.0;
            }
            return new LatRet(total.get(), millis, pTiles);
        }

        public static class LatDefaultPrinter implements LatPrinter {
            @Override
            public void log(String name, LatRet ret) {
                log.debug("name:{}, {}",name, ret);
            }
        }

        public static class LatRet {
            public double[] millis;
            public int[] pTiles;
            public long total;

            @Override
            public String toString() {
                String s = "total:"+total+" ";
                DecimalFormat df = new DecimalFormat("#.0000");
                for(int i=0; i<millis.length; i++) {
                    s+=pTiles[i]+"%: < "+df.format(millis[i])+"ms ";
                }
                return "Latencies: "+s;
            }

            public LatRet(long total, double[] millis, int[] pTiles) {
                this.millis = millis;
                this.pTiles = pTiles;
                this.total = total;
            }
        }
    }

    public static class Timer extends Thread {

        private String name;
        long beginTime = -1, lastSnapshotTime = -1;
        AtomicLong opsSoFar = new AtomicLong(0);
        AtomicLong opsSinceLastSnapshot = new AtomicLong(0);
        private Printer printer;
        public AtomicBoolean enabled = new AtomicBoolean(true);

        public void reset() {
            lastSnapshotTime = beginTime = System.nanoTime();
            opsSinceLastSnapshot.set(0);
            opsSoFar.set(0);
            log.debug("======resetting timer====");
        }

        public Timer(String name) {
            this(new DefaultPrinter(), name);
        }


        public Timer(Class name) {
            this(new DefaultPrinter(), name.getName());
        }

        public Timer(Printer p) {
            this(p, "noname");
        }

        public Timer(Printer p, String name) {
            this.name = name;
            this.printer = p;
            setDaemon(true);
            reset();
            start();
        }

        public void setPrinter(Printer printer) {
            this.printer = printer;
        }

        @Override
        public void run() {
            reset();

            while(true) {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(enabled.get()) doLog();
                if(interrupted()) break;
            }
        }


        public void doLog() {
            printer.log(name, snap());
        }

        public void count(long opsSinceLastCall) {
            opsSinceLastSnapshot.addAndGet(opsSinceLastCall);
        }

        public void count() {
            opsSinceLastSnapshot.incrementAndGet();
        }

        public void cumulativeCount(long cumulative) {
            opsSinceLastSnapshot.addAndGet(cumulative - opsSinceLastSnapshot.get());
        }

        public Ret snap() {
            if(beginTime <0) throw new RuntimeException("not initialized");
            long now = System.nanoTime();
            long ops = opsSinceLastSnapshot.getAndSet(0);
            long cumulativeOps = opsSoFar.addAndGet(ops);

            double qps = ops *1e9/(now- lastSnapshotTime);
            double totalqps = (cumulativeOps+ops)*1e9/(now - beginTime);

            lastSnapshotTime = now;

            return new Ret((long)qps, (long)totalqps, ops, opsSoFar.get());
        }

        public static class Ret {
            public long qps, totalQps, ops, totalOps;

            public Ret(long qps, long totalQps, long ops, long totalOps) {
                this.qps = qps;
                this.totalQps = totalQps;
                this.ops = ops;
                this.totalOps = totalOps;
            }

            @Override
            public String toString() {
                return "Ret{" +
                        "qps=" + qps +
                        ", totalQps=" + totalQps +
                        ", ops=" + ops +
                        ", totalOps=" + totalOps +
                        '}';
            }
        }

        public interface Printer {
            void log(String name, Ret ret);
        }

        public static class DefaultPrinter implements Printer {
            @Override
            public void log(String name, Ret ret) {
                log.debug("name:{}, {}",name, ret);
            }
        }
    }

    public static class BadStatusCodeException extends RuntimeException {
        public BadStatusCodeException(String s) {
            super(s);
        }
    }
}
