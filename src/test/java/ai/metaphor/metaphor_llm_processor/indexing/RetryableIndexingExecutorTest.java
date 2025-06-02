package ai.metaphor.metaphor_llm_processor.indexing;

import ai.metaphor.metaphor_llm_processor.configproperties.IndexingConfigProperties;
import ai.metaphor.metaphor_llm_processor.model.DocumentChunkStatus;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RetryableIndexingExecutorTest {

    private final DocumentIndexingService documentIndexingService = Mockito.mock(DocumentIndexingService.class);
    private final IndexingConfigProperties indexingConfigProperties = new IndexingConfigProperties(
            3, 30, "q.testindexingq"
    );

    private final RetryableIndexingExecutor retryableIndexingExecutor = new RetryableIndexingExecutor(
            documentIndexingService, indexingConfigProperties
    );

    @Test
    public void givenSourcePathAndOriginTestTryInitialIndexing() {
        String source = "http://test.com/johndoe";
        String origin = "test.com";

        var now = Instant.now();
        var documentChunkOne = new IndexedDocumentChunk(
                "test-id-1", "test-document-id-1", "Test 1234567890", DocumentChunkStatus.PENDING, 1,
                null, now, now, now
        );
        var documentChunkTwo = new IndexedDocumentChunk(
                "test-id-2", "test-document-id-2", "Test 0987654321", DocumentChunkStatus.PENDING, 2,
                null, now, now, now
        );
        List<IndexedDocumentChunk> chunks = List.of(documentChunkOne, documentChunkTwo);
        Mockito.when(documentIndexingService.indexFromURL(source, origin)).thenReturn(chunks);

        IndexingReport indexingReport = retryableIndexingExecutor.tryInitialIndexing(source, origin);

        Assertions.assertThat(indexingReport.getChunks()).isEqualTo(chunks);
        Assertions.assertThat(indexingReport.passed()).isTrue();
        Assertions.assertThat(indexingReport.retryableExceptionOccurred()).isFalse();
    }


    @Test
    public void givenSourcePathAndOriginWhenIndexingThrowsAnErrorAndRetryIsPossible() {
        String source = "http://test.com/johndoe";
        String origin = "test.com";

        Mockito.doThrow(new RuntimeException("Indexing failed..."))
                .when(documentIndexingService).indexFromURL(source, origin);
        IndexingReport indexingReport = retryableIndexingExecutor.tryIndexing(source, origin, 1);

        Assertions.assertThat(indexingReport.getChunks()).isNull();
        Assertions.assertThat(indexingReport.passed()).isFalse();
        Assertions.assertThat(indexingReport.hasException()).isTrue();
    }


    @Test
    public void givenSourcePathAndOriginWhenIndexingThrowsAnErrorAndRetryIsNotPossible() {
        String source = "http://test.com/johndoe";
        String origin = "test.com";

        Mockito.doThrow(new RuntimeException("Indexing failed..."))
                .when(documentIndexingService).indexFromURL(source, origin);
        IndexingReport indexingReport = retryableIndexingExecutor.tryIndexing(source, origin, 2);

        Assertions.assertThat(indexingReport.getChunks()).isNull();
        Assertions.assertThat(indexingReport.passed()).isFalse();
        Assertions.assertThat(indexingReport.hasException()).isTrue();
        Assertions.assertThat(indexingReport.retryableExceptionOccurred()).isFalse();
    }
}