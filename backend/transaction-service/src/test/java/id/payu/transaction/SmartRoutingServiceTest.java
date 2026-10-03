package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransferMethod;
import id.payu.transaction.domain.model.TransferRoute;
import id.payu.transaction.domain.port.in.SmartRoutingUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SmartRoutingService.
 */
@DisplayName("SmartRoutingService Tests")
class SmartRoutingServiceTest {

    private SmartRoutingService smartRoutingService;

    @BeforeEach
    void setUp() {
        smartRoutingService = new SmartRoutingService();
    }

    @Nested
    @DisplayName("Find Best Routes")
    class FindBestRoutesTests {

        @Test
        @DisplayName("Should return routes sorted by fee (cheapest first)")
        void shouldReturnRoutesSortedByFeeCheapestFirst() {
            List<TransferRoute> routes = smartRoutingService.findBestRoutes(Money.idr("100000"), null);

            assertThat(routes).hasSizeGreaterThanOrEqualTo(1);
            // First route should be cheapest
            assertThat(routes.get(0).getFee()).isLessThanOrEqualTo(
                    routes.get(routes.size() - 1).getFee()
            );
        }

        @Test
        @DisplayName("Should filter out ineligible routes for amount")
        void shouldFilterOutIneligibleRoutesForAmount() {
            // Amount too small for RTGS
            List<TransferRoute> routes = smartRoutingService.findBestRoutes(Money.idr("10000"), null);

            assertThat(routes).noneMatch(r -> r.getMethod() == TransferMethod.RTGS);
        }

        @Test
        @DisplayName("Should include RTGS for large amounts")
        void shouldIncludeRtgsForLargeAmounts() {
            // Amount large enough for RTGS
            List<TransferRoute> routes = smartRoutingService.findBestRoutes(Money.idr("500000000"), null);

            assertThat(routes).anyMatch(r -> r.getMethod() == TransferMethod.RTGS);
        }
    }

    @Nested
    @DisplayName("Find Fastest Routes")
    class FindFastestRoutesTests {

        @Test
        @DisplayName("Should return routes sorted by speed (fastest first)")
        void shouldReturnRoutesSortedBySpeedFastestFirst() {
            List<TransferRoute> routes = smartRoutingService.findFastestRoutes(Money.idr("100000"), null);

            assertThat(routes).hasSizeGreaterThanOrEqualTo(1);
            // BI-FAST should be fastest
            assertThat(routes.get(0).getMethod()).isEqualTo(TransferMethod.BI_FAST);
        }
    }

    @Nested
    @DisplayName("Get Recommended Route")
    class GetRecommendedRouteTests {

        @Test
        @DisplayName("Should recommend BI-FAST for small amounts")
        void shouldRecommendBiFastForSmallAmounts() {
            SmartRoutingUseCase.RouteRecommendation recommendation =
                    smartRoutingService.getRecommendedRoute(Money.idr("50000"), null);

            assertThat(recommendation).isNotNull();
            assertThat(recommendation.route().getMethod()).isEqualTo(TransferMethod.BI_FAST);
        }

        @Test
        @DisplayName("Should recommend RTGS for large amounts")
        void shouldRecommendRtgsForLargeAmounts() {
            SmartRoutingUseCase.RouteRecommendation recommendation =
                    smartRoutingService.getRecommendedRoute(Money.idr("500000000"), null);

            assertThat(recommendation).isNotNull();
            assertThat(recommendation.route().getMethod()).isEqualTo(TransferMethod.RTGS);
            assertThat(recommendation.reason()).containsIgnoringCase("high-value");
        }

        @Test
        @DisplayName("Should include total cost in recommendation")
        void shouldIncludeTotalCostInRecommendation() {
            SmartRoutingUseCase.RouteRecommendation recommendation =
                    smartRoutingService.getRecommendedRoute(Money.idr("100000"), null);

            assertThat(recommendation.totalCost().getAmount())
                    .isEqualByComparingTo(new BigDecimal("102500")); // 100000 + 2500 fee
        }
    }

    @Nested
    @DisplayName("Calculate Total Cost")
    class CalculateTotalCostTests {

        @Test
        @DisplayName("Should calculate total cost with BI-FAST fee")
        void shouldCalculateTotalCostWithBiFastFee() {
            Money total = smartRoutingService.calculateTotalCost(Money.idr("100000"), TransferMethod.BI_FAST);

            assertThat(total.getAmount()).isEqualByComparingTo(new BigDecimal("102500"));
        }

        @Test
        @DisplayName("Should calculate total cost with RTGS fee")
        void shouldCalculateTotalCostWithRtgsFee() {
            Money total = smartRoutingService.calculateTotalCost(Money.idr("100000000"), TransferMethod.RTGS);

            assertThat(total.getAmount()).isEqualByComparingTo(new BigDecimal("100025000"));
        }
    }

    @Nested
    @DisplayName("Is Method Available")
    class IsMethodAvailableTests {

        @Test
        @DisplayName("Should return true for available method")
        void shouldReturnTrueForAvailableMethod() {
            boolean available = smartRoutingService.isMethodAvailable(TransferMethod.BI_FAST, Money.idr("100000"));

            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("Should return false for unavailable method (amount too small)")
        void shouldReturnFalseForUnavailableMethodAmountTooSmall() {
            boolean available = smartRoutingService.isMethodAvailable(TransferMethod.RTGS, Money.idr("10000"));

            assertThat(available).isFalse();
        }
    }
}
