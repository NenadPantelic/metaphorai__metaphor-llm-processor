package ai.metaphor.metaphor_llm_processor.configproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "indexing")
public record IndexingConfigProperties(int maxRetry, int retryIntervalInMillis) {

}
