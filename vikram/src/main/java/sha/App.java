package sha;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ibm.icu.text.NumberFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static sha.Utils.readJsonFromClasspath;

public class App 
{
    static NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private static final Logger log = LogManager.getLogger();
    private static Settings s;
    ObjectMapper mapper = new ObjectMapper();
    static final DateTimeFormatter formatter =
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter();
    private CSVRecord headers;

    public static void main( String[] args ) {
        try {
            App obj = new App();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
//            obj.test();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void test() {
        String r = "^syndicate";
        log.debug("{}", Pattern.compile(r).matcher("syndicate foo").find());
    }

    private void sort() throws IOException {
        CSVParser records = CSVFormat.RFC4180.parse(Files.newBufferedReader(Paths.get("/Volumes/ramd/combined.csv")));
        Iterator<CSVRecord> it = records.iterator();

        CSVRecord headers = it.next();

        List<CSVRecord> recordList = Lists.newArrayList(it);
        Comparator<CSVRecord> c = (o1, o2) -> {
            LocalDateTime d1 = LocalDate.parse(o1.get(2), formatter).atStartOfDay();
            LocalDateTime d2 = LocalDate.parse(o2.get(2), formatter).atStartOfDay();
            return (int)Duration.between( d2, d1).toDays();
        };

        recordList.sort(c);
        BufferedWriter writer = Files.newBufferedWriter(Paths.get("/Volumes/ramd/combined_sorted.csv"));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        csvPrinter.printRecord(headers);

        for (CSVRecord record : recordList) {
            csvPrinter.printRecord(record);
        }
        csvPrinter.flush();
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
        CSVParser records = CSVFormat.RFC4180.parse(Files.newBufferedReader(Paths.get("/Volumes/ramd/combined_sorted.csv")));
        headers = records.iterator().next();
        Set<Long> set = new HashSet<>();

        Set<String> uniqueHeaders = new HashSet<>();
        for(int i = 0; i< headers.size(); i++) {
            String header = headers.get(i).trim();
        }
        int count = 0;
        List<Txn> list = new ArrayList<>();
        for(CSVRecord r : records) {
            if(r.size() - headers.size() > 1) {
                log.debug("count:{}", count);
                throw new RuntimeException(String.format("foooo %s %s", r.size(), headers.size()));
            }
            count++;
            Txn txn = new Txn(r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r);
            if(set.contains(txn.rec)) {
                continue;
            }
            set.add(txn.rec);
            list.add(txn);
        }
        process(list);

    }
    private void process(List<Txn> txns) throws IOException {
        Map<String, Long> type = new TreeMap<>();
        List<String> banks = Files.readAllLines(Paths.get("/Users/sharath.g/Dropbox/vikram/banks"));
        log.debug("{}", banks);
        long count=0;
        String regex[] = {
                "^inb (neft|rtgs|grpt) s[bt]in?h?(\\d+) (.*)",
        };
        Pattern[] p = new Pattern[regex.length];
        for(int i=0; i<regex.length; i++) {
            p[i] = Pattern.compile(regex[i]);
        }

        outer:for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
            if(t.amnt > 0) {
                continue;
            }
            String key = t.narr;

            xxx:for (Pattern pattern : p) {
                Matcher matcher = pattern.matcher(key);
                if(matcher.find()) {
                    String namish = matcher.group(3);
                    for (String bank : banks) {
                        String r = "^"+bank;
                        if(Pattern.compile(r).matcher(namish).find()) {
                            String name = namish.replaceAll(r, "");
                            type.putIfAbsent(name, 0L);
                            type.compute(name, (k, v) -> v + t.amnt);
                            break;
                        }
                    }

                }
            }
        }


//        List<Map.Entry<String, Long>> list = new ArrayList<>(type.entrySet());
        List<Map.Entry<String, Long>> list = sortByVal(type);
        for (int i = 0; i <list.size(); i++) {
            Map.Entry<String, Long> entry = list.get(i);
            log.debug("{} {}", entry.getKey(), f(entry.getValue()));
            count += entry.getValue();
        }
        log.debug("accounted:   {} size:{} ", f(count), type.size());

    }

    private void process1(List<Txn> txns) {
        Map<String, Long> type = new TreeMap<>();
        long count=0;
        String regex[] = {
                "^inb (neft|rtgs|grpt) s[bt]in?h?(\\d+) (.*)",
        };
//        String regex[] = {"inb grpt sbi(\\d+) (.*)"};
        Pattern[] p = new Pattern[regex.length];
        for(int i=0; i<regex.length; i++) {
            p[i] = Pattern.compile(regex[i]);
        }

        outer:for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
            if(t.amnt > 0) {
                continue;
            }
            String key = t.narr;

            xxx:for (Pattern pattern : p) {
                Matcher matcher = pattern.matcher(key);
                if(matcher.find()) {
                    String namish = matcher.group(3);
                    type.putIfAbsent(namish, 0L);
                    type.compute(namish, (k,v)->v+t.amnt);
                }
            }
        }
        List<String> bank = new ArrayList<>();
        String cur = "";
        for (Map.Entry<String, Long> entry : type.entrySet()) {
            Ret ret = common(entry.getKey(), cur);
            if(ret.count > 0 && ret.common.length()>1) {
                cur = ret.common;
            } else {
                bank.add(cur);
                cur = entry.getKey();
            }
        }
        for (String s1 : bank) {
            log.debug("{}", s1);
        }

//
//        List<Map.Entry<String, Long>> list = new ArrayList<>(type.entrySet());
////        List<Map.Entry<String, Long>> list = sortByVal(type);
//        for (int i = 0; i <list.size(); i++) {
//            Map.Entry<String, Long> entry = list.get(i);
//            log.debug("{} {}", entry.getKey(), f(entry.getValue()));
//            count += entry.getValue();
//        }
//        log.debug("accounted:   {} size:{} ", f(count), type.size());

    }

    private Ret common(String key, String cur) {
        String[] s1 = key.split("\\s+");
        String[] s2 = cur.split("\\s+");
        int count = 0;
        List<String> xx = new ArrayList<>();
        for (int i = 0; i < Math.min(s1.length, s2.length); i++) {
            if(s1[i].equalsIgnoreCase(s2[i])) {
                count++;
                xx.add(s1[i]);
            } else {
                break;
            }
        }
        return new Ret(count, String.join(" ", xx));


    }

    static class Ret {
        int count;
        String common;

        public Ret(int count, String common) {
            this.count = count;
            this.common = common;
        }
    }
    private void process5(List<Txn> txns) throws JsonProcessingException {
        Map<String, Long> type = new TreeMap<>();
        int count = 0;
        for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
            if(t.narr.contains("pavan bh")) {
                log.debug("{}", t);
                if(t.amnt < 0) {
                    count += t.amnt;
                }

            }
        }
        log.debug("{}", count);


    }
    private void process6(List<Txn> txns) {
        long a = 0;
        Txn save = null;
        outer:for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
            if(t.amnt > 0) {
                a++;
            }
        }
        log.debug("{}", a);
    }

    private void process9(List<Txn> txns) {
        Map<Txn, Long> type = new TreeMap<>();

        String regex[] = {
                "^\\d\\d\\w"
        };
//        String regex[] = {"inb grpt sbi(\\d+) (.*)"};
        Pattern[] p = new Pattern[regex.length];
        for(int i=0; i<regex.length; i++) {
            p[i] = Pattern.compile(regex[i]);
        }

        long count = 0;
        outer:for (int i=0; i<txns.size(); i++) {

            Txn t = txns.get(i);
            if(t.amnt > 0) {
                continue;
            }
            String key = t.narr;
            type.putIfAbsent(t, 0L);
            type.compute(t, (k,v)->v+t.amnt);
//            for (Pattern pattern : p) {
//                Matcher matcher = pattern.matcher(key);
//                if(matcher.find()) {
//                    type.putIfAbsent(key, 0L);
//                    type.compute(key, (k,v)->v+Math.abs(t.amnt));
////                    continue outer;
//                }
//
//            }
//            type.putIfAbsent("misc", 0L);
//            type.compute("misc", (k,v)->v+Math.abs(t.amnt));
//            type.putIfAbsent(key, 0L);
//            type.compute(key, (k,v)->v+Math.abs(t.amnt));

//            log.debug("{}", key);

        }

        List<Map.Entry<Txn, Long>> list = sortByVal(type);
//        Collections.reverse(list);
        for (int i = 0; i <100; i++) {
            Map.Entry<Txn, Long> entry = list.get(i);
            log.debug("{} {}", entry.getKey().narr, f(entry.getValue()));
            count += entry.getValue();
//            if(i>10) {
//                break;
//            }
        }
        log.debug("accounted:   {} size:{} ", f(count), type.size());
    }

    private void processs(List<Txn> txns) {
        outer:for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
        }
    }
    private void process7(List<Txn> txns) {
        // 142 crores
        String regex[] = {
                "^inb (neft|rtgs|grpt) s[bt]in?h?(\\d+) (.*)",
        };
        Pattern[] p = new Pattern[regex.length];
        for(int i=0; i<regex.length; i++) {
            p[i] = Pattern.compile(regex[i]);
        }
        Map<String, Long> type = new TreeMap<>();


        long count = 0;
        List<Txn> list = new ArrayList<>();
        outer:for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
            if(t.amnt > 0) {
                continue;
            }
            String key = t.narr;
            for (Pattern pattern : p) {
                Matcher matcher = pattern.matcher(key);
                if(matcher.find()) {
                    type.putIfAbsent(matcher.group(3), 0L);
                    type.compute(matcher.group(3), (k,v)->v+Math.abs(t.amnt));
//                    list.add(t);
//                    count++;
                }
            }
        }
//        list.sort(Comparator.comparingLong(a -> a.amnt));
//        for (Txn txn : list) {
//            log.debug("{}", txn);
//        }

//
        long a = 0;
        for (Map.Entry<String, Long> entry : sortByVal(type)) {
            log.debug("{} {}", entry.getKey(), f(entry.getValue()));
            if(entry.getValue() > 100_00_000) {
                a+=entry.getValue();
            }
            count += entry.getValue();
        }
        log.debug("accounted:{} count:{}, a:{}", type.size(), count, a);

    }

    private void process8(List<Txn> txns) throws JsonProcessingException {
        Map<String, Long> type = new TreeMap<>();

        String regex[] = {
                "chandorkar",
                "^utr = stin?h?(\\d+) benef a/c (\\w?\\w?[\\d\\w]+)$",
                "^inb neft utr no: sbin(\\d+)$",
                "^inb grpt utr no: sbi(\\d+)$",
                "^inb (neft|rtgs|grpt) s[bt]in?h?(\\d+) (.*)",
                "inb ([\\w-]+)$",
                "utr = sti(\\d+) benef a/c (\\d+)$",
                "to clg",
                "to clng",
                "r:stinh(\\d+) (.*)$",
                "by clg",
                "paid to self",
                "vikram ?([\\w.]*):>(.*)",
                "^\\d\\d-?\\w",
                "^inb "
        };
//        String regex[] = {"inb grpt sbi(\\d+) (.*)"};
        Pattern[] p = new Pattern[regex.length];
        for(int i=0; i<regex.length; i++) {
            p[i] = Pattern.compile(regex[i]);
        }

        long count = 0;
        outer:for (int i=0; i<txns.size(); i++) {

            Txn t = txns.get(i);
            if(t.amnt > 0) {
                continue;
            }
            String key = t.narr;
            for (Pattern pattern : p) {
                if(pattern.matcher(key).find()) {
//                    type.putIfAbsent(pattern.toString(), 0L);
//                    type.compute(pattern.toString(), (k,v)->v+Math.abs(t.amnt));
                    continue outer;
                }

            }
//            type.putIfAbsent("misc", 0L);
//            type.compute("misc", (k,v)->v+Math.abs(t.amnt));
            type.putIfAbsent(key, 0L);
            type.compute(key, (k,v)->v+Math.abs(t.amnt));

//            log.debug("{}", key);

        }
        for (Map.Entry<String, Long> entry : sortByVal(type)) {
            log.debug("{} {}", entry.getKey(), f(entry.getValue()));
            count += entry.getValue();
        }
        log.debug("accounted:{} size:{}", f(count), type.size());


    }


    private void process3(List<Txn> txns) throws JsonProcessingException {
        Utils.LatencyTimer.LatPrinter p = new Utils.LatencyTimer.LatPrinter() {
            @Override
            public void log(String name, Utils.LatencyTimer.LatRet ret) {
                String s = String.format("name:%s max:%s   ", name, nf.format(ret.maxNanos));

                for(int i=0; i<ret.nanos.length; i++) {
                    s+=ret.pTiles[i]+"%:"+ (nf.format((long)ret.nanos[i])+"  ");
                }
                log.debug("{}", s);
            }
        };
        Utils.LatencyTimer positive = new Utils.LatencyTimer(p, "positive");
        Utils.LatencyTimer negative = new Utils.LatencyTimer(p, "negative");
        positive.enabled.set(false);
        Map<String, Integer> type = new HashMap<>();
        List<Integer> histo = new ArrayList<>();
        LocalDate prev = txns.get(0).tranDate;
        final long interval = 30;
        int count = 0;
        for (int i=1; i<txns.size(); i++) {
            Txn t = txns.get(i);
            count++;
            if(t.amnt > 0) {
                positive.count(t.amnt);
            } else {
                negative.count(Math.abs(t.amnt));
            }
        }
        positive.doLog();
        negative.doLog();
//        log.debug("size:{}",  histo.size());
    }


    private void process2(List<Txn> txns) {
        Set<Long> set = new HashSet<>();
        long d = 0;
        long  prevBal = 0;
        long maxDiff =0;
        long mindiff = 0;
        for (int i=0; i<txns.size(); i++) {
            Txn t = txns.get(i);
            if(set.contains(t.rec)) {
                continue;
            }
            set.add(t.rec);
            if(t.bal != prevBal + t.amnt) {
                long diff = (t.bal - (prevBal + t.amnt));
                d+=diff;
                log.debug(String.format("csv_row:%d prevBal:%d + amnt:%d != %d / %d (from sheet) diff: %d, max_diff:%d, min_diff:%d", (i+2), prevBal, t.amnt, prevBal+t.amnt, t.bal, diff, Math.max(diff, maxDiff), Math.min(diff, mindiff)));
                prevBal = t.bal;
                maxDiff = Math.max(diff, maxDiff);
                mindiff = Math.min(diff, mindiff);
//                throw new RuntimeException("");
            } else {
                prevBal += t.amnt;
            }
        }

        System.out.println(d);

    }

    <K, V extends Comparable<V>> List<Map.Entry<K, V>> sortByVal(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparing(Map.Entry::getValue));
        return list;
    }

    public static String f(long a) {
        return nf.format(a);
    }
}
