package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.model.DocumentChunkStatus;
import ai.metaphor.metaphor_llm_processor.model.DocumentStatus;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocument;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class MetaphorReprocessingConsumer {


    private static final List<DocumentStatus> FINAL_STATUSES = List.of(DocumentStatus.DONE, DocumentStatus.INCOMPLETE);
    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public MetaphorReprocessingConsumer(IndexedDocumentRepository documentRepository,
                                        IndexedDocumentChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void consume(String documentId) {
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
}
