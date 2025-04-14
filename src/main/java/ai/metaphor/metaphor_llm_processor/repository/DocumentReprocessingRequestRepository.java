package ai.metaphor.metaphor_llm_processor.repository;

import ai.metaphor.metaphor_llm_processor.model.DocumentReprocessingRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentReprocessingRequestRepository extends MongoRepository<DocumentReprocessingRequest, String> {

    Optional<DocumentReprocessingRequest> findByDocumentId(String documentId);

    void deleteByDocumentId(String documentId);
}
