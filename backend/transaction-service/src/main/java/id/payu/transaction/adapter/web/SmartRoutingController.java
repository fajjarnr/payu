package id.payu.transaction.adapter.web;

import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransferRoute;
import id.payu.transaction.domain.port.in.SmartRoutingUseCase;
import id.payu.transaction.dto.RouteRecommendationResponse;
import id.payu.transaction.dto.TransferRouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for smart transfer routing.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Finding best transfer routes</li>
 *   <li>Getting route recommendations</li>
 *   <li>Calculating transfer costs</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/transfers/routes")
@Tag(name = "Smart Routing", description = "Smart Transfer Routing API")
@SecurityRequirement(name = "bearerAuth")
public class SmartRoutingController {

    private final SmartRoutingUseCase smartRoutingUseCase;

    public SmartRoutingController(SmartRoutingUseCase smartRoutingUseCase) {
        this.smartRoutingUseCase = smartRoutingUseCase;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find best transfer routes", description = "Returns eligible routes sorted by fee (cheapest first)")
    public ResponseEntity<List<TransferRouteResponse>> findBestRoutes(
            @RequestParam @Parameter(description = "Transfer amount") BigDecimal amount,
            @RequestParam(defaultValue = "IDR") @Parameter(description = "Currency code") String currency,
            @RequestParam(required = false) @Parameter(description = "Destination bank code") String bankCode) {

        Money money = Money.of(amount, currency);
        List<TransferRoute> routes = smartRoutingUseCase.findBestRoutes(money, bankCode);

        List<TransferRouteResponse> responses = routes.stream()
                .map(route -> TransferRouteResponse.fromEntity(route, true))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/fastest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find fastest routes", description = "Returns eligible routes sorted by speed (fastest first)")
    public ResponseEntity<List<TransferRouteResponse>> findFastestRoutes(
            @RequestParam @Parameter(description = "Transfer amount") BigDecimal amount,
            @RequestParam(defaultValue = "IDR") @Parameter(description = "Currency code") String currency,
            @RequestParam(required = false) @Parameter(description = "Destination bank code") String bankCode) {

        Money money = Money.of(amount, currency);
        List<TransferRoute> routes = smartRoutingUseCase.findFastestRoutes(money, bankCode);

        List<TransferRouteResponse> responses = routes.stream()
                .map(route -> TransferRouteResponse.fromEntity(route, true))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/recommend")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recommended route", description = "Returns the recommended route with explanation")
    public ResponseEntity<RouteRecommendationResponse> getRecommendedRoute(
            @RequestParam @Parameter(description = "Transfer amount") BigDecimal amount,
            @RequestParam(defaultValue = "IDR") @Parameter(description = "Currency code") String currency,
            @RequestParam(required = false) @Parameter(description = "Destination bank code") String bankCode) {

        Money money = Money.of(amount, currency);
        SmartRoutingUseCase.RouteRecommendation recommendation =
                smartRoutingUseCase.getRecommendedRoute(money, bankCode);

        if (recommendation == null) {
            return ResponseEntity.noContent().build();
        }

        RouteRecommendationResponse response = RouteRecommendationResponse.builder()
                .route(TransferRouteResponse.fromEntity(recommendation.route(), true))
                .reason(recommendation.reason())
                .totalCost(recommendation.totalCost().getAmount())
                .currency(recommendation.totalCost().getCurrency().getCurrencyCode())
                .estimatedTime(recommendation.estimatedTime())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all available routes", description = "Returns all transfer routes with eligibility for amount")
    public ResponseEntity<List<TransferRouteResponse>> getAllRoutes(
            @RequestParam @Parameter(description = "Transfer amount") BigDecimal amount,
            @RequestParam(defaultValue = "IDR") @Parameter(description = "Currency code") String currency,
            @RequestParam(required = false) @Parameter(description = "Destination bank code") String bankCode) {

        Money money = Money.of(amount, currency);
        List<TransferRoute> routes = smartRoutingUseCase.getAllRoutes();

        List<TransferRouteResponse> responses = routes.stream()
                .map(route -> TransferRouteResponse.fromEntity(
                        route, route.isEligibleFor(money)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
