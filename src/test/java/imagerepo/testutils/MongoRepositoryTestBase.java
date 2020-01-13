package imagerepo.testutils;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.After;
import org.junit.Before;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

/**
 * Uses an in-memory Mongo implementation for unit tests
 */
public class MongoRepositoryTestBase {

    private MongoServer server;
    private MongoClient client;
    private MongoTemplate mongoTemplate;

    @Before
    public final void setupMongo() {
        server = new MongoServer(new MemoryBackend());
        client = new MongoClient(new ServerAddress(server.bind()));
        mongoTemplate = new MongoTemplate(new SimpleMongoDbFactory(client, "test"));
    }

    @After
    public void cleanup() {
        server.shutdown();
        client.close();
    }

    protected MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
