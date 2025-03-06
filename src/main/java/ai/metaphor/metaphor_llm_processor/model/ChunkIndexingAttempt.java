package ai.metaphor.metaphor_llm_processor.model;

import java.time.Instant;

public record ChunkIndexingAttempt(int attemptNo,
                                   Instant time,
                                   String error) {
}
