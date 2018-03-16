import Bolt.MaxTimestampBolt;
import Bolt.RecordBolt;
import Bolt.RecordCheckBolt;
import Bolt.TimestampBolt;
import Spout.SourceSpout;
import cassandra.CassandraClient;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
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
        builder.setSpout("source", new SourceSpout(), 10);

        builder.setBolt("timestamp-process", new TimestampBolt(), 4)
                .shuffleGrouping("source", "Timestamp");
        builder.setBolt("max-timestamp-process", new MaxTimestampBolt(), 4)
                .shuffleGrouping("source", "MaxTimestamp");
        builder.setBolt("record-check", new RecordCheckBolt(), 4)
                .shuffleGrouping("source", "Record");

        builder.setBolt("record-process", new RecordBolt(), 4)
                .shuffleGrouping("record-check");

        String seedTimestamp = timestampFormat.format(new Date(0));

        this.pool = new RedisClient().pool;
        Jedis jedis = pool.getResource();

        for (String t: getTables()) {
            jedis.lpush(TASKS_VARIABLE_NAME, String.format("%s|%s|%s", t, 0L, seedTimestamp));
        }

        jedis.close();
        pool.close();
    }

    private ArrayList<String> getTables() {
        CassandraClient client = new CassandraClient();
        ArrayList<String> tableList = new ArrayList<String>();
        ResultSet ts = client.execute("SELECT * FROM system_schema.tables");
        for (Row t: ts) {
            String ks = t.getString("keyspace_name");
            if (!ks.startsWith("system"))
                tableList.add(ks + "." + t.getString("table_name"));
        }
        return tableList;
    }
}
