package rcs.fileupload.testutils;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.After;
import org.junit.Before;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

public class InMemoryMongoRepositoryTestBase {

    private MongoServer server;
    private MongoTemplate mongoTemplate;

    @Before
    public final void setupMongo() {
        server = new MongoServer(new MemoryBackend());
        String connectionString = server.getLocalAddress().getHostString() + "/test";
        mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(connectionString));
    }

    @After
    public void cleanup() {
        server.shutdown();
    }

    protected MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
