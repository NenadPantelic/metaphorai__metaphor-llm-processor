package ai.metaphor.metaphor_llm_processor.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("metaphor")
public class MetaphorPromptConfigProperties {

    public String systemPrompt;

    public MetaphorPromptConfigProperties(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
