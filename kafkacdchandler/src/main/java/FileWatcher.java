import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.commitlog.CommitLogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

/***
 * Modified based on:
 * https://github.com/smartcat-labs/cassandra-kafka-connector/blob/master/cassandra-cdc/src/main/java/io/smartcat/cassandra/cdc/Reader.java
 */

public class FileWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);

    private final WatchService watcher;
    private final Path dir;
    private final WatchKey key;
    private final CommitLogReader commitLogReader;
    private final KafkaCommitLogReadHandler commitLogReadHandler;

    /**
     * Creates a WatchService and registers the given directory
     */
    public FileWatcher(Map<String, Object> configuration) throws IOException {
        this.dir = Paths.get((String) YamlUtils.select(configuration, "cassandra.cdc_raw_directory"));
        watcher = FileSystems.getDefault().newWatchService();
        key = dir.register(watcher, ENTRY_CREATE);
        commitLogReader = new CommitLogReader();
        commitLogReadHandler = new KafkaCommitLogReadHandler(configuration);
        DatabaseDescriptor.toolInitialization();
        Schema.instance.loadFromDisk(false);
    }

    /**
     * Process all events for keys queued to the watcher
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void processEvents() throws InterruptedException, IOException {
        while (true) {
            WatchKey aKey = watcher.take();
            if (!key.equals(aKey)) {
                LOG.error("WatchKey not recognized.");
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind != ENTRY_CREATE) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path relativePath = ev.context();
                Path absolutePath = dir.resolve(relativePath);
                processCommitLogSegment(absolutePath);
                Files.delete(absolutePath);

                // print out event
                LOG.debug("{}: {}", event.kind().name(), absolutePath);
            }
            key.reset();
        }
    }

    private void processCommitLogSegment(Path path) throws IOException {
        LOG.debug("Processing commitlog segment...");
        commitLogReader.readCommitLogSegment(commitLogReadHandler, path.toFile(), false);
        LOG.debug("Commitlog segment processed.");
    }
}
