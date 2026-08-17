package id.payu.transaction.interfaces.dto;

import java.math.BigDecimal;

/**
 * Response DTO for route recommendation.
 */
public class RouteRecommendationResponse {

    private TransferRouteResponse route;
    private String reason;
    private BigDecimal totalCost;
    private String currency;
    private String estimatedTime;

    public RouteRecommendationResponse() {
    }

    public RouteRecommendationResponse(TransferRouteResponse route, String reason, BigDecimal totalCost,
                                       String currency, String estimatedTime) {
        this.route = route;
        this.reason = reason;
        this.totalCost = totalCost;
        this.currency = currency;
        this.estimatedTime = estimatedTime;
    }

    public static RouteRecommendationResponseBuilder builder() {
        return new RouteRecommendationResponseBuilder();
    }

    public TransferRouteResponse getRoute() {
        return route;
    }

    public void setRoute(TransferRouteResponse route) {
        this.route = route;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public static class RouteRecommendationResponseBuilder {
        private TransferRouteResponse route;
        private String reason;
        private BigDecimal totalCost;
        private String currency;
        private String estimatedTime;

        public RouteRecommendationResponseBuilder route(TransferRouteResponse route) {
            this.route = route;
            return this;
        }

        public RouteRecommendationResponseBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public RouteRecommendationResponseBuilder totalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
            return this;
        }

        public RouteRecommendationResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public RouteRecommendationResponseBuilder estimatedTime(String estimatedTime) {
            this.estimatedTime = estimatedTime;
            return this;
        }

        public RouteRecommendationResponse build() {
            return new RouteRecommendationResponse(route, reason, totalCost, currency, estimatedTime);
        }
    }
}
