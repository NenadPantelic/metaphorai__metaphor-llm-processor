package ai.metaphor.metaphor_llm_processor.llm;

import ai.metaphor.metaphor_llm_processor.configproperties.MetaphorPromptConfigProperties;
import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorLLMReport;
import ai.metaphor.metaphor_llm_processor.llm.client.LLMClient;
import ai.metaphor.metaphor_llm_processor.model.DocumentChunkStatus;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class MetaphorLLMServiceTest {

    private final PromptTemplate promptTemplate = Mockito.mock(PromptTemplate.class);
    private final LLMClient llmClient = Mockito.mock(LLMClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MetaphorPromptConfigProperties metaphorPromptConfigProperties = new MetaphorPromptConfigProperties(
            "You are a linguistic expert for metaphor"
    );


    private final MetaphorLLMService metaphorLLMService = new MetaphorLLMService(
            promptTemplate, llmClient, metaphorPromptConfigProperties, objectMapper
    );


    @Test
    public void givenIndexedDocumentChunkWhenAnalyzeMetaphorShouldReturnReport() {
        String text = "Test text";
        IndexedDocumentChunk documentChunk = IndexedDocumentChunk.builder()
                .id("test-chunk-id")
                .documentId("test-document-id")
                .status(DocumentChunkStatus.PENDING)
                .text(text)
                .build();

        String phrase = "The classroom was a zoo.";
        int offset = 0;
        String explanation = "This is a metaphor because it compares the classroom to a zoo without using 'like' or " +
                "'as' (which would make it a simile). The metaphor suggests that the classroom was chaotic, noisy, " +
                "or disorganized, much like a zoo filled with animals. It conveys an image of wild activity and disorder in" +
                "the classroom, though it's not literally a zoo.";
        String expectedMetaphorReportSerialized = String.format("""
                {
                "phrase":"%s",\r
                "offset":%d,\r
                "explanation":"%s"
                }
                """, phrase, offset, explanation);

        String prompt = "Test prompt";
        Mockito.doReturn(prompt)
                .when(promptTemplate)
                .render(Map.of(MetaphorLLMService.KEY_QUESTION, text));

        Mockito.doReturn(expectedMetaphorReportSerialized)
                .when(llmClient)
                .generate(metaphorPromptConfigProperties.systemPrompt(), prompt);

        MetaphorLLMReport metaphorLLMReport = metaphorLLMService.analyzeMetaphor(documentChunk);

        Assertions.assertThat(metaphorLLMReport.phrase()).isEqualTo(phrase);
        Assertions.assertThat(metaphorLLMReport.offset()).isEqualTo(offset);
        Assertions.assertThat(metaphorLLMReport.explanation()).isEqualTo(explanation);
    }
}