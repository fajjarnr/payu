package id.payu.support.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.support.application.service.FaqService;
import id.payu.support.application.service.SupportTicketService;
import id.payu.support.interfaces.dto.*;
import id.payu.support.application.service.AgentService;
import id.payu.support.application.service.AgentTrainingService;
import id.payu.support.application.service.TrainingModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import id.payu.support.domain.TrainingStatus;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
@Tag(name = "Support Management", description = "API for managing support team training")
public class SupportController extends BaseController {

    private final AgentService agentService;
    private final TrainingModuleService trainingModuleService;
    private final AgentTrainingService agentTrainingService;
    private final SupportTicketService ticketService;
    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "Support service status", description = "Returns support service health and available endpoints")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupportStatus() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "service", "support-service",
                "status", "UP",
                "version", "1.0.0"
        )));
    }

    @GetMapping("/training-status")
    @Operation(summary = "Get overall training status", description = "Retrieve training statistics including active and trained agents")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training status retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrainingStatusResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrainingStatus() {
        long activeAgents = agentService.countActiveAgents();
        long trainedAgents = agentTrainingService.countFullyTrainedAgents();

        return ok(Map.of(
            "activeAgents", activeAgents,
            "trainedAgents", trainedAgents,
            "trainingPercentage", activeAgents > 0 ? (trainedAgents * 100.0 / activeAgents) : 0.0
        ));
    }

    @GetMapping("/agents")
    @Operation(summary = "Get all support agents", description = "Retrieve list of all support agents")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agents retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<AgentResponse>>> getAllAgents() {
        return ok(agentService.getAllAgents());
    }

    @PostMapping("/agents")
    @PreAuthorize("hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Create a new support agent", description = "Register a new support agent in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Agent created successfully",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AgentResponse>> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        AgentResponse agent = agentService.createAgent(request);
        URI location = URI.create("/api/v1/support/agents/" + agent.id());
        return created(agent, location.toString());
    }

    @GetMapping("/agents/{id}")
    @Operation(summary = "Get agent by ID", description = "Retrieve a specific support agent by their ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent found",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AgentResponse>> getAgentById(
            @Parameter(description = "Agent ID", required = true) @PathVariable Long id) {
        AgentResponse agent = agentService.getAgentById(id);
        if (agent == null) {
            return notFound("AGENT_404", "Agent not found");
        }
        return ok(agent);
    }

    @GetMapping("/agents/employee/{employeeId}")
    @Operation(summary = "Get agent by employee ID", description = "Retrieve a support agent by their employee ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent found",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AgentResponse>> getAgentByEmployeeId(
            @Parameter(description = "Employee ID", required = true) @PathVariable String employeeId) {
        AgentResponse agent = agentService.getAgentByEmployeeId(employeeId);
        if (agent == null) {
            return notFound("AGENT_404", "Agent not found");
        }
        return ok(agent);
    }

    @PatchMapping("/agents/{id}/status")
    @PreAuthorize("hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Update agent active status", description = "Activate or deactivate a support agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent status updated successfully",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AgentResponse>> updateAgentStatus(
            @Parameter(description = "Agent ID", required = true) @PathVariable Long id,
            @RequestBody Map<String, Boolean> status) {
        AgentResponse agent = agentService.updateAgentStatus(id, status.get("active"));
        if (agent == null) {
            return notFound("AGENT_404", "Agent not found");
        }
        return ok(agent);
    }

    @GetMapping("/modules")
    @Operation(summary = "Get all training modules", description = "Retrieve list of all training modules")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training modules retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<TrainingModuleResponse>>> getAllModules() {
        return ok(trainingModuleService.getAllTrainingModules());
    }

    @GetMapping("/modules/mandatory")
    @Operation(summary = "Get mandatory training modules", description = "Retrieve all mandatory training modules that are active")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mandatory training modules retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<TrainingModuleResponse>>> getMandatoryModules() {
        return ok(trainingModuleService.getMandatoryModules());
    }

    @PostMapping("/modules")
    @PreAuthorize("hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Create a new training module", description = "Create a new training module in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Training module created successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<TrainingModuleResponse>> createModule(
            @Valid @RequestBody CreateTrainingModuleRequest request) {
        TrainingModuleResponse module = trainingModuleService.createModule(request);
        URI location = URI.create("/api/v1/support/modules/" + module.id());
        return created(module, location.toString());
    }

    @GetMapping("/modules/{id}")
    @Operation(summary = "Get training module by ID", description = "Retrieve a specific training module by its ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training module found",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training module not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<TrainingModuleResponse>> getModuleById(
            @Parameter(description = "Training module ID", required = true) @PathVariable Long id) {
        TrainingModuleResponse module = trainingModuleService.getModuleById(id);
        if (module == null) {
            return notFound("MODULE_404", "Training module not found");
        }
        return ok(module);
    }

    @PatchMapping("/modules/{id}/status")
    @PreAuthorize("hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Update training module status", description = "Update the status of a training module")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training module status updated successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training module not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<TrainingModuleResponse>> updateModuleStatus(
            @Parameter(description = "Training module ID", required = true) @PathVariable Long id,
            @RequestBody Map<String, String> status) {
        TrainingStatus trainingStatus = TrainingStatus.valueOf(status.get("status"));
        TrainingModuleResponse module = trainingModuleService.updateModuleStatus(id, trainingStatus);
        if (module == null) {
            return notFound("MODULE_404", "Training module not found");
        }
        return ok(module);
    }

    @GetMapping("/trainings")
    @Operation(summary = "Get all agent trainings", description = "Retrieve all agent training records")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent trainings retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<AgentTrainingResponse>>> getAllTrainings() {
        return ok(agentTrainingService.getAllAgentTrainings());
    }

    @GetMapping("/trainings/agent/{agentId}")
    @Operation(summary = "Get trainings for a specific agent", description = "Retrieve all training records for a specific agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent trainings retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<AgentTrainingResponse>>> getTrainingsByAgent(
            @Parameter(description = "Agent ID", required = true) @PathVariable Long agentId) {
        return ok(agentTrainingService.getTrainingsByAgent(agentId));
    }

    @GetMapping("/trainings/module/{moduleId}")
    @Operation(summary = "Get trainings for a specific module", description = "Retrieve all training records for a specific module")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Module trainings retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training module not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<AgentTrainingResponse>>> getTrainingsByModule(
            @Parameter(description = "Training module ID", required = true) @PathVariable Long moduleId) {
        return ok(agentTrainingService.getTrainingsByModule(moduleId));
    }

    @GetMapping("/trainings/agent/{agentId}/module/{moduleId}")
    @Operation(summary = "Get specific training for an agent", description = "Retrieve a specific training record for an agent and module")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training found",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AgentTrainingResponse>> getAgentTraining(
            @Parameter(description = "Agent ID", required = true) @PathVariable Long agentId,
            @Parameter(description = "Training module ID", required = true) @PathVariable Long moduleId) {
        AgentTrainingResponse training = agentTrainingService.getAgentTraining(agentId, moduleId);
        if (training == null) {
            return notFound("TRAINING_404", "Training not found");
        }
        return ok(training);
    }

    @PostMapping("/trainings/assign")
    @PreAuthorize("hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Assign training to an agent", description = "Assign a training module to an agent or update existing training status")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Training assigned successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent or training module not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AgentTrainingResponse>> assignTraining(
            @Valid @RequestBody AssignTrainingRequest request) {
        AgentTrainingResponse training = agentTrainingService.assignTraining(request);
        URI location = URI.create(String.format("/api/v1/support/trainings/agent/%d/module/%d",
                request.agentId(), request.moduleId()));
        return created(training, location.toString());
    }

    @GetMapping("/trainings/agent/{agentId}/status")
    @Operation(summary = "Check if agent is fully trained", description = "Check if an agent has completed all mandatory training")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAgentTrainingStatus(
            @Parameter(description = "Agent ID", required = true) @PathVariable Long agentId) {
        boolean fullyTrained = agentTrainingService.isAgentFullyTrained(agentId);
        return ok(Map.of(
            "agentId", agentId,
            "fullyTrained", fullyTrained
        ));
    }

    // --- ADR-0051 Support Ticket & FAQ (BE-SUPP-001) ponytail: minimal ITIL lifecycle, RLS tenant_id stub, outbox log ---
    @PostMapping("/tickets")
    @Operation(summary = "Create support ticket", description = "ITIL: OPEN→IN_PROGRESS→WAITING_CUSTOMER→RESOLVED→CLOSED, SLA 24h, idempotency X-Idempotency-Key")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(@RequestBody Map<String,String> body,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idem) {
        String userId = currentUserId();
        SupportTicketResponse saved = ticketService.create(userId, body);
        // ponytail: outbox payu.support.ticket-created.v1 → log; add outbox-starter when Kafka needed + DLQ .dlq
        org.slf4j.LoggerFactory.getLogger(SupportController.class).info("ticket-created {} user {}", saved.id(), userId);
        URI loc = URI.create("/api/v1/support/tickets/" + saved.id());
        return created(saved, loc.toString());
    }

    @GetMapping("/tickets")
    @Operation(summary = "List user tickets")
    public ResponseEntity<ApiResponse<List<SupportTicketResponse>>> listTickets(@RequestParam(required = false) String status) {
        String userId = currentUserId();
        return ok(ticketService.list(userId, status));
    }

    @GetMapping("/faqs")
    @Operation(summary = "List FAQs")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> listFaqs(@RequestParam(required = false) String category) {
        return ok(faqService.list(category));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) return auth.getName();
        return "anonymous";
    }

    // Inner class for schema documentation
    @Schema(description = "Training status response")
    private static class TrainingStatusResponse {
        @Schema(description = "Number of active agents")
        public long activeAgents;

        @Schema(description = "Number of fully trained agents")
        public long trainedAgents;

        @Schema(description = "Training completion percentage")
        public double trainingPercentage;
    }
}
