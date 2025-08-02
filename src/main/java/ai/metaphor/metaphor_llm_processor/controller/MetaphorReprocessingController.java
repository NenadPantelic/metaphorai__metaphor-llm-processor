package ai.metaphor.metaphor_llm_processor.controller;

import ai.metaphor.metaphor_llm_processor.dto.metaphor.MetaphorReprocessingRequest;
import ai.metaphor.metaphor_llm_processor.service.MetaphorReprocessingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/metaphor-reprocessing")
public class MetaphorReprocessingController {

    private final MetaphorReprocessingService metaphorReprocessingService;

    public MetaphorReprocessingController(MetaphorReprocessingService metaphorReprocessingService) {
        this.metaphorReprocessingService = metaphorReprocessingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void handleReprocessingRequest(@Valid @RequestBody MetaphorReprocessingRequest metaphorReprocessingRequest) {
        log.info("Received a request to handle a reprocessing request...");
        metaphorReprocessingService.handleReprocessingRequest(metaphorReprocessingRequest);
    }
}
