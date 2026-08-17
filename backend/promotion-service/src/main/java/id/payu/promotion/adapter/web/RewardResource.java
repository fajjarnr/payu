package id.payu.promotion.adapter.web;

import id.payu.promotion.domain.model.Reward;
import id.payu.promotion.interfaces.dto.RewardResponse;
import id.payu.promotion.interfaces.dto.RewardSummaryResponse;
import id.payu.promotion.application.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rewards")
@Tag(name = "Rewards", description = "Reward management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RewardResource {

    private final RewardService rewardService;

    public RewardResource(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    /**
     * READY-069: List all rewards. Returns empty list — add paginated
     * listAll() in production.
     */
    @GetMapping
    @Operation(summary = "List rewards", description = "List reward records (paginated in production)")
    public ResponseEntity<?> listRewards() {
        return ResponseEntity.ok(java.util.List.of());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reward by ID", description = "Retrieve reward details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward found",
            content = @Content(schema = @Schema(implementation = RewardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Reward not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getReward(@PathVariable UUID id) {
        Optional<Reward> rewardOpt = rewardService.getReward(id);
        if (rewardOpt.isPresent()) {
            return ResponseEntity.ok(RewardResponse.from(rewardOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("RewardEntity not found"));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get rewards by account", description = "Retrieve all rewards for an account with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rewards retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<RewardResponse>> getRewardsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<Reward> rewards = rewardService.getRewardsByAccount(accountId, limit, offset);
        return ResponseEntity.ok(rewards.stream().map(RewardResponse::from).toList());
    }

    @GetMapping("/account/{accountId}/summary")
    @Operation(summary = "Get reward summary", description = "Retrieve reward summary for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward summary retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RewardSummaryResponse> getRewardSummary(@PathVariable String accountId) {
        RewardSummaryResponse summary = rewardService.getRewardSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    record ErrorResponse(String message) {}
}
