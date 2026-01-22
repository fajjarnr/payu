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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/v1/support")
@Tag(name = "Support Management", description = "API for managing support team training")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupportResource {

    @Inject
    AgentService agentService;

    @Inject
    TrainingModuleService trainingModuleService;

    @Inject
    AgentTrainingService agentTrainingService;

    @GET
    @Path("/training-status")
    @Operation(summary = "Get overall training status")
    public Response getTrainingStatus() {
        long activeAgents = agentService.countActiveAgents();
        long trainedAgents = agentTrainingService.countFullyTrainedAgents();

        return Response.ok(Map.of(
            "activeAgents", activeAgents,
            "trainedAgents", trainedAgents,
            "trainingPercentage", activeAgents > 0 ? (trainedAgents * 100.0 / activeAgents) : 0.0
        )).build();
    }

    @GET
    @Path("/agents")
    @Operation(summary = "Get all support agents")
    public Response getAllAgents() {
        return Response.ok(agentService.getAllAgents()).build();
    }

    @POST
    @Path("/agents")
    @Operation(summary = "Create a new support agent")
    public Response createAgent(@Valid CreateAgentRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(agentService.createAgent(request))
                .build();
    }

    @GET
    @Path("/agents/{id}")
    @Operation(summary = "Get agent by ID")
    public Response getAgentById(@PathParam("id") Long id) {
        AgentResponse agent = agentService.getAgentById(id);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(agent).build();
    }

    @GET
    @Path("/agents/employee/{employeeId}")
    @Operation(summary = "Get agent by employee ID")
    public Response getAgentByEmployeeId(@PathParam("employeeId") String employeeId) {
        AgentResponse agent = agentService.getAgentByEmployeeId(employeeId);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(agent).build();
    }

    @PATCH
    @Path("/agents/{id}/status")
    @Operation(summary = "Update agent active status")
    public Response updateAgentStatus(@PathParam("id") Long id, Map<String, Boolean> status) {
        AgentResponse agent = agentService.updateAgentStatus(id, status.get("active"));
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(agent).build();
    }

    @GET
    @Path("/modules")
    @Operation(summary = "Get all training modules")
    public Response getAllModules() {
        return Response.ok(trainingModuleService.getAllTrainingModules()).build();
    }

    @GET
    @Path("/modules/mandatory")
    @Operation(summary = "Get mandatory training modules")
    public Response getMandatoryModules() {
        return Response.ok(trainingModuleService.getMandatoryModules()).build();
    }

    @POST
    @Path("/modules")
    @Operation(summary = "Create a new training module")
    public Response createModule(@Valid CreateTrainingModuleRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(trainingModuleService.createModule(request))
                .build();
    }

    @GET
    @Path("/modules/{id}")
    @Operation(summary = "Get training module by ID")
    public Response getModuleById(@PathParam("id") Long id) {
        TrainingModuleResponse module = trainingModuleService.getModuleById(id);
        if (module == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(module).build();
    }

    @PATCH
    @Path("/modules/{id}/status")
    @Operation(summary = "Update training module status")
    public Response updateModuleStatus(@PathParam("id") Long id, Map<String, String> status) {
        TrainingModule.TrainingStatus trainingStatus = TrainingModule.TrainingStatus.valueOf(status.get("status"));
        TrainingModuleResponse module = trainingModuleService.updateModuleStatus(id, trainingStatus);
        if (module == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(module).build();
    }

    @GET
    @Path("/trainings")
    @Operation(summary = "Get all agent trainings")
    public Response getAllTrainings() {
        return Response.ok(agentTrainingService.getAllAgentTrainings()).build();
    }

    @GET
    @Path("/trainings/agent/{agentId}")
    @Operation(summary = "Get trainings for a specific agent")
    public Response getTrainingsByAgent(@PathParam("agentId") Long agentId) {
        return Response.ok(agentTrainingService.getTrainingsByAgent(agentId)).build();
    }

    @GET
    @Path("/trainings/module/{moduleId}")
    @Operation(summary = "Get trainings for a specific module")
    public Response getTrainingsByModule(@PathParam("moduleId") Long moduleId) {
        return Response.ok(agentTrainingService.getTrainingsByModule(moduleId)).build();
    }

    @GET
    @Path("/trainings/agent/{agentId}/module/{moduleId}")
    @Operation(summary = "Get specific training for an agent")
    public Response getAgentTraining(@PathParam("agentId") Long agentId, @PathParam("moduleId") Long moduleId) {
        AgentTrainingResponse training = agentTrainingService.getAgentTraining(agentId, moduleId);
        if (training == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(training).build();
    }

    @POST
    @Path("/trainings/assign")
    @Operation(summary = "Assign training to an agent")
    public Response assignTraining(@Valid AssignTrainingRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(agentTrainingService.assignTraining(request))
                .build();
    }

    @GET
    @Path("/trainings/agent/{agentId}/status")
    @Operation(summary = "Check if agent is fully trained")
    public Response checkAgentTrainingStatus(@PathParam("agentId") Long agentId) {
        boolean fullyTrained = agentTrainingService.isAgentFullyTrained(agentId);
        return Response.ok(Map.of(
            "agentId", agentId,
            "fullyTrained", fullyTrained
        )).build();
    }
}
