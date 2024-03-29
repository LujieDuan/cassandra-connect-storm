import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/*
 * Computes tumbling window average
 */
public class TumblingWindowAvgBolt extends BaseWindowedBolt {
    private OutputCollector collector;
    private static final Logger LOG = LoggerFactory.getLogger(RandomIntegerSpout.class);

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(TupleWindow inputWindow) {
        int sum = 0;
        List<Tuple> tuplesInWindow = inputWindow.get();
        LOG.debug("Events in current window: " + tuplesInWindow.size());
        if (tuplesInWindow.size() > 0) {
            /*
             * Since this is a tumbling window calculation,
             * we use all the tuples in the window to compute the avg.
             */
            for (Tuple tuple : tuplesInWindow) {
                sum += (Integer) tuple.getValue(0);
            }
            collector.emit(new Values(sum / tuplesInWindow.size()));
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("avg"));
    }
}