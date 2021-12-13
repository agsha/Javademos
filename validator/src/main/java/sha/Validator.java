package sha;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.ImmutableMap;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static sha.Utils.readClasspathFile;
import static sha.Utils.readJsonFromClasspath;

public class Validator
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            Validator obj = new Validator();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
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
    sha.Utils.LatencyTimer timer = new sha.Utils.LatencyTimer("");
    sha.Utils.Timer timer1 = new sha.Utils.Timer("");

    static ObjectMapper mapper = new ObjectMapper();

    public Validator() {
        init();
    }

    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        init();
        String s = readClasspathFile("schema.json");
        NodeDef def = preProcess(s);
        String json = readClasspathFile("RequestEvent.json");
        NodeDef pps = preProcess(readClasspathFile("pps.json"));
        JsonNode jsonNode = mapper.readTree(json);
        JsonNode j2 = jsonNode.get("children").get("fkint/cp/ca_discover/ProductPageServed").get(0).get("data");
        jsonNode = jsonNode.get("data");
        ValidationError validate;
        while (true) {
            timer1.count();
            timer.count();
            validate = validate(jsonNode, def);
            if (validate.errors != null) {
                log.error("validation errors:{} ", String.join("\n", validate.errors));
            }

            validate = validate(j2, pps);
            if (validate.errors != null) {
                log.error("validation errors:{} ", String.join("\n", validate.errors));
            }
//            return;

        }
    }

    public void go2() throws Exception {
        String s = readClasspathFile("schema.json");
        String s1 = readClasspathFile("pps.json");
        JsonSchema m1 = getJsonSchemaFromStringContent(s);
        JsonSchema m2 = getJsonSchemaFromStringContent(s1);
        JsonNode j1 = mapper.readTree(readClasspathFile("RequestEvent.json"));
        JsonNode j2 = j1.get("children").get("fkint/cp/ca_discover/ProductPageServed").get(0).get("data");
        j1 = j1.get("data");
        while(true) {
            timer1.count();
            timer.count();
            Set<ValidationMessage> validationMessages = jsonSchemaValidate(m1, j1);
            if(validationMessages.size() > 0) {
                log.info("{}", validationMessages);
            }
            Set<ValidationMessage> set = jsonSchemaValidate(m2, j2);
            if(set.size() > 0) {
                log.info("{}", set);
            }
        }


    }

    private void init() {
        ImmutableMap.Builder builder = new ImmutableMap.Builder();
        builder.putAll(ImmutableMap.of(ObjectNode.class, "object", ArrayNode.class, "array", NumericNode.class, "integer", BooleanNode.class, "boolean", NullNode.class, "null"));
        builder.put(TextNode.class, "string");
        builder.put(LongNode.class, "integer");
        builder.put(IntNode.class, "integer");
        builder.put(DoubleNode.class, "integer");
        builder.put(FloatNode.class, "integer");

        classToTypeMap = builder.build();
    }

    public NodeDef preProcess(String schema) throws IOException {
        JsonNode jsonNode = mapper.readTree(schema);
        LinkedList<Qobject> q = new LinkedList<>();
        NodeDef nodeDef = new NodeDef("root");
        Qobject initial = new Qobject(nodeDef, jsonNode);
        q.add(initial);
        while(q.size() > 0) {
            Qobject qobjectNow = q.remove();
            NodeDef defNow = qobjectNow.def;
            JsonNode jsonNodeNow = qobjectNow.jsonNode;
            defNow.requiredFields = getRequiredFields(jsonNodeNow);
            addType(jsonNodeNow, q, defNow);
            addAllowedTypes(jsonNodeNow, q, defNow);
        }
        return nodeDef;
    }

    private void addAllowedTypes(JsonNode jsonNode, LinkedList<Qobject> q, NodeDef defNow) {
        ArrayNode anyOf = (ArrayNode)jsonNode.get("anyOf");
        if(anyOf == null) {
            return;
        }
        for(int i=0; i<anyOf.size(); i++) {
            addType(anyOf.get(i), q, defNow);
        }
    }

    private void addType(JsonNode jsonNode, LinkedList<Qobject> q, NodeDef defNow) {
        if(!jsonNode.hasNonNull("type")) {
            return;
        }
        String type = jsonNode.get("type").asText();
        // TODO remove this hack
        if(type.equals("number")) {
            type = "integer";
        }
        NodeType nodeType = new NodeType(type);
        addValidators(jsonNode, nodeType);
        switch (type) {
            case "array":
                NodeDef nodeDef = new NodeDef("arrayChild");
                nodeType.children.put("arrayChild", nodeDef);
                q.add(new Qobject(nodeDef, jsonNode.get("items")));
                break;
            case "object":
                addProperties(q, nodeType, jsonNode);
                break;
        }
        defNow.allowedTypes.put(type, nodeType);
    }

    private void addValidators(JsonNode jsonNode, NodeType nodeType) {
        addRangeValidator(jsonNode.path("minimum"), jsonNode.path("maximum"), nodeType);
        addRangeValidator(jsonNode.path("minLength"), jsonNode.path("maximum"), nodeType);
        addRangeValidator(jsonNode.path("minItems"), jsonNode.path("maxItems"), nodeType);

        if(jsonNode.hasNonNull("enum")) {
            AllowedValuesValidator validator = new AllowedValuesValidator();
            JsonNode anEnum = jsonNode.get("enum");
            for(int i=0; i<anEnum.size(); i++) {
                validator.addAllowedValue(anEnum.get(i).asText());
            }
            nodeType.validators.add(validator);
        }
        if(jsonNode.hasNonNull("pattern")) {
            PatternValidator validator = new PatternValidator(jsonNode.get("pattern").asText());
            nodeType.validators.add(validator);
        }
    }

    public void addRangeValidator(JsonNode min, JsonNode max, NodeType nodeType) {
        if(min instanceof DoubleNode || min instanceof FloatNode) {
            nodeType.validators.add(new RangeDoubleValidator(min.asDouble(-Double.MAX_VALUE), max.asDouble(Double.MAX_VALUE)));
        }
        if(min instanceof IntNode || min instanceof ShortNode || min instanceof LongNode) {
            nodeType.validators.add(new RangeValidator(min.asLong(Long.MIN_VALUE), max.asLong(Long.MAX_VALUE)));
        }
    }

    private void addProperties(LinkedList<Qobject> q, NodeType nodeType, JsonNode jsonNode) {
        Iterator<Map.Entry<String, JsonNode>> properties = jsonNode.path("properties").fields();
        while(properties.hasNext()) {
            Map.Entry<String, JsonNode> propertyEntry = properties.next();
            NodeDef nodeDef = new NodeDef(propertyEntry.getKey());
            nodeType.children.put(propertyEntry.getKey(), nodeDef);
            q.add(new Qobject(nodeDef, propertyEntry.getValue()));
        }
    }


    private Set<String> getRequiredFields(JsonNode jsonNode) {
        Set<String> requiredSet = new HashSet<>();
        ArrayNode requiredNode = (ArrayNode) jsonNode.get("required");
        if(requiredNode != null) {
            for (int i = 0; i < requiredNode.size(); i++) {
                requiredSet.add(requiredNode.get(i).asText());
            }
        }
        return requiredSet;
    }

    public ValidationError validate(JsonNode root, NodeDef rootDef) {
        ValidationError error = new ValidationError();
        LinkedList<Node> q = new LinkedList<>();
        q.add(new Node("root", root, rootDef, null));
        while (q.size() > 0) {
            Node node = q.remove();
            NodeType nodeType = validateType(node, error);
            if(nodeType == null) {
                continue;
            }
            doValidate(node, nodeType, node.jsonNode, error);
            if (node.jsonNode instanceof ObjectNode) {
                Set<String> requiredCopy = new HashSet<>(node.nodeDef.requiredFields);
                Iterator<Map.Entry<String, JsonNode>> fields = node.jsonNode.fields();
                while(fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    requiredCopy.remove(entry.getKey());
                    NodeDef childDef = nodeType.children.get(entry.getKey());
                    if(childDef == null) {
                        error.addError(String.format("%s.%s: unrecognized field", fqn(node), entry.getKey()));
                        continue;
                    }
                    q.add(new Node(entry.getKey(), entry.getValue(), childDef, node));
                }
                if(requiredCopy.size() > 0){
                    error.addError(String.format("%s The following required fields are missing %s", fqn(node), requiredCopy.toString()));
                }
            } else if (node.jsonNode instanceof ArrayNode) {
                for(int i=0; i<node.jsonNode.size(); i++) {
                    q.add(new Node(node.name, node.jsonNode.get(i), nodeType.children.get("arrayChild"), i, node.parent));
                }
            }
        }
        return error;
    }

    private void doValidate(Node node, NodeType nodeType, JsonNode jsonNode, ValidationError error) {
        if(nodeType.validators.size() == 0) {
            return;
        }
        for (MyValidator validator : nodeType.validators) {
//            log.info("validating {} with {}", fqn(node), validator.getClass().getSimpleName());
            String msg = null;
            if(jsonNode instanceof TextNode) {
                msg = validator.validateStr(jsonNode.asText());
            } else if(jsonNode instanceof ArrayNode) {
                msg = validator.validateArrayNode(jsonNode);
            } else if(jsonNode instanceof DoubleNode || jsonNode instanceof FloatNode || jsonNode instanceof DecimalNode) {
                msg = validator.validateDouble(jsonNode.asDouble());
            } else if(jsonNode instanceof IntNode || jsonNode instanceof ShortNode || jsonNode instanceof LongNode) {
                msg = validator.validateLong(jsonNode.asLong());
            }
            if(msg != null) {
                error.addError(fqn(node) + " " + msg);
            }
        }
    }

    private String fqn(Node node) {
        if(node.parent == null) {
            return "$";
        }
        if(node.fqn != null) {
            return node.fqn;
        }
        String fqn = fqn(node.parent);
        if(node.index != -1) {
            return fqn+"[\""+node.name+"\"]"+"["+node.index+"]";
        }
        return node.fqn = fqn+"[\""+node.name+"\"]";
    }



    private static Map<Class, String> classToTypeMap;

    private NodeType validateType(Node node, ValidationError error) {
            String type = null;
        try {
            type = classToTypeMap.get(node.jsonNode.getClass());
        }catch (Exception e) {
            log.info("foo");
        }
        if(!node.nodeDef.allowedTypes.containsKey(type)) {
            error.addError(String.format("%s: JsonNode class: %s inferred type: %s. Allowed types are %s", fqn(node), node.jsonNode.getClass().getSimpleName(), type, node.nodeDef.allowedTypes.keySet()));
            return null;
        }
        return node.nodeDef.allowedTypes.get(type);
    }


    public static class ValidationError {
        public List<String> errors;
        public void addError(String message) {
            if(errors == null) {
                errors = new ArrayList<>();
            }
            errors.add(message);
        }
    }

    public static class Node {
        public String name;
        public JsonNode jsonNode;
        public NodeDef nodeDef;
        public int index = -1;
        public Node parent;
        public String fqn;

        public Node(String name, JsonNode jsonNode, NodeDef nodeDef, Node parent) {
            this.name = name;
            this.jsonNode = jsonNode;
            this.nodeDef = nodeDef;
            this.parent = parent;
        }

        public Node(String name, JsonNode jsonNode, NodeDef nodeDef, int index, Node parent) {
            this.name = name;
            this.jsonNode = jsonNode;
            this.nodeDef = nodeDef;
            this.index = index;
            this.parent = parent;
        }
    }

    public static class Qobject{
        public NodeDef def;
        public JsonNode jsonNode;

        public Qobject(NodeDef def, JsonNode jsonNode) {
            this.def = def;
            this.jsonNode = jsonNode;
        }
    }


    public static class NodeDef {
        public String name;
        public Map<String, NodeType> allowedTypes = new HashMap<>();
        public Set<String> requiredFields = new HashSet<>();

        public NodeDef(String name) {
            this.name = name;
        }
    }

    public static class NodeType {
        public String type;
        public Map<String, NodeDef> children = new HashMap<>();
        public List<MyValidator> validators = new ArrayList<>();

        public NodeType(String type) {
            this.type = type;
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

    public interface MyValidator {
        String validateLong(long value);
        String validateDouble(double value);
        String validateStr(String value);
        String validateArrayNode(JsonNode jsonNode);
    }

    public static class RangeValidator implements MyValidator{
        public long min;
        public long max;

        public RangeValidator(long min, long max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String validateLong(long value) {
            if(value < min || value > max) {
                return String.format("value:%d has to be between %d and %d", value, min, max);
            }
            return null;
        }

        @Override
        public String validateDouble(double value) {
            if(value < min || value > max) {
                return String.format("value:%f has to be between %d and %d", value, min, max);
            }
            return null;
        }

        @Override
        public String validateStr(String value) {
            if(value.length() < min || value.length() > max) {
                return String.format("length of %s must be between %s and %s", value.length(), min, max);
            }
            return null;
        }

        @Override
        public String validateArrayNode(JsonNode jsonNode) {
            if(jsonNode.size() < min || jsonNode.size() > max) {
                return String.format("length of array must be between %s and %s", min, max);
            }
            return null;
        }
    }

    public static class RangeDoubleValidator implements MyValidator {

        public double min;
        public double max;

        public RangeDoubleValidator(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String validateLong(long value) {
            if(value < min || value > max) {
                return String.format("value:%d has to be between %f and %f", value, min, max);
            }
            return null;
        }

        @Override
        public String validateDouble(double value) {
            if(value < min || value > max) {
                return String.format("value:%f has to be between %f and %f", value, min, max);
            }
            return null;
        }

        @Override
        public String validateStr(String value) {
            return null;
        }

        @Override
        public String validateArrayNode(JsonNode jsonNode) {
            return null;
        }
    }

    public static class PatternValidator implements MyValidator {

        private final Pattern pattern;
        private String regex;

        public PatternValidator(String regex) {
            pattern = Pattern.compile(regex);
            this.regex = regex;
        }

        @Override
        public String validateLong(long value) {
            return null;
        }

        @Override
        public String validateDouble(double value) {
            return null;
        }

        @Override
        public String validateStr(String value) {
            if(!pattern.matcher(value).matches()) {
                return String.format("%s does not match the pattern %s", value, regex);
            }
            return null;
        }

        @Override
        public String validateArrayNode(JsonNode jsonNode) {
            return null;
        }
    }

    public static class AllowedValuesValidator implements MyValidator {
        Set<String> allowedValues = new HashSet<>();

        public void addAllowedValue(String value) {
            allowedValues.add(value);
        }

        @Override
        public String validateLong(long value) {
            return null;
        }

        @Override
        public String validateDouble(double value) {
            return null;
        }

        @Override
        public String validateStr(String value) {
            if(!allowedValues.contains(value)) {
                return String.format("%s is not one of allowed values %s", value, allowedValues.toString());
            }
            return null;
        }

        @Override
        public String validateArrayNode(JsonNode jsonNode) {
            return null;
        }
    }
}
