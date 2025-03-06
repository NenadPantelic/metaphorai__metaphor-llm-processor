package ai.metaphor.metaphor_llm_processor.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public record Metaphor(@EqualsAndHashCode.Include @NotBlank String chunkId,
                       @EqualsAndHashCode.Include @Min(0) int offset,
                       // metaphor phrase
                       @NotBlank String phrase,
                       String explanation,
                       @CreatedDate
                       Instant createdAt,
                       @LastModifiedDate
                       Instant updatedAt) {
}
