package ai.metaphor.metaphor_llm_processor.exception;

public class PromptException extends RuntimeException {

    public PromptException(String message) {
        super(message);
    }

    public PromptException(String message, Throwable cause) {
        super(message, cause);
    }

    public PromptException(Throwable cause) {
        super(cause);
    }
}
