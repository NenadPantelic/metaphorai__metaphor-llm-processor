package ai.metaphor.metaphor_llm_processor.dto.metaphor;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MetaphorReprocessingRequest(@NotBlank String documentId, List<String> reasons) {
}
