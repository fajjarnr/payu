package id.payu.promotion.resource;

import id.payu.promotion.domain.Promotion;
import id.payu.promotion.dto.CreatePromotionRequest;
import id.payu.promotion.dto.UpdatePromotionRequest;
import id.payu.promotion.dto.ClaimPromotionRequest;
import id.payu.promotion.dto.PromotionResponse;
import id.payu.promotion.dto.RewardResponse;
import id.payu.promotion.service.PromotionService;
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
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Promotions", description = "Promotion management APIs")
public class PromotionResource extends BaseController {

    private static final Logger LOG = Logger.getLogger(PromotionResource.class);

    @Inject
    PromotionService promotionService;

    @POST
    @Operation(summary = "Create promotion", description = "Create a new promotion campaign")
    @APIResponse(responseCode = "201", description = "Promotion created successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    public Response createPromotion(@Valid CreatePromotionRequest request) {
        try {
            Promotion promotion = promotionService.createPromotion(request);
            return created(PromotionResponse.from(promotion), "/api/v1/promotions/{id}", promotion.id);
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_001", e.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update promotion", description = "Update an existing promotion")
    @APIResponse(responseCode = "200", description = "Promotion updated successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "404", description = "Promotion not found")
    public Response updatePromotion(@PathParam("id") UUID id, UpdatePromotionRequest request) {
        try {
            Promotion promotion = promotionService.updatePromotion(id, request);
            return ok(PromotionResponse.from(promotion));
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_002", e.getMessage());
        }
    }

    @POST
    @Path("/{code}/claim")
    @Operation(summary = "Claim promotion", description = "Claim a promotion using a promo code")
    @APIResponse(responseCode = "201", description = "Promotion claimed successfully",
            content = @Content(schema = @Schema(implementation = RewardResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request or promotion not claimable")
    public Response claimPromotion(@PathParam("code") String code, @Valid ClaimPromotionRequest request) {
        try {
            id.payu.promotion.domain.Reward reward = promotionService.claimPromotion(code, request);
            return created(RewardResponse.from(reward), "/api/v1/promotions/rewards/{id}", reward.id);
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_003", e.getMessage());
        }
    }

    @POST
    @Path("/{id}/activate")
    @Operation(summary = "Activate promotion", description = "Activate a promotion campaign")
    @APIResponse(responseCode = "200", description = "Promotion activated successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "404", description = "Promotion not found")
    public Response activatePromotion(@PathParam("id") UUID id) {
        try {
            Promotion promotion = promotionService.activatePromotion(id);
            return ok(PromotionResponse.from(promotion));
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_004", e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get promotion by ID", description = "Retrieve promotion details by ID")
    @APIResponse(responseCode = "200", description = "Promotion found",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class)))
    @APIResponse(responseCode = "404", description = "Promotion not found")
    public Response getPromotion(@PathParam("id") UUID id) {
        return promotionService.getPromotion(id)
            .map(promotion -> ok(PromotionResponse.from(promotion)))
            .orElse(notFound("PROMO_404", "Promotion not found"));
    }

    @GET
    @Path("/code/{code}")
    @Operation(summary = "Get promotion by code", description = "Retrieve promotion details by promo code")
    @APIResponse(responseCode = "200", description = "Promotion found",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class)))
    @APIResponse(responseCode = "404", description = "Promotion not found")
    public Response getPromotionByCode(@PathParam("code") String code) {
        return promotionService.getPromotionByCode(code)
            .map(promotion -> ok(PromotionResponse.from(promotion)))
            .orElse(notFound("PROMO_404", "Promotion not found"));
    }

    @GET
    @Operation(summary = "Get active promotions", description = "Retrieve all currently active promotions")
    @APIResponse(responseCode = "200", description = "Active promotions retrieved successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class)))
    public Response getActivePromotions() {
        List<Promotion> promotions = Promotion.<Promotion>find(
            "status = ?1 and startDate <= ?2 and endDate >= ?3",
            Promotion.Status.ACTIVE, LocalDateTime.now(), LocalDateTime.now())
            .list();
        return ok(promotions.stream().map(PromotionResponse::from).toList());
    }
}
