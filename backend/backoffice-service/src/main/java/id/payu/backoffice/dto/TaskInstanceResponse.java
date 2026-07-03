package id.payu.backoffice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Kogito user task instance")
public record TaskInstanceResponse(
        @Schema(description = "Task instance ID", example = "bcbe9d60-4847-45f0-8069-e983f3f055e6")
        String id,

        @Schema(description = "User task definition ID", example = "UserTask_1")
        String userTaskId,

        @Schema(description = "Human-readable task name", example = "First Line Approval")
        String taskName,

        @Schema(description = "Task description")
        String description,

        @Schema(description = "Current task status (Reserved, Ready, Completed)", example = "Reserved")
        String status,

        @Schema(description = "Process instance ID")
        String processInstanceId,

        @Schema(description = "Process ID", example = "loan-origination")
        String processId,

        @Schema(description = "Task input variables displayed to user")
        Map<String, Object> inputs,

        @Schema(description = "Allowed transition IDs", example = "[\"claim\", \"complete\"]")
        java.util.List<String> allowedTransitions
) {}
