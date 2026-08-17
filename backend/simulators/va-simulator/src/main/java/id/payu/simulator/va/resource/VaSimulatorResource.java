package id.payu.simulator.va.resource;

import id.payu.simulator.va.interfaces.dto.VaInquiryRequest;
import id.payu.simulator.va.interfaces.dto.VaInquiryResponse;
import id.payu.simulator.va.interfaces.dto.VaPaymentRequest;
import id.payu.simulator.va.interfaces.dto.VaPaymentResponse;
import id.payu.simulator.va.interfaces.dto.VaRegistrationRequest;
import id.payu.simulator.va.interfaces.dto.VaRegistrationResponse;
import id.payu.simulator.va.entity.VirtualAccount;
import id.payu.simulator.va.service.VaSimulatorService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * REST API resource for Virtual Account Simulator.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/va/inquiry - Inquire about VA status</li>
 *   <li>POST /api/v1/va/pay - Make payment to VA</li>
 *   <li>POST /api/v1/va/register - Register new VA (called by PayU)</li>
 *   <li>GET /api/v1/va/{vaNumber} - Get VA details</li>
 *   <li>GET /api/v1/health - Health check</li>
 * </ul>
 *
 * <p>Part of E-15 IMP-042: Virtual Account Payment Collection
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VaSimulatorResource {

    @Inject
    VaSimulatorService vaService;

    /**
     * Inquire about a Virtual Account.
     * Called by simulated bank to validate VA before payment.
     */
    @POST
    @Path("/va/inquiry")
    public Response inquiry(@Valid VaInquiryRequest request) {
        Log.infof("POST /va/inquiry vaNumber=%s", request.vaNumber());

        try {
            VaInquiryResponse response = vaService.inquiry(request);

            int statusCode = switch (response.responseCode()) {
                case "00" -> 200;
                case "14" -> 404;
                case "54" -> 410; // Gone - expired
                case "94" -> 409; // Conflict - already paid
                default -> 500;
            };

            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing VA inquiry");
            return Response.serverError()
                .entity(VaInquiryResponse.notFound(request.vaNumber()))
                .build();
        }
    }

    /**
     * Make a payment to a Virtual Account.
     * Called by simulated bank when customer pays to VA.
     */
    @POST
    @Path("/va/pay")
    public Response pay(@Valid VaPaymentRequest request) {
        Log.infof("POST /va/pay vaNumber=%s, amount=%s", request.vaNumber(), request.amount());

        try {
            VaPaymentResponse response = vaService.processPayment(request);

            int statusCode = switch (response.responseCode()) {
                case "00" -> 200;
                case "14" -> 404;
                case "54" -> 410; // Gone - expired
                case "94" -> 409; // Conflict - already paid
                case "13" -> 400; // Bad request - amount mismatch
                case "68" -> 202; // Accepted - payment recorded but callback failed
                default -> 500;
            };

            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing VA payment");
            return Response.serverError()
                .entity(new VaPaymentResponse(
                    "99", "System error: " + e.getMessage(),
                    request.vaNumber(), null, null, null, null, null, null))
                .build();
        }
    }

    /**
     * Register a new Virtual Account (called by PayU).
     * Simulates bank VA creation.
     */
    @POST
    @Path("/va/register")
    public Response register(@Valid VaRegistrationRequest request) {
        Log.infof("POST /va/register vaNumber=%s, bank=%s", request.vaNumber(), request.bankCode());

        try {
            VirtualAccount va = vaService.registerVa(
                request.vaNumber(),
                request.bankCode(),
                request.bankName(),
                request.partnerId(),
                request.amount(),
                request.currency(),
                request.expiresAt(),
                request.callbackUrl(),
                request.externalId()
            );

            VaRegistrationResponse response = new VaRegistrationResponse(
                "00",
                "VA registered successfully",
                va.vaNumber,
                va.bankCode,
                va.bankName,
                va.amount,
                va.currency,
                va.expiresAt.toString(),
                va.status.name()
            );

            return Response.status(201).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error registering VA");
            return Response.serverError()
                .entity(new VaRegistrationResponse(
                    "99", "Registration failed: " + e.getMessage(),
                    null, null, null, null, null, null, null))
                .build();
        }
    }

    /**
     * Get Virtual Account details by VA number.
     */
    @GET
    @Path("/va/{vaNumber}")
    public Response getVa(@PathParam("vaNumber") String vaNumber) {
        Log.infof("GET /va/%s", vaNumber);

        VirtualAccount va = VirtualAccount.findByVaNumber(vaNumber);

        if (va == null) {
            return Response.status(404)
                .entity(new VaDetailResponse(false, "VA not found", null))
                .build();
        }

        return Response.ok(new VaDetailResponse(true, "Success",
            new VaInfo(
                va.vaNumber,
                va.bankCode,
                va.bankName,
                va.amount,
                va.currency,
                va.status.name(),
                va.customerName,
                va.description,
                va.paidAmount,
                va.paymentReference,
                va.paidAt != null ? va.paidAt.toString() : null,
                va.expiresAt.toString()
            ))).build();
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse("UP", "va-simulator", "1.0.0")).build();
    }

    // Record classes for responses
    public record HealthResponse(String status, String service, String version) {}

    public record VaDetailResponse(boolean success, String message, VaInfo data) {}

    public record VaInfo(
        String vaNumber,
        String bankCode,
        String bankName,
        BigDecimal amount,
        String currency,
        String status,
        String customerName,
        String description,
        BigDecimal paidAmount,
        String paymentReference,
        String paidAt,
        String expiresAt
    ) {}
}
