package id.payu.backoffice.adapter.web;

import id.payu.backoffice.interfaces.dto.TaskInstanceResponse;
import id.payu.backoffice.interfaces.dto.TaskTransitionRequest;
import id.payu.shared.restclient.PayuRestClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backoffice/tasks")
@Tag(name = "Task Inbox", description = "Human task inbox for loan approval workflow (Kogito proxy)")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TaskInboxController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(TaskInboxController.class);

    private final PayuRestClient restClient;

    @Value("${payu.kogito.task-api.url:http://loan-origination-process:8080}")
    private String kogitoTaskApiUrl;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'LOAN_OFFICER', 'RISK_MANAGER')")
    @Operation(
            summary = "Get pending tasks for current user",
            description = "Proxies Kogito user task query to retrieve tasks awaiting action for the authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pending tasks"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.interfaces.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "502", description = "Kogito service unavailable",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.interfaces.dto.ApiResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.interfaces.dto.ApiResponse<List<Map<String, Object>>>> getPendingTasks(
            @Parameter(description = "Filter by user (defaults to authenticated user)", example = "john")
            @RequestParam(required = false) String user) {

        String resolvedUser = resolveTaskUser(user);
        log.info("Fetching pending tasks for user: {}", resolvedUser);

        try {
            ParameterizedTypeReference<List<Map<String, Object>>> typeRef =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<List<Map<String, Object>>> response = restClient.get(
                    "kogito-task-api",
                    kogitoTaskApiUrl + "/usertasks/instance?user=" + resolvedUser,
                    typeRef);

            return ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch tasks from Kogito: {}", e.getMessage(), e);
            return ResponseEntity.status(502)
                    .body(id.payu.backoffice.interfaces.dto.ApiResponse.error("KOGITO_UNAVAILABLE", "Kogito task API unavailable: " + e.getMessage()));
        }
    }

    @PostMapping("/{taskId}/transition")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'LOAN_OFFICER', 'RISK_MANAGER')")
    @Operation(
            summary = "Transition a task (claim, complete, release, skip)",
            description = "Executes a lifecycle transition on a Kogito user task"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task transitioned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transition",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.interfaces.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.interfaces.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "502", description = "Kogito service unavailable",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.interfaces.dto.ApiResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.interfaces.dto.ApiResponse<Map<String, Object>>> transitionTask(
            @PathVariable String taskId,
            @Valid @RequestBody TaskTransitionRequest request,
            @Parameter(description = "User performing the transition (defaults to authenticated user)", example = "john")
            @RequestParam(required = false) String user) {

        String resolvedUser = resolveTaskUser(user);
        log.info("Transitioning task {} to '{}' for user: {}", taskId, request.transitionId(), resolvedUser);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restClient.post(
                    "kogito-task-api",
                    kogitoTaskApiUrl + "/usertasks/instance/" + taskId + "/transition?user=" + resolvedUser,
                    request.outputData() != null
                            ? Map.of("transitionId", request.transitionId(), "outputData", request.outputData())
                            : Map.of("transitionId", request.transitionId()),
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            return ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to transition task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(502)
                    .body(id.payu.backoffice.interfaces.dto.ApiResponse.error("KOGITO_UNAVAILABLE", "Kogito task API unavailable: " + e.getMessage()));
        }
    }

    private String resolveTaskUser(String userParam) {
        if (userParam != null && !userParam.isEmpty()) {
            return userParam;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "unknown";
    }
}
