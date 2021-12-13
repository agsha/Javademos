package sha;


import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.kafka.clients.producer.*;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static sha.Utils.readJsonFromClasspath;

public class KProducer
{
    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            KProducer obj = new KProducer();
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
        Map<String, Object> config = new HashMap<>();

        config.put("compression.type", "lz4");
        config.put("metadata.max.age.ms", "300000");
        config.put("request.timeout.ms", "5000");
        config.put("batch.size", "20384");
        config.put("acks", "-1");
        config.put("reconnect.backoff.ms", "1000");
        config.put("bootstrap.servers", "10.33.231.246:9092,10.34.134.185:9092,10.33.17.200:9092");
        config.put("receive.buffer.bytes", "32768");
        config.put("retry.backoff.ms", "50");
        config.put("buffer.memory", "100663296");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("max.request.size", "2097152");
        config.put("retries", "0");
        config.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        config.put("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        config.put("max.block.ms", "10000");
        config.put("send.buffer.bytes", "131072");
        config.put("connections.max.idle.ms", "300000");
        config.put("max.in.flight.requests.per.connection", "3");
        config.put("linger.ms", "30");
        config.put("client.id", "prod-dart-service");


        ProducerRecord<String, byte[]> pr = new ProducerRecord<>("dart.fkint.cp.ca_common.RequestEvent", ("{ \"eventId\": \"e1781649-6572-4e94-8c45-9b76adfe567b\", \"eventTime\": 1518283236000, \"traceId\": \"a06dfe00-44b6-4cba-984c-6ab4551f05f3\", \"childrenEventNames\": [\"fkint/cp/ca_discover/ProductPageServed\"], \"seqId\": \"001518283236000000000\", \"ingestedAt\": 1518283236743, \"parentId\": null, \"test\": \"NFR_TEST\", \"parentVersion\": null, \"schemaVersion\": \"2.5\", \"encodingType\": \"JSON\", \"data\": { \"debug\": { \"hostName\": \"fk-mobile-api-mapi-client-grp-18-457494\" }, \"core\": { \"timestamp\": 1518283236742, \"requestId\": \"e1781649-6572-4e94-8c45-9b76adfe567b\", \"sourceClient\": \"AndroidApp\", \"URI\": \"/3/page/dynamic/product\" }, \"visit\": { \"visitorId\": \"VI1816BC5E890745DC8C0531FBC956F878\", \"location\": { \"info\": { \"geolocation\": {}, \"ipLocation\": { \"ip\": \"49.15.203.205\", \"country\": \"India\", \"state\": \"Kerala\", \"city\": \"UNKNOWN\" }, \"userInsightLocation\": { \"city\": \"MALAPURAM\", \"state\": \"KERALA\" } }, \"IP\": \"49.15.203.205\" }, \"sdkAdId\": \"\", \"pincode\": \"679339\", \"sessionId\": \"SI49EA9419F8264F708B787ED78327D9AF\", \"adId\": \"\", \"visitId\": \"0e8d5c7c3cbd37b525370c5d37118eb2-1518282840820\", \"userAgent\": { \"osInfo\": { \"version\": \"5.1\", \"family\": \"Android\" }, \"appInfo\": { \"version\": \"860400\", \"type\": \"Retail\" }, \"deviceInfo\": { \"type\": \"Mobile\", \"deviceId\": \"0e8d5c7c3cbd37b525370c5d37118eb2\", \"family\": \"Micromax E451\" }, \"browserInfo\": { \"version\": \"59.0.3071\", \"family\": \"Chrome Mobile\" } }, \"abIds\": [\"97884dfe\", \"c8381068\"], \"isPreprod\": false, \"networkType\": \"4G\", \"accountId\": \"ACCC30833EB03F94598B33FE249CB471B55G\" } }, \"children\": { \"fkint/cp/ca_discover/ProductPageServed\": [{ \"eventId\": \"e1781649-6572-4e94-8c45-9b76adfe567b.BELEEVDNNWHPCFG5\", \"eventTime\": 1518283236000, \"traceId\": \"a06dfe00-44b6-4cba-984c-6ab4551f05f3\", \"seqId\": \"001518283236000000000\", \"ingestedAt\": 1518283236743, \"parentId\": \"e1781649-6572-4e94-8c45-9b76adfe567b\", \"test\": \"NFR_TEST\", \"parentVersion\": null, \"schemaVersion\": \"1.10\", \"encodingType\": \"JSON\", \"data\": { \"itemId\": \"ITMEEVDNWESTGWZ8\", \"analyticsInfo\": { \"superCategory\": \"MenAccessory\", \"category\": \"MenFashionAccessory\", \"subCategory\": \"FashionBeltBuckle\", \"vertical\": \"FashionBelt\" }, \"fetchId\": \"e1781649-6572-4e94-8c45-9b76adfe567b.BELEEVDNNWHPCFG5\", \"swatches\": [{ \"attribute\": \"color\", \"option\": \"Black\" }], \"vertical\": \"belt\", \"isEbook\": false, \"isVideoAvaiable\": false, \"isImagesAvailable\": false, \"listings\": [{ \"offerIds\": [\"nb:mp:015c17ce07\"], \"deliveryOptionInfo\": [{ \"ffApplied\": false, \"codAvailable\": true, \"price\": { \"currency\": \"INR\", \"value\": 65 }, \"serviceable\": true, \"deliveryDate\": 1519064999000, \"speed\": \"REGULAR\" }], \"availibilityStatus\": \"In Stock\", \"listingId\": \"LSTBELEEVDNNWHPCFG5XTR99D\", \"listingState\": \"current\", \"mrp\": 599, \"offerCount\": 6, \"finalPrice\": 177, \"fsp\": 112, \"isServiceable\": true, \"offerSummary\": [{ \"type\": \"BASKETPRICE_PAYMENT_DISCOUNT\", \"id\": \"nb:mp:015c17ce07\" }], \"sellerId\": \"608f062b459e4027\", \"flipkartAdvantage\": false }, { \"offerIds\": [\"nb:mp:015c17ce07\"], \"deliveryOptionInfo\": [{ \"ffApplied\": false, \"codAvailable\": true, \"price\": { \"currency\": \"INR\", \"value\": 58 }, \"serviceable\": true, \"deliveryDate\": 1519151399000, \"speed\": \"REGULAR\" }], \"availibilityStatus\": \"In Stock\", \"listingId\": \"LSTBELEEVDNNWHPCFG5SOSPHT\", \"listingState\": \"current\", \"mrp\": 599, \"offerCount\": 6, \"finalPrice\": 198, \"fsp\": 140, \"isServiceable\": true, \"offerSummary\": [{ \"type\": \"BASKETPRICE_PAYMENT_DISCOUNT\", \"id\": \"nb:mp:015c17ce07\" }], \"sellerId\": \"14caca4d3acf4b76\", \"flipkartAdvantage\": false }, { \"offerIds\": [\"nb:mp:015c17ce07\"], \"deliveryOptionInfo\": [{ \"ffApplied\": false, \"codAvailable\": true, \"price\": { \"currency\": \"INR\", \"value\": 58 }, \"serviceable\": true, \"deliveryDate\": 1519064999000, \"speed\": \"REGULAR\" }], \"availibilityStatus\": \"In Stock\", \"listingId\": \"LSTBELEEVDNNWHPCFG5NHDRPJ\", \"listingState\": \"current\", \"mrp\": 599, \"offerCount\": 6, \"finalPrice\": 207, \"fsp\": 149, \"isServiceable\": true, \"offerSummary\": [{ \"type\": \"BASKETPRICE_PAYMENT_DISCOUNT\", \"id\": \"nb:mp:015c17ce07\" }], \"sellerId\": \"a98dbbb8c75e4fe9\", \"flipkartAdvantage\": false }, { \"offerIds\": [\"nb:mp:015c17ce07\"], \"deliveryOptionInfo\": [{ \"ffApplied\": false, \"codAvailable\": true, \"price\": { \"currency\": \"INR\", \"value\": 58 }, \"serviceable\": true, \"deliveryDate\": 1519064999000, \"speed\": \"REGULAR\" }], \"availibilityStatus\": \"In Stock\", \"listingId\": \"LSTBELEEVDNNWHPCFG5DU2RER\", \"listingState\": \"current\", \"mrp\": 599, \"offerCount\": 6, \"finalPrice\": 278, \"fsp\": 220, \"isServiceable\": true, \"offerSummary\": [{ \"type\": \"BASKETPRICE_PAYMENT_DISCOUNT\", \"id\": \"nb:mp:015c17ce07\" }], \"sellerId\": \"10de58d43a4648ea\", \"flipkartAdvantage\": false }], \"productState\": \"IN_STOCK\", \"listingsCount\": 4, \"isVisualDiscoverEnabled\": false, \"productFamily\": [], \"isDigital\": false, \"isSwatchAvailable\": false, \"ugc\": { \"reviewCount\": 6, \"ratingBase\": 5, \"avgRating\": 3.0, \"ratingCount\": 35 }, \"marketplaceId\": \"FLIPKART\", \"importanceType\": \"NONCORE\", \"urgency\": { \"messageShown\": false }, \"productId\": \"BELEEVDNNWHPCFG5\" } }] } }").getBytes());


        MetricRegistry metricRegistry = new MetricRegistry();
        Meter ok = metricRegistry.meter("ok");
        Meter failure = metricRegistry.meter("failure");
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(3, TimeUnit.SECONDS);
        Semaphore semaphore = new Semaphore(1000);


        Producer<String, byte[]> producer = new KafkaProducer<>(config);
        Callback cb = new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                semaphore.release();
                if(exception != null) {
                    failure.mark();
                } else {
                    ok.mark();
                }
            }
        };
        while (true) {
            semaphore.acquire();
            producer.send(pr, cb);
        }
    }

}
