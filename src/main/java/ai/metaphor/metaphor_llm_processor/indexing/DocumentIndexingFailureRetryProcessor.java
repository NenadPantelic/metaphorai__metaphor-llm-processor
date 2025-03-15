package ai.metaphor.metaphor_llm_processor.indexing;

import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingAttempt;
import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingFailure;
import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingFailureStatus;
import ai.metaphor.metaphor_llm_processor.repository.DocumentIndexingFailureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class DocumentIndexingFailureRetryProcessor {

    private final DocumentIndexingFailureRepository indexingFailureRepository;
    private final DocumentIndexingService documentIndexingService;

    public DocumentIndexingFailureRetryProcessor(DocumentIndexingFailureRepository indexingFailureRepository,
                                                 DocumentIndexingService documentIndexingService) {
        this.indexingFailureRepository = indexingFailureRepository;
        this.documentIndexingService = documentIndexingService;
    }

    /**
     * Process the document indexing failure.
     * It tries finding a failure eligible for retry and takes another shot.
     */
    // TODO: add cron expression
    public void process() {
        log.info("Try finding a document indexing failure eligible for retry...");
        var failureToProcessOptional = indexingFailureRepository.findOldestAttemptedFailureEligibleForRetry();

        if (failureToProcessOptional.isEmpty()) {
            log.info("No document indexing failure eligible for retry has been found");
            return;
        }

        var failureToProcess = failureToProcessOptional.get();
        tryIndexingDocument(failureToProcess);
    }

    // TODO: isolate the common code to a separate component
    private void tryIndexingDocument(DocumentIndexingFailure documentIndexingFailure) {
        String source = documentIndexingFailure.getSource();
        String origin = documentIndexingFailure.getOrigin();

        var attemptsSoFar = documentIndexingFailure.getAttempts().size();
        log.info("Try indexing an article[path = {}, origin = {}] which indexing previously failed {} time(s)",
                source, origin, attemptsSoFar
        );

        Instant now = Instant.now();
        try {
            documentIndexingService.indexFromURL(source, origin);
            log.info("Document indexing [path = {}, origin = {}] has passed with {}", source, origin, attemptsSoFar + 1);
            documentIndexingFailure.setStatus(DocumentIndexingFailureStatus.INDEXING_PASSED);
        } catch (Exception e) {
            log.error("An indexing of the document from {} failed.", source, e);

            var documentIndexingAttempt = new DocumentIndexingAttempt(e.getMessage(), now);
            documentIndexingFailure.addIndexingAttempt(documentIndexingAttempt);

            // TODO: make it reading from config
            if (documentIndexingFailure.getAttempts().size() >= 3) {
                documentIndexingFailure.setStatus(DocumentIndexingFailureStatus.ALL_ATTEMPTS_FAILED);
            }
        } finally {
            // TODO: check
            indexingFailureRepository.save(documentIndexingFailure);
        }
    }
}
