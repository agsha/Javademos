package sha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by sharath.g on 12/21/16.
 */
public class HighChart implements Utils.LatencyTimer.LatPrinter {

    public List<Utils.LatencyTimer.LatRet> list = Collections.synchronizedList(new ArrayList<Utils.LatencyTimer.LatRet>());
    ObjectMapper mapper = new ObjectMapper();
    private final JsonNode tree;

    public HighChart() {
        try {
            tree = mapper.readTree(ClassLoader.getSystemResourceAsStream("chart.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void log(String name, Utils.LatencyTimer.LatRet ret) {
        list.add(ret);
    }

    public void finishRun() {
        List<Utils.LatencyTimer.LatRet> copy = new ArrayList<>();
        synchronized (list) {
            copy = new ArrayList<>(list);
            list.clear();
        }
        //do something with copy


    }

}
