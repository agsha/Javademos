package sha;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

import static sha.Utils.readClasspathFile;
import static sha.Utils.readJsonFromClasspath;

public class App 
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    static ObjectMapper mapper = new ObjectMapper();

    public static void main( String[] args ) {
        try {
            App obj = new App();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            obj.go1();
//            log.info("Using settings:{}", dumps(s));
        } catch (Exception e) {
            log.error("", e);
        }
    }



    private void go() throws Exception{
        String s = readClasspathFile("schema.json");
        Def def = preProcess(s);
        String requestEvent = readClasspathFile("RequestEvent.json");
        sha.Utils.LatencyTimer timer = new sha.Utils.LatencyTimer("");
        JsonNode data = mapper.readTree(requestEvent).get("data");
        sha.Utils.Timer timer1 = new sha.Utils.Timer("");
        while(true) {
            timer.count();
            timer1.count();
            traverse(data, def);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }


    Map<String, List<Validator>> validators = new HashMap<>();
    StringBuilder errors = new StringBuilder();


    public Def preProcess(String schema) throws IOException {
        JsonNode jsonNode = mapper.readTree(schema);
        LinkedList<Qobject> q = new LinkedList<>();
        Def def = new Def("$","object", true, false, null);
        Qobject initial = new Qobject(def, jsonNode);
        q.add(initial);
        while(q.size() > 0) {
            Qobject qobjNow = q.remove();
            Def defNow = qobjNow.def;
            JsonNode jsonNodeNow = qobjNow.jsonNode;
            log.info("defno:{} ", defNow);


            Set<String> requiredSet = new HashSet<>();
            ArrayNode requiredNode = (ArrayNode) jsonNodeNow.get("required");
            if(requiredNode != null) {
                for (int i = 0; i < requiredNode.size(); i++) {
                    requiredSet.add(requiredNode.get(i).asText());
                }
            }

            Iterator<Map.Entry<String, JsonNode>> properties = jsonNodeNow.path("properties").fields();
            while(properties.hasNext()) {
                Map.Entry<String, JsonNode> propertyEntry = properties.next();
                String propertyName = propertyEntry.getKey();
                JsonNode propertyValue = getPropertyValue(propertyEntry.getValue());
                boolean repeated = propertyValue.get("type").asText().equals("array");
                if(repeated) {
                    propertyValue = propertyValue.get("items");
                }
                String type =  propertyValue.get("type").asText();
                boolean required = requiredSet.contains(propertyName);
                Def defNext = new Def(propertyName, type, required, repeated, defNow);
                defNow.children.put(propertyName, defNext);
                // TODO: add validators
                Qobject nextObt = new Qobject(defNext, propertyValue);
                q.add(nextObt);
            }
        }
        return def;
    }

    private JsonNode getPropertyValue(JsonNode value) {
        ArrayNode anyOf = (ArrayNode)value.get("anyOf");
        if(anyOf == null) {
            return value;
        }
        for(int i=0; i<anyOf.size(); i++) {
            if(!anyOf.get(i).get("type").asText().equals("null")) {
                return anyOf.get(i);
            }
        }
        throw new RuntimeException("couldnt find any property");
    }


    /**
     * All teh code from here:
     */
    private void traverse(JsonNode root, Def defRoot) throws Exception {
        LinkedList<Node> q = new LinkedList<>();
        q.add(new Node("root", root, "root", defRoot, false));
        while (q.size() > 0) {
            Node node = q.remove();
//            log.info("validating {}", node.name);
            JsonNode jsonNodeNow = node.value;
            Def defNow = node.def;
            if (jsonNodeNow instanceof ObjectNode) {
                if(!defNow.type.equals("object")) {
                    throw new RuntimeException("expected object");
                }
                Iterator<Map.Entry<String, JsonNode>> fields = jsonNodeNow.fields();
                while(fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    Def defChild = defNow.children.get(entry.getKey());
                    q.add(new Node(entry.getKey(), entry.getValue(), node.fqn +"."+entry.getKey(), defChild, false));
                }
            } else if (jsonNodeNow instanceof ArrayNode) {
                if(!defNow.repeated) {
                    throw new RuntimeException("uneexpected array");
                }
                for(int i=0; i<jsonNodeNow.size(); i++) {
                    q.add(new Node(node.name, jsonNodeNow.get(i), node.fqn +"["+i+"]", defNow, true));
                }
            } else {
                switch (defNow.type) {
                    case "string":
                        if(!(node.value instanceof TextNode)) {
                            throw new RuntimeException(String.format("name:%s with fqn:%s is defined as %s but jsonnode class type is %s", node.name, node.fqn, defNow.type, jsonNodeNow.getClass()));
                        }
                        break;
                    case "number":
                    case "integer":
                        if(!(node.value instanceof NumericNode)) {
                            throw new RuntimeException(String.format("name:%s with fqn:%s is defined as %s but jsonnode class type is %s", node.name, node.fqn, defNow.type, jsonNodeNow.getClass()));
                        }
                        break;
                    case "boolean":
                        if(!(node.value instanceof BooleanNode)) {
                            throw new RuntimeException(String.format("name:%s with fqn:%s is defined as %s but jsonnode class type is %s", node.name, node.fqn, defNow.type, jsonNodeNow.getClass()));
                        }
                        break;
                    default:
                        throw new RuntimeException(String.format("name:%s with fqn:%s is defined as %s but jsonnode class type is %s", node.name, node.fqn, defNow.type, jsonNodeNow.getClass()));


                }
            }
        }
    }

    private void validate(Node node) {
        if(node.def.type.equals("string")) {

        }
    }


    public static class Node {
        public String name;
        public JsonNode value;
        public String fqn;
        public Def def;
        public boolean expanded;

        public Node(String name, JsonNode value, String fqn, Def def, boolean expanded) {
            this.name = name;
            this.value = value;
            this.fqn = fqn;
            this.def = def;
            this.expanded = expanded;
        }
    }

    public static class Qobject{
        public Def def;
        public JsonNode jsonNode;

        public Qobject(Def def, JsonNode jsonNode) {
            this.def = def;
            this.jsonNode = jsonNode;
        }
    }

    public static class Def {
        String name;
        String type;
        boolean required;
        boolean repeated;
        Def parent;

        List<Validator> validators = new ArrayList<>();
        Map<String, Def> children = new HashMap<>();

        public Def(String name, String type, boolean required, boolean repeated, Def parent) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.repeated = repeated;
            this.parent = parent;
        }

        @Override
        public String toString() {
            return "Def{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", required=" + required +
                    ", repeated=" + repeated +
                    ", parent=" + (parent == null?"null":parent.name) +

                    '}';
        }
    }

    public Set<ValidationMessage> jsonSchemaValidate(JsonSchema jsonSchema, JsonNode jsonNode) {
        return jsonSchema.validate(jsonNode);
    }

    protected JsonSchema getJsonSchemaFromStringContent(String schemaContent) throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        JsonSchema schema = factory.getSchema(schemaContent);
        return schema;
    }

}
