package ai.metaphor.metaphor_llm_processor.llm;

import ai.metaphor.metaphor_llm_processor.configproperties.MetaphorPromptConfigProperties;
import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorLLMReport;
import ai.metaphor.metaphor_llm_processor.exception.LLMException;
import ai.metaphor.metaphor_llm_processor.llm.client.LLMClient;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MetaphorLLMService {


    private static final TypeReference<List<MetaphorLLMReport>> METAPHOR_LLM_REPORT_LIST = new TypeReference<>() {
    };

    private final PromptProvider promptProvider;
    private final LLMClient llmClient;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;

    protected MetaphorLLMService(PromptProvider promptProvider,
                                 LLMClient llmClient,
                                 MetaphorPromptConfigProperties metaphorPromptConfigProperties,
                                 ObjectMapper objectMapper) {
        this.promptProvider = promptProvider;
        this.llmClient = llmClient;
        this.systemPrompt = metaphorPromptConfigProperties.systemPrompt();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a prompt to LLM with an intention to analyze text and find metaphor phrases in it.
     *
     * @param documentChunk an indexed documentChunk being analyzed
     * @return {@link MetaphorLLMReport}
     */
    public List<MetaphorLLMReport> analyzeMetaphor(IndexedDocumentChunk documentChunk) {
        log.info("Analyze metaphor in chunk: {}", documentChunk);
        String prompt = promptProvider.getPrompt(documentChunk);
        String response = llmClient.generate(systemPrompt, prompt);
        log.info("LLM response: {}", response);
        return deserialize(response);
    }

    /**
     * Sends a prompt to LLM with an intention to analyze text and find metaphor phrases in it.
     *
     * @param documentChunk an indexed documentChunk being analyzed
     * @return {@link MetaphorLLMReport}
     */
    public List<MetaphorLLMReport> analyzeMetaphorWithAdditionalDirective(IndexedDocumentChunk documentChunk) {
        log.info("Analyze metaphor in chunk: {}", documentChunk);
        String prompt = promptProvider.getPromptWithDirective(documentChunk);
        String response = llmClient.generate(systemPrompt, prompt);
        log.info("LLM response: {}", response);
        return deserialize(response);
    }

    private List<MetaphorLLMReport> deserialize(String content) {
        try {
            return objectMapper.readValue(content, METAPHOR_LLM_REPORT_LIST);
        } catch (JsonProcessingException e) {
            throw new LLMException(
                    String.format("Unable to deserialize the content returned by LLM to MetaphorLLMReport type. " +
                            "Original content: %s", content), e
            );
        }
    }

}
