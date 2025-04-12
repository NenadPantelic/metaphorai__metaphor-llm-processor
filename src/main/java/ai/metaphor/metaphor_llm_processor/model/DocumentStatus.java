package ai.metaphor.metaphor_llm_processor.model;

public enum DocumentStatus {

    // not yet ready, still being indexed
    NOT_READY,
    // not yet processed, waiting for its turn
    PENDING,
    // being processed at the moment
    PROCESSING,
    // waiting reprocessing
    PENDING_REPROCESSING,
    // processing has been done, but some chunks have not been processed (error)
    INCOMPLETE,
    // completely processed
    DONE
}
