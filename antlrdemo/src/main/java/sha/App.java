package sha;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sha.Utils.readJsonFromClasspath;

public class App 
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            App obj = new App();
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


    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        ObjectMapper m = new ObjectMapper();
        JsonNode jsonNode = m.readValue(Files.readAllBytes(Paths.get("/Users/sharath.g/hive_json")), JsonNode.class);
        ObjectMapper m2 = new ObjectMapper();
        ArrayNode an = (ArrayNode) jsonNode;
        int failed = 0;
        int total = 0;
        for (JsonNode node : an) {
            if(!node.get("finish").asBoolean()) continue;
            String hive = node.get("hive").textValue();
            ArrayNode an2;
            try {
                an2 = m2.readValue(hive, ArrayNode.class);
            } catch (Exception e) {
                failed++;
                continue;
            }
            for (JsonNode jn2 : an2) {
                String schema = jn2.get("schema").asText();
                try {
                    doAntlrStuff(schema);
                    total++;
                } catch (Exception e) {
                    failed++;
                    log.error("{}", schema, e);
                }
            }
        }

        log.debug("total tally: total:{} failed:{}", total, failed);
    }


    private void doAntlrStuff(String schema) throws Exception {
//        CharStream charStream = CharStreams.fromString(new String(Files.readAllBytes(Paths.get("/Users/sharath.g/antlr_hive/input"))));
        CharStream charStream = CharStreams.fromString(schema);
        ExprLexer lexer = new ExprLexer(charStream);
        TokenStream tokens = new CommonTokenStream(lexer);
        ExprParser parser = new ExprParser(tokens);
        Attr attr = new Attr(parser.expr());
        treeWalk(attr);
        compare(attr, attr);
        log.debug("finished compare: all good. they are equal. totally compared {} nodes, {} terminal nodes", nodes, tn);
        nodes = 0;
        tn = 0;
    }


    int nodes = 0;
    int tn = 0;
    public void compare(Attr a, Attr b) throws Exception {
        nodes++;
        // both a and b have the map
        if(!a.getClass().equals(b.getClass())) {
            log.error("nodes are not the same class. {} {}", a, b);
            throw new Exception();
        }
        if(a.attrChildren.size() != b.attrChildren.size()) {
            log.error("number of children are not equal: {} != {] a:{}   b:{}", a.attrChildren.size(), b.attrChildren.size(), a, b);
        }

        if(a.node instanceof TerminalNode) {
            TerminalNode tn = (TerminalNode) (a.node);
            if(!tn.getText().equals( b.node.getText())) {
                log.error("terminal node texts are not equal {} {}", tn.getText(), b.node.getText());
            }
            this.tn++;
        }
        for(int i=0; i<a.attrChildren.size(); i++) {
            ParseTree child = a.attrChildren.get(i).node;
            if(child instanceof ExprParser.TermContext) {
                ExprParser.TermContext tc = (ExprParser.TermContext) child;
                String word = tc.WORD().getText();
                if(!b.children.containsKey(word)) {
                    log.error("a contains word:{} but not b. a:{}   b:{}", word, a, b);
                    throw new Exception();
                }
                compare(a.children.get(word), b.children.get(word));
            } else {
                compare(a.attrChildren.get(i), b.attrChildren.get(i));
            }
        }
    }
    static class Attr {
        ParseTree node;
        List<Attr> attrChildren =  new ArrayList<>();

        Map<String, Attr> children = new HashMap<>(); // map of term nodes to the children

        public Attr(ParseTree node) {
            this.node = node;
        }
    }

    public void treeWalk(Attr current) {
        ParseTree node = current.node;
        for(int i=0; i<node.getChildCount(); i++) {
            ParseTree child = node.getChild(i);
            Attr attr = new Attr(child);
            current.attrChildren.add(attr);
            if(child instanceof ExprParser.TermContext) {
                ExprParser.TermContext tc = (ExprParser.TermContext) child;
                current.children.put(tc.WORD().getText(), attr);
            }
            treeWalk(attr);
        }
    }
}
