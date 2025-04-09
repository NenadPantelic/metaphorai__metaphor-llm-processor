package ai.metaphor.metaphor_llm_processor.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
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
    @NotNull
    @Min(1)
    @Builder.Default
    private int order = 1;
    @Builder.Default
    private List<ChunkProcessingAttempt> attempts = new ArrayList<>();
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    private Instant lastProcessingAttemptedAt;

    public void addAttempt(ChunkProcessingAttempt chunkProcessingAttempt) {
        this.attempts.add(chunkProcessingAttempt);
    }
}
