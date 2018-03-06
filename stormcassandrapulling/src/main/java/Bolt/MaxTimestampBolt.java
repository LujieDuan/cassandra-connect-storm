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

import java.util.Date;
import java.util.Map;

import static Spout.SourceSpout.TASKS_VARIABLE_NAME;
import static Spout.SourceSpout.timestampFormat;

public class MaxTimestampBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(MaxTimestampBolt.class);
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

        Date timestamp = (Date) tuple.getValue(1);

        String timestampString = timestampFormat.format(timestamp);

        LOG.debug("Submit Tasks: {} - {}", tableName, timestampString);
        jedis.lpush(TASKS_VARIABLE_NAME, String.format("%s|%s", tableName, timestampString));
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