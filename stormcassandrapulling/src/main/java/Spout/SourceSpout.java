package Spout;

import cassandra.CassandraClient;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.utils.UUIDs;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SourceSpout extends BaseRichSpout{
    private static final Logger LOG = LoggerFactory.getLogger(SourceSpout.class);
    private CassandraClient CClinet;
    private JedisPool pool;
    private Jedis jedis;

    SpoutOutputCollector _collector;
    Random _rand;

    static public String TASKS_VARIABLE_NAME = "tasks";
    static public SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    static public String TIMESTAMP_COLUMN = "last_modified_uuid";
    static public String PRIMARY_KEY_COLUMN = "last_modified_uuid";

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        _collector = collector;
        _rand = new Random();
        pool = new RedisClient().pool;
        CClinet = new CassandraClient();
        this.jedis = pool.getResource();
    }

    @Override
    public void nextTuple() {
        String[] currentTask = jedis.brpop(0, TASKS_VARIABLE_NAME).get(1).split("[|]");

        String tableName = currentTask[0];
        String timestamp = currentTask[1];

        String query = String.format("SELECT * FROM %s " +
                        "WHERE %s > %s ALLOW FILTERING",
                tableName, TIMESTAMP_COLUMN, UUIDs.startOf(Long.parseLong(timestamp)));

        LOG.warn(query);

        ResultSet newRecords = CClinet.execute(query);

        Date max = new Date(Long.parseLong(timestamp));
        Boolean hasRecord = false;

        for (Row r : newRecords) {
            hasRecord = true;
            Date t = new Date(UUIDs.unixTimestamp(r.getUUID(TIMESTAMP_COLUMN)));
            UUID k = r.getUUID(PRIMARY_KEY_COLUMN);
            if (t.after(max)) max = t;
            _collector.emit("Timestamp", new Values(tableName, t));
            LOG.debug("Emitting Timestamp: {} - {}", tableName, t);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.getColumnDefinitions().size() - 1; i ++) {
                sb.append(r.getObject(i).toString());
            }
            _collector.emit("Record", new Values(tableName, sb.toString(), k.toString()));
            LOG.debug("Emitting Record: {} - {}", tableName, sb.toString(), k.toString());
        }

        _collector.emit("MaxTimestamp", new Values(tableName, max));
        LOG.debug("Emitting MaxTimestamp: {} - {}", tableName, max);

        //TODO if not emit max - can delay next task
    }
    @Override
    public void ack(Object id) {
    }

    @Override
    public void fail(Object id) {
    }

    @Override
    public void close() {
        this.jedis.close();
        this.pool.close();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream("Record", new Fields("Table", "Record", "Key"));
        declarer.declareStream("Timestamp", new Fields("Table", "Timestamp"));
        declarer.declareStream("MaxTimestamp", new Fields("Table", "MaxTimestamp"));
    }
}
