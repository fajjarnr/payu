package id.payu.portal.resource;

import id.payu.portal.dto.SandboxPaymentRequest;
import id.payu.portal.dto.SandboxPaymentResponse;
import id.payu.portal.dto.SandboxPaymentStatusResponse;
import id.payu.portal.dto.SandboxRefundRequest;
import id.payu.portal.dto.SandboxRefundResponse;
import id.payu.portal.service.SandboxService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/sandbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Partner Sandbox", description = "Partner Sandbox Environment for testing with mock data and simulated latencies")
public class SandboxResource {

    @Inject
    SandboxService sandboxService;

    @POST
    @Path("/payments")
    @Operation(summary = "Create a sandbox payment", description = "Creates a mock payment with simulated processing time")
    public Uni<Response> createPayment(SandboxPaymentRequest request) {
        return sandboxService.createPayment(request)
            .onItem().transform(response -> {
                if (response == null) {
                    return Response.serverError()
                        .entity("{\"error\":\"Failed to create sandbox payment\"}")
                        .build();
                }
                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(t -> {
                Log.errorf("Error creating sandbox payment: %s", t.getMessage());
                return Response.serverError()
                    .entity("{\"error\":\"Internal server error\"}")
                    .build();
            });
    }

    @GET
    @Path("/payments/{paymentReferenceNo}")
    @Operation(summary = "Get sandbox payment status", description = "Retrieves the status of a mock payment")
    public Uni<Response> getPaymentStatus(@PathParam("paymentReferenceNo") String paymentReferenceNo) {
        return sandboxService.getPaymentStatus(paymentReferenceNo)
            .onItem().transform(response -> {
                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Payment not found\"}")
                        .build();
                }
                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(t -> {
                Log.errorf("Error getting sandbox payment status: %s", t.getMessage());
                return Response.serverError()
                    .entity("{\"error\":\"Internal server error\"}")
                    .build();
            });
    }

    @POST
    @Path("/payments/{paymentReferenceNo}/refund")
    @Operation(summary = "Create a sandbox refund", description = "Creates a mock refund for a payment with simulated processing time")
    public Uni<Response> createRefund(
        @PathParam("paymentReferenceNo") String paymentReferenceNo,
        SandboxRefundRequest request) {
        return sandboxService.createRefund(paymentReferenceNo, request)
            .onItem().transform(response -> {
                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Original payment not found\"}")
                        .build();
                }
                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(t -> {
                Log.errorf("Error creating sandbox refund: %s", t.getMessage());
                return Response.serverError()
                    .entity("{\"error\":\"Internal server error\"}")
                    .build();
            });
    }

    @DELETE
    @Path("/data")
    @Operation(summary = "Clear sandbox data", description = "Clears all mock payments and refunds")
    public Uni<Response> clearData() {
        return sandboxService.clearData()
            .onItem().transform(ignored -> Response.ok("{\"message\":\"Sandbox data cleared\"}").build())
            .onFailure().recoverWithItem(t -> {
                Log.errorf("Error clearing sandbox data: %s", t.getMessage());
                return Response.serverError()
                    .entity("{\"error\":\"Failed to clear sandbox data\"}")
                    .build();
            });
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get sandbox statistics", description = "Returns statistics about the sandbox environment")
    public Uni<Response> getStats() {
        return sandboxService.getStats()
            .onItem().transform(stats -> Response.ok(stats).build())
            .onFailure().recoverWithItem(t -> {
                Log.errorf("Error getting sandbox stats: %s", t.getMessage());
                return Response.serverError()
                    .entity("{\"error\":\"Failed to get sandbox statistics\"}")
                    .build();
            });
    }

    @GET
    @Path("/mock-data/examples")
    @Operation(summary = "Get mock data examples", description = "Returns example payloads for testing")
    public Response getMockDataExamples() {
        String examples = """
            {
              "paymentExample": {
                "partnerReferenceNo": "TEST-PARTNER-REF-001",
                "amount": {
                  "value": 100000.00,
                  "currency": "IDR"
                },
                "beneficiaryAccountNo": "1234567890",
                "beneficiaryBankCode": "014",
                "sourceAccountNo": "9876543210",
                "additionalInfo": {
                  "description": "Test payment"
                }
              },
              "refundExample": {
                "refundReferenceNo": "TEST-REFUND-REF-001",
                "reason": "Customer request"
              }
            }
            """;
        return Response.ok(examples).build();
    }
}
