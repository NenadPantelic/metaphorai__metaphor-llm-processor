package ai.metaphor.metaphor_llm_processor.model;

public enum DocumentStatus {

    // not yet processed, waiting for its turn
    PENDING,
    // being processed at the moment
    PROCESSING,
    // processing has been done, but some chunks have not been processed (error)
    INCOMPLETE,
    // completely processed
    DONE
}
