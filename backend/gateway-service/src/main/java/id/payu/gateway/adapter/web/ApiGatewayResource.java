package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.CircuitBreakerService;
import id.payu.gateway.application.service.RetryAndTimeoutService;
import id.payu.gateway.application.service.RouteRegistry;
import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.adapter.filter.TenantFilter;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Blocking
public class ApiGatewayResource {

    @Inject
    GatewayConfig config;

    @Inject
    Vertx vertx;

    @Context
    UriInfo uriInfo;

    @Inject
    CircuitBreakerService circuitBreakerService;

    @Inject
    RetryAndTimeoutService retryAndTimeoutService;

    @Inject
    RouteRegistry routeRegistry;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    // ==================== Account Service ====================
    @POST @Path("/accounts/{path: .*}")
    public Uni<Response> accountPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "POST", body, headers);
    }
    @GET @Path("/accounts/{path: .*}")
    public Uni<Response> accountGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "GET", body, headers);
    }
    @PUT @Path("/accounts/{path: .*}")
    public Uni<Response> accountPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/accounts/{path: .*}")
    public Uni<Response> accountDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "DELETE", body, headers);
    }

    @POST @Path("/accounts")
    public Uni<Response> accountRootPost(String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts", "POST", body, headers);
    }
    @GET @Path("/accounts")
    public Uni<Response> accountRootGet(String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts", "GET", body, headers);
    }

    // ==================== Wallet Service ====================
    @GET @Path("/wallets/{path: .*}")
    public Uni<Response> walletGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "GET", body, headers);
    }
    @POST @Path("/wallets/{path: .*}")
    public Uni<Response> walletPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "POST", body, headers);
    }
    @PUT @Path("/wallets/{path: .*}")
    public Uni<Response> walletPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/wallets/{path: .*}")
    public Uni<Response> walletDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "DELETE", body, headers);
    }

    @POST @Path("/wallets")
    public Uni<Response> walletRootPost(String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets", "POST", body, headers);
    }
    @GET @Path("/wallets")
    public Uni<Response> walletRootGet(String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets", "GET", body, headers);
    }

    // ==================== Transaction Service ====================
    @GET @Path("/transactions/{path: .*}")
    public Uni<Response> transactionGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, "GET", body, headers);
    }
    @POST @Path("/transactions/{path: .*}")
    public Uni<Response> transactionPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, "POST", body, headers);
    }

    @GET @Path("/transactions")
    public Uni<Response> transactionRootGet(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions", "GET", body, headers);
    }
    @PUT @Path("/transactions/{path: .*}")
    public Uni<Response> transactionPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/transactions/{path: .*}")
    public Uni<Response> transactionDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, "DELETE", body, headers);
    }

    @POST @Path("/transactions")
    public Uni<Response> transactionRootPost(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions", "POST", body, headers);
    }

    // ==================== Disbursement Service (via Transaction Service) ====================
    @GET @Path("/disbursements/{path: .*}")
    public Uni<Response> disbursementGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/disbursements/" + path, "GET", body, headers);
    }
    @POST @Path("/disbursements/{path: .*}")
    public Uni<Response> disbursementPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/disbursements/" + path, "POST", body, headers);
    }
    @PUT @Path("/disbursements/{path: .*}")
    public Uni<Response> disbursementPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/disbursements/" + path, "PUT", body, headers);
    }
    @GET @Path("/disbursements")
    public Uni<Response> disbursementRootGet(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/disbursements", "GET", body, headers);
    }
    @POST @Path("/disbursements")
    public Uni<Response> disbursementRootPost(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/disbursements", "POST", body, headers);
    }

    // ==================== Virtual Account Service (via Transaction Service) ====================
    @GET @Path("/payments/va/{path: .*}")
    public Uni<Response> virtualAccountGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/payments/va/" + path, "GET", body, headers);
    }
    @POST @Path("/payments/va/{path: .*}")
    public Uni<Response> virtualAccountPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/payments/va/" + path, "POST", body, headers);
    }
    @PUT @Path("/payments/va/{path: .*}")
    public Uni<Response> virtualAccountPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/payments/va/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/payments/va/{path: .*}")
    public Uni<Response> virtualAccountDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/payments/va/" + path, "DELETE", body, headers);
    }
    @GET @Path("/payments/va")
    public Uni<Response> virtualAccountRootGet(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/payments/va", "GET", body, headers);
    }
    @POST @Path("/payments/va")
    public Uni<Response> virtualAccountRootPost(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/payments/va", "POST", body, headers);
    }

    // ==================== Billing Service ====================
    @GET @Path("/billers/{path: .*}")
    public Uni<Response> billerGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/billers/" + path, "GET", body, headers);
    }
    @GET @Path("/billers")
    public Uni<Response> billerRootGet(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/billers", "GET", body, headers);
    }

    @GET @Path("/payments/{path: .*}")
    public Uni<Response> paymentGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/payments/" + path, "GET", body, headers);
    }
    @POST @Path("/payments/{path: .*}")
    public Uni<Response> paymentPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/payments/" + path, "POST", body, headers);
    }
    @POST @Path("/payments")
    public Uni<Response> paymentRootPost(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/payments", "POST", body, headers);
    }

    // ==================== Notification Service ====================
    @GET @Path("/notifications/{path: .*}")
    public Uni<Response> notificationGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("notification-service", "/api/v1/notifications/" + path, "GET", body, headers);
    }
    @GET @Path("/notifications")
    public Uni<Response> notificationRootGet(String body, @Context HttpHeaders headers) {
        return proxy("notification-service", "/api/v1/notifications", "GET", body, headers);
    }
    @POST @Path("/notifications")
    public Uni<Response> notificationRootPost(String body, @Context HttpHeaders headers) {
        return proxy("notification-service", "/api/v1/notifications", "POST", body, headers);
    }
    @POST @Path("/notifications/{path: .*}")
    public Uni<Response> notificationPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("notification-service", "/api/v1/notifications/" + path, "POST", body, headers);
    }

    // ==================== Card Service ====================
    @GET @Path("/cards/{path: .*}")
    public Uni<Response> cardGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "GET", body, headers);
    }
    @POST @Path("/cards/{path: .*}")
    public Uni<Response> cardPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "POST", body, headers);
    }
    @PUT @Path("/cards/{path: .*}")
    public Uni<Response> cardPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "PUT", body, headers);
    }

    @DELETE @Path("/cards/{path: .*}")
    public Uni<Response> cardDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "DELETE", body, headers);
    }

    @GET @Path("/cards")
    public Uni<Response> cardRootGet(String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards", "GET", body, headers);
    }
    @POST @Path("/cards")
    public Uni<Response> cardRootPost(String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards", "POST", body, headers);
    }

    // ==================== Auth Service ====================
    @POST @Path("/auth/{path: .*}")
    public Uni<Response> authPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("auth-service", "/api/v1/auth/" + path, "POST", body, headers);
    }
    @GET @Path("/auth/{path: .*}")
    public Uni<Response> authGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("auth-service", "/api/v1/auth/" + path, "GET", body, headers);
    }

    // ==================== Partner Service ====================
    @GET @Path("/partners/{path: .*}")
    public Uni<Response> partnerGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/partners/" + path, "GET", body, headers);
    }
    @POST @Path("/partners/{path: .*}")
    public Uni<Response> partnerPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/partners/" + path, "POST", body, headers);
    }
    @PUT @Path("/partners/{path: .*}")
    public Uni<Response> partnerPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/partners/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/partners/{path: .*}")
    public Uni<Response> partnerDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/partners/" + path, "DELETE", body, headers);
    }

    @GET @Path("/partners")
    public Uni<Response> partnerRootGet(String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/partners", "GET", body, headers);
    }
    @POST @Path("/partners")
    public Uni<Response> partnerRootPost(String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/partners", "POST", body, headers);
    }

    @POST @Path("/v1/partner/{path: .*}")
    public Uni<Response> snapPartnerPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/v1/partner/" + path, "POST", body, headers);
    }
    @GET @Path("/v1/partner/{path: .*}")
    public Uni<Response> snapPartnerGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("partner-service", "/v1/partner/" + path, "GET", body, headers);
    }

    // ==================== Partner Statement API (ADR-002: Dual Format) ====================
    @GET @Path("/v1/partner/statements")
    public Uni<Response> partnerStatements(String body, @Context HttpHeaders headers) {
        return proxy("statement-service", "/api/v1/statements", "GET", body, headers);
    }
    @POST @Path("/v1/partner/statements/generate")
    public Uni<Response> partnerStatementGenerate(String body, @Context HttpHeaders headers) {
        return proxy("statement-service", "/api/v1/statements/generate", "POST", body, headers);
    }

    // ==================== Promotion Service ====================
    @GET @Path("/promotions/{path: .*}")
    public Uni<Response> promotionGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/promotions/" + path, "GET", body, headers);
    }
    @POST @Path("/promotions/{path: .*}")
    public Uni<Response> promotionPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/promotions/" + path, "POST", body, headers);
    }
    @PUT @Path("/promotions/{path: .*}")
    public Uni<Response> promotionPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/promotions/" + path, "PUT", body, headers);
    }

    @GET @Path("/promotions")
    public Uni<Response> promotionRootGet(String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/promotions", "GET", body, headers);
    }
    @POST @Path("/promotions")
    public Uni<Response> promotionRootPost(String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/promotions", "POST", body, headers);
    }

    // Cashbacks
    @GET @Path("/cashbacks/{path: .*}")
    public Uni<Response> cashbackGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/cashbacks/" + path, "GET", body, headers);
    }
    @POST @Path("/cashbacks/{path: .*}")
    public Uni<Response> cashbackPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/cashbacks/" + path, "POST", body, headers);
    }

    @POST @Path("/cashbacks")
    public Uni<Response> cashbackRootPost(String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/cashbacks", "POST", body, headers);
    }

    // Loyalty Points
    @GET @Path("/loyalty-points/{path: .*}")
    public Uni<Response> loyaltyPointsGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/loyalty-points/" + path, "GET", body, headers);
    }
    @POST @Path("/loyalty-points/{path: .*}")
    public Uni<Response> loyaltyPointsPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/loyalty-points/" + path, "POST", body, headers);
    }

    @POST @Path("/loyalty-points")
    public Uni<Response> loyaltyPointsRootPost(String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/loyalty-points", "POST", body, headers);
    }

    // Rewards
    @GET @Path("/rewards/{path: .*}")
    public Uni<Response> rewardGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/rewards/" + path, "GET", body, headers);
    }

    // Referrals
    @GET @Path("/referrals/{path: .*}")
    public Uni<Response> referralGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/referrals/" + path, "GET", body, headers);
    }
    @POST @Path("/referrals/{path: .*}")
    public Uni<Response> referralPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/referrals/" + path, "POST", body, headers);
    }

    @POST @Path("/referrals")
    public Uni<Response> referralRootPost(String body, @Context HttpHeaders headers) {
        return proxy("promotion-service", "/api/v1/referrals", "POST", body, headers);
    }

    // ==================== Lending Service ====================
    @GET @Path("/lending/{path: .*}")
    public Uni<Response> lendingGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("lending-service", "/api/v1/lending/" + path, "GET", body, headers);
    }
    @POST @Path("/lending/{path: .*}")
    public Uni<Response> lendingPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("lending-service", "/api/v1/lending/" + path, "POST", body, headers);
    }

    @GET @Path("/lending")
    public Uni<Response> lendingRootGet(String body, @Context HttpHeaders headers) {
        return proxy("lending-service", "/api/v1/lending", "GET", body, headers);
    }

    // ==================== Investment Service ====================
    @GET @Path("/investments/{path: .*}")
    public Uni<Response> investmentGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("investment-service", "/api/v1/investments/" + path, "GET", body, headers);
    }
    @POST @Path("/investments/{path: .*}")
    public Uni<Response> investmentPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("investment-service", "/api/v1/investments/" + path, "POST", body, headers);
    }

    @GET @Path("/investments")
    public Uni<Response> investmentRootGet(String body, @Context HttpHeaders headers) {
        return proxy("investment-service", "/api/v1/investments", "GET", body, headers);
    }

    // ==================== Compliance Service ====================
    @GET @Path("/compliance/{path: .*}")
    public Uni<Response> complianceGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("compliance-service", "/api/v1/compliance/" + path, "GET", body, headers);
    }
    @POST @Path("/compliance/{path: .*}")
    public Uni<Response> compliancePost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("compliance-service", "/api/v1/compliance/" + path, "POST", body, headers);
    }

    // ==================== Backoffice Service ====================
    @GET @Path("/backoffice/{path: .*}")
    public Uni<Response> backofficeGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("backoffice-service", "/api/v1/backoffice/" + path, "GET", body, headers);
    }
    @POST @Path("/backoffice/{path: .*}")
    public Uni<Response> backofficePost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("backoffice-service", "/api/v1/backoffice/" + path, "POST", body, headers);
    }
    @PUT @Path("/backoffice/{path: .*}")
    public Uni<Response> backofficePut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("backoffice-service", "/api/v1/backoffice/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/backoffice/{path: .*}")
    public Uni<Response> backofficeDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("backoffice-service", "/api/v1/backoffice/" + path, "DELETE", body, headers);
    }

    @POST @Path("/backoffice")
    public Uni<Response> backofficeRootPost(String body, @Context HttpHeaders headers) {
        return proxy("backoffice-service", "/api/v1/backoffice", "POST", body, headers);
    }
    @GET @Path("/backoffice")
    public Uni<Response> backofficeRootGet(String body, @Context HttpHeaders headers) {
        return proxy("backoffice-service", "/api/v1/backoffice", "GET", body, headers);
    }

    // ==================== Analytics Service ====================
    @GET @Path("/analytics/{path: .*}")
    public Uni<Response> analyticsGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("analytics-service", "/api/v1/analytics/" + path, "GET", body, headers);
    }
    @POST @Path("/analytics/{path: .*}")
    public Uni<Response> analyticsPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("analytics-service", "/api/v1/analytics/" + path, "POST", body, headers);
    }

    @GET @Path("/analytics")
    public Uni<Response> analyticsRootGet(String body, @Context HttpHeaders headers) {
        return proxy("analytics-service", "/api/v1/analytics", "GET", body, headers);
    }

    // ==================== Support Service ====================
    @GET @Path("/support/{path: .*}")
    public Uni<Response> supportGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("support-service", "/api/v1/support/" + path, "GET", body, headers);
    }
    @POST @Path("/support/{path: .*}")
    public Uni<Response> supportPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("support-service", "/api/v1/support/" + path, "POST", body, headers);
    }
    @PATCH @Path("/support/{path: .*}")
    public Uni<Response> supportPatch(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("support-service", "/api/v1/support/" + path, "PATCH", body, headers);
    }

    @GET @Path("/support")
    public Uni<Response> supportRootGet(String body, @Context HttpHeaders headers) {
        return proxy("support-service", "/api/v1/support", "GET", body, headers);
    }
    @POST @Path("/support")
    public Uni<Response> supportRootPost(String body, @Context HttpHeaders headers) {
        return proxy("support-service", "/api/v1/support", "POST", body, headers);
    }

    // ==================== CMS Service ====================
    @GET @Path("/contents/{path: .*}")
    public Uni<Response> contentGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "GET", body, headers);
    }
    @POST @Path("/contents/{path: .*}")
    public Uni<Response> contentPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "POST", body, headers);
    }
    @PUT @Path("/contents/{path: .*}")
    public Uni<Response> contentPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/contents/{path: .*}")
    public Uni<Response> contentDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "DELETE", body, headers);
    }
    @PATCH @Path("/contents/{path: .*}")
    public Uni<Response> contentPatch(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "PATCH", body, headers);
    }

    @GET @Path("/contents")
    public Uni<Response> contentRootGet(String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents", "GET", body, headers);
    }
    @POST @Path("/contents")
    public Uni<Response> contentRootPost(String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents", "POST", body, headers);
    }

    // Public CMS endpoints (no auth required)
    @GET @Path("/public/contents/{path: .*}")
    public Uni<Response> publicContentGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/public/contents/" + path, "GET", body, headers);
    }

    @GET @Path("/public/contents")
    public Uni<Response> publicContentRootGet(String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/public/contents", "GET", body, headers);
    }

    // ==================== Dispute Service ====================
    @GET @Path("/disputes/{path: .*}")
    public Uni<Response> disputeGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/disputes/" + path, "GET", body, headers);
    }
    @POST @Path("/disputes/{path: .*}")
    public Uni<Response> disputePost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/disputes/" + path, "POST", body, headers);
    }
    @PUT @Path("/disputes/{path: .*}")
    public Uni<Response> disputePut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/disputes/" + path, "PUT", body, headers);
    }
    @GET @Path("/disputes")
    public Uni<Response> disputeRootGet(String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/disputes", "GET", body, headers);
    }
    @POST @Path("/disputes")
    public Uni<Response> disputeRootPost(String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/disputes", "POST", body, headers);
    }

    // ==================== Refund Service (via Dispute Service) ====================
    @GET @Path("/refunds/{path: .*}")
    public Uni<Response> refundGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/refunds/" + path, "GET", body, headers);
    }
    @POST @Path("/refunds/{path: .*}")
    public Uni<Response> refundPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/refunds/" + path, "POST", body, headers);
    }
    @GET @Path("/refunds")
    public Uni<Response> refundRootGet(String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/refunds", "GET", body, headers);
    }
    @POST @Path("/refunds")
    public Uni<Response> refundRootPost(String body, @Context HttpHeaders headers) {
        return proxy("dispute-service", "/api/v1/refunds", "POST", body, headers);
    }

    // ==================== FX Service ====================
    @GET @Path("/fx/{path: .*}")
    public Uni<Response> fxGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("fx-service", "/v1/" + path, "GET", body, headers);
    }
    @POST @Path("/fx/{path: .*}")
    public Uni<Response> fxPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("fx-service", "/v1/" + path, "POST", body, headers);
    }
    @GET @Path("/fx")
    public Uni<Response> fxRootGet(String body, @Context HttpHeaders headers) {
        return proxy("fx-service", "/v1", "GET", body, headers);
    }

    // ==================== KYC Service ====================
    @GET @Path("/kyc/{path: .*}")
    public Uni<Response> kycGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("kyc-service", "/api/v1/kyc/" + path, "GET", body, headers);
    }
    @POST @Path("/kyc/{path: .*}")
    public Uni<Response> kycPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("kyc-service", "/api/v1/kyc/" + path, "POST", body, headers);
    }
    @GET @Path("/kyc")
    public Uni<Response> kycRootGet(String body, @Context HttpHeaders headers) {
        return proxy("kyc-service", "/api/v1/kyc", "GET", body, headers);
    }
    @POST @Path("/kyc")
    public Uni<Response> kycRootPost(String body, @Context HttpHeaders headers) {
        return proxy("kyc-service", "/api/v1/kyc", "POST", body, headers);
    }

    // ==================== Partner KYC API (ADR-001: Hybrid KYC) ====================
    @POST @Path("/v1/partner/kyc/verify")
    public Uni<Response> partnerKycVerify(String body, @Context HttpHeaders headers) {
        return proxy("kyc-service", "/api/v1/kyc/verify/start", "POST", body, headers);
    }
    @GET @Path("/v1/partner/kyc/{id}")
    public Uni<Response> partnerKycStatus(@PathParam("id") String id, String body, @Context HttpHeaders headers) {
        return proxy("kyc-service", "/api/v1/kyc/verify/" + id, "GET", body, headers);
    }

    // ==================== Statement Service ====================
    @GET @Path("/statements/{path: .*}")
    public Uni<Response> statementGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("statement-service", "/api/v1/statements/" + path, "GET", body, headers);
    }
    @POST @Path("/statements/{path: .*}")
    public Uni<Response> statementPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("statement-service", "/api/v1/statements/" + path, "POST", body, headers);
    }
    @GET @Path("/statements")
    public Uni<Response> statementRootGet(String body, @Context HttpHeaders headers) {
        return proxy("statement-service", "/api/v1/statements", "GET", body, headers);
    }
    @POST @Path("/statements")
    public Uni<Response> statementRootPost(String body, @Context HttpHeaders headers) {
        return proxy("statement-service", "/api/v1/statements", "POST", body, headers);
    }

    // ==================== Subscription Service (via Billing) ====================
    @GET @Path("/subscriptions/{path: .*}")
    public Uni<Response> subscriptionGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/subscriptions/" + path, "GET", body, headers);
    }
    @POST @Path("/subscriptions/{path: .*}")
    public Uni<Response> subscriptionPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/subscriptions/" + path, "POST", body, headers);
    }
    @GET @Path("/subscriptions")
    public Uni<Response> subscriptionRootGet(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/subscriptions", "GET", body, headers);
    }
    @POST @Path("/subscriptions")
    public Uni<Response> subscriptionRootPost(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/subscriptions", "POST", body, headers);
    }

    // ==================== TopUp Service (via Billing) ====================
    @GET @Path("/topup/{path: .*}")
    public Uni<Response> topupGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/topup/" + path, "GET", body, headers);
    }
    @POST @Path("/topup/{path: .*}")
    public Uni<Response> topupPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/topup/" + path, "POST", body, headers);
    }
    @GET @Path("/topup")
    public Uni<Response> topupRootGet(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/topup", "GET", body, headers);
    }
    @POST @Path("/topup")
    public Uni<Response> topupRootPost(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/topup", "POST", body, headers);
    }

    // ==================== Billing Service - TopUp via /billing prefix ====================
    @GET @Path("/billing/topup/{path: .*}")
    public Uni<Response> billingTopupGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/topup/" + path, "GET", body, headers);
    }
    @POST @Path("/billing/topup/{path: .*}")
    public Uni<Response> billingTopupPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/topup/" + path, "POST", body, headers);
    }

    // ==================== CMS Service (alternate /cms prefix) ====================
    @GET @Path("/cms/{path: .*}")
    public Uni<Response> cmsGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "GET", body, headers);
    }
    @POST @Path("/cms/{path: .*}")
    public Uni<Response> cmsPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "POST", body, headers);
    }
    @PUT @Path("/cms/{path: .*}")
    public Uni<Response> cmsPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/cms/{path: .*}")
    public Uni<Response> cmsDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents/" + path, "DELETE", body, headers);
    }
    @GET @Path("/cms")
    public Uni<Response> cmsRootGet(String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents", "GET", body, headers);
    }
    @POST @Path("/cms")
    public Uni<Response> cmsRootPost(String body, @Context HttpHeaders headers) {
        return proxy("cms-service", "/api/v1/contents", "POST", body, headers);
    }

    // ==================== Product Catalog Service ====================
    @GET @Path("/products/{path: .*}")
    public Uni<Response> productGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("product-catalog-service", "/products/" + path, "GET", body, headers);
    }
    @GET @Path("/products")
    public Uni<Response> productRootGet(String body, @Context HttpHeaders headers) {
        return proxy("product-catalog-service", "/products", "GET", body, headers);
    }
    @POST @Path("/products")
    public Uni<Response> productRootPost(String body, @Context HttpHeaders headers) {
        return proxy("product-catalog-service", "/products", "POST", body, headers);
    }

    // ==================== Integration Service ====================
    @GET @Path("/integration/{path: .*}")
    public Uni<Response> integrationGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("integration-service", "/api/v1/integration/" + path, "GET", body, headers);
    }
    @POST @Path("/integration/{path: .*}")
    public Uni<Response> integrationPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("integration-service", "/api/v1/integration/" + path, "POST", body, headers);
    }

    @GET @Path("/integration")
    public Uni<Response> integrationRootGet(String body, @Context HttpHeaders headers) {
        return proxy("integration-service", "/api/v1/integration", "GET", body, headers);
    }


    // ==================== Dynamic Route (IMP-007) ====================
    // Uses RouteRegistry to match path prefix → backend service dynamically.
    // This allows adding new routes via YAML config without code changes.

    /**
     * Dynamic route resolution for any path registered in RouteRegistry.
     * Falls back to 404 if no route matches.
     */
    private Uni<Response> dynamicRoute(String path, String method, String body, HttpHeaders headers) {
        Optional<RouteRegistry.ResolvedRoute> resolved = routeRegistry.resolve(path);
        if (resolved.isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(404)
                            .entity("{\"error\":\"NOT_FOUND\",\"message\":\"No route found for path: /api/v1/" + path + "\",\"status\":404}")
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }

        RouteRegistry.ResolvedRoute route = resolved.get();

        // Check if HTTP method is allowed
        if (!route.definition().methods().contains(method)) {
            return Uni.createFrom().item(
                    Response.status(405)
                            .entity("{\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Method " + method + " not allowed\",\"status\":405}")
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }

        return proxy(route.serviceName(), route.targetPath(), method, body, headers);
    }

    // ==================== Proxy Logic ====================
    private Uni<Response> proxy(String serviceName, String path, String method,
                                 String body, HttpHeaders headers) {

        GatewayConfig.ServiceConfig serviceConfig = config.services().get(serviceName);
        if (serviceConfig == null) {
            String errorMsg = String.format("Service %s not configured in gateway", serviceName);
            Log.error(errorMsg);
            return Uni.createFrom().item(Response.status(502).entity(errorMsg).build());
        }

        // Capture query string from the incoming request and append to downstream path
        String queryString = (uriInfo != null) ? uriInfo.getRequestUri().getRawQuery() : null;
        String fullPath = (queryString != null && !queryString.isEmpty())
                ? path + "?" + queryString
                : path;

        // Wrap the actual call with circuit breaker + retry + timeout
        return circuitBreakerService.execute(serviceName, () -> {
            Uni<Response> call = doProxy(serviceName, serviceConfig, fullPath, method, body, headers);

            // Apply retry with backoff
            call = retryAndTimeoutService.executeWithRetry(serviceName, call);

            // Apply timeout
            Duration timeout = retryAndTimeoutService.getTimeout(serviceName);
            call = call.ifNoItem().after(timeout).fail();

            return call;
        });
    }

    /**
     * Performs the actual HTTP proxy call via Vert.x WebClient.
     */
    private Uni<Response> doProxy(String serviceName, GatewayConfig.ServiceConfig serviceConfig,
                                   String path, String method, String body, HttpHeaders headers) {

        String baseUrl = serviceConfig.url();
        URI targetUri = URI.create(baseUrl);

        Log.infof("Proxying to %s: %s %s%s", serviceName, method, baseUrl, path);

        var request = webClient.request(
            io.vertx.core.http.HttpMethod.valueOf(method),
            targetUri.getPort() != -1 ? targetUri.getPort() : 80,
            targetUri.getHost(),
            path
        );

        // Forward matching headers (simple version)
        if (headers != null) {
            headers.getRequestHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Host") && !k.equalsIgnoreCase("Content-Length")) {
                    request.putHeader(k, v);
                }
            });
        }

        // Forward tenant ID from filter context
        String tenantId = headers.getHeaderString(TenantFilter.TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            request.putHeader(TenantFilter.TENANT_ID_HEADER, tenantId);
        }

        request.putHeader("X-Forwarded-Host", "localhost:8080");

        Uni<HttpResponse<Buffer>> responseUni;
        if (body != null && !body.isBlank()) {
            responseUni = request.sendBuffer(Buffer.buffer(body));
        } else {
            responseUni = request.send();
        }

        return responseUni.map(response -> {
            Response.ResponseBuilder builder = Response.status(response.statusCode());

            if (response.body() != null) {
                builder.entity(response.bodyAsString());
            }

            response.headers().forEach(entry -> {
                 if (!entry.getKey().equalsIgnoreCase("Transfer-Encoding")) {
                    builder.header(entry.getKey(), entry.getValue());
                 }
            });

            return builder.build();
        }).onFailure().recoverWithItem(t -> {
            Log.errorf("Failed proxy to %s: %s", serviceName, t.getMessage());
            return Response.status(503).entity("Service Unavailable").build();
        });
    }
}
