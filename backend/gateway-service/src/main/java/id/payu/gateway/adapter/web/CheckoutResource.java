package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.CheckoutService;
import id.payu.gateway.application.service.PaymentMethodService;
import id.payu.gateway.dto.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * Hosted checkout resource (Snap-style).
 * Partners create checkout tokens, customers use them to select payment methods and pay.
 *
 * Part of E-15 IMP-043: Hosted Checkout Page
 */
@Path("/api/v1/checkout")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CheckoutResource {

    @Inject
    CheckoutService checkoutService;

    @Inject
    PaymentMethodService paymentMethodService;

    /**
     * Create a checkout token for hosted checkout.
     * Returns token + checkout URL for iframe or redirect.
     */
    @POST
    @Path("/tokens")
    public Response createCheckoutToken(CheckoutService.CreateCheckoutRequest request) {
        Log.infof("POST /checkout/tokens partner=%s order=%s", request.partnerId(), request.orderId());

        CheckoutService.CheckoutSession session = checkoutService.createCheckoutToken(request);

        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(session))
                .build();
    }

    /**
     * Get checkout session details (used by the checkout page).
     */
    @GET
    @Path("/tokens/{token}")
    public Response getCheckoutSession(@PathParam("token") String token) {
        CheckoutService.CheckoutSession session = checkoutService.getSession(token);

        // Also return available payment methods
        PaymentMethodService.PaymentContext ctx = new PaymentMethodService.PaymentContext(
                session.amount(), session.currency(), null, null, session.partnerId()
        );
        List<PaymentMethodService.PaymentMethodInfo> methods = paymentMethodService.getAvailableMethods(ctx);

        Map<String, Object> result = Map.of(
                "session", session,
                "paymentMethods", methods
        );

        return Response.ok(ApiResponse.success(result)).build();
    }

    /**
     * Complete checkout — select payment method and finalize.
     */
    @POST
    @Path("/tokens/{token}/pay")
    public Response completeCheckout(
            @PathParam("token") String token,
            Map<String, String> payload) {

        String paymentMethod = payload.get("paymentMethod");
        String paymentReference = payload.get("paymentReference");

        if (paymentMethod == null || paymentMethod.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(
                            new id.payu.gateway.dto.ApiError("BAD_REQUEST",
                                    "paymentMethod is required", null, 400, null, null, null)))
                    .build();
        }

        CheckoutService.CheckoutSession completed = checkoutService.completeCheckout(
                token, paymentMethod, paymentReference);

        return Response.ok(ApiResponse.success(completed)).build();
    }

    /**
     * Serve a simple HTML checkout page (server-rendered).
     * In production this would be a full Thymeleaf/Qute template.
     */
    @GET
    @Path("/page/{token}")
    @Produces(MediaType.TEXT_HTML)
    public Response checkoutPage(@PathParam("token") String token) {
        try {
            CheckoutService.CheckoutSession session = checkoutService.getSession(token);

            String html = """
                <!DOCTYPE html>
                <html>
                <head><title>PayU Checkout</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: -apple-system, sans-serif; max-width: 480px; margin: 40px auto; padding: 0 20px; }
                    .amount { font-size: 2em; font-weight: bold; color: #1a73e8; }
                    .method { padding: 12px; margin: 8px 0; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; }
                    .method:hover { background: #f0f4ff; border-color: #1a73e8; }
                    .btn { background: #1a73e8; color: white; border: none; padding: 14px; width: 100%%; border-radius: 8px; font-size: 1.1em; cursor: pointer; }
                </style></head>
                <body>
                    <h2>PayU Checkout</h2>
                    <p>%s</p>
                    <p class="amount">%s %s</p>
                    <p>Order: %s</p>
                    <hr>
                    <h3>Select Payment Method</h3>
                    <div class="method">💳 PayU Wallet</div>
                    <div class="method">🏦 BCA Virtual Account</div>
                    <div class="method">🏦 BNI Virtual Account</div>
                    <div class="method">📱 QRIS</div>
                    <div class="method">🔄 PayLater</div>
                    <br>
                    <button class="btn">Pay Now</button>
                    <p style="text-align:center;color:#888;margin-top:20px">Secured by PayU</p>
                </body></html>
                """.formatted(
                    session.itemName() != null ? session.itemName() : session.orderId(),
                    session.currency(),
                    session.amount().toPlainString(),
                    session.orderId()
            );

            return Response.ok(html).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("<html><body><h2>Checkout not found</h2></body></html>")
                    .build();
        }
    }
}
