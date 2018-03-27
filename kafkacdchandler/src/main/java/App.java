import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        String configPath = "/resources/Handler.yml";
        if (args.length >= 1) {
            configPath = args[0];
        }

        LOG.info("Start!");
        Map<String, Object> configuration = YamlUtils.load(configPath);
        new FileWatcher(configuration).processEvents();

    }
}
