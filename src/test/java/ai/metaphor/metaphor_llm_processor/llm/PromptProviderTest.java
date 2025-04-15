package ai.metaphor.metaphor_llm_processor.llm;

import ai.metaphor.metaphor_llm_processor.exception.PromptException;
import ai.metaphor.metaphor_llm_processor.model.DocumentChunkStatus;
import ai.metaphor.metaphor_llm_processor.model.DocumentReprocessingRequest;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import ai.metaphor.metaphor_llm_processor.repository.DocumentReprocessingRequestRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PromptProviderTest {

    private static final String PROMPT_TEMPLATE = "Metaphor processing test example begin:\n" +
            "{text}\n" +
            "Metaphor processing test example end.";
    private static final String PROMPT_TEMPLATE_WITH_DIRECTIVE = "Metaphor reprocessing test example begin:\n" +
            "{text}\n" +
            "Now comes the directive. \n" +
            "{directive} \n" +
            "Metaphor reprocessing test example end.";

    private static final String TEXT_PLACEHOLDER = "{text}";
    private static final String DIRECTIVE_PLACEHOLDER = "{directive}";

    private final PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
    private final PromptTemplate promptTemplateWithDirective = new PromptTemplate(PROMPT_TEMPLATE_WITH_DIRECTIVE);

    private final DocumentReprocessingRequestRepository reprocessingRequestRepository = Mockito.mock(
            DocumentReprocessingRequestRepository.class
    );

    private final PromptProvider promptProvider = new PromptProvider(
            promptTemplate, promptTemplateWithDirective, reprocessingRequestRepository
    );

    @Test
    public void givenDocumentChunkWhenGetPromptShouldReturnRenderedTemplate() {
        String text = "Test text";
        IndexedDocumentChunk documentChunk = IndexedDocumentChunk.builder()
                .id("test-chunk-id")
                .documentId("test-document-id")
                .status(DocumentChunkStatus.PENDING)
                .text(text)
                .build();

        var expectedPrompt = PROMPT_TEMPLATE.replace(TEXT_PLACEHOLDER, text);
        var actualPrompt = promptProvider.getPrompt(documentChunk);

        Assertions.assertThat(actualPrompt).isEqualTo(expectedPrompt);
    }

    @Test
    public void givenDocumentChunkWhenGetPromptWithDirectiveShouldReturnRenderedTemplate() {
        String text = "Test text";
        String documentId = "test-document-id";
        IndexedDocumentChunk documentChunk = IndexedDocumentChunk.builder()
                .id("test-chunk-id")
                .documentId(documentId)
                .status(DocumentChunkStatus.PENDING)
                .text(text)
                .build();

        var directive = "Test directive";
        DocumentReprocessingRequest reprocessingRequest = DocumentReprocessingRequest.builder()
                .documentId(documentId)
                .directive(directive)
                .directive(directive)
                .build();
        Mockito.doReturn(Optional.of(reprocessingRequest))
                .when(reprocessingRequestRepository).findByDocumentId(documentId);

        var expectedPrompt = PROMPT_TEMPLATE_WITH_DIRECTIVE.replace(TEXT_PLACEHOLDER, text)
                .replace(DIRECTIVE_PLACEHOLDER, directive);
        var actualPrompt = promptProvider.getPromptWithDirective(documentChunk);

        Assertions.assertThat(actualPrompt).isEqualTo(expectedPrompt);
    }

    @Test
    // naming :-(
    public void givenDocumentChunkWhenGetPromptWithDirectiveAndReprocessingRequestDoesNotExistShouldThrowException() {
        String text = "Test text";
        String documentId = "test-document-id";
        IndexedDocumentChunk documentChunk = IndexedDocumentChunk.builder()
                .id("test-chunk-id")
                .documentId(documentId)
                .status(DocumentChunkStatus.PENDING)
                .text(text)
                .build();

        Mockito.doReturn(Optional.empty())
                .when(reprocessingRequestRepository).findByDocumentId(documentId);

        try {
            promptProvider.getPromptWithDirective(documentChunk);
            Assertions.fail("Should have failed, but didn't");
        } catch (Exception e) {
            Assertions.assertThat(e).isInstanceOf(PromptException.class);
        }
    }
}