package ai.metaphor.metaphor_llm_processor.exception;

public class LLMException extends RuntimeException {

    public LLMException() {
    }

    public LLMException(String message) {
        super(message);
    }

    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
}
