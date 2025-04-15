package ai.metaphor.metaphor_llm_processor.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("prompt")
public record MetaphorPromptConfigProperties(String systemPrompt) {
}
