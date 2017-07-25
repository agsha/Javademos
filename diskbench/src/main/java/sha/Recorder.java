package sha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sharath.g on 12/21/16.
 */
public  class Recorder implements Utils.Timer.Printer, Utils.LatencyTimer.LatPrinter {
    private static final Logger log = LogManager.getLogger();

    String file;
    List<Holder> holders = new ArrayList<>();
    Holder current;
    ObjectMapper mapper = new ObjectMapper();
    Utils.Timer.DefaultPrinter dp = new Utils.Timer.DefaultPrinter();
    Utils.LatencyTimer.LatDefaultPrinter ldp = new Utils.LatencyTimer.LatDefaultPrinter();

    public Recorder(String simpleName) {
        file = Paths.get(System.getProperty("user.home"), simpleName).toString();
    }

    @Override
    public synchronized void log(String name, Utils.Timer.Ret ret) {
        if(ret.qps  > 100) {
            current.list.add(ret);
        }
        dp.log(name, ret);
    }

    @Override
    public synchronized void log(String name, Utils.LatencyTimer.LatRet ret) {
        current.latencyList.add(ret);
        ldp.log(name, ret);
    }

    public synchronized void startRun(Object object) {
        current = new Holder(object);
        holders.add(current);
    }

    public synchronized void finish() {
        try {
            JsonNode root = mapper.createObjectNode();
            ((ObjectNode)root).put("title", "Multithreaded cache effects");
            ((ObjectNode)root).put("description", "Multithreaded cache effects  number of cpus = "+Runtime.getRuntime().availableProcessors());
            ((ObjectNode)root).set("rawData", mapper.valueToTree(holders));
            Files.write(Paths.get(file), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes());
            System.out.println("finished dumping all data");
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int bound(int n, int min, int max) {
        n = Math.min(n, max);
        n = Math.max(n, min);
        return n;
    }
    public static void main(String[] args) throws IOException {
        Recorder hc = new Recorder(Recorder.class.getSimpleName());
        JsonNode n = hc.mapper.readTree(Files.readAllBytes(Paths.get(hc.file))).get("rawData");
    }


    public static class Holder {
        public final Object params;
        public final List<Utils.Timer.Ret> list = new ArrayList<>();
        public final List<Utils.LatencyTimer.LatRet> latencyList = new ArrayList<>();

        public Holder(Object params) {
            this.params = params;
        }
    }
}
