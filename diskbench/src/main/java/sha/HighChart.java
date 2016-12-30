package sha;

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
public  class HighChart extends Utils.LatencyTimer.LatDefaultPrinter {

    List<Run> runs = new ArrayList<>();
    Run currentRun;
    ObjectMapper mapper = new ObjectMapper();
    @Override
    public synchronized void log(String name, Utils.LatencyTimer.LatRet ret) {
        currentRun.list.add(ret);
        super.log(name, ret);
    }

    public synchronized void startRun(int threads) {
        currentRun = new Run(threads);
        runs.add(currentRun);
    }
    public synchronized void  finish() {
        try {
            JsonNode root;
            root = mapper.readTree(ClassLoader.getSystemResourceAsStream("chart.json"));
            ((ObjectNode)root).put("title", "Threads trying to increment an AtomicLong");
            ((ObjectNode)root).put("desc", "Threads trying to increment an AtomicLong description");
            ((ObjectNode)root).set("rawData", mapper.valueToTree(runs));
            ArrayNode charts = (ArrayNode) root.path("charts");



            ArrayNode labels = mapper.createArrayNode();
            ((ObjectNode)charts.path(0).path("highchart").path("xAxis")).set("categories", labels);
            int nPtiles = runs.get(0).list.get(0).pTiles.length;

            // add the x labels
            for (int p=0; p>runs.get(0).list.get(0).pTiles.length; p++) {
                double pTile = runs.get(0).list.get(0).pTiles[p];
                labels.add(pTile);
            }

            ArrayNode seriesArray = (ArrayNode)charts.path(0).path("highchart").path("series");
            ObjectNode seriesObjTemplate = (ObjectNode)seriesArray.path(0);

            // add the series data
            for (int r=0; r<runs.size(); r++) {
                Run run = runs.get(r);
                int n = run.list.size();
                int f = n-4; f = bound(f, 0, n-1);
                int l = n-2; l = bound(l, 0, n-1);
                ObjectNode seriesObj = seriesObjTemplate.deepCopy();
                seriesArray.add(seriesObj);

                ArrayNode data = (ArrayNode)seriesObj.get("data");
                data.removeAll();

                for (int p=0; p<nPtiles; p++) {

                    long sum = 0;
                    for(int i=f; i<=l; i++) {
                        sum += run.list.get(i).nanos[p];
                    }
                    double val = sum/(l-f+1);
                    data.add(val);
                }
            }

            Files.write(Paths.get("/Users/sharath.g/firstChart.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes());



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
    public static void main(String[] args) {
        HighChart hc = new HighChart();
        hc.finish();
    }

    class Run implements Serializable {
        public int threads;
        public List<Utils.LatencyTimer.LatRet> list = Collections.synchronizedList(new ArrayList<Utils.LatencyTimer.LatRet>());

        public Run(int threads) {
            this.threads = threads;
        }

        public Run() {
        }
    }

}
