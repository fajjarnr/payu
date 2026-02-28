package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.PartnerRateLimitService;
import id.payu.gateway.domain.entity.PartnerRatePlan;
import id.payu.gateway.domain.entity.RatePlan;
import id.payu.gateway.domain.repository.PartnerRatePlanRepository;
import id.payu.gateway.domain.repository.RatePlanRepository;
import id.payu.gateway.domain.vo.RateLimit;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

/**
 * REST Resource for managing Partner Rate Plans.
 *
 * <p>
 * Provides endpoints for:
 * - CRUD operations on RatePlans
 * - Assigning RatePlans to partners
 * - Querying partner rate limits
 * - Checking rate limit status
 */
@Path("/api/v1/admin/rate-plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class PartnerRatePlanResource {

    @Inject
    RatePlanRepository ratePlanRepository;

    @Inject
    PartnerRatePlanRepository partnerRatePlanRepository;

    @Inject
    PartnerRateLimitService partnerRateLimitService;

    // Rate Plan CRUD Operations

    @GET
    public Multi<RatePlan> listRatePlans(
            @QueryParam("active") @DefaultValue("true") boolean activeOnly) {
        if (activeOnly) {
            return ratePlanRepository.findAllActive();
        }
        return ratePlanRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getRatePlan(@PathParam("id") String id) {
        return ratePlanRepository.findById(id)
            .map(optional -> optional
                .map(plan -> Response.ok(plan).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build()));
    }

    @POST
    public Uni<Response> createRatePlan(CreateRatePlanRequest request) {
        RatePlan plan = new RatePlan(
            java.util.UUID.randomUUID().toString(),
            request.name(),
            request.description(),
            RateLimit.of(request.requestsPerMinute(), request.requestsPerHour(), request.requestsPerDay())
        );

        // Add endpoint overrides if provided
        if (request.endpointOverrides() != null) {
            request.endpointOverrides().forEach((endpoint, limits) ->
                plan.addEndpointOverride(endpoint, RateLimit.of(
                    limits.getOrDefault("requestsPerMinute", 60),
                    limits.getOrDefault("requestsPerHour", 1000),
                    limits.getOrDefault("requestsPerDay", 10000)
                ))
            );
        }

        return ratePlanRepository.save(plan)
            .map(saved -> Response.status(Response.Status.CREATED).entity(saved).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> updateRatePlan(@PathParam("id") String id, UpdateRatePlanRequest request) {
        return ratePlanRepository.findById(id)
            .flatMap(optional -> {
                if (optional.isEmpty()) {
                    return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                }

                RatePlan plan = optional.get();

                if (request.name() != null) {
                    plan.updateName(request.name());
                }
                if (request.description() != null) {
                    plan.updateDescription(request.description());
                }
                if (request.requestsPerMinute() != null || request.requestsPerHour() != null
                    || request.requestsPerDay() != null) {
                    RateLimit current = plan.getDefaultLimit();
                    plan.updateDefaultLimit(RateLimit.of(
                        request.requestsPerMinute() != null ? request.requestsPerMinute() : current.requestsPerMinute(),
                        request.requestsPerHour() != null ? request.requestsPerHour() : current.requestsPerHour(),
                        request.requestsPerDay() != null ? request.requestsPerDay() : current.requestsPerDay()
                    ));
                }

                return ratePlanRepository.save(plan)
                    .map(updated -> Response.ok(updated).build());
            });
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteRatePlan(@PathParam("id") String id) {
        return ratePlanRepository.deleteById(id)
            .map(deleted -> deleted
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build());
    }

    // Partner Assignment Operations

    @POST
    @Path("/assignments")
    public Uni<Response> assignRatePlan(AssignRatePlanRequest request) {
        return partnerRateLimitService.assignRatePlan(request.partnerId(), request.ratePlanId())
            .map(assignment -> Response.status(Response.Status.CREATED).entity(assignment).build())
            .onFailure().recoverWithItem(e -> {
                Log.errorf(e, "Failed to assign rate plan");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
            });
    }

    @GET
    @Path("/partners/{partnerId}/rate-plan")
    public Uni<Response> getPartnerRatePlan(@PathParam("partnerId") String partnerId) {
        return partnerRateLimitService.getPartnerRatePlan(partnerId)
            .map(optional -> optional
                .map(plan -> Response.ok(plan).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No rate plan assigned for partner: " + partnerId))
                    .build()));
    }

    @GET
    @Path("/partners/{partnerId}/limits")
    public Uni<Response> getPartnerLimits(
            @PathParam("partnerId") String partnerId,
            @QueryParam("endpoint") String endpoint) {

        String path = endpoint != null ? endpoint : "/api/v1/*";

        return partnerRateLimitService.getEffectiveRateLimit(partnerId, path)
            .map(rateLimit -> {
                if (rateLimit == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No rate limit configured for partner"))
                        .build();
                }

                return Response.ok(Map.of(
                    "partnerId", partnerId,
                    "endpoint", path,
                    "requestsPerMinute", rateLimit.requestsPerMinute(),
                    "requestsPerHour", rateLimit.requestsPerHour(),
                    "requestsPerDay", rateLimit.requestsPerDay()
                )).build();
            });
    }

    @GET
    @Path("/partners/{partnerId}/status")
    public Uni<Response> checkPartnerRateLimitStatus(
            @PathParam("partnerId") String partnerId,
            @QueryParam("endpoint") @DefaultValue("/api/v1/accounts") String endpoint) {

        return partnerRateLimitService.checkRateLimit(partnerId, endpoint)
            .map(result -> Response.ok(Map.of(
                "partnerId", partnerId,
                "endpoint", endpoint,
                "allowed", result.allowed(),
                "remaining", result.remaining(),
                "limit", result.limit(),
                "current", result.current(),
                "retryAfter", result.retryAfter(),
                "limitingWindow", result.limitingWindow()
            )).build());
    }

    // DTOs

    public record CreateRatePlanRequest(
        String name,
        String description,
        int requestsPerMinute,
        int requestsPerHour,
        int requestsPerDay,
        Map<String, Map<String, Integer>> endpointOverrides
    ) {}

    public record UpdateRatePlanRequest(
        String name,
        String description,
        Integer requestsPerMinute,
        Integer requestsPerHour,
        Integer requestsPerDay
    ) {}

    public record AssignRatePlanRequest(
        String partnerId,
        String ratePlanId
    ) {}
}
