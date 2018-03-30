import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.commitlog.CommitLogDescriptor;
import org.apache.cassandra.db.commitlog.CommitLogReadHandler;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.io.IOException;

public class KafkaCommitLogReadHandler implements CommitLogReadHandler{

    private static final Logger LOG = LoggerFactory.getLogger(KafkaCommitLogReadHandler.class);

    private final String topic;
    private final Producer<String, String> producer;

    public KafkaCommitLogReadHandler(Map<String, Object> configuration) {
        topic = (String) YamlUtils.select(configuration, "kafka.topic");
        producer = new KafkaProducer<>((Map) YamlUtils.select(configuration, "kafka.configuration"));
    }

    /**
     * Handle an error during segment read, signaling whether or not you want the reader to skip the remainder of the
     * current segment on error.
     *
     * @param e CommitLogReadException w/details on exception state
     * @return boolean indicating whether to stop reading
     * @throws IOException In the event the handler wants forceful termination of all processing, throw IOException.
     */
    @Override
    public boolean shouldSkipSegmentOnError(CommitLogReadException e) throws IOException {
        LOG.debug("Should skip segment on error.");
        e.printStackTrace();
        return true;
    }

    /**
     * In instances where we cannot recover from a specific error and don't care what the reader thinks
     *
     * @param e CommitLogReadException w/details on exception state
     * @throws IOException
     */
    @Override
    public void handleUnrecoverableError(CommitLogReadException e) throws IOException {
        LOG.debug("Handle unrecoverable error called.");
        throw new NotImplementedException();
    }

    /**
     * Process a deserialized mutation
     *
     * @param mutation deserialized mutation
     * @param size serialized size of the mutation
     * @param entryLocation filePointer offset inside the CommitLogSegment for the record
     * @param desc CommitLogDescriptor for mutation being processed
     */
    @Override
    public void handleMutation(Mutation mutation, int size, int entryLocation, CommitLogDescriptor desc) {
        LOG.debug("Handle mutation started...");
        for (PartitionUpdate partitionUpdate : mutation.getPartitionUpdates()) {
            process(partitionUpdate);
        }
        LOG.debug("Handle mutation finished...");
    }

    @SuppressWarnings("unchecked")
    private void process(Partition partition) {
        LOG.debug("Process method started...");

        String keyspace = partition.metadata().ksName;
        String tableName = partition.metadata().cfName;
        String key = getKey(partition);
        JSONObject obj = new JSONObject();
        obj.put("keyspace", keyspace);
        obj.put("table", tableName);
        obj.put("key", key);
        if (partitionIsDeleted(partition)) {
            obj.put("partitionDeleted", true);
        } else {
            UnfilteredRowIterator it = partition.unfilteredIterator();
            List<JSONObject> rows = new ArrayList<>();
            while (it.hasNext()) {
                Unfiltered un = it.next();
                if (un.isRow()) {
                    JSONObject jsonRow = new JSONObject();
                    Clustering clustering = (Clustering) un.clustering();
                    String clusteringKey = clustering.toCQLString(partition.metadata());
                    jsonRow.put("clusteringKey", clusteringKey);
                    Row row = partition.getRow(clustering);

                    if (rowIsDeleted(row)) {
                        obj.put("rowDeleted", true);
                    } else {
                        Iterator<Cell> cells = row.cells().iterator();
                        Iterator<ColumnDefinition> columns = row.columns().iterator();
                        List<JSONObject> cellObjects = new ArrayList<>();
                        while (cells.hasNext() && columns.hasNext()) {
                            JSONObject jsonCell = new JSONObject();
                            ColumnDefinition columnDef = columns.next();
                            Cell cell = cells.next();
                            jsonCell.put("name", columnDef.name.toString());
                            if (cell.isTombstone()) {
                                jsonCell.put("deleted", true);
                            } else {
                                String data = columnDef.type.getString(cell.value());
                                jsonCell.put("value", data);
                            }
                            cellObjects.add(jsonCell);
                        }
                        jsonRow.put("cells", cellObjects);
                    }
                    rows.add(jsonRow);
                } else if (un.isRangeTombstoneMarker()) {
                    obj.put("rowRangeDeleted", true);
                    ClusteringBound bound = (ClusteringBound) un.clustering();
                    List<JSONObject> bounds = new ArrayList<>();
                    for (int i = 0; i < bound.size(); i++) {
                        String clusteringBound = partition.metadata().comparator.subtype(i).getString(bound.get(i));
                        JSONObject boundObject = new JSONObject();
                        boundObject.put("clusteringKey", clusteringBound);
                        if (i == bound.size() - 1) {
                            if (bound.kind().isStart()) {
                                boundObject.put("inclusive",
                                        bound.kind() == ClusteringPrefix.Kind.INCL_START_BOUND);
                            }
                            if (bound.kind().isEnd()) {
                                boundObject.put("inclusive",
                                        bound.kind() == ClusteringPrefix.Kind.INCL_END_BOUND);
                            }
                        }
                        bounds.add(boundObject);
                    }
                    obj.put((bound.kind().isStart() ? "start" : "end"), bounds);
                }
            }
            obj.put("rows", rows);
        }
        LOG.debug("Creating json value...");
        String value = obj.toJSONString();
        LOG.debug("Created json value '{}'", value);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        LOG.debug("Created producer record with topic {}, key {}, value {}", topic, key, value);
        producer.send(record);
        LOG.debug("Sent record to kafka.");
    }

    private boolean partitionIsDeleted(Partition partition) {
        return partition.partitionLevelDeletion().markedForDeleteAt() > Long.MIN_VALUE;
    }

    private boolean rowIsDeleted(Row row) {
        return row.deletion().time().markedForDeleteAt() > Long.MIN_VALUE;
    }

    private String getKey(Partition partition) {
        return partition.metadata().getKeyValidator().getString(partition.partitionKey().getKey());
    }

}
