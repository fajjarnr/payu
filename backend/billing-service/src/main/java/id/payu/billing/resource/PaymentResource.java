package id.payu.billing.resource;

import id.payu.billing.domain.BillPayment;
import id.payu.billing.dto.CreatePaymentRequest;
import id.payu.billing.dto.PaymentResponse;
import id.payu.billing.service.PaymentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * REST resource for bill payments.
 */
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = Logger.getLogger(PaymentResource.class);

    @Inject
    PaymentService paymentService;

    @POST
    public Response createPayment(@Valid CreatePaymentRequest request) {
        LOG.infof("Received payment request: biller=%s, customerId=%s", 
            request.billerCode(), request.customerId());

        try {
            BillPayment payment = paymentService.createPayment(request);
            return Response.status(Response.Status.CREATED)
                .entity(PaymentResponse.from(payment))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getPayment(@PathParam("id") UUID id) {
        return paymentService.getPayment(id)
            .map(payment -> Response.ok(PaymentResponse.from(payment)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Payment not found"))
                .build());
    }

    @GET
    @Path("/reference/{referenceNumber}")
    public Response getPaymentByReference(@PathParam("referenceNumber") String referenceNumber) {
        return BillPayment.<BillPayment>find("referenceNumber", referenceNumber)
            .firstResultOptional()
            .map(payment -> Response.ok(PaymentResponse.from(payment)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Payment not found"))
                .build());
    }

    record ErrorResponse(String message) {}
}
