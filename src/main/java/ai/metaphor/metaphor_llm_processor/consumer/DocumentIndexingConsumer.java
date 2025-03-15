package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.dto.indexing.ArticleURL;
import ai.metaphor.metaphor_llm_processor.indexing.DocumentIndexingService;
import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingAttempt;
import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingFailure;
import ai.metaphor.metaphor_llm_processor.model.OriginType;
import ai.metaphor.metaphor_llm_processor.repository.DocumentIndexingFailureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component // as of now until RMQ dep is added
public class DocumentIndexingConsumer {

    private final DocumentIndexingService documentIndexingService;
    private final DocumentIndexingFailureRepository documentIndexingFailureRepository;

    public DocumentIndexingConsumer(DocumentIndexingService documentIndexingService,
                                    DocumentIndexingFailureRepository documentIndexingFailureRepository) {
        this.documentIndexingService = documentIndexingService;
        this.documentIndexingFailureRepository = documentIndexingFailureRepository;
    }

    public void indexArticle(ArticleURL articleUrl) {
        String source = articleUrl.source();
        String origin = articleUrl.origin();
        log.info("Indexing an article: path = {}, origin = {}", source, origin);

        Instant now = Instant.now();
        try {
            documentIndexingService.indexFromURL(source, origin);
        } catch (Exception e) { // TODO: make it more granular
            log.error("An indexing of the document from {} failed.", source, e);
            // TODO: now every indexing attempt should be retried
            reportDocumentIndexingFailure(source, origin, e.getMessage(), now);
        }
    }

    private void reportDocumentIndexingFailure(String source,
                                               String origin,
                                               String errorMessage,
                                               Instant timestamp) {
        try {
            DocumentIndexingAttempt attempt = new DocumentIndexingAttempt(errorMessage, timestamp);
            DocumentIndexingFailure documentIndexingFailure = DocumentIndexingFailure.builder()
                    .type(OriginType.URL)
                    .source(source)
                    .origin(origin)
                    .lastIndexingAttempt(timestamp)
                    .attempts(Set.of(attempt))
                    .build();
            documentIndexingFailure = documentIndexingFailureRepository.save(documentIndexingFailure);
            log.info("Document indexing failure stored: {}", documentIndexingFailure);
        } catch (Exception e) {
            log.error("Failed to store a document indexing failure[source = {}, origin = {}] due to {}.",
                    source, origin, errorMessage, e
            );
        }
    }
}
