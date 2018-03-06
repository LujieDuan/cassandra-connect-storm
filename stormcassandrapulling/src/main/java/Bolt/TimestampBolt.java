package Bolt;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

public class TimestampBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(TimestampBolt.class);
    private final String TAGS_VARIABLE_NAME = "tags";
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
        String tag = tuple.getString(0);

        LOG.debug("Submit Tags: {}", tag);

        jedis.lpush(TAGS_VARIABLE_NAME, tag);
        // _collector.emit(new Values(tag));

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
        // Final Bolt
        // declarer.declare(new Fields("Tag"));
    }
}