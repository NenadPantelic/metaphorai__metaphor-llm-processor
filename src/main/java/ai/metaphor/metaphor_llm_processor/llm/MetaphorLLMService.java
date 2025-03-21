package ai.metaphor.metaphor_llm_processor.llm;

import ai.metaphor.metaphor_llm_processor.configproperties.MetaphorPromptConfigProperties;
import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorLLMReport;
import ai.metaphor.metaphor_llm_processor.exception.LLMException;
import ai.metaphor.metaphor_llm_processor.llm.client.LLMClient;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class MetaphorLLMService {

    static final String KEY_QUESTION = "question";

    private final PromptTemplate promptTemplate;
    private final LLMClient llmClient;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;

    protected MetaphorLLMService(PromptTemplate promptTemplate,
                                 LLMClient llmClient,
                                 MetaphorPromptConfigProperties metaphorPromptConfigProperties,
                                 ObjectMapper objectMapper) {
        this.promptTemplate = promptTemplate;
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
    public MetaphorLLMReport analyzeMetaphor(IndexedDocumentChunk documentChunk) {
        log.info("Analyze metaphor in chunk: {}", documentChunk);
        String prompt = preparePromptTemplate(documentChunk.getText());
        String response = llmClient.generate(systemPrompt, prompt);
        return deserialize(response);
    }

    private String preparePromptTemplate(String originalUserPrompt) {
        var templateMap = new HashMap<String, Object>();
        templateMap.put(KEY_QUESTION, originalUserPrompt);
        return promptTemplate.render(templateMap);
    }

    private MetaphorLLMReport deserialize(String content) {
        try {
            return objectMapper.readValue(content, MetaphorLLMReport.class);
        } catch (JsonProcessingException e) {
            throw new LLMException(
                    String.format("Unable to deserialize the content returned by LLM to MetaphorLLMReport type. " +
                            "Original content: %s", content), e
            );
        }
    }

}
