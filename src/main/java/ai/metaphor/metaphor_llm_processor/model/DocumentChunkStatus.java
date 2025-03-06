package ai.metaphor.metaphor_llm_processor.model;

public enum DocumentChunkStatus {

    PENDING,
    PROCESSING,
    PROCESSED,
    NEXT_ATTEMPT_NEEDED,
    FAILED_TO_PROCESS
}
