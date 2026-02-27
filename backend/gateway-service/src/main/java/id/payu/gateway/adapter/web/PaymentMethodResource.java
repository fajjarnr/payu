package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.PaymentMethodService;
import id.payu.gateway.dto.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resource for payment method selection.
 * Returns available payment methods with eligibility, fees, and settlement times.
 *
 * Part of E-15 IMP-041: Payment Method Selection API
 */
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PaymentMethodResource {

    @Inject
    PaymentMethodService paymentMethodService;

    @GET
    @Path("/methods")
    public Response getPaymentMethods(
            @QueryParam("amount") BigDecimal amount,
            @QueryParam("currency") @DefaultValue("IDR") String currency,
            @QueryParam("kycStatus") String kycStatus,
            @QueryParam("userId") String userId,
            @QueryParam("partnerId") String partnerId) {

        Log.infof("GET /payments/methods amount=%s currency=%s", amount, currency);

        PaymentMethodService.PaymentContext context = new PaymentMethodService.PaymentContext(
                amount, currency, kycStatus, userId, partnerId
        );

        List<PaymentMethodService.PaymentMethodInfo> methods = paymentMethodService.getAvailableMethods(context);

        return Response.ok(ApiResponse.success(methods)).build();
    }
}
