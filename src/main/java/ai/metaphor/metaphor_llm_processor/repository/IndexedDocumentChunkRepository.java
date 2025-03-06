package ai.metaphor.metaphor_llm_processor.repository;

import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IndexedDocumentChunkRepository extends MongoRepository<IndexedDocumentChunk, String> {
}
