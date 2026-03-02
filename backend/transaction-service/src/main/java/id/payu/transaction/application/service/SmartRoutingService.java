package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransferMethod;
import id.payu.transaction.domain.model.TransferRoute;
import id.payu.transaction.domain.port.in.SmartRoutingUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Application service for smart transfer routing.
 *
 * <p>This service provides intelligent routing recommendations for transfers
 * based on amount, fees, speed, and bank compatibility. It helps users choose
 * the optimal transfer method for their needs.
 *
 * <p>Routing Strategies:
 * <ul>
 *   <li><b>Cheapest First</b> - Default, sorts by fee ascending</li>
 *   <li><b>Fastest First</b> - Sorts by estimated time ascending</li>
 *   <li><b>Recommended</b> - Balances cost and speed for optimal experience</li>
 * </ul>
 *
 * <p>Route Eligibility:
 * <ul>
 *   <li>Amount must be within route's min/max limits</li>
 *   <li>Currency must match (IDR only currently supported)</li>
 *   <li>Bank code may affect availability for some methods</li>
 * </ul>
 *
 * @see TransferRoute
 * @see SmartRoutingUseCase
 */
@Service
public class SmartRoutingService implements SmartRoutingUseCase {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmartRoutingService.class);



    private static final List<TransferRoute> DEFAULT_ROUTES = List.of(
            TransferRoute.biFast(),
            TransferRoute.skn(),
            TransferRoute.rtgs()
    );

    @Override
    public List<TransferRoute> findBestRoutes(Money amount, String bankCode) {
        log.debug("Finding best routes for amount: {}, bank: {}", amount, bankCode);

        List<TransferRoute> eligibleRoutes = getEligibleRoutes(amount, bankCode);

        // Sort by fee (cheapest first)
        eligibleRoutes.sort(Comparator.naturalOrder());

        log.debug("Found {} eligible routes", eligibleRoutes.size());
        return eligibleRoutes;
    }

    @Override
    public List<TransferRoute> findFastestRoutes(Money amount, String bankCode) {
        log.debug("Finding fastest routes for amount: {}, bank: {}", amount, bankCode);

        List<TransferRoute> eligibleRoutes = getEligibleRoutes(amount, bankCode);

        // Sort by estimated time (fastest first)
        eligibleRoutes.sort(Comparator.comparing(TransferRoute::getEstimatedTime));

        return eligibleRoutes;
    }

    @Override
    public RouteRecommendation getRecommendedRoute(Money amount, String bankCode) {
        log.debug("Getting recommended route for amount: {}, bank: {}", amount, bankCode);

        List<TransferRoute> eligibleRoutes = getEligibleRoutes(amount, bankCode);

        if (eligibleRoutes.isEmpty()) {
            return null;
        }

        // Recommendation logic:
        // 1. For small amounts (< 100K): BI-FAST (cheap and fast)
        // 2. For medium amounts (100K - 100M): BI-FAST (best value)
        // 3. For large amounts (> 100M): RTGS (required for high value)
        // 4. For non-urgent: SKN (cheapest for medium amounts)

        TransferRoute recommended = eligibleRoutes.get(0); // Default to cheapest
        String reason;

        Money oneHundredMillion = Money.idr("100000000");

        if (amount.isGreaterThanOrEqualTo(oneHundredMillion)) {
            // Large amount - RTGS is required
            recommended = eligibleRoutes.stream()
                    .filter(r -> r.getMethod() == TransferMethod.RTGS)
                    .findFirst()
                    .orElse(recommended);
            reason = "Required for high-value transfers (100M+ IDR)";
        } else if (amount.isLessThanOrEqualTo(Money.idr("100000"))) {
            // Small amount - BI-FAST is best
            recommended = eligibleRoutes.stream()
                    .filter(r -> r.getMethod() == TransferMethod.BI_FAST)
                    .findFirst()
                    .orElse(recommended);
            reason = "Fast and cost-effective for small amounts";
        } else {
            // Medium amount - BI-FAST offers best balance
            recommended = eligibleRoutes.stream()
                    .filter(r -> r.getMethod() == TransferMethod.BI_FAST)
                    .findFirst()
                    .orElse(recommended);
            reason = "Best balance of speed and cost";
        }

        Money totalCost = recommended.calculateTotalAmount(amount);

        return new RouteRecommendation(
                recommended,
                reason,
                totalCost,
                recommended.getEstimatedTimeDisplay()
        );
    }

    @Override
    public boolean isMethodAvailable(TransferMethod method, Money amount) {
        return getAllRoutes().stream()
                .filter(r -> r.getMethod() == method)
                .anyMatch(r -> r.isEligibleFor(amount));
    }

    @Override
    public Money calculateTotalCost(Money amount, TransferMethod method) {
        TransferRoute route = getAllRoutes().stream()
                .filter(r -> r.getMethod() == method)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown method: " + method));

        return route.calculateTotalAmount(amount);
    }

    @Override
    public List<TransferRoute> getAllRoutes() {
        return new ArrayList<>(DEFAULT_ROUTES);
    }

    /**
     * Gets routes eligible for the given amount and bank code.
     *
     * @param amount the transfer amount
     * @param bankCode optional bank code
     * @return list of eligible routes
     */
    private List<TransferRoute> getEligibleRoutes(Money amount, String bankCode) {
        List<TransferRoute> eligible = new ArrayList<>();

        for (TransferRoute route : DEFAULT_ROUTES) {
            if (route.isEligibleFor(amount)) {
                // TODO: Add bank-specific eligibility checks
                // Some banks may not support certain methods
                eligible.add(route);
            }
        }

        return eligible;
    }
}
