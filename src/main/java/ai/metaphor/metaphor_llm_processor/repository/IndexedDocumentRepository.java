package ai.metaphor.metaphor_llm_processor.repository;

import ai.metaphor.metaphor_llm_processor.model.IndexedDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IndexedDocumentRepository extends MongoRepository<IndexedDocument, String> {
}
