package Spout;

import cassandra.CassandraClient;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SourceSpout extends BaseRichSpout{
    private static final Logger LOG = LoggerFactory.getLogger(SourceSpout.class);
    private CassandraClient CClinet;
    private JedisPool pool;
    private Jedis jedis;

    SpoutOutputCollector _collector;
    Random _rand;

    private String TAGS_VARIABLE_NAME = "tasks";
    private String TIMESTAMP_COLUMN = "last_modified";
    private String QUERY_LIMIT = "100";

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
        String[] currentTask = jedis.brpop(0, TAGS_VARIABLE_NAME).get(0).split("|");

        String tableName = currentTask[0];
        String timestamp = currentTask[1];

        ResultSet newRecords = CClinet.execute(String.format("SELECT FROM %s WHERE %s > '%s' LIMIT %s",
                tableName, TIMESTAMP_COLUMN, timestamp, QUERY_LIMIT));

        Date max = new Date(0);

        for (Row r : newRecords) {
            Date t = r.getTimestamp(TIMESTAMP_COLUMN);
            if (t.after(max)) max = t;
            _collector.emit("Timestamp", new Values(tableName, t));
            LOG.debug("Emitting Timestamp: {} - {}", tableName, t);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.getColumnDefinitions().size() - 1; i ++) {
                sb.append(r.getString(i));
            }
            _collector.emit("Record", new Values(tableName, sb.toString()));
            LOG.debug("Emitting Record: {} - {}", tableName, sb.toString());
        }
        _collector.emit("MaxTimestamp", new Values(tableName, max));
        LOG.debug("Emitting MaxTimestamp: {} - {}", tableName, max);
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
        declarer.declareStream("Record", new Fields("Table", "Record"));
        declarer.declareStream("Timestamp", new Fields("Table", "Timestamp"));
        declarer.declareStream("MaxTimestamp", new Fields("Table", "MaxTimestamp"));
    }
}
