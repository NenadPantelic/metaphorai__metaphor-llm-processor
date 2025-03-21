package ai.metaphor.metaphor_llm_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class MetaphorLlmProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetaphorLlmProcessorApplication.class, args);
    }

}
