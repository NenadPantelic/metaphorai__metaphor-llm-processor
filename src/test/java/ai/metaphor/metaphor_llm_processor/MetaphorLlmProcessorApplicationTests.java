package ai.metaphor.metaphor_llm_processor;

import ai.metaphor.metaphor_llm_processor.indexing.DocumentIndexingService;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MetaphorLlmProcessorApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private DocumentIndexingService documentIndexingService;


    // dummy test to check how the indexing works
    // TODO: remove
   // @Test
    public void run() {
        // thetimes.com
        String url = "https://www.thetimes.com/uk/technology-uk/article/i-used-my-21st-birthday-money-to-patent-an-app-that-protects-women-vf0bkqgt3";
        String origin = "thetimes.com";

        List<IndexedDocumentChunk> chunks = documentIndexingService.indexFromURL(url, origin);
        System.out.println(chunks);
    }
}
