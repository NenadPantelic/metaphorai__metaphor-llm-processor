package ai.metaphor.metaphor_llm_processor.model;

import java.time.Instant;

public record ChunkProcessingAttempt(Instant time,
                                     String error) {
}
