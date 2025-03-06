package ai.metaphor.metaphor_llm_processor.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Builder
@Data
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@NoArgsConstructor
// to avoid the name confusion with the Spring AI org.springframework.ai.document.Document
public class IndexedDocument {

    @Id
    private String id;
    @NotBlank
    private String name;
    @NotBlank
    private String text;
    @NotBlank
    private String origin;
    @NotBlank
    private String path;
    @NotBlank
    private OriginType type;
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;
    @Builder.Default
    private Set<Metaphor> metaphors = new HashSet<>();
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
