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
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromotionResource {

    private static final Logger LOG = Logger.getLogger(PromotionResource.class);

    @Inject
    PromotionService promotionService;

    @POST
    public Response createPromotion(@Valid CreatePromotionRequest request) {
        try {
            Promotion promotion = promotionService.createPromotion(request);
            return Response.status(Response.Status.CREATED)
                .entity(PromotionResponse.from(promotion))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updatePromotion(@PathParam("id") UUID id, UpdatePromotionRequest request) {
        try {
            Promotion promotion = promotionService.updatePromotion(id, request);
            return Response.ok(PromotionResponse.from(promotion)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{code}/claim")
    public Response claimPromotion(@PathParam("code") String code, @Valid ClaimPromotionRequest request) {
        try {
            id.payu.promotion.domain.Reward reward = promotionService.claimPromotion(code, request);
            return Response.status(Response.Status.CREATED)
                .entity(RewardResponse.from(reward))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/activate")
    public Response activatePromotion(@PathParam("id") UUID id) {
        try {
            Promotion promotion = promotionService.activatePromotion(id);
            return Response.ok(PromotionResponse.from(promotion)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getPromotion(@PathParam("id") UUID id) {
        return promotionService.getPromotion(id)
            .map(promotion -> Response.ok(PromotionResponse.from(promotion)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Promotion not found"))
                .build());
    }

    @GET
    @Path("/code/{code}")
    public Response getPromotionByCode(@PathParam("code") String code) {
        return promotionService.getPromotionByCode(code)
            .map(promotion -> Response.ok(PromotionResponse.from(promotion)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Promotion not found"))
                .build());
    }

    @GET
    public Response getActivePromotions() {
        List<Promotion> promotions = Promotion.<Promotion>find(
            "status = ?1 and startDate <= ?2 and endDate >= ?3",
            Promotion.Status.ACTIVE, LocalDateTime.now(), LocalDateTime.now())
            .list();
        return Response.ok(promotions.stream().map(PromotionResponse::from).toList()).build();
    }

    record ErrorResponse(String message) {}
}
