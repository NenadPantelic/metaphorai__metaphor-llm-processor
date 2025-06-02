package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.dto.indexing.Article;
import ai.metaphor.metaphor_llm_processor.indexing.IndexingReport;
import ai.metaphor.metaphor_llm_processor.indexing.RetryableIndexingExecutor;
import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingAttempt;
import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingFailure;
import ai.metaphor.metaphor_llm_processor.model.OriginType;
import ai.metaphor.metaphor_llm_processor.repository.DocumentIndexingFailureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component

public class DocumentIndexingConsumer {

    private final RetryableIndexingExecutor retryableIndexingExecutor;
    private final DocumentIndexingFailureRepository documentIndexingFailureRepository;

    public DocumentIndexingConsumer(RetryableIndexingExecutor retryableIndexingExecutor,
                                    DocumentIndexingFailureRepository documentIndexingFailureRepository) {
        this.retryableIndexingExecutor = retryableIndexingExecutor;
        this.documentIndexingFailureRepository = documentIndexingFailureRepository;
    }

    @RabbitListener(queues = "#{@'indexing-ai.metaphor.metaphor_llm_processor.configproperties.IndexingConfigProperties'.queue}")// TODO: think about the concurrency
    public void indexArticle(Article article) {
        String source = article.source();
        String origin = article.origin();
        log.info("Indexing an article: path = {}, origin = {}", source, origin);

        Instant now = Instant.now();
        IndexingReport indexingReport = retryableIndexingExecutor.tryInitialIndexing(source, origin);

        if (indexingReport.retryableExceptionOccurred()) {
            reportDocumentIndexingFailure(source, origin, indexingReport.getException().getMessage(), now);
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
                    .attempts(List.of(attempt))
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
