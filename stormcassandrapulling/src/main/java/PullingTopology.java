import Bolt.MaxTimestampBolt;
import Bolt.RecordBolt;
import Bolt.TimestampBolt;
import Spout.SourceSpout;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.Date;

import static Spout.SourceSpout.TASKS_VARIABLE_NAME;
import static Spout.SourceSpout.timestampFormat;

public class PullingTopology {

    private static final Logger LOG = LoggerFactory.getLogger(PullingTopology.class);
    JedisPool pool;
    TopologyBuilder builder;

    public PullingTopology(TopologyBuilder builder) {
        this.builder = builder;
    }

    public void build() {
        builder.setSpout("source", new SourceSpout(), 1);
        builder.setBolt("timestamp-process", new TimestampBolt(), 4)
                .shuffleGrouping("source", "Timestamp");
        builder.setBolt("max-timestamp-process", new MaxTimestampBolt(), 4)
                .shuffleGrouping("source", "MaxTimestamp");
        builder.setBolt("record-process", new RecordBolt(), 4)
                .shuffleGrouping("source", "Record");

        String seedTimestamp = timestampFormat.format(new Date(0));

        this.pool = new RedisClient().pool;
        Jedis jedis = pool.getResource();
        jedis.lpush(TASKS_VARIABLE_NAME, String.format("k1.t1|%s", seedTimestamp));
        jedis.lpush(TASKS_VARIABLE_NAME, String.format("k1.t2|%s", seedTimestamp));
        jedis.lpush(TASKS_VARIABLE_NAME, String.format("k2.t1|%s", seedTimestamp));
        jedis.lpush(TASKS_VARIABLE_NAME, String.format("k2.t2|%s", seedTimestamp));
        jedis.close();
        pool.close();
    }
}
