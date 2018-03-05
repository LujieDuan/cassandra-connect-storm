package Spout;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SourceSpout extends BaseRichSpout{
    private static final Logger LOG = LoggerFactory.getLogger(SourceSpout.class);
    private TumblrClient TClinet;
    private JedisPool pool;
    private int clientCount;
    private Jedis jedis;

    SpoutOutputCollector _collector;
    Random _rand;

    private String TAGS_VARIABLE_NAME = "tags";

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        _collector = collector;
        _rand = new Random();
        pool = new RedisClient().pool;

        try {
            TClinet = new TumblrClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.jedis = pool.getResource();
        clientCount = TClinet.numClient();
    }

    @Override
    public void nextTuple() {
        JumblrClient currentClient = TClinet.getClient(_rand.nextInt(clientCount));

        String currentTag = jedis.brpop(0, TAGS_VARIABLE_NAME).get(0);

        List<Post> currentPosts = getPostsWithTag(currentClient, currentTag);

        for (Post p : currentPosts) {
            String blog = p.getBlogName();
            _collector.emit("Blog", new Values(blog));
            LOG.debug("Emitting blog: {}", blog);

            List<String> tags = p.getTags();
            for (String t : tags) {
                _collector.emit("Tag", new Values(t));
                LOG.debug("Emitting tag: {}", t);
            }

            if (p instanceof TextPost) {
                TextPost asTextPost = (TextPost) p;
                String title = asTextPost.getSourceTitle();
                String body = asTextPost.getBody();
                _collector.emit("Post", new Values(title));
                _collector.emit("Post", new Values(body));
                LOG.debug("Emitting post: {}", title);
                LOG.debug("Emitting post: {}", body);
            }
        }
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
        declarer.declareStream("Post", new Fields("Post"));
        declarer.declareStream("Tag", new Fields("Tag"));
        declarer.declareStream("Blog", new Fields("Blog"));
    }

    /**
     * TODO: tagged can use options
     * @param client
     * @param tag
     * @return
     */
    private List<Post> getPostsWithTag(JumblrClient client, String tag) {
        return client.tagged(tag);
    }

}
