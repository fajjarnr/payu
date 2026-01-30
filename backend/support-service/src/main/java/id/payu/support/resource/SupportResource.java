package id.payu.support.resource;

import id.payu.support.domain.TrainingModule;
import id.payu.support.dto.*;
import id.payu.support.service.AgentService;
import id.payu.support.service.AgentTrainingService;
import id.payu.support.service.TrainingModuleService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/v1/support")
@Tag(name = "Support Management", description = "API for managing support team training")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupportResource extends BaseController {

    @Inject
    AgentService agentService;

    @Inject
    TrainingModuleService trainingModuleService;

    @Inject
    AgentTrainingService agentTrainingService;

    @GET
    @Path("/training-status")
    @Operation(summary = "Get overall training status")
    @APIResponse(responseCode = "200", description = "Training status retrieved successfully")
    public Response getTrainingStatus() {
        long activeAgents = agentService.countActiveAgents();
        long trainedAgents = agentTrainingService.countFullyTrainedAgents();

        return ok(Map.of(
            "activeAgents", activeAgents,
            "trainedAgents", trainedAgents,
            "trainingPercentage", activeAgents > 0 ? (trainedAgents * 100.0 / activeAgents) : 0.0
        ));
    }

    @GET
    @Path("/agents")
    @Operation(summary = "Get all support agents")
    @APIResponse(responseCode = "200", description = "Agents retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentResponse.class)))
    public Response getAllAgents() {
        return ok(agentService.getAllAgents());
    }

    @POST
    @Path("/agents")
    @Operation(summary = "Create a new support agent")
    @APIResponse(responseCode = "201", description = "Agent created successfully",
            content = @Content(schema = @Schema(implementation = AgentResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    public Response createAgent(@Valid CreateAgentRequest request) {
        AgentResponse agent = agentService.createAgent(request);
        return created(agent, "/api/v1/support/agents/{id}", agent.getId());
    }

    @GET
    @Path("/agents/{id}")
    @Operation(summary = "Get agent by ID")
    @APIResponse(responseCode = "200", description = "Agent found",
            content = @Content(schema = @Schema(implementation = AgentResponse.class)))
    @APIResponse(responseCode = "404", description = "Agent not found")
    public Response getAgentById(@PathParam("id") Long id) {
        AgentResponse agent = agentService.getAgentById(id);
        if (agent == null) {
            return notFound("AGENT_404", "Agent not found");
        }
        return ok(agent);
    }

    @GET
    @Path("/agents/employee/{employeeId}")
    @Operation(summary = "Get agent by employee ID")
    @APIResponse(responseCode = "200", description = "Agent found",
            content = @Content(schema = @Schema(implementation = AgentResponse.class)))
    @APIResponse(responseCode = "404", description = "Agent not found")
    public Response getAgentByEmployeeId(@PathParam("employeeId") String employeeId) {
        AgentResponse agent = agentService.getAgentByEmployeeId(employeeId);
        if (agent == null) {
            return notFound("AGENT_404", "Agent not found");
        }
        return ok(agent);
    }

    @PATCH
    @Path("/agents/{id}/status")
    @Operation(summary = "Update agent active status")
    @APIResponse(responseCode = "200", description = "Agent status updated successfully",
            content = @Content(schema = @Schema(implementation = AgentResponse.class)))
    @APIResponse(responseCode = "404", description = "Agent not found")
    public Response updateAgentStatus(@PathParam("id") Long id, Map<String, Boolean> status) {
        AgentResponse agent = agentService.updateAgentStatus(id, status.get("active"));
        if (agent == null) {
            return notFound("AGENT_404", "Agent not found");
        }
        return ok(agent);
    }

    @GET
    @Path("/modules")
    @Operation(summary = "Get all training modules")
    @APIResponse(responseCode = "200", description = "Training modules retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class)))
    public Response getAllModules() {
        return ok(trainingModuleService.getAllTrainingModules());
    }

    @GET
    @Path("/modules/mandatory")
    @Operation(summary = "Get mandatory training modules")
    @APIResponse(responseCode = "200", description = "Mandatory training modules retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class)))
    public Response getMandatoryModules() {
        return ok(trainingModuleService.getMandatoryModules());
    }

    @POST
    @Path("/modules")
    @Operation(summary = "Create a new training module")
    @APIResponse(responseCode = "201", description = "Training module created successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    public Response createModule(@Valid CreateTrainingModuleRequest request) {
        TrainingModuleResponse module = trainingModuleService.createModule(request);
        return created(module, "/api/v1/support/modules/{id}", module.getId());
    }

    @GET
    @Path("/modules/{id}")
    @Operation(summary = "Get training module by ID")
    @APIResponse(responseCode = "200", description = "Training module found",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class)))
    @APIResponse(responseCode = "404", description = "Training module not found")
    public Response getModuleById(@PathParam("id") Long id) {
        TrainingModuleResponse module = trainingModuleService.getModuleById(id);
        if (module == null) {
            return notFound("MODULE_404", "Training module not found");
        }
        return ok(module);
    }

    @PATCH
    @Path("/modules/{id}/status")
    @Operation(summary = "Update training module status")
    @APIResponse(responseCode = "200", description = "Training module status updated successfully",
            content = @Content(schema = @Schema(implementation = TrainingModuleResponse.class)))
    @APIResponse(responseCode = "404", description = "Training module not found")
    public Response updateModuleStatus(@PathParam("id") Long id, Map<String, String> status) {
        TrainingModule.TrainingStatus trainingStatus = TrainingModule.TrainingStatus.valueOf(status.get("status"));
        TrainingModuleResponse module = trainingModuleService.updateModuleStatus(id, trainingStatus);
        if (module == null) {
            return notFound("MODULE_404", "Training module not found");
        }
        return ok(module);
    }

    @GET
    @Path("/trainings")
    @Operation(summary = "Get all agent trainings")
    @APIResponse(responseCode = "200", description = "Agent trainings retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class)))
    public Response getAllTrainings() {
        return ok(agentTrainingService.getAllAgentTrainings());
    }

    @GET
    @Path("/trainings/agent/{agentId}")
    @Operation(summary = "Get trainings for a specific agent")
    @APIResponse(responseCode = "200", description = "Agent trainings retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class)))
    public Response getTrainingsByAgent(@PathParam("agentId") Long agentId) {
        return ok(agentTrainingService.getTrainingsByAgent(agentId));
    }

    @GET
    @Path("/trainings/module/{moduleId}")
    @Operation(summary = "Get trainings for a specific module")
    @APIResponse(responseCode = "200", description = "Module trainings retrieved successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class)))
    public Response getTrainingsByModule(@PathParam("moduleId") Long moduleId) {
        return ok(agentTrainingService.getTrainingsByModule(moduleId));
    }

    @GET
    @Path("/trainings/agent/{agentId}/module/{moduleId}")
    @Operation(summary = "Get specific training for an agent")
    @APIResponse(responseCode = "200", description = "Training found",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class)))
    @APIResponse(responseCode = "404", description = "Training not found")
    public Response getAgentTraining(@PathParam("agentId") Long agentId, @PathParam("moduleId") Long moduleId) {
        AgentTrainingResponse training = agentTrainingService.getAgentTraining(agentId, moduleId);
        if (training == null) {
            return notFound("TRAINING_404", "Training not found");
        }
        return ok(training);
    }

    @POST
    @Path("/trainings/assign")
    @Operation(summary = "Assign training to an agent")
    @APIResponse(responseCode = "201", description = "Training assigned successfully",
            content = @Content(schema = @Schema(implementation = AgentTrainingResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    public Response assignTraining(@Valid AssignTrainingRequest request) {
        AgentTrainingResponse training = agentTrainingService.assignTraining(request);
        return created(training, "/api/v1/support/trainings/agent/{agentId}/module/{moduleId}",
                request.getAgentId(), request.getModuleId());
    }

    @GET
    @Path("/trainings/agent/{agentId}/status")
    @Operation(summary = "Check if agent is fully trained")
    @APIResponse(responseCode = "200", description = "Training status retrieved successfully")
    public Response checkAgentTrainingStatus(@PathParam("agentId") Long agentId) {
        boolean fullyTrained = agentTrainingService.isAgentFullyTrained(agentId);
        return ok(Map.of(
            "agentId", agentId,
            "fullyTrained", fullyTrained
        ));
    }
}
