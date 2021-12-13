package sha;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Created by sharath.g on 9/13/17.
 */
public class KafkaUtil {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws InterruptedException, IOException {
        Properties props = new Properties();
        props.put("bootstrap.servers", "10.33.44.247:9092");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("max.partition.fetch.bytes", 1048576 * 2);
        props.put("group.id", "s1");
        int totalSize = 0;
        KafkaConsumer consumer = new KafkaConsumer(props);
        List<PartitionInfo> list = consumer.partitionsFor("fb.fkint.cp.ca_discover.ProductSummaryImpression");
        for (PartitionInfo partitionInfo : list) {
            System.out.println(String.format("partition:%d leader:%s", partitionInfo.partition(), partitionInfo.leader().host()));
        }


//        List<TopicPartition> topicPartitionList = new ArrayList<TopicPartition>();
//        TopicPartition topicPartition = new TopicPartition("dart.fkint.scp.oms.Order_Item", 0);
//        topicPartitionList.add(topicPartition);
//        consumer.assign(topicPartitionList);
//        consumer.partitionsFor()
//        long a = Long.valueOf(378791246 );
//        long b = Long.valueOf(378791246 + 10);
//        consumer.seek(topicPartition, a);
//        Long latestAvailableOffsets = b;
//        boolean isRunning = true;
//        Map<TopicPartition, Long> consumedOffsets = new HashMap<TopicPartition, Long>();
//        Long startTime = System.currentTimeMillis();
//        int count = 0;
//        int last = 0;
//        while (isRunning) {
//            long x = System.nanoTime();
//            System.out.println("starting poll");
//            ConsumerRecords<String, ConsumerRecord> consumerRecords = consumer.poll(1000);
//            long y = System.nanoTime();
////            System.out.println(String.format("count:%d durationMs:%d last:%d", count, (y-x)/1000000, last));
//            last = 0;
//            for (ConsumerRecord consumerRecord : consumerRecords) {
//                count++;
//                last++;
////                bufferedWriter.newLine();
////                bufferedWriter.write(consumerRecord.value().toString() + " " + consumerRecord.serializedValueSize() + " " + consumerRecord.offset());
////                totalSize = totalSize + consumerRecord.serializedValueSize();
//                System.out.println(String.format("off:%d", consumerRecord.offset()));
//                System.out.println(consumerRecord.offset() + " " + consumerRecord.partition() +" "+consumerRecord.timestampType());
////
////                if (consumerRecord.offset() == latestAvailableOffsets) {
////                    System.out.println(consumerRecord.offset() + " " + consumerRecord.partition() +" "+consumerRecord.timestamp());
////                    System.out.println(totalSize);
////                    isRunning = false;
////                }
//            }
//        }
//        System.out.println(String.format("msg: %d, trpt:%d", (b-a), (b-a)*1000/(System.currentTimeMillis()-startTime)));
////        bufferedWriter.close();
        consumer.close();
    }
}
