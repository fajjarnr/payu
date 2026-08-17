package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.PaymentMethodService;
import id.payu.gateway.interfaces.dto.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resource for payment method selection.
 * Returns available payment methods with eligibility, fees, and settlement times.
 *
 * Part of E-15 IMP-041: Payment Method Selection API
 */
@Path("/api/v1/payments/methods")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Payment Methods", description = "Payment method selection and eligibility")
public class PaymentMethodResource {

    @Inject
    PaymentMethodService paymentMethodService;

    @GET
    @Operation(summary = "Get available payment methods", description = "Returns available payment methods with eligibility, fees, and settlement times based on payment context")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Payment methods retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodService.PaymentMethodInfo.class))),
        @APIResponse(responseCode = "400", description = "Invalid request parameters"),
        @APIResponse(responseCode = "401", description = "Authentication required")
    })
    public Response getPaymentMethods(
            @Parameter(description = "Payment amount", example = "100000.00") @QueryParam("amount") BigDecimal amount,
            @Parameter(description = "Currency code (ISO 4217)", example = "IDR") @QueryParam("currency") @DefaultValue("IDR") String currency,
            @Parameter(description = "KYC status of user") @QueryParam("kycStatus") String kycStatus,
            @Parameter(description = "User ID") @QueryParam("userId") String userId,
            @Parameter(description = "Partner ID for partner-specific methods") @QueryParam("partnerId") String partnerId) {

        Log.infof("GET /payments/methods amount=%s currency=%s", amount, currency);

        PaymentMethodService.PaymentContext context = new PaymentMethodService.PaymentContext(
                amount, currency, kycStatus, userId, partnerId
        );

        List<PaymentMethodService.PaymentMethodInfo> methods = paymentMethodService.getAvailableMethods(context);

        return Response.ok(ApiResponse.success(methods)).build();
    }
}
