package ai.metaphor.metaphor_llm_processor.service;

import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorReprocessingRequest;

public interface MetaphorReprocessingService {

    void handleReprocessingRequest(MetaphorReprocessingRequest metaphorReprocessingRequest);
}
