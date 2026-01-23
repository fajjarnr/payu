package id.payu.billing.resource;

import id.payu.billing.domain.BillPayment;
import id.payu.billing.dto.TopUpRequest;
import id.payu.billing.dto.TopUpResponse;
import id.payu.billing.service.PaymentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/api/v1/topup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TopUpResource {

    private static final Logger LOG = Logger.getLogger(TopUpResource.class);

    @Inject
    PaymentService paymentService;

    @POST
    public Response createTopUp(@Valid TopUpRequest request) {
        LOG.infof("Received top-up request: provider=%s, walletNumber=%s, amount=%s",
            request.provider(), request.walletNumber(), request.amount());

        try {
            BillPayment payment = paymentService.createTopUp(request);
            return Response.status(Response.Status.CREATED)
                .entity(TopUpResponse.from(payment))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getTopUp(@PathParam("id") UUID id) {
        return paymentService.getPayment(id)
            .map(payment -> Response.ok(TopUpResponse.from(payment)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Top-up not found"))
                .build());
    }

    @GET
    @Path("/reference/{referenceNumber}")
    public Response getTopUpByReference(@PathParam("referenceNumber") String referenceNumber) {
        return BillPayment.<BillPayment>find("referenceNumber", referenceNumber)
            .firstResultOptional()
            .map(payment -> Response.ok(TopUpResponse.from(payment)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Top-up not found"))
                .build());
    }

    @GET
    @Path("/providers")
    public Response getProviders() {
        return Response.ok(
            new java.util.ArrayList<>() {{
                add(new ProviderInfo("GOPAY", "GoPay"));
                add(new ProviderInfo("OVO", "OVO"));
                add(new ProviderInfo("DANA", "DANA"));
                add(new ProviderInfo("LINKAJA", "LinkAja"));
            }}
        ).build();
    }

    record ErrorResponse(String message) {}

    record ProviderInfo(String code, String name) {}
}