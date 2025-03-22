package ai.metaphor.metaphor_llm_processor.indexing;

import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public class IndexingReport {

    @Getter
    private final List<IndexedDocumentChunk> chunks;
    @Getter
    private final Exception exception;
    private final boolean retryPossible;

    public boolean passed() {
        return chunks != null && exception == null;
    }

    public boolean hasException() {
        return exception != null;
    }

    public boolean retryableExceptionOccurred() {
        return hasException() && retryPossible;
    }
}
