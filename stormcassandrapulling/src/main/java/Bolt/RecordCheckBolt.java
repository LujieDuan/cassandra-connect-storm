package Bolt;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

public class RecordCheckBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(RecordCheckBolt.class);
    JedisPool pool;
    Jedis jedis;
    OutputCollector _collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this._collector = collector;
        this.pool = new RedisClient().pool;
        jedis = pool.getResource();
    }

    @Override
    public void execute(Tuple tuple)
    {
        String tableName = tuple.getString(0);
        String record = tuple.getString(1);
        String primaryKey = tuple.getString(2);

        LOG.debug("Check Record: {} - {}", tableName, primaryKey);

        if (!jedis.sismember(tableName, primaryKey)) {
            jedis.sadd(tableName, primaryKey);
            _collector.emit(new Values(tableName, record));
        }
    }

    @Override
    public void cleanup(){
        super.cleanup();
        jedis.close();
        pool.close();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
        declarer.declare(new Fields("Table", "Record"));
    }
}