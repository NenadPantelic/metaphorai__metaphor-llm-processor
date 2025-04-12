package ai.metaphor.metaphor_llm_processor.repository;

import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;
import java.util.List;

public interface IndexedDocumentChunkRepository extends MongoRepository<IndexedDocumentChunk, String> {

    @Aggregation(pipeline = {
            "{$match: { 'documentId': ?1, 'status': {$in: ['PENDING', 'NEXT_ATTEMPT_NEEDED', 'PENDING_REPROCESSING']}}}",
            "{$sort: {'order': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocumentChunk> findFirstChunkEligibleForProcessing(String documentId);

    List<IndexedDocumentChunk> findByDocumentId(String documentId);

    int countByDocumentId(String documentId);

    @Query(value = "{'status': 'SUCCESSFULLY_PROCESSED'}", count = true)
    int countSuccessfullyProcessedByDocumentId(String documentId);

    @Query(value = "{'status': 'FAILED_TO_PROCESS'}", count = true)
    int countProcessingFailuresByDocumentId(String documentId);
}
