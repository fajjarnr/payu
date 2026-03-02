package id.payu.gateway.adapter.grpc;

import id.payu.grpc.common.Money;
import id.payu.wallet.grpc.BalanceResponse;
import id.payu.wallet.grpc.CommitReservationRequest;
import id.payu.wallet.grpc.CreditRequest;
import id.payu.wallet.grpc.DebitRequest;
import id.payu.wallet.grpc.GetBalanceRequest;
import id.payu.wallet.grpc.ReleaseReservationRequest;
import id.payu.wallet.grpc.ReservationResponse;
import id.payu.wallet.grpc.ReserveBalanceRequest;
import id.payu.wallet.grpc.TransactionResponse;
import id.payu.wallet.grpc.TransferRequest;
import id.payu.wallet.grpc.WalletService;

import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST-to-gRPC bridge for wallet-service (IMP-033).
 *
 * <p>Translates REST JSON requests from frontend/partners into gRPC calls
 * to the wallet backend, then converts protobuf responses back to JSON-friendly Maps.
 *
 * <p>Uses the Quarkus Mutiny gRPC client; all methods return {@link Uni} for
 * non-blocking integration with RESTEasy Reactive.
 */
@ApplicationScoped
public class WalletGrpcBridge {

    @GrpcClient("wallet-service")
    WalletService walletClient;

    // ── Queries ────────────────────────────────────────────────

    /**
     * Get wallet balance via gRPC.
     */
    public Uni<Map<String, Object>> getBalance(String walletId, String accountId) {
        Log.infof("gRPC getBalance walletId=%s accountId=%s", walletId, accountId);

        GetBalanceRequest request = GetBalanceRequest.newBuilder()
                .setWalletId(walletId)
                .setAccountId(accountId != null ? accountId : "")
                .build();

        return walletClient.getBalance(request)
                .onItem().transform(this::mapBalanceResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    /**
     * Get available balance (excludes reserved amounts).
     */
    public Uni<Map<String, Object>> getAvailableBalance(String walletId, String accountId) {
        Log.infof("gRPC getAvailableBalance walletId=%s", walletId);

        GetBalanceRequest request = GetBalanceRequest.newBuilder()
                .setWalletId(walletId)
                .setAccountId(accountId != null ? accountId : "")
                .build();

        return walletClient.getAvailableBalance(request)
                .onItem().transform(this::mapBalanceResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    // ── Mutations ──────────────────────────────────────────────

    /**
     * Debit from wallet.
     */
    public Uni<Map<String, Object>> debit(String walletId, String accountId,
                                          String currency, String amount,
                                          String referenceId, String description) {
        Log.infof("gRPC debit walletId=%s amount=%s %s", walletId, amount, currency);

        DebitRequest request = DebitRequest.newBuilder()
                .setWalletId(walletId)
                .setAccountId(accountId != null ? accountId : "")
                .setAmount(Money.newBuilder().setCurrency(currency).setAmount(amount).build())
                .setReferenceId(referenceId != null ? referenceId : "")
                .setDescription(description != null ? description : "")
                .build();

        return walletClient.debit(request)
                .onItem().transform(this::mapTransactionResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    /**
     * Credit to wallet.
     */
    public Uni<Map<String, Object>> credit(String walletId, String accountId,
                                           String currency, String amount,
                                           String referenceId, String description) {
        Log.infof("gRPC credit walletId=%s amount=%s %s", walletId, amount, currency);

        CreditRequest request = CreditRequest.newBuilder()
                .setWalletId(walletId)
                .setAccountId(accountId != null ? accountId : "")
                .setAmount(Money.newBuilder().setCurrency(currency).setAmount(amount).build())
                .setReferenceId(referenceId != null ? referenceId : "")
                .setDescription(description != null ? description : "")
                .build();

        return walletClient.credit(request)
                .onItem().transform(this::mapTransactionResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    /**
     * Transfer between wallets.
     */
    public Uni<Map<String, Object>> transfer(String fromWalletId, String toWalletId,
                                             String fromAccountId, String toAccountId,
                                             String currency, String amount,
                                             String referenceId, String description) {
        Log.infof("gRPC transfer from=%s to=%s amount=%s %s",
                fromWalletId, toWalletId, amount, currency);

        TransferRequest request = TransferRequest.newBuilder()
                .setFromWalletId(fromWalletId)
                .setToWalletId(toWalletId)
                .setFromAccountId(fromAccountId != null ? fromAccountId : "")
                .setToAccountId(toAccountId != null ? toAccountId : "")
                .setAmount(Money.newBuilder().setCurrency(currency).setAmount(amount).build())
                .setReferenceId(referenceId != null ? referenceId : "")
                .setDescription(description != null ? description : "")
                .build();

        return walletClient.transfer(request)
                .onItem().transform(this::mapTransactionResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    // ── Reservations ───────────────────────────────────────────

    /**
     * Reserve balance for later use (e.g. escrow, hold).
     */
    public Uni<Map<String, Object>> reserveBalance(String walletId, String accountId,
                                                   String currency, String amount,
                                                   String referenceId, String description) {
        Log.infof("gRPC reserveBalance walletId=%s amount=%s %s", walletId, amount, currency);

        ReserveBalanceRequest request = ReserveBalanceRequest.newBuilder()
                .setWalletId(walletId)
                .setAccountId(accountId != null ? accountId : "")
                .setAmount(Money.newBuilder().setCurrency(currency).setAmount(amount).build())
                .setReferenceId(referenceId != null ? referenceId : "")
                .setDescription(description != null ? description : "")
                .build();

        return walletClient.reserveBalance(request)
                .onItem().transform(this::mapReservationResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    /**
     * Commit (finalize) a previously created reservation.
     */
    public Uni<Map<String, Object>> commitReservation(String reservationId) {
        Log.infof("gRPC commitReservation id=%s", reservationId);

        CommitReservationRequest request = CommitReservationRequest.newBuilder()
                .setReservationId(reservationId)
                .build();

        return walletClient.commitReservation(request)
                .onItem().transform(this::mapTransactionResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    /**
     * Release (cancel) a previously created reservation.
     */
    public Uni<Map<String, Object>> releaseReservation(String reservationId) {
        Log.infof("gRPC releaseReservation id=%s", reservationId);

        ReleaseReservationRequest request = ReleaseReservationRequest.newBuilder()
                .setReservationId(reservationId)
                .build();

        return walletClient.releaseReservation(request)
                .onItem().transform(this::mapTransactionResponse)
                .onFailure(StatusRuntimeException.class)
                    .recoverWithItem(this::mapGrpcError);
    }

    // ── Protobuf → JSON-Map converters ─────────────────────────

    private Map<String, Object> mapBalanceResponse(BalanceResponse r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("walletId", r.getWalletId());
        map.put("accountId", r.getAccountId());
        map.put("balance", mapMoney(r.getBalance()));
        map.put("availableBalance", mapMoney(r.getAvailableBalance()));
        map.put("reservedBalance", mapMoney(r.getReservedBalance()));
        map.put("timestamp", toIso(r.getTimestamp()));
        return map;
    }

    private Map<String, Object> mapTransactionResponse(TransactionResponse r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", r.getSuccess());
        map.put("transactionId", r.getTransactionId());
        map.put("ledgerEntryId", r.getLedgerEntryId());
        map.put("newBalance", mapMoney(r.getNewBalance()));
        map.put("timestamp", toIso(r.getTimestamp()));
        if (r.hasError()) {
            map.put("error", mapError(r.getError()));
        }
        return map;
    }

    private Map<String, Object> mapReservationResponse(ReservationResponse r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", r.getSuccess());
        map.put("reservationId", r.getReservationId());
        map.put("reservedAmount", mapMoney(r.getReservedAmount()));
        map.put("availableBalance", mapMoney(r.getAvailableBalance()));
        map.put("timestamp", toIso(r.getTimestamp()));
        if (r.hasError()) {
            map.put("error", mapError(r.getError()));
        }
        return map;
    }

    private Map<String, String> mapMoney(Money m) {
        if (m == null || m.equals(Money.getDefaultInstance())) {
            return Map.of("currency", "", "amount", "0");
        }
        return Map.of("currency", m.getCurrency(), "amount", m.getAmount());
    }

    private Map<String, String> mapError(id.payu.grpc.common.ErrorDetail e) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("code", e.getCode());
        map.put("message", e.getMessage());
        if (!e.getField().isEmpty()) {
            map.put("field", e.getField());
        }
        return map;
    }

    private String toIso(id.payu.grpc.common.Timestamp ts) {
        if (ts == null || ts.equals(id.payu.grpc.common.Timestamp.getDefaultInstance())) {
            return Instant.now().toString();
        }
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).toString();
    }

    // ── gRPC error → JSON-Map converter ────────────────────────

    private Map<String, Object> mapGrpcError(Throwable t) {
        StatusRuntimeException sre = (StatusRuntimeException) t;
        io.grpc.Status status = sre.getStatus();

        Log.errorf("gRPC call failed: %s - %s", status.getCode(), status.getDescription());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", false);
        map.put("grpcCode", status.getCode().name());
        map.put("message", status.getDescription() != null
                ? status.getDescription()
                : status.getCode().name());
        map.put("timestamp", Instant.now().toString());
        return map;
    }
}
