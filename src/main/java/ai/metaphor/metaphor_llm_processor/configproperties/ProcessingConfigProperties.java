package ai.metaphor.metaphor_llm_processor.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "processing")
public record ProcessingConfigProperties(int maxRetry, int intervalInMillis) {

}
