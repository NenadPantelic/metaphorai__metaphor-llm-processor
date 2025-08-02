package ai.metaphor.metaphor_llm_processor.processor;

import ai.metaphor.metaphor_llm_processor.configproperties.ProcessingConfigProperties;
import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorLLMReport;
import ai.metaphor.metaphor_llm_processor.llm.MetaphorLLMService;
import ai.metaphor.metaphor_llm_processor.model.*;
import ai.metaphor.metaphor_llm_processor.repository.DocumentReprocessingRequestRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetaphorProcessor {

    private static final Map<DocumentStatus, DocumentStatus> DOCUMENT_STATUS_TRANSITION_MAP = Map.of(
            DocumentStatus.PENDING, DocumentStatus.PROCESSING,
            DocumentStatus.PENDING_REPROCESSING, DocumentStatus.REPROCESSING
    );

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository;
    private final MetaphorLLMService metaphorLLMService;
    private final int maxProcessingRetries;

    public MetaphorProcessor(IndexedDocumentRepository documentRepository,
                             IndexedDocumentChunkRepository chunkRepository,
                             DocumentReprocessingRequestRepository documentReprocessingRequestRepository,
                             MetaphorLLMService metaphorLLMService,
                             ProcessingConfigProperties processingConfigProperties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentReprocessingRequestRepository = documentReprocessingRequestRepository;
        this.metaphorLLMService = metaphorLLMService;
        this.maxProcessingRetries = processingConfigProperties.maxRetry();
    }

    @Scheduled(fixedDelayString = "#{@'processing-ai.metaphor.metaphor_llm_processor.configproperties.ProcessingConfigProperties'.intervalInMillis}")
    public void process() {
        log.info("Processing the next document/chunk...");

        // NOTE: document reprocessing is expected to happen after the initial processing
        Optional<IndexedDocument> documentOptional = documentRepository.findOldestEligibleDocumentForProcessing();
        if (documentOptional.isEmpty()) {
            log.error("There is no any document that is ready for processing...");
            return;
        }

        var document = documentOptional.get();
        log.info("Document[id = {}, status = {}] is chosen. Its next chunk is about to be processed.",
                document.getId(), document.getStatus()
        );
        updateDocumentStatusIfNeeded(document);
        documentRepository.save(document);

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findFirstChunkEligibleForProcessing(document.getId());
        if (chunkOptional.isEmpty()) {
            log.info("There is no chunk waiting to be processed...");
            document.setStatus(DocumentStatus.INCOMPLETE); // should not happen
            documentRepository.save(document);
            return;
        }

        IndexedDocumentChunk chunkToProcess = chunkOptional.get();
        String chunkId = chunkToProcess.getId();
        String chunkDocumentId = chunkToProcess.getDocumentId();
        log.info("Chunk[id = {}, documentId = {}] is about to be processed.", chunkId, chunkDocumentId);
        var now = Instant.now();

        try {
            chunkToProcess.setStatus(DocumentChunkStatus.PROCESSING);
            chunkToProcess = chunkRepository.save(chunkToProcess);

            var metaphors = analyzeMetaphors(document, chunkToProcess);
            document.addMetaphors(metaphors);

            chunkToProcess.setStatus(DocumentChunkStatus.SUCCESSFULLY_PROCESSED);
            chunkToProcess.setLastProcessingAttemptedAt(now);
            chunkToProcess.addAttempt(new ChunkProcessingAttempt(now, null));

            chunkRepository.save(chunkToProcess);
            log.info("Successfully processed chunk[id = {}, documentId = {}]", chunkId, chunkDocumentId);
            updateDocumentIfAllChunksProcessed(chunkId, chunkDocumentId, document);
        } catch (Exception e) {
            // TODO: LLM can be unavailable or return a result that is not serializable (though this should not happen
            // with newer versions of LLM)
            log.warn("The document[id = {}] processing failed. Reason: {}", document.getId(), e.getMessage(), e);
            chunkToProcess.addAttempt(new ChunkProcessingAttempt(now, e.getMessage()));
            chunkToProcess.setLastProcessingAttemptedAt(now);

            if (chunkToProcess.getAttempts().size() >= maxProcessingRetries) {
                log.warn("Processing attempt exhausted for chunk[id = {}, documentId = {}]", chunkId, chunkDocumentId);
                chunkToProcess.setStatus(DocumentChunkStatus.FAILED_TO_PROCESS);
                chunkRepository.save(chunkToProcess);
                updateDocumentIfAllChunksProcessed(chunkId, chunkDocumentId, document);
            } else {
                chunkToProcess.setStatus(DocumentChunkStatus.NEXT_ATTEMPT_NEEDED);
                chunkRepository.save(chunkToProcess);
            }
        }
    }

    Set<Metaphor> analyzeMetaphors(IndexedDocument document, IndexedDocumentChunk chunk) {
        List<MetaphorLLMReport> metaphorLLMReports;

        if (document.getStatus() == DocumentStatus.PROCESSING) {
            metaphorLLMReports = metaphorLLMService.analyzeMetaphor(chunk);
        } else { // REPROCESSING
            metaphorLLMReports = metaphorLLMService.analyzeMetaphorWithAdditionalDirective(chunk);
        }

        return metaphorLLMReports
                .stream()
                .map(report -> convertLLMReportToMetaphor(chunk.getId(), report))
                .collect(Collectors.toSet());
    }

    Metaphor convertLLMReportToMetaphor(String chunkId, MetaphorLLMReport report) {
        if (report == null) {
            return null;
        }

        return Metaphor.builder()
                .phrase(report.phrase())
                .offset(report.offset())
                .explanation(report.explanation())
                .type(MetaphorType.valueOf(report.metaphorType()))
                .chunkId(chunkId)
                .build();
    }

    void updateDocumentStatusIfNeeded(IndexedDocument document) {
        var newStatus = DOCUMENT_STATUS_TRANSITION_MAP.get(document.getStatus());
        if (newStatus != null) {
            document.setStatus(newStatus);
        }
    }

    void updateDocumentIfAllChunksProcessed(String chunkId, String chunkDocumentId, IndexedDocument document) {
        log.info("Checking if chunkId '{}' was the last chunk of document[id = {}]", chunkId, chunkDocumentId);
        int allChunksCount = chunkRepository.countByDocumentId(chunkDocumentId);
        // TODO: can be one aggregating query
        int successfullyProcessedCount = chunkRepository.countSuccessfullyProcessedByDocumentId(chunkDocumentId);
        int processingFailuresCount = chunkRepository.countProcessingFailuresByDocumentId(chunkDocumentId);

        log.info("Document[id = {}] chunk processing completeness report: processed with success = {}, " +
                        "processed with failure = {}, total = {}", chunkDocumentId, successfullyProcessedCount,
                processingFailuresCount, allChunksCount);

        if (successfullyProcessedCount + processingFailuresCount == allChunksCount) {
            var currentStatus = document.getStatus();
            log.info("All chunks of a document[id = {}] are processed.", chunkDocumentId);
            DocumentStatus documentStatus = processingFailuresCount == 0 ?
                    DocumentStatus.DONE :
                    DocumentStatus.INCOMPLETE;
            document.setStatus(documentStatus);
            documentRepository.save(document);

            if (currentStatus == DocumentStatus.REPROCESSING) {
                tryRemoveReprocessingRequest(document.getId());
            }
        }
    }

    void tryRemoveReprocessingRequest(String documentId) {
        try {
            documentReprocessingRequestRepository.deleteByDocumentId(documentId);
        } catch (Exception e) {
            log.error("Unable to delete the document reprocessing request[documentId = {}]. This will block the next " +
                    "such request for the same document.", documentId, e);
        }
    }

}
