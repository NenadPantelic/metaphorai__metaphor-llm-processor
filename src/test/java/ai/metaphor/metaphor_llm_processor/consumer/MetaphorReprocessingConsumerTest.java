package ai.metaphor.metaphor_llm_processor.consumer;

import ai.metaphor.metaphor_llm_processor.model.*;
import ai.metaphor.metaphor_llm_processor.repository.DocumentReprocessingRequestRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class MetaphorReprocessingConsumerTest {


    private final IndexedDocumentRepository documentRepository = Mockito.mock(IndexedDocumentRepository.class);
    private final IndexedDocumentChunkRepository chunkRepository = Mockito.mock(IndexedDocumentChunkRepository.class);
    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository = Mockito.mock(
            DocumentReprocessingRequestRepository.class
    );

    private final MetaphorReprocessingConsumer metaphorReprocessingConsumer = new MetaphorReprocessingConsumer(
            documentRepository, chunkRepository, documentReprocessingRequestRepository
    );

    @Test
    public void givenNonExistentDocumentWhenConsumeShouldDoNothing() {
        var documentId = "test-document-id";
        Mockito.doReturn(Optional.empty()).when(documentRepository).findById(documentId);

        ArgumentCaptor<DocumentReprocessingRequest> argumentCaptor = ArgumentCaptor.forClass(
                DocumentReprocessingRequest.class
        );
        metaphorReprocessingConsumer.consume(documentId, null);

        Mockito.verify(documentReprocessingRequestRepository, Mockito.never())
                .save(argumentCaptor.capture());
    }

    @Test
    public void givenDocumentInProcessingStatusWhenConsumeShouldDoNothing() {
        var documentId = "test-document-id";
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.PROCESSING)
                .build();
        Mockito.doReturn(Optional.of(document)).when(documentRepository).findById(documentId);

        ArgumentCaptor<DocumentReprocessingRequest> argumentCaptor = ArgumentCaptor.forClass(
                DocumentReprocessingRequest.class
        );
        metaphorReprocessingConsumer.consume(documentId, null);

        Mockito.verify(documentReprocessingRequestRepository, Mockito.never())
                .save(argumentCaptor.capture());
    }

    @Test
    public void givenDocumentWhenCreatingReprocessingRequestThrowsExceptionShouldDoNothing() {
        var documentId = "test-document-id";
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.DONE)
                .build();

        Mockito.doReturn(Optional.of(document)).when(documentRepository).findById(documentId);

        ArgumentCaptor<DocumentReprocessingRequest> reprocessingRequestArgumentCaptor = ArgumentCaptor.forClass(
                DocumentReprocessingRequest.class
        );
        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(
                IndexedDocument.class
        );
        ArgumentCaptor<List<IndexedDocumentChunk>> documentChunkArgumentCaptor = ArgumentCaptor.forClass(
                (Class) List.class
        );

        var reasons = List.of("test reason 1", "test reason 2");
        var directive = String.format("%s\n%s", reasons.get(0), reasons.get(1));
        var expectedReprocessingRequest = DocumentReprocessingRequest.builder()
                .documentId(documentId)
                .reasons(reasons)
                .directive(directive)
                .build();
        Mockito.doThrow(new RuntimeException("Unable to create a document reprocessing request"))
                .when(documentReprocessingRequestRepository)
                .save(expectedReprocessingRequest);

        metaphorReprocessingConsumer.consume(documentId, reasons);

        Mockito.verify(documentReprocessingRequestRepository, Mockito.times(1))
                .save(reprocessingRequestArgumentCaptor.capture());
        Mockito.verify(documentRepository, Mockito.never())
                .save(documentArgumentCaptor.capture());
        Mockito.verify(chunkRepository, Mockito.never())
                .saveAll(documentChunkArgumentCaptor.capture());
    }

    @Test
    public void givenDocumentWhenConsumeShouldCreateReprocessingRequest() {
        var documentId = "test-document-id";
        var chunkIdOne = "test-document-chunk-id-1";
        var chunkIdTwo = "test-document-chunk-id-2";

        var now = Instant.now();
        var metaphorOne = new Metaphor(
                chunkIdOne, 3, "test-phrase-1", MetaphorType.DIRECT, "test-explanation-1", now, now
        );
        var metaphorTwo = new Metaphor(
                chunkIdTwo, 5, "test-phrase-2", MetaphorType.INDIRECT, "test-explanation-2", now, now
        );
        Set<Metaphor> metaphors = new HashSet<>();
        metaphors.add(metaphorOne);
        metaphors.add(metaphorTwo);
        var document = IndexedDocument.builder()
                .id(documentId)
                .status(DocumentStatus.DONE)
                .metaphors(metaphors)
                .build();

        Mockito.doReturn(Optional.of(document)).when(documentRepository).findById(documentId);

        ArgumentCaptor<DocumentReprocessingRequest> reprocessingRequestArgumentCaptor = ArgumentCaptor.forClass(
                DocumentReprocessingRequest.class
        );
        ArgumentCaptor<IndexedDocument> documentArgumentCaptor = ArgumentCaptor.forClass(
                IndexedDocument.class
        );
        ArgumentCaptor<List<IndexedDocumentChunk>> documentChunkArgumentCaptor = ArgumentCaptor.forClass(
                (Class) List.class
        );

        var reasons = List.of("test reason 1", "test reason 2");
        var directive = String.format("%s\n%s", reasons.get(0), reasons.get(1));
        var expectedReprocessingRequest = DocumentReprocessingRequest.builder()
                .documentId(documentId)
                .reasons(reasons)
                .directive(directive)
                .build();
        Mockito.doReturn(expectedReprocessingRequest).when(documentReprocessingRequestRepository)
                .save(expectedReprocessingRequest);

        List<ChunkProcessingAttempt> attemptsOne = new ArrayList<>();
        attemptsOne.add(new ChunkProcessingAttempt(now, null));

        var chunkOne = IndexedDocumentChunk.builder()
                .id(chunkIdOne)
                .documentId(documentId)
                .text("test-chunk-text-1")
                .status(DocumentChunkStatus.SUCCESSFULLY_PROCESSED)
                .attempts(attemptsOne)
                .build();

        List<ChunkProcessingAttempt> attemptsTwo = new ArrayList<>();
        attemptsTwo.add(new ChunkProcessingAttempt(now, null));

        var chunkTwo = IndexedDocumentChunk.builder()
                .id(chunkIdTwo)
                .documentId(documentId)
                .text("test-chunk-text-2")
                .status(DocumentChunkStatus.FAILED_TO_PROCESS)
                .attempts(attemptsTwo)
                .build();
        var chunks = List.of(chunkOne, chunkTwo);
        Mockito.doReturn(chunks).when(chunkRepository).findByDocumentId(documentId);

        metaphorReprocessingConsumer.consume(documentId, reasons);

        Mockito.verify(documentReprocessingRequestRepository, Mockito.times(1))
                .save(reprocessingRequestArgumentCaptor.capture());
        Mockito.verify(documentRepository, Mockito.times(1))
                .save(documentArgumentCaptor.capture());
        Mockito.verify(chunkRepository, Mockito.times(1))
                .saveAll(documentChunkArgumentCaptor.capture());

        var reprocessingRequest = reprocessingRequestArgumentCaptor.getValue();
        var savedDocument = documentArgumentCaptor.getValue();
        var savedChunks = documentChunkArgumentCaptor.getValue();

        // assert DocumentReprocessingRequest
        Assertions.assertThat(reprocessingRequest).isEqualTo(expectedReprocessingRequest);

        // assert document
        Assertions.assertThat(savedDocument.getId()).isEqualTo(documentId);
        Assertions.assertThat(savedDocument.getStatus()).isEqualTo(DocumentStatus.PENDING_REPROCESSING);
        Assertions.assertThat(savedDocument.getMetaphors()).isEmpty();

        // assert chunks
        var savedChunkOne = savedChunks.get(0);
        Assertions.assertThat(savedChunkOne.getId()).isEqualTo(chunkIdOne);
        Assertions.assertThat(savedChunkOne.getAttempts()).isEmpty();
        Assertions.assertThat(savedChunkOne.getStatus()).isEqualTo(DocumentChunkStatus.PENDING_REPROCESSING);

        var savedChunkTwo = savedChunks.get(1);
        Assertions.assertThat(savedChunkTwo.getId()).isEqualTo(chunkIdTwo);
        Assertions.assertThat(savedChunkTwo.getAttempts()).isEmpty();
        Assertions.assertThat(savedChunkTwo.getStatus()).isEqualTo(DocumentChunkStatus.PENDING_REPROCESSING);
    }
}