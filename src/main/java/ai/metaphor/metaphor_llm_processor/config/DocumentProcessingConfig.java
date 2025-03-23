package ai.metaphor.metaphor_llm_processor.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class DocumentProcessingConfig {

    private static final String PROMPT_TEMPLATE_PATH = "prompt-templates/metaphor-prompt-template.st";

    @Bean
    TextSplitter textSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    PromptTemplate promptTemplate() {
        var promptTemplateResource = new ClassPathResource(PROMPT_TEMPLATE_PATH);
        return new PromptTemplate(promptTemplateResource);
    }
}
