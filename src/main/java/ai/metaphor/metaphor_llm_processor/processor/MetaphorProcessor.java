package ai.metaphor.metaphor_llm_processor.processor;

import ai.metaphor.metaphor_llm_processor.configproperties.ProcessingConfigProperties;
import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorLLMReport;
import ai.metaphor.metaphor_llm_processor.llm.MetaphorLLMService;
import ai.metaphor.metaphor_llm_processor.model.*;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetaphorProcessor {

    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final MetaphorLLMService metaphorLLMService;
    private final int maxProcessingRetries;

    public MetaphorProcessor(IndexedDocumentRepository documentRepository,
                             IndexedDocumentChunkRepository chunkRepository,
                             MetaphorLLMService metaphorLLMService,
                             ProcessingConfigProperties processingConfigProperties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.metaphorLLMService = metaphorLLMService;
        this.maxProcessingRetries = processingConfigProperties.maxRetry();
    }


    @Scheduled(fixedDelayString = "#{@'processing-ai.metaphor.metaphor_llm_processor.configproperties.ProcessingConfigProperties'.intervalInMillis}")
    public void process() {
        log.info("Processing the next chunk...");

        Optional<IndexedDocumentChunk> chunkOptional = chunkRepository.findOldestAttemptedChunkEligibleForProcessing();
        if (chunkOptional.isEmpty()) {
            log.info("There is no chunk waiting to be processed...");
            return;
        }

        IndexedDocumentChunk chunkToProcess = chunkOptional.get();
        String chunkId = chunkToProcess.getId();
        String chunkDocumentId = chunkToProcess.getDocumentId();

        var documentOptional = documentRepository.findById(chunkDocumentId);
        if (documentOptional.isEmpty()) {
            // should not happen
            log.error("Got in inconsistent state, the documentOptional[id = {}] was deleted, but its chunks are still there...",
                    chunkDocumentId);
            chunkToProcess.setStatus(DocumentChunkStatus.FAILED_TO_PROCESS);
            chunkRepository.save(chunkToProcess);
            return;
        }
        var document = documentOptional.get();

        log.info("Chunk[id = {}, documentId = {}] is about to be processed.", chunkId, chunkDocumentId);
        var now = Instant.now();

        // TODO: document status should be updated in any case - PROCESSING, INCOMPLETE, COMPLETE
        try {
            chunkToProcess.setStatus(DocumentChunkStatus.PROCESSING);
            chunkToProcess = chunkRepository.save(chunkToProcess);

            var metaphorLLMReports = metaphorLLMService.analyzeMetaphor(chunkToProcess);
            var metaphors = metaphorLLMReports
                    .stream()
                    .map(report -> convertLLMReportToMetaphor(chunkId, report))
                    .collect(Collectors.toSet());
            document.addMetaphors(metaphors);

            chunkToProcess.setStatus(DocumentChunkStatus.PROCESSED);
            chunkToProcess.setLastProcessingAttemptedAt(now);
            chunkToProcess.addAttempt(new ChunkProcessingAttempt(now, null));

            chunkRepository.save(chunkToProcess);
            log.info("Successfully processed chunk[id = {}, documentId = {}]", chunkId, chunkDocumentId);
            updateDocumentIfAllChunksProcessed(chunkId, chunkDocumentId, document);
        } catch (Exception e) {
            // TODO: LLM can be unavailable or return a result that is not serializable (though this should not happen
            // with newer versions of LLM)
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

    private void updateDocumentIfAllChunksProcessed(String chunkId, String chunkDocumentId, IndexedDocument document) {
        if (allDocumentChunksAreProcessed(chunkId, chunkDocumentId)) {
            log.info("All chunks of a document[id = {}] are processed.", chunkDocumentId);
            document.setStatus(DocumentStatus.DONE);
            documentRepository.save(document);
        }
    }

    private boolean allDocumentChunksAreProcessed(String chunkId, String chunkDocumentId) {
        log.info("Checking if chunkId '{}' was the last chunk of document[id = {}]", chunkId, chunkDocumentId);
        int allChunksCount = chunkRepository.countByDocumentId(chunkDocumentId);
        int processedChunksCount = chunkRepository.countProcessedByDocumentId(chunkDocumentId);

        log.info("Document[id = {}] chunk processing completeness report: {}/{}", chunkDocumentId,
                processedChunksCount, allChunksCount);
        return processedChunksCount == allChunksCount;
    }

    Metaphor convertLLMReportToMetaphor(String chunkId, MetaphorLLMReport report) {
        if (report == null) {
            return null;
        }

        return Metaphor.builder()
                .phrase(report.phrase())
                .offset(report.offset())
                .explanation(report.explanation())
                .chunkId(chunkId)
                .build();
    }
}
