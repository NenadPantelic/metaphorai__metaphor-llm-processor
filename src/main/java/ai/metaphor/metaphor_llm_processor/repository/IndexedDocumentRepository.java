package ai.metaphor.metaphor_llm_processor.repository;

import ai.metaphor.metaphor_llm_processor.model.IndexedDocument;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IndexedDocumentRepository extends MongoRepository<IndexedDocument, String> {

    @Aggregation(pipeline = {
            "{$match: { 'status': {$in: ['PENDING', 'PROCESSING']}}}",
            "{$sort: {'createdAt': 1}}",
            "{$limit: 1}"
    })
    Optional<IndexedDocument> findOldestEligibleDocumentForProcessing();
}
