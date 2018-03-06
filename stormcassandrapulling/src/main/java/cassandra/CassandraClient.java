package cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraClient {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraClient.class);

    Cluster cluster;
    Session session;

    public CassandraClient() {
        try {
            cluster = Cluster.builder()
                    .addContactPoint("127.0.0.1")
                    .build();
            Session session = cluster.connect();

            ResultSet rs = session.execute("select release_version from system.local");
            Row row = rs.one();
            LOG.debug("Connected to Cassandra with version = " + row.getString("release_version"));
        } finally {
            if (cluster != null) cluster.close();
        }
    }

    public ResultSet execute(String query) {
        try {
            return session.execute("select release_version from system.local");
        } finally {
            if (cluster != null) cluster.close();
        }
    }


}
