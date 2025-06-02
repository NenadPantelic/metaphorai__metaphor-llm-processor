package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.model.DocumentChunkStatus;
import ai.metaphor.metaphor_llm_processor.model.DocumentReprocessingRequest;
import ai.metaphor.metaphor_llm_processor.model.DocumentStatus;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocument;
import ai.metaphor.metaphor_llm_processor.repository.DocumentReprocessingRequestRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class MetaphorReprocessingConsumer {

    private static final List<DocumentStatus> FINAL_STATUSES = List.of(DocumentStatus.DONE, DocumentStatus.INCOMPLETE);
    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository;

    public MetaphorReprocessingConsumer(IndexedDocumentRepository documentRepository,
                                        IndexedDocumentChunkRepository chunkRepository,
                                        DocumentReprocessingRequestRepository documentReprocessingRequestRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentReprocessingRequestRepository = documentReprocessingRequestRepository;
    }

    @RabbitListener(queues = "#{@'processing-ai.metaphor.metaphor_llm_processor.configproperties.ProcessingConfigProperties'.reprocessingQueue}")
    public void consume(String documentId, List<String> reasons) {
        log.info("Processing the document[id = {}]", documentId);

        Optional<IndexedDocument> documentOptional = documentRepository.findById(documentId);
        if (documentOptional.isEmpty()) {
            log.error("There is no document with id {} to be processed...", documentId);
            return;
        }

        var document = documentOptional.get();
        if (!FINAL_STATUSES.contains(document.getStatus())) {
            log.error("Document[id = {}] is not in final state, hence cannot be processed again.", documentId);
            return;
        }

        var documentReprocessingRequestOptional = tryCreateReprocessingRequest(documentId, reasons);
        if (documentReprocessingRequestOptional.isEmpty()) {
            return;
        }

        document.setStatus(DocumentStatus.PENDING_REPROCESSING);
        document.clearAllMetaphors();
        documentRepository.save(document);

        var chunks = chunkRepository.findByDocumentId(documentId);
        chunks.forEach(chunk -> {
            chunk.clearAllAttempts();
            chunk.setStatus(DocumentChunkStatus.PENDING_REPROCESSING);
        });
        chunkRepository.saveAll(chunks);
    }

    private Optional<DocumentReprocessingRequest> tryCreateReprocessingRequest(String documentId, List<String> reasons) {
        try {
            var reprocessingRequest = DocumentReprocessingRequest.builder()
                    .documentId(documentId)
                    .reasons(reasons)
                    .directive(getDirectiveFromReprocessingRequest(reasons))
                    .build();
            reprocessingRequest = documentReprocessingRequestRepository.save(reprocessingRequest);
            return Optional.of(reprocessingRequest);
        } catch (Exception e) {
            log.error("Unable to store the document processing request[documentId = {}, reasons = {}]",
                    documentId, reasons, e);
            return Optional.empty();
        }
    }

    private String getDirectiveFromReprocessingRequest(List<String> reasons) {
        // TODO - need another template for this
        // maybe to map constants to more verbose directions and then concatenate them
        // consult with A
        return String.join("\n", reasons);
    }
}
