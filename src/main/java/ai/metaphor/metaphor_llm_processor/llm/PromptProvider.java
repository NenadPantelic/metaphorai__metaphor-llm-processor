package ai.metaphor.metaphor_llm_processor.llm;

import ai.metaphor.metaphor_llm_processor.exception.PromptException;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import ai.metaphor.metaphor_llm_processor.repository.DocumentReprocessingRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class PromptProvider {

    private static final String KEY_TEXT = "text";
    private static final String KEY_DIRECTIVE = "directive";

    private final PromptTemplate promptTemplate;
    private final PromptTemplate promptTemplateWithDirective;

    private final DocumentReprocessingRequestRepository documentReprocessingRequestRepository;

    public PromptProvider(@Qualifier("promptTemplate") PromptTemplate promptTemplate,
                          @Qualifier("promptTemplateWithDirective") PromptTemplate promptTemplateWithDirective,
                          DocumentReprocessingRequestRepository documentReprocessingRequestRepository) {
        this.promptTemplate = promptTemplate;
        this.promptTemplateWithDirective = promptTemplateWithDirective;
        this.documentReprocessingRequestRepository = documentReprocessingRequestRepository;
    }

    public String getPrompt(IndexedDocumentChunk chunk) {
        var templateMap = new HashMap<String, Object>();
        templateMap.put(KEY_TEXT, chunk.getText());
        return promptTemplate.render(templateMap);
    }

    public String getPromptWithDirective(IndexedDocumentChunk chunk) {
        var documentId = chunk.getDocumentId();
        var directive = getDirectiveFromReprocessingRequest(documentId);
        var templateMap = new HashMap<String, Object>();
        templateMap.put(KEY_TEXT, chunk.getText());
        templateMap.put(KEY_DIRECTIVE, directive);
        return promptTemplateWithDirective.render(templateMap);
    }

    private String getDirectiveFromReprocessingRequest(String documentId) {
        var reprocessingRequestOptional = documentReprocessingRequestRepository.findByDocumentId(documentId);
        if (reprocessingRequestOptional.isEmpty()) {
            log.error("Document reprocessing request for documentId '{}' not found. Unable to get the directive.", documentId);
            throw new PromptException(
                    String.format(
                            "Document reprocessing request for documentId '%s' not found. Unable to get the directive.",
                            documentId
                    )
            );
        }

        return reprocessingRequestOptional.get().getDirective();
    }
}
