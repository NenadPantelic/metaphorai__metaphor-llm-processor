package ai.metaphor.metaphor_llm_processor.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;

//@Testcontainers
//@SpringBootTest
// NOTE: facing a problem connecting to MongoDB, stream is prematurely closed, don't have time to deal with it atm
public class DatabaseIntegrationSetup {

//    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0").withExposedPorts(27017)
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "metaphorai-db-user")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "metaphorai-db-password")
            .withEnv("MONGO_INITDB_DATABASE", "metaphorai");

//    @DynamicPropertySource
    static void containersProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
    }
}
