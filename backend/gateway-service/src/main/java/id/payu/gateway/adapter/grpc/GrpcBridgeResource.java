package id.payu.gateway.adapter.grpc;

import id.payu.gateway.dto.ApiError;
import id.payu.gateway.dto.ApiResponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * JAX-RS resource exposing the gRPC→REST bridge for wallet-service (IMP-033).
 *
 * <p>Provides internal endpoints under {@code /api/internal/grpc/wallet} that
 * translate incoming REST/JSON requests into gRPC calls via {@link WalletGrpcBridge},
 * then return protobuf responses as JSON.
 *
 * <p><b>Note:</b> These endpoints are intended for internal/testing use.
 * Production traffic should use the standard gateway proxy routes.
 */
@Path("/api/internal/grpc/wallet")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "gRPC Bridge - Wallet", description = "Internal REST-to-gRPC bridge for wallet-service (IMP-033)")
public class GrpcBridgeResource {

    @Inject
    WalletGrpcBridge walletBridge;

    // ── Balance Queries ────────────────────────────────────────

    @GET
    @Path("/balance/{walletId}")
    @Operation(summary = "Get wallet balance via gRPC",
               description = "Proxies to wallet-service GetBalance gRPC call")
    @APIResponse(responseCode = "200", description = "Balance retrieved successfully")
    @APIResponse(responseCode = "502", description = "gRPC backend unavailable")
    public Uni<Response> getBalance(
            @Parameter(description = "Wallet ID", required = true)
            @PathParam("walletId") String walletId,
            @Parameter(description = "Account ID (optional)")
            @QueryParam("accountId") String accountId) {

        return walletBridge.getBalance(walletId, accountId)
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/balance/" + walletId));
    }

    @GET
    @Path("/available-balance/{walletId}")
    @Operation(summary = "Get available balance via gRPC",
               description = "Proxies to wallet-service GetAvailableBalance gRPC call")
    @APIResponse(responseCode = "200", description = "Available balance retrieved successfully")
    public Uni<Response> getAvailableBalance(
            @PathParam("walletId") String walletId,
            @QueryParam("accountId") String accountId) {

        return walletBridge.getAvailableBalance(walletId, accountId)
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/available-balance/" + walletId));
    }

    // ── Debit / Credit / Transfer ──────────────────────────────

    @POST
    @Path("/debit")
    @Operation(summary = "Debit from wallet via gRPC")
    @APIResponse(responseCode = "200", description = "Debit executed successfully")
    public Uni<Response> debit(DebitCreditRequest body) {
        return walletBridge.debit(
                body.walletId(), body.accountId(),
                body.currency(), body.amount(),
                body.referenceId(), body.description())
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/debit"));
    }

    @POST
    @Path("/credit")
    @Operation(summary = "Credit to wallet via gRPC")
    @APIResponse(responseCode = "200", description = "Credit executed successfully")
    public Uni<Response> credit(DebitCreditRequest body) {
        return walletBridge.credit(
                body.walletId(), body.accountId(),
                body.currency(), body.amount(),
                body.referenceId(), body.description())
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/credit"));
    }

    @POST
    @Path("/transfer")
    @Operation(summary = "Transfer between wallets via gRPC")
    @APIResponse(responseCode = "200", description = "Transfer executed successfully")
    public Uni<Response> transfer(TransferRequestDto body) {
        return walletBridge.transfer(
                body.fromWalletId(), body.toWalletId(),
                body.fromAccountId(), body.toAccountId(),
                body.currency(), body.amount(),
                body.referenceId(), body.description())
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/transfer"));
    }

    // ── Reservations ───────────────────────────────────────────

    @POST
    @Path("/reserve")
    @Operation(summary = "Reserve balance via gRPC")
    @APIResponse(responseCode = "200", description = "Balance reserved successfully")
    public Uni<Response> reserveBalance(DebitCreditRequest body) {
        return walletBridge.reserveBalance(
                body.walletId(), body.accountId(),
                body.currency(), body.amount(),
                body.referenceId(), body.description())
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/reserve"));
    }

    @POST
    @Path("/reservation/{reservationId}/commit")
    @Operation(summary = "Commit a reservation via gRPC")
    @APIResponse(responseCode = "200", description = "Reservation committed successfully")
    public Uni<Response> commitReservation(@PathParam("reservationId") String reservationId) {
        return walletBridge.commitReservation(reservationId)
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/reservation/" + reservationId + "/commit"));
    }

    @POST
    @Path("/reservation/{reservationId}/release")
    @Operation(summary = "Release a reservation via gRPC")
    @APIResponse(responseCode = "200", description = "Reservation released successfully")
    public Uni<Response> releaseReservation(@PathParam("reservationId") String reservationId) {
        return walletBridge.releaseReservation(reservationId)
                .onItem().transform(data -> toResponse(data, "/api/internal/grpc/wallet/reservation/" + reservationId + "/release"));
    }

    // ── Helpers ────────────────────────────────────────────────

    /**
     * Convert bridge result to HTTP Response, checking for gRPC-level errors.
     */
    private Response toResponse(Map<String, Object> data, String path) {
        // If the bridge returned a gRPC error map, translate to appropriate HTTP status
        if (data.containsKey("grpcCode")) {
            int httpStatus = grpcCodeToHttpStatus((String) data.get("grpcCode"));
            return Response.status(httpStatus)
                    .entity(ApiResponse.error(ApiError.of(
                            (String) data.get("grpcCode"),
                            (String) data.get("message"),
                            path,
                            httpStatus)))
                    .build();
        }

        // Check domain-level success flag
        Object success = data.get("success");
        if (Boolean.FALSE.equals(success)) {
            return Response.status(422)
                    .entity(ApiResponse.error(ApiError.of(
                            "WALLET_OPERATION_FAILED",
                            "Wallet operation returned success=false",
                            path,
                            422)))
                    .build();
        }

        return Response.ok(ApiResponse.success(data)).build();
    }

    /**
     * Map gRPC status codes to HTTP status codes.
     */
    private int grpcCodeToHttpStatus(String grpcCode) {
        return switch (grpcCode) {
            case "NOT_FOUND"            -> 404;
            case "ALREADY_EXISTS"       -> 409;
            case "PERMISSION_DENIED"    -> 403;
            case "UNAUTHENTICATED"      -> 401;
            case "INVALID_ARGUMENT"     -> 400;
            case "FAILED_PRECONDITION"  -> 412;
            case "RESOURCE_EXHAUSTED"   -> 429;
            case "UNIMPLEMENTED"        -> 501;
            case "UNAVAILABLE"          -> 503;
            case "DEADLINE_EXCEEDED"    -> 504;
            case "INTERNAL"             -> 500;
            case "CANCELLED"            -> 499; // client closed request
            default                     -> 502; // bad gateway
        };
    }

    // ── Request DTOs ───────────────────────────────────────────

    /**
     * Request body for debit, credit, and reserve operations.
     */
    public record DebitCreditRequest(
            String walletId,
            String accountId,
            String currency,
            String amount,
            String referenceId,
            String description
    ) {}

    /**
     * Request body for transfer operations.
     */
    public record TransferRequestDto(
            String fromWalletId,
            String toWalletId,
            String fromAccountId,
            String toAccountId,
            String currency,
            String amount,
            String referenceId,
            String description
    ) {}
}
