package id.payu.promotion.resource;

import id.payu.promotion.domain.Reward;
import id.payu.promotion.dto.RewardResponse;
import id.payu.promotion.dto.RewardSummaryResponse;
import id.payu.promotion.service.RewardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/rewards")
@Produces(MediaType.APPLICATION_JSON)
public class RewardResource {

    @Inject
    RewardService rewardService;

    @GET
    @Path("/{id}")
    public Response getReward(@PathParam("id") UUID id) {
        return rewardService.getReward(id)
            .map(reward -> Response.ok(RewardResponse.from(reward)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Reward not found"))
                .build());
    }

    @GET
    @Path("/account/{accountId}")
    public Response getRewardsByAccount(@PathParam("accountId") String accountId,
                                        @QueryParam("limit") @DefaultValue("50") int limit,
                                        @QueryParam("offset") @DefaultValue("0") int offset) {
        List<Reward> rewards = rewardService.getRewardsByAccount(accountId, limit, offset);
        return Response.ok(rewards.stream().map(RewardResponse::from).toList()).build();
    }

    @GET
    @Path("/account/{accountId}/summary")
    public Response getRewardSummary(@PathParam("accountId") String accountId) {
        RewardSummaryResponse summary = rewardService.getRewardSummary(accountId);
        return Response.ok(summary).build();
    }

    record ErrorResponse(String message) {}
}
