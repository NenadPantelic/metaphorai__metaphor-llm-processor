package ai.metaphor.metaphor_llm_processor.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@Document(collection = "document_chunks")
@NoArgsConstructor
public class IndexedDocumentChunk {

    @Id
    private String id;
    @NotBlank
    private String documentId;
    @NotBlank
    private String text;
    @Builder.Default
    private DocumentChunkStatus status = DocumentChunkStatus.PENDING;
    private List<ChunkIndexingAttempt> attempts;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    private Instant lastProcessingAttemptedAt;
}
