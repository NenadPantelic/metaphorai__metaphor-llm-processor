package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.dto.indexing.ArticleURL;
import ai.metaphor.metaphor_llm_processor.indexing.IndexingReport;
import ai.metaphor.metaphor_llm_processor.indexing.RetryableIndexingExecutor;
import ai.metaphor.metaphor_llm_processor.model.*;
import ai.metaphor.metaphor_llm_processor.repository.DocumentIndexingFailureRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class DocumentIndexingConsumerTest {

    private final RetryableIndexingExecutor retryableIndexingExecutor = Mockito.mock(RetryableIndexingExecutor.class);
    private final DocumentIndexingFailureRepository documentIndexingFailureRepository = Mockito.mock(DocumentIndexingFailureRepository.class);

    private final DocumentIndexingConsumer documentIndexingConsumer = new DocumentIndexingConsumer(
            retryableIndexingExecutor, documentIndexingFailureRepository
    );

    @Test
    public void givenArticleUrlTestIndexArticle() {
        var articleUrl = new ArticleURL("test-source", "test-origin");

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
        IndexingReport indexingReport = new IndexingReport(chunks, null, false);
        Mockito.when(retryableIndexingExecutor.tryInitialIndexing(articleUrl.source(), articleUrl.origin()))
                .thenReturn(indexingReport);

        Mockito.verifyNoInteractions(documentIndexingFailureRepository);
    }

    @Test
    public void givenArticleUrlWhenRetryableExceptionOccurredThenIndexArticleShouldStoreDocumentIndexingFailure() {
        String source = "test-source";
        String origin = "test-origin";
        var articleUrl = new ArticleURL(source, origin);
        String errorMessage = "Document indexing failed.";

        var indexingReport = new IndexingReport(
                null, new RuntimeException("Document indexing failed."), true
        );
        ArgumentCaptor<DocumentIndexingFailure> argumentCaptor = ArgumentCaptor.forClass(
                DocumentIndexingFailure.class
        );
        Mockito.when(retryableIndexingExecutor.tryInitialIndexing(articleUrl.source(), articleUrl.origin()))
                .thenReturn(indexingReport);
        documentIndexingConsumer.indexArticle(articleUrl);

        Mockito.verify(
                documentIndexingFailureRepository, Mockito.times(1)
        ).save(argumentCaptor.capture());
        var documentIndexingFailure = argumentCaptor.getValue();

        Assertions.assertThat(documentIndexingFailure.getSource()).isEqualTo(source);
        Assertions.assertThat(documentIndexingFailure.getOrigin()).isEqualTo(origin);
        Assertions.assertThat(documentIndexingFailure.getType()).isEqualTo(OriginType.URL);
        Assertions.assertThat(documentIndexingFailure.getStatus()).isEqualTo(DocumentIndexingFailureStatus.ELIGIBLE_FOR_RETRY);

        List<DocumentIndexingAttempt> documentIndexingAttempts = new ArrayList<>(documentIndexingFailure.getAttempts());
        Assertions.assertThat(documentIndexingAttempts.size()).isEqualTo(1);
        Assertions.assertThat(documentIndexingAttempts.get(0).error()).isEqualTo(errorMessage);
    }

    @Test
    public void givenArticleUrlWhenNonRetryableExceptionOccurredThenIndexArticleShouldNotStoreDocumentIndexingFailure() {
        String source = "test-source";
        String origin = "test-origin";
        var articleUrl = new ArticleURL(source, origin);

        var indexingReport = new IndexingReport(
                null, new RuntimeException("Document indexing failed."), false
        );
        Mockito.when(retryableIndexingExecutor.tryInitialIndexing(articleUrl.source(), articleUrl.origin()))
                .thenReturn(indexingReport);

        documentIndexingConsumer.indexArticle(articleUrl);

        Mockito.verifyNoInteractions(documentIndexingFailureRepository);
    }
}