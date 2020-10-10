import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkTopology {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkTopology.class);
    TopologyBuilder builder;
    int port = 9092;
    public BenchmarkTopology(TopologyBuilder builder) {
        this.builder = builder;
    }

    public void build() {
        builder.setSpout("kafka", new KafkaSpout<>(KafkaSpoutConfig.builder("127.0.0.1:" + port, "topics").build()), 1);
//
//        builder.setBolt("slidingsum", new SlidingWindowSumBolt().withWindow(BaseWindowedBolt.Count.of(30), BaseWindowedBolt.Count.of(10)), 1)
//                .shuffleGrouping("integer");
//        builder.setBolt("tumblingavg", new TumblingWindowAvgBolt().withTumblingWindow(BaseWindowedBolt.Count.of(3)), 1)
//                .shuffleGrouping("slidingsum");
        builder.setBolt("printer", new PrinterBolt(), 1).shuffleGrouping("kafka");

    }


}
