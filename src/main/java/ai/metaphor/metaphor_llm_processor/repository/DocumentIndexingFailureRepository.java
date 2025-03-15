package ai.metaphor.metaphor_llm_processor.repository;

import ai.metaphor.metaphor_llm_processor.model.DocumentIndexingFailure;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentIndexingFailureRepository extends MongoRepository<DocumentIndexingFailure, String> {

    // TODO: add query here
    Optional<DocumentIndexingFailure> findOldestAttemptedFailureEligibleForRetry();
}
