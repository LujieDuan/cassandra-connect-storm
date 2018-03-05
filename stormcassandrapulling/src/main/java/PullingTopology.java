import Bolt.TimestampBolt;
import Spout.SourceSpout;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.sql.Timestamp;

public class PullingTopology {

    private static final Logger LOG = LoggerFactory.getLogger(PullingTopology.class);
    JedisPool pool;
    TopologyBuilder builder;

    private final String TABLES_VARIABLE_NAME = "tasks";

    public PullingTopology(TopologyBuilder builder) {
        this.builder = builder;
    }

    public void build() {
        builder.setSpout("source", new SourceSpout(), 10);
        builder.setBolt("tag-process", new TimestampBolt(), 4).shuffleGrouping("source", "Tag");

        String seedTimestamp = new Timestamp(0).toString();

        this.pool = new RedisClient().pool;
        Jedis jedis = pool.getResource();
        jedis.lpush(TABLES_VARIABLE_NAME, String.format("k1.t1|%s", seedTimestamp));
        jedis.close();
        pool.close();
    }
}
