package hello.jacksondemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * mvn compile exec:java -Dexec.mainClass="hello.jacksondemo.JacksonDemo"
 * @author sgururaj
 */
public class JacksonDemo {
    private static final Logger log = LogManager.getLogger();
    public static void main(String[] args) throws IOException {
        JacksonDemo demo = new JacksonDemo();
        demo.start();

    }

    public void start() throws IOException {
        MyPojo pojo = new MyPojo();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pojo);
        log.debug(json);

        MyPojo newPojo = mapper.readValue(json, MyPojo.class);
        log.debug(newPojo);
        assertTrue(pojo.equals(newPojo));  // the equals method is overridden

    }

    public static class MyPojo {
        public String s = "embedded string";
        public int i = Integer.MAX_VALUE - 100;
        public boolean b = true;
        public double d = Math.PI;
        public String[] sa = new String[] {"a", "string", "array"};
        public List<String> ls = ImmutableList.of("test", "arraylist");
        public Map<String, String> ms = ImmutableMap.of("key", "value");
        public Nested nested = new Nested(4); // 4 levels of nesting!

        @Override
        public boolean equals(Object obj) {
            MyPojo p = (MyPojo)obj;
            return Objects.deepEquals(s, p.s)
                    &&Objects.deepEquals(i, p.i)
                    &&Objects.deepEquals(b, p.b)
                    &&Math.abs(d - p.d) < 1e-7
                    &&Objects.deepEquals(sa, p.sa)
                    &&Objects.deepEquals(nested, p.nested);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("s", s)
                    .add("i", i)
                    .add("b", b)
                    .add("d", d)
                    .add("sa", sa)
                    .add("ls", ls)
                    .add("ms", ms)
                    .add("nested", nested).toString();
        }
    }

    public static class Nested {
        public int nestingLevelInt = 0;
        public Map<String, Nested> map = new HashMap<>();
        public List<Nested> list = new ArrayList<>();
        String nestingLevelString = "";

        // default constructor required for jackson
        public Nested(){
        }

        public Nested(int nestingLevel) {
            if(nestingLevel<=0) return;
            Nested nested = new Nested(nestingLevel-1);
            map.put(""+nestingLevel, nested);
            list.add(nested);
            nestingLevelInt = nestingLevel;
            nestingLevelString = nestingLevel+"";
        }

        @Override
        public boolean equals(Object obj) {
            Nested o = (Nested)obj;
            return Objects.deepEquals(nestingLevelInt, o.nestingLevelInt)
                    &&Objects.deepEquals(list, o.list)
                    &&Objects.deepEquals(map, o.map);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("nestingLevel", nestingLevelInt)
                    .add("map", map)
                    .add("list", list)
                    .toString();
        }
    }
}
