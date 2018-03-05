package redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {

    private static String REDIS_URL = "redisdb";
    private static int REDIS_PORT = 6379;
    private String url;

    public JedisPool pool;

    public RedisClient() {
        this.url = REDIS_URL;
        initialPool();
    }

    public RedisClient(String url) {
        this.url = url;
        initialPool();
    }

    private void initialPool() {
        pool = new JedisPool(new JedisPoolConfig(), url, REDIS_PORT);
    }

}
