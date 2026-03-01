package id.payu.simulator.bifast.resource;

import id.payu.simulator.bifast.dto.*;
import id.payu.simulator.bifast.service.BiFastService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Sandbox controller for deterministic test scenarios.
 * Provides predictable responses for integration testing.
 */
@Path("/sandbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SandboxController {

    @Inject
    BiFastService biFastService;

    // Test account numbers for deterministic responses
    private static final String TEST_BCA_ACCOUNT = "1234567890";
    private static final String TEST_BNI_ACCOUNT = "0987654321";
    private static final String TEST_MANDIRI_ACCOUNT = "1122334455";
    private static final String TEST_INVALID_ACCOUNT = "0000000000";

    /**
     * Deterministic inquiry endpoint for sandbox testing.
     * Returns predictable responses based on account number.
     */
    @POST
    @Path("/inquiry")
    public Response sandboxInquiry(SandboxInquiryRequest request) {
        Log.infof("Sandbox inquiry: bank={}, account={}",
                request.bankCode(), request.accountNumber());

        String accountNumber = request.accountNumber();
        String bankCode = request.bankCode();

        // Deterministic responses based on account number
        if (TEST_INVALID_ACCOUNT.equals(accountNumber)) {
            return Response.status(404)
                    .entity(new InquiryResponse(
                            "14",
                            "Invalid account number",
                            null,
                            null,
                            null,
                            null
                    ))
                    .build();
        }

        // Success responses with deterministic names
        String accountName = switch (accountNumber) {
            case TEST_BCA_ACCOUNT -> "John Doe (Test)";
            case TEST_BNI_ACCOUNT -> "Jane Smith (Test)";
            case TEST_MANDIRI_ACCOUNT -> "Bob Wilson (Test)";
            default -> "Test User " + accountNumber.substring(0, 4);
        };

        return Response.ok()
                .entity(new InquiryResponse(
                        "00",
                        "Success",
                        bankCode,
                        accountNumber,
                        accountName,
                        "ACTIVE"
                ))
                .build();
    }

    /**
     * Deterministic transfer endpoint for sandbox testing.
     * Returns predictable responses based on amount and account.
     */
    @POST
    @Path("/transfer")
    public Response sandboxTransfer(SandboxTransferRequest request) {
        Log.infof("Sandbox transfer: {} to {}, amount={}",
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount());

        String destAccount = request.destinationAccountNumber();
        BigDecimal amount = request.amount();

        // Deterministic failure: insufficient funds for very large amounts
        if (amount.compareTo(new BigDecimal("999999999")) > 0) {
            return Response.status(400)
                    .entity(new TransferResponse(
                            "51",
                            "Insufficient funds",
                            generateReferenceNumber(),
                            null,
                            null,
                            "FAILED"
                    ))
                    .build();
        }

        // Deterministic failure: invalid account
        if (TEST_INVALID_ACCOUNT.equals(destAccount)) {
            return Response.status(400)
                    .entity(new TransferResponse(
                            "14",
                            "Invalid destination account",
                            generateReferenceNumber(),
                            null,
                            null,
                            "FAILED"
                    ))
                    .build();
        }

        // Deterministic pending for Mandiri
        if (TEST_MANDIRI_ACCOUNT.equals(destAccount)) {
            return Response.accepted()
                    .entity(new TransferResponse(
                            "09",
                            "Transaction pending",
                            generateReferenceNumber(),
                            null,
                            java.time.LocalDateTime.now().toString(),
                            "PENDING"
                    ))
                    .build();
        }

        // Success for all other cases
        return Response.ok()
                .entity(new TransferResponse(
                        "00",
                        "Transfer successful",
                        generateReferenceNumber(),
                        request.destinationBankCode(),
                        java.time.LocalDateTime.now().toString(),
                        "COMPLETED"
                ))
                .build();
    }

    /**
     * Get sandbox test scenarios.
     */
    @GET
    @Path("/scenarios")
    public Response getScenarios() {
        Map<String, Object> scenarios = new HashMap<>();

        scenarios.put("success_bca", Map.of(
                "description", "Successful transfer to BCA test account",
                "accountNumber", TEST_BCA_ACCOUNT,
                "bankCode", "BCA",
                "expectedResponse", "00 - Success"
        ));

        scenarios.put("success_bni", Map.of(
                "description", "Successful transfer to BNI test account",
                "accountNumber", TEST_BNI_ACCOUNT,
                "bankCode", "BNI",
                "expectedResponse", "00 - Success"
        ));

        scenarios.put("pending_mandiri", Map.of(
                "description", "Pending transfer to Mandiri test account",
                "accountNumber", TEST_MANDIRI_ACCOUNT,
                "bankCode", "MANDIRI",
                "expectedResponse", "09 - Pending"
        ));

        scenarios.put("invalid_account", Map.of(
                "description", "Transfer to invalid account",
                "accountNumber", TEST_INVALID_ACCOUNT,
                "bankCode", "BCA",
                "expectedResponse", "14 - Invalid account"
        ));

        scenarios.put("insufficient_funds", Map.of(
                "description", "Transfer with insufficient funds",
                "accountNumber", TEST_BCA_ACCOUNT,
                "bankCode", "BCA",
                "amount", "999999999999",
                "expectedResponse", "51 - Insufficient funds"
        ));

        return Response.ok(scenarios).build();
    }

    /**
     * Get sandbox test accounts.
     */
    @GET
    @Path("/test-accounts")
    public Response getTestAccounts() {
        Map<String, Object> accounts = new HashMap<>();

        accounts.put("bca", Map.of(
                "accountNumber", TEST_BCA_ACCOUNT,
                "accountName", "John Doe (Test)",
                "bankCode", "BCA",
                "status", "ACTIVE"
        ));

        accounts.put("bni", Map.of(
                "accountNumber", TEST_BNI_ACCOUNT,
                "accountName", "Jane Smith (Test)",
                "bankCode", "BNI",
                "status", "ACTIVE"
        ));

        accounts.put("mandiri", Map.of(
                "accountNumber", TEST_MANDIRI_ACCOUNT,
                "accountName", "Bob Wilson (Test)",
                "bankCode", "MANDIRI",
                "status", "ACTIVE"
        ));

        return Response.ok(accounts).build();
    }

    private String generateReferenceNumber() {
        return "SBX" + System.currentTimeMillis();
    }

    // Request/Response records
    public record SandboxInquiryRequest(String bankCode, String accountNumber) {}
    public record SandboxTransferRequest(
            String sourceBankCode,
            String sourceAccountNumber,
            String destinationBankCode,
            String destinationAccountNumber,
            BigDecimal amount,
            String description) {}
    public record InquiryResponse(
            String responseCode,
            String responseMessage,
            String bankCode,
            String accountNumber,
            String accountName,
            String accountStatus) {}
    public record TransferResponse(
            String responseCode,
            String responseMessage,
            String referenceNumber,
            String bankReference,
            String completedAt,
            String status) {}
}
