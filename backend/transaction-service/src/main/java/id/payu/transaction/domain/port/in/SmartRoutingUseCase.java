package id.payu.transaction.domain.model;

import java.util.List;

/**
 * Input port defining the smart routing use cases.
 *
 * <p>This interface defines operations for finding optimal transfer routes
 * based on amount, bank code, and user preferences. It supports fee optimization,
 * speed prioritization, and reliability-based routing decisions.</p>
 *
 * <p>Key Operations:</p>
 * <ul>
 *   <li>Find best routes for a given amount</li>
 *   <li>Get route recommendations with explanations</li>
 *   <li>Check route eligibility</li>
 *   <li>Calculate total cost including fees</li>
 * </ul>
 *
 * @see TransferRoute
 * @see TransferMethod
 */
public interface SmartRoutingUseCase {

    /**
     * Finds the best transfer routes for a given amount.
     * Routes are sorted by fee (cheapest first) and filtered by eligibility.
     *
     * @param amount the transfer amount
     * @param bankCode optional bank code for bank-specific routing rules
     * @return list of eligible routes sorted by fee
     */
    List<TransferRoute> findBestRoutes(Money amount, String bankCode);

    /**
     * Finds routes sorted by speed (fastest first).
     *
     * @param amount the transfer amount
     * @param bankCode optional bank code for bank-specific routing rules
     * @return list of eligible routes sorted by speed
     */
    List<TransferRoute> findFastestRoutes(Money amount, String bankCode);

    /**
     * Gets the recommended route with explanation.
     * Considers amount, fees, and speed to provide the best option.
     *
     * @param amount the transfer amount
     * @param bankCode optional bank code
     * @return route recommendation with reasoning
     */
    RouteRecommendation getRecommendedRoute(Money amount, String bankCode);

    /**
     * Checks if a specific transfer method is available for the amount.
     *
     * @param method the transfer method to check
     * @param amount the transfer amount
     * @return true if the method is available for this amount
     */
    boolean isMethodAvailable(TransferMethod method, Money amount);

    /**
     * Calculates the total cost including fees for a route.
     *
     * @param amount the transfer amount
     * @param method the transfer method
     * @return the total cost (amount + fee)
     */
    Money calculateTotalCost(Money amount, TransferMethod method);

    /**
     * Gets all available transfer methods with their default routes.
     *
     * @return list of all available routes
     */
    List<TransferRoute> getAllRoutes();

    /**
     * Record representing a route recommendation with explanation.
     */
    record RouteRecommendation(
            TransferRoute route,
            String reason,
            Money totalCost,
            String estimatedTime
    ) {}
}
