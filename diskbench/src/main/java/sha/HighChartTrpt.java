package sha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by sharath.g on 12/21/16.
 */
public  class HighChartTrpt extends Utils.Timer.DefaultPrinter {
    String file;
    List<Run> runs = new ArrayList<>();
    Run currentRun;
    ObjectMapper mapper = new ObjectMapper();

    public HighChartTrpt(String simpleName) {
        file = Paths.get(System.getProperty("user.home"), simpleName).toString();
    }

    @Override
    public synchronized void log(String name, Utils.Timer.Ret ret) {
        currentRun.list.add(ret);
        super.log(name, ret);
    }

    public synchronized void startRun(int threads, int workingSet) {
        currentRun = new Run(threads, workingSet);
        runs.add(currentRun);
    }

    public synchronized void finishLite() {
        try {
            JsonNode root;
            root = mapper.readTree(ClassLoader.getSystemResourceAsStream("chart.json"));
            ((ObjectNode)root).put("title", "Threads trying to increment a threadlocal integer");
            ((ObjectNode)root).put("description", "Threads trying to increment a threadlocal integer number of cpus = "+Runtime.getRuntime().availableProcessors());
            ((ObjectNode)root).set("rawData", mapper.valueToTree(runs));
            Files.write(Paths.get(file), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes());

            System.out.println("finished dumping contents to file");

//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void  finish() {
        try {
            JsonNode root;
            root = mapper.readTree(ClassLoader.getSystemResourceAsStream("chart.json"));
            ((ObjectNode)root).put("title", "Threads trying to increment a threadlocal integer");
            ((ObjectNode)root).put("description", "Threads trying to increment a threadlocal integer number of cpus = "+Runtime.getRuntime().availableProcessors());
            ((ObjectNode)root).set("rawData", mapper.valueToTree(runs));
            ArrayNode charts = (ArrayNode) root.path("charts");



            ArrayNode labels = mapper.createArrayNode();
            ((ObjectNode)charts.path(0).path("highchart").path("title")).put("text", "Threads trying to increment a variable");
            ((ObjectNode)charts.path(0).path("highchart").path("xAxis")).set("categories", labels);
            ((ObjectNode)charts.path(0).path("highchart").path("xAxis").path("title")).put("text", "number of threads");
            ((ObjectNode)charts.path(0).path("highchart").path("yAxis").path("title")).put("text", "total number of increments across all threads");

            for (Run run : runs) {
                labels.add(run.threads);
            }


            ArrayNode seriesArray = (ArrayNode)charts.path(0).path("highchart").path("series");
            ObjectNode seriesObjTemplate = (ObjectNode)seriesArray.path(0);
            seriesArray.removeAll();

            ObjectNode seriesObj = seriesObjTemplate.deepCopy();
            seriesObj.put("name", "");
            seriesArray.add(seriesObj);

            ArrayNode data = (ArrayNode)seriesObj.get("data");
            data.removeAll();


            // add the series data
            for (int r=0; r<runs.size(); r++) {
                Run run = runs.get(r);
                int n = run.list.size();
                int f = n-4; f = bound(f, 0, n-1);
                int l = n-2; l = bound(l, 0, n-1);
                long sum = 0;
                for(int i=f; i<=l; i++) {
                    sum += run.list.get(i).qps;
                }
                double val = sum/(l-f+1);
                data.add(val);

            }

            Files.write(Paths.get(file), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes());



            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static int bound(int n, int min, int max) {
        n = Math.min(n, max);
        n = Math.max(n, min);
        return n;
    }
    public static void main(String[] args) throws IOException {
        HighChartTrpt hc = new HighChartTrpt(HighChartTrpt.class.getSimpleName());
        JsonNode n = hc.mapper.readTree(Files.readAllBytes(Paths.get(hc.file))).get("rawData");
        String s = hc.mapper.writeValueAsString(n);
        hc.runs = hc.mapper.readValue(s, new TypeReference<List<Run>>() {
        });


        hc.finish();
    }

    public static class Run implements Serializable {
        public int threads;
        public int workingSet;
        public List<Utils.Timer.Ret> list = Collections.synchronizedList(new ArrayList<Utils.Timer.Ret>());

        public Run(int threads, int workingSet) {
            this.threads = threads;
            this.workingSet = workingSet;
        }

        public Run() {
        }
    }

}
