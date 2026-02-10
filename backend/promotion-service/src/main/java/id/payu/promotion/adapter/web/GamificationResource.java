package id.payu.promotion.adapter.web;

import id.payu.promotion.dto.*;
import id.payu.promotion.application.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@Tag(name = "Gamification", description = "Daily check-ins, badges, and level progression APIs")
@SecurityRequirement(name = "bearerAuth")
public class GamificationResource {

    private final GamificationService gamificationService;

    public GamificationResource(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @PostMapping("/checkin")
    @Operation(summary = "Perform daily check-in")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Check-in recorded successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = DailyCheckinResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DailyCheckinResponse> performDailyCheckin(@RequestParam String accountId) {
        DailyCheckinResponse response = gamificationService.performDailyCheckin(accountId);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/checkin/today")
    @Operation(summary = "Get today's check-in status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check-in status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DailyCheckinResponse> getTodayCheckin(@RequestParam String accountId) {
        DailyCheckinResponse response = gamificationService.getTodayCheckin(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkin/streak")
    @Operation(summary = "Get current streak count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Streak count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Integer> getCurrentStreak(@RequestParam String accountId) {
        return ResponseEntity.ok(gamificationService.getCurrentStreak(accountId));
    }

    @PostMapping("/transaction")
    @Operation(summary = "Process transaction for gamification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GamificationEventResponse> processTransaction(@Valid @RequestBody ProcessTransactionRequest request) {
        return ResponseEntity.ok(gamificationService.processTransaction(request));
    }

    @GetMapping("/level")
    @Operation(summary = "Get user level and XP")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User level retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User level not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserLevelResponse> getUserLevel(@RequestParam String accountId) {
        UserLevelResponse response = gamificationService.getUserLevel(accountId);
        if (response == null) {
            throw new RuntimeException("User level not found");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/badges")
    @Operation(summary = "Get user earned badges")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Badges retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<EarnedBadgeResponse>> getUserBadges(@RequestParam String accountId) {
        return ResponseEntity.ok(gamificationService.getUserBadges(accountId));
    }

    @GetMapping("/badges/progress")
    @Operation(summary = "Get badge progress")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Badge progress retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<BadgeProgressResponse>> getBadgeProgress(@RequestParam String accountId) {
        return ResponseEntity.ok(gamificationService.getBadgeProgress(accountId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get gamification summary")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GamificationSummaryResponse> getSummary(@RequestParam String accountId) {
        return ResponseEntity.ok(gamificationService.getSummary(accountId));
    }
}
