package id.payu.promotion.resource;

import id.payu.promotion.dto.*;
import id.payu.promotion.service.GamificationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/gamification")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Gamification", description = "Daily check-ins, badges, and level progression APIs")
@Authenticated
public class GamificationResource {

    @Inject
    GamificationService gamificationService;

    @POST
    @Path("/checkin")
    @Operation(summary = "Perform daily check-in")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response performDailyCheckin(@QueryParam("accountId") String accountId) {
        DailyCheckinResponse response = gamificationService.performDailyCheckin(accountId);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/checkin/today")
    @Operation(summary = "Get today's check-in status")
    public Response getTodayCheckin(@QueryParam("accountId") String accountId) {
        DailyCheckinResponse response = gamificationService.getTodayCheckin(accountId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/checkin/streak")
    @Operation(summary = "Get current streak count")
    public Integer getCurrentStreak(@QueryParam("accountId") String accountId) {
        return gamificationService.getCurrentStreak(accountId);
    }

    @POST
    @Path("/transaction")
    @Operation(summary = "Process transaction for gamification")
    public GamificationEventResponse processTransaction(@Valid ProcessTransactionRequest request) {
        return gamificationService.processTransaction(request);
    }

    @GET
    @Path("/level")
    @Operation(summary = "Get user level and XP")
    public UserLevelResponse getUserLevel(@QueryParam("accountId") String accountId) {
        UserLevelResponse response = gamificationService.getUserLevel(accountId);
        if (response == null) {
            throw new NotFoundException("User level not found");
        }
        return response;
    }

    @GET
    @Path("/badges")
    @Operation(summary = "Get user earned badges")
    public List<EarnedBadgeResponse> getUserBadges(@QueryParam("accountId") String accountId) {
        return gamificationService.getUserBadges(accountId);
    }

    @GET
    @Path("/badges/progress")
    @Operation(summary = "Get badge progress")
    public List<BadgeProgressResponse> getBadgeProgress(@QueryParam("accountId") String accountId) {
        return gamificationService.getBadgeProgress(accountId);
    }

    @GET
    @Path("/summary")
    @Operation(summary = "Get gamification summary")
    public GamificationSummaryResponse getSummary(@QueryParam("accountId") String accountId) {
        return gamificationService.getSummary(accountId);
    }
}
