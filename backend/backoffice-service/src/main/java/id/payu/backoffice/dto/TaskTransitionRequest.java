package id.payu.backoffice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "Kogito task transition request")
public record TaskTransitionRequest(
        @NotBlank(message = "Transition ID is required")
        @Schema(description = "Transition to execute (claim, release, complete, skip)", example = "complete")
        String transitionId,

        @Schema(description = "Task output data passed on completion", example = "{\"approved\": true, \"comment\": \"Looks good\"}")
        Map<String, Object> outputData
) {}
