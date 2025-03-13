package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.dto.indexing.ArticleURL;
import ai.metaphor.metaphor_llm_processor.indexing.DocumentIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component // as of now until RMQ dep is added
public class DocumentIndexingConsumer {

    private final DocumentIndexingService documentIndexingService;

    public DocumentIndexingConsumer(DocumentIndexingService documentIndexingService) {
        this.documentIndexingService = documentIndexingService;
    }

    public void indexArticle(ArticleURL articleUrl) {
        String sourcePath = articleUrl.url();
        String sourceOrigin = articleUrl.source();
        log.info("Indexing an article: path = {}, origin = {}", sourcePath, sourceOrigin);

        Instant now = Instant.now();

        try {
            documentIndexingService.indexFromURL(sourcePath, sourceOrigin);
        } catch (Exception e) { // TODO: make it more granular
            log.error("An indexing of the document from {} failed.", sourcePath, e);
            // TODO: retry indexing if needed
        }
    }
}
