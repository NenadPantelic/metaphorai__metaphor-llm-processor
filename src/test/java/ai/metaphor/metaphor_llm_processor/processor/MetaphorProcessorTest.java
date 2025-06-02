package ai.metaphor.metaphor_llm_processor.processor;

import ai.metaphor.metaphor_llm_processor.configproperties.ProcessingConfigProperties;
import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorLLMReport;
import ai.metaphor.metaphor_llm_processor.llm.MetaphorLLMService;
import ai.metaphor.metaphor_llm_processor.model.*;
import ai.metaphor.metaphor_llm_processor.repository.DocumentReprocessingRequestRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MetaphorProcessorTest {

    private final IndexedDocumentRepository documentRepository = Mockito.mock(IndexedDocumentRepository.class);
    private final IndexedDocumentChunkRepository chunkRepository = Mockito.mock(IndexedDocumentChunkRepository.class);
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository = Mockito.mock(
            DocumentReprocessingRequestRepository.class
    );
    private final MetaphorLLMService metaphorLLMService = Mockito.mock(MetaphorLLMService.class);
    private final ProcessingConfigProperties processingConfigProperties = new ProcessingConfigProperties(
            3, 100, "q.testreprocessingq"
    );

    private final MetaphorProcessor metaphorProcessor = new MetaphorProcessor(
            documentRepository, chunkRepository, documentReprocessingRequestRepository,
            metaphorLLMService, processingConfigProperties
    );

    @Test
    public void testProcessWhenNoEligibleDocument() {
        Mockito.doReturn(Optional.empty())
                .when(documentRepository)
                .findOldestEligibleDocumentForProcessing();

        metaphorProcessor.process();

        Mockito.verify(documentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testProcess() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PENDING)
                .build();
        Mockito.doReturn(Optional.of(document))
                .when(documentRepository)
                .findOldestEligibleDocumentForProcessing();

        var chunkId = UUID.randomUUID().toString();
        var documentChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .build();

        Mockito.doReturn(Optional.of(documentChunk))
                .when(chunkRepository)
                .findFirstChunkEligibleForProcessing(documentId);

        var processingChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .status(DocumentChunkStatus.PROCESSING)
                .build();
        Mockito.doReturn(processingChunk).when(chunkRepository).save(documentChunk);

        var metaphorReportOne = new MetaphorLLMReport(
                "test-phrase-1", 5, "direct", "test-explanation-1"
        );
        var metaphorReportTwo = new MetaphorLLMReport(
                "test-phrase-2", 15, "direct", "test-explanation-2"
        );
        var metaphorReports = List.of(metaphorReportOne, metaphorReportTwo);
        Mockito.doReturn(metaphorReports).when(metaphorLLMService).analyzeMetaphor(documentChunk);

        Mockito.doReturn(2).when(chunkRepository).countByDocumentId(documentId);
        Mockito.doReturn(2).when(chunkRepository).countSuccessfullyProcessedByDocumentId(documentId);
        Mockito.doReturn(0).when(chunkRepository).countProcessingFailuresByDocumentId(documentId);

        metaphorProcessor.process();

        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(2)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.DONE);

        ArgumentCaptor<IndexedDocumentChunk> chunkArgumentCaptor = ArgumentCaptor.forClass(IndexedDocumentChunk.class);
        Mockito.verify(
                chunkRepository, Mockito.times(2)
        ).save(chunkArgumentCaptor.capture());

        var savedChunk = chunkArgumentCaptor.getValue();
        Assertions.assertThat(savedChunk.getId()).isEqualTo(chunkId);
        Assertions.assertThat(savedChunk.getDocumentId()).isEqualTo(documentId);
        Assertions.assertThat(savedChunk.getStatus()).isEqualTo(DocumentChunkStatus.SUCCESSFULLY_PROCESSED);
        Assertions.assertThat(savedChunk.getAttempts().size()).isEqualTo(1);
        Assertions.assertThat(savedChunk.getAttempts().get(0).error()).isNull();

        var savedMetaphors = savedDocument.getMetaphors()
                .stream()
                .sorted(Comparator.comparing(Metaphor::getOffset))
                .toList();

        Assertions.assertThat(savedMetaphors.size()).isEqualTo(2);
        var firstMetaphor = savedMetaphors.get(0);
        Assertions.assertThat(firstMetaphor.getChunkId()).isEqualTo(chunkId);
        Assertions.assertThat(firstMetaphor.getExplanation()).isEqualTo(metaphorReportOne.explanation());
        Assertions.assertThat(firstMetaphor.getOffset()).isEqualTo(metaphorReportOne.offset());
        Assertions.assertThat(firstMetaphor.getPhrase()).isEqualTo(metaphorReportOne.phrase());
        Assertions.assertThat(firstMetaphor.getType()).isEqualTo(MetaphorType.DIRECT);

        var secondMetaphor = savedMetaphors.get(1);
        Assertions.assertThat(secondMetaphor.getChunkId()).isEqualTo(chunkId);
        Assertions.assertThat(secondMetaphor.getExplanation()).isEqualTo(metaphorReportTwo.explanation());
        Assertions.assertThat(secondMetaphor.getOffset()).isEqualTo(metaphorReportTwo.offset());
        Assertions.assertThat(secondMetaphor.getPhrase()).isEqualTo(metaphorReportTwo.phrase());
        Assertions.assertThat(firstMetaphor.getType()).isEqualTo(MetaphorType.DIRECT);
    }

    @Test
    public void testProcessWhenNoEligibleDocumentChunk() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PENDING)
                .build();
        Mockito.doReturn(Optional.of(document))
                .when(documentRepository)
                .findOldestEligibleDocumentForProcessing();

        Mockito.doReturn(Optional.empty()).when(chunkRepository)
                .findFirstChunkEligibleForProcessing(documentId);

        metaphorProcessor.process();

        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(2)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.INCOMPLETE);
    }

    @Test
    public void testProcessWhenMetaphorProcessingThrowsException() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PENDING)
                .build();
        Mockito.doReturn(Optional.of(document))
                .when(documentRepository)
                .findOldestEligibleDocumentForProcessing();

        var chunkId = UUID.randomUUID().toString();
        var documentChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .build();

        Mockito.doReturn(Optional.of(documentChunk))
                .when(chunkRepository)
                .findFirstChunkEligibleForProcessing(documentId);

        var processingChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .status(DocumentChunkStatus.PROCESSING)
                .build();
        Mockito.doReturn(processingChunk).when(chunkRepository).save(documentChunk);

        var errMessage = "Metaphor reporting failed...";
        Mockito.doThrow(new RuntimeException(errMessage)).when(metaphorLLMService).analyzeMetaphor(documentChunk);

        metaphorProcessor.process();

        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(1)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.PROCESSING);

        ArgumentCaptor<IndexedDocumentChunk> chunkArgumentCaptor = ArgumentCaptor.forClass(IndexedDocumentChunk.class);
        Mockito.verify(
                chunkRepository, Mockito.times(2)
        ).save(chunkArgumentCaptor.capture());

        var savedChunk = chunkArgumentCaptor.getValue();
        Assertions.assertThat(savedChunk.getId()).isEqualTo(chunkId);
        Assertions.assertThat(savedChunk.getDocumentId()).isEqualTo(documentId);
        Assertions.assertThat(savedChunk.getStatus()).isEqualTo(DocumentChunkStatus.NEXT_ATTEMPT_NEEDED);
        Assertions.assertThat(savedChunk.getAttempts().size()).isEqualTo(1);
        Assertions.assertThat(savedChunk.getAttempts().get(0).error()).isEqualTo(errMessage);
    }


    @Test
    public void testProcessWhenMetaphorProcessingThrowsExceptionAllAttemptsExhaustedNotAllChunksProcessed() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PENDING)
                .build();
        Mockito.doReturn(Optional.of(document))
                .when(documentRepository)
                .findOldestEligibleDocumentForProcessing();

        var chunkId = UUID.randomUUID().toString();
        List<ChunkProcessingAttempt> attempts = new ArrayList<>();
        attempts.add(new ChunkProcessingAttempt(Instant.now(), "error-1"));
        attempts.add(new ChunkProcessingAttempt(Instant.now(), "error-2"));

        var documentChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .attempts(attempts)
                .build();

        Mockito.doReturn(Optional.of(documentChunk))
                .when(chunkRepository)
                .findFirstChunkEligibleForProcessing(documentId);

        var processingChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .status(DocumentChunkStatus.PROCESSING)
                .attempts(attempts)
                .build();
        Mockito.doReturn(processingChunk).when(chunkRepository).save(documentChunk);

        var errMessage = "Metaphor reporting failed...";
        Mockito.doThrow(new RuntimeException(errMessage)).when(metaphorLLMService).analyzeMetaphor(documentChunk);

        Mockito.doReturn(3).when(chunkRepository).countByDocumentId(documentId);
        Mockito.doReturn(1).when(chunkRepository).countSuccessfullyProcessedByDocumentId(documentId);
        Mockito.doReturn(1).when(chunkRepository).countProcessingFailuresByDocumentId(documentId);

        metaphorProcessor.process();

        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(1)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.PROCESSING);

        ArgumentCaptor<IndexedDocumentChunk> chunkArgumentCaptor = ArgumentCaptor.forClass(IndexedDocumentChunk.class);
        Mockito.verify(
                chunkRepository, Mockito.times(2)
        ).save(chunkArgumentCaptor.capture());

        var savedChunk = chunkArgumentCaptor.getValue();
        Assertions.assertThat(savedChunk.getId()).isEqualTo(chunkId);
        Assertions.assertThat(savedChunk.getDocumentId()).isEqualTo(documentId);
        Assertions.assertThat(savedChunk.getStatus()).isEqualTo(DocumentChunkStatus.FAILED_TO_PROCESS);
        Assertions.assertThat(savedChunk.getAttempts().size()).isEqualTo(3);
        Assertions.assertThat(savedChunk.getAttempts().get(2).error()).isEqualTo(errMessage);
    }

    /// internal functions testing ///
    @Test
    public void testAnalyzeMetaphorsDocumentInReprocessingState() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.REPROCESSING)
                .build();

        var chunkId = UUID.randomUUID().toString();
        var documentChunk = IndexedDocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .build();
        var metaphorReportOne = new MetaphorLLMReport(
                "test-phrase-1", 5, "direct", "test-explanation-1"
        );
        var metaphorReportTwo = new MetaphorLLMReport(
                "test-phrase-2", 15, "direct", "test-explanation-2"
        );
        var metaphorReports = List.of(metaphorReportOne, metaphorReportTwo);
        Mockito.doReturn(metaphorReports).when(metaphorLLMService).analyzeMetaphorWithAdditionalDirective(documentChunk);

        metaphorProcessor.analyzeMetaphors(document, documentChunk);
        var metaphors = metaphorProcessor.analyzeMetaphors(document, documentChunk)
                .stream()
                .sorted(Comparator.comparing(Metaphor::getOffset))
                .toList();

        Assertions.assertThat(metaphors.size()).isEqualTo(2);
        var firstMetaphor = metaphors.get(0);
        Assertions.assertThat(firstMetaphor.getChunkId()).isEqualTo(chunkId);
        Assertions.assertThat(firstMetaphor.getExplanation()).isEqualTo(metaphorReportOne.explanation());
        Assertions.assertThat(firstMetaphor.getOffset()).isEqualTo(metaphorReportOne.offset());
        Assertions.assertThat(firstMetaphor.getPhrase()).isEqualTo(metaphorReportOne.phrase());
        Assertions.assertThat(firstMetaphor.getType()).isEqualTo(MetaphorType.DIRECT);

        var secondMetaphor = metaphors.get(1);
        Assertions.assertThat(secondMetaphor.getChunkId()).isEqualTo(chunkId);
        Assertions.assertThat(secondMetaphor.getExplanation()).isEqualTo(metaphorReportTwo.explanation());
        Assertions.assertThat(secondMetaphor.getOffset()).isEqualTo(metaphorReportTwo.offset());
        Assertions.assertThat(secondMetaphor.getPhrase()).isEqualTo(metaphorReportTwo.phrase());
        Assertions.assertThat(firstMetaphor.getType()).isEqualTo(MetaphorType.DIRECT);
    }

    @Test
    public void testConvertLLMReportToMetaphorWhenReportIsNull() {
        Assertions.assertThat(metaphorProcessor.convertLLMReportToMetaphor("chunk-id", null)).isNull();
    }

    @ParameterizedTest
    @MethodSource("provideDocumentsWithStatuses")
    public void testUpdateDocumentStatusIfNeeded(IndexedDocument document, DocumentStatus expectedStatus) {
        metaphorProcessor.updateDocumentStatusIfNeeded(document);
        Assertions.assertThat(document.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void testUpdateDocumentIfAllChunksProcessed() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PROCESSING)
                .build();

        var chunkId = UUID.randomUUID().toString();

        Mockito.doReturn(2).when(chunkRepository).countByDocumentId(documentId);
        Mockito.doReturn(2).when(chunkRepository).countSuccessfullyProcessedByDocumentId(documentId);
        Mockito.doReturn(0).when(chunkRepository).countProcessingFailuresByDocumentId(documentId);

        metaphorProcessor.updateDocumentIfAllChunksProcessed(chunkId, documentId, document);
        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(1)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.DONE);
    }

    @Test
    public void testUpdateDocumentIfAllChunksProcessedIncompleteProcessing() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PROCESSING)
                .build();

        var chunkId = UUID.randomUUID().toString();

        Mockito.doReturn(2).when(chunkRepository).countByDocumentId(documentId);
        Mockito.doReturn(1).when(chunkRepository).countSuccessfullyProcessedByDocumentId(documentId);
        Mockito.doReturn(1).when(chunkRepository).countProcessingFailuresByDocumentId(documentId);

        metaphorProcessor.updateDocumentIfAllChunksProcessed(chunkId, documentId, document);
        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(1)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.INCOMPLETE);
    }

    @Test
    public void testUpdateDocumentIfAllChunksProcessedNotAllChunksProcessed() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PROCESSING)
                .build();

        var chunkId = UUID.randomUUID().toString();

        Mockito.doReturn(2).when(chunkRepository).countByDocumentId(documentId);
        Mockito.doReturn(1).when(chunkRepository).countSuccessfullyProcessedByDocumentId(documentId);
        Mockito.doReturn(0).when(chunkRepository).countProcessingFailuresByDocumentId(documentId);

        metaphorProcessor.updateDocumentIfAllChunksProcessed(chunkId, documentId, document);
        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.never()
        ).save(documentArgumentCaptor.capture());
    }

    @Test
    public void testUpdateDocumentIfAllChunksProcessedReprocessingRequestDeletionFailed() {
        var documentId = UUID.randomUUID().toString();
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.REPROCESSING)
                .build();

        var chunkId = UUID.randomUUID().toString();

        Mockito.doReturn(2).when(chunkRepository).countByDocumentId(documentId);
        Mockito.doReturn(2).when(chunkRepository).countSuccessfullyProcessedByDocumentId(documentId);
        Mockito.doReturn(0).when(chunkRepository).countProcessingFailuresByDocumentId(documentId);

        Mockito.doThrow(new RuntimeException("Exception...")).when(documentReprocessingRequestRepository)
                .deleteByDocumentId(documentId);
        metaphorProcessor.updateDocumentIfAllChunksProcessed(chunkId, documentId, document);
        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(IndexedDocument.class);
        Mockito.verify(
                documentRepository, Mockito.times(1)
        ).save(documentArgumentCaptor.capture());

        var savedDocument = documentArgumentCaptor.getValue();
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.DONE);
    }

    @Test
    public void testTryRemoveReprocessingRequestWhenExceptionIsThrown() {
        var documentId = "test-doc-id";
        Mockito.doThrow(new RuntimeException("Exception"))
                .when(documentReprocessingRequestRepository)
                .deleteByDocumentId(documentId);
        metaphorProcessor.tryRemoveReprocessingRequest(documentId);
    }

    private static Stream<Arguments> provideDocumentsWithStatuses() {
        return Stream.of(
                Arguments.of(
                        IndexedDocument.builder()
                                .id("document-id-1")
                                .status(DocumentStatus.PENDING)
                                .build(),
                        DocumentStatus.PROCESSING),
                Arguments.of(IndexedDocument.builder()
                                .id("document-id-2")
                                .status(DocumentStatus.PENDING_REPROCESSING)
                                .build(),
                        DocumentStatus.REPROCESSING),
                Arguments.of(IndexedDocument.builder()
                                .id("document-id-3")
                                .status(DocumentStatus.DONE)
                                .build(),
                        DocumentStatus.DONE)
        );
    }
}