package ai.metaphor.metaphor_llm_processor;

import ai.metaphor.metaphor_llm_processor.model.*;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;
import java.util.Set;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MetaphorLlmProcessorApplication {

    private final IndexedDocumentRepository indexedDocumentRepository;

    public MetaphorLlmProcessorApplication(IndexedDocumentRepository indexedDocumentRepository) {
        this.indexedDocumentRepository = indexedDocumentRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(MetaphorLlmProcessorApplication.class, args);
    }

    @Bean
    CommandLineRunner runner() {
        return args -> {
            IndexedDocument document = IndexedDocument.builder()
                    .name("test")
                    .text("test 232 np1")
                    .type(OriginType.URL)
                    .metaphors(
                            Set.of(
                                    new Metaphor("123", 4, "phrase", MetaphorType.INDIRECT, "no explanation", Instant.now(), Instant.now())
                            )
                    )
                    .path("test path")
                    .status(DocumentStatus.PENDING)
                    .origin("test")
                    .build();

            indexedDocumentRepository.save(document);
        };
    }

}
