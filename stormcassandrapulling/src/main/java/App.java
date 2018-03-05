import org.apache.storm.Config;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import util.StormRunner;

import java.io.IOException;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static final int DEFAULT_RUNTIME_IN_SECONDS = 60;
    private static Config topologyConfig;

    public static void main(String[] args) throws Exception {

        TopologyBuilder builder = new TopologyBuilder();



        // Build individual component
        new PullingTopology(builder).build();

        topologyConfig = createTopologyConfiguration();

        String topologyName = "DefaultTopology";
        if (args.length >= 1) {
            topologyName = args[0];
        }
        boolean runLocally = false;
        if (args.length >= 2 && args[1].equalsIgnoreCase("local")) {
            runLocally = true;
        }

        LOG.info("Topology name: " + topologyName);
        if (runLocally) {
            LOG.info("Running in local mode");
            StormRunner.runTopologyLocally(builder.createTopology(), topologyName, topologyConfig, DEFAULT_RUNTIME_IN_SECONDS);
        }
        else {
            LOG.info("Running in remote (cluster) mode");
            StormRunner.runTopologyRemotely(builder.createTopology(), topologyName, topologyConfig);
        }

    }

    private static Config createTopologyConfiguration() {
        Config conf = new Config();
        conf.setDebug(true);
        return conf;
    }


}
