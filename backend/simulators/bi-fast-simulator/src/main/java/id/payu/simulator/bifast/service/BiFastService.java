package id.payu.simulator.bifast.service;

import id.payu.simulator.bifast.config.SimulatorConfig;
import id.payu.simulator.bifast.interfaces.dto.*;
import id.payu.simulator.bifast.entity.BankAccount;
import id.payu.simulator.bifast.entity.Transfer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Random;
import java.util.UUID;
import id.payu.simulator.bifast.entity.AccountStatus;
import id.payu.simulator.bifast.entity.TransferStatus;

/**
 * Service for BI-FAST simulation operations.
 */
@ApplicationScoped
public class BiFastService {

    private final Random random = new Random();

    @Inject
    SimulatorConfig config;

    @Inject
    WebhookDispatcher webhookDispatcher;

    /**
     * Simulate account inquiry — supports X-Simulate header for deterministic chaos (ADR-0056).
     */
    @Transactional
    public InquiryResponse inquiry(InquiryRequest request, String simulate) {
        String mode = normalizeSimulate(simulate);
        if (mode != null) {
            switch (mode) {
                case "blocked" -> {
                    BankAccount acc = BankAccount.findByBankAndAccount(request.bankCode(), request.accountNumber());
                    if (acc != null) return InquiryResponse.blocked(acc);
                    return new InquiryResponse(request.bankCode(), request.accountNumber(), null, "BLOCKED", "62", "Account is blocked");
                }
                case "timeout" -> { simulateTimeout(); return InquiryResponse.timeout(); }
                case "rate-limit" -> { return new InquiryResponse(request.bankCode(), request.accountNumber(), null, "RATE_LIMIT", "42", "Rate limit exceeded"); }
                case "5xx" -> { return InquiryResponse.error("Simulated internal error"); }
                case "success" -> { /* fall through to normal without random failure */ }
                default -> { /* unknown mode ignore */ }
            }
        }
        // Simulate network latency
        simulateLatency();
        // Check for random failure only when not forced success
        if (!"success".equals(mode) && shouldSimulateFailure()) {
            Log.warn("Simulating random failure for inquiry");
            return InquiryResponse.error("Simulated random failure");
        }
        BankAccount account = BankAccount.findByBankAndAccount(request.bankCode(), request.accountNumber());
        if (account == null) {
            Log.infof("Account not found: %s-%s", request.bankCode(), request.accountNumber());
            return InquiryResponse.notFound(request.bankCode(), request.accountNumber());
        }
        return switch (account.status) {
            case ACTIVE -> InquiryResponse.success(account);
            case BLOCKED, DORMANT -> InquiryResponse.blocked(account);
            case TIMEOUT -> { simulateTimeout(); yield InquiryResponse.timeout(); }
        };
    }

    @Transactional
    public InquiryResponse inquiry(InquiryRequest request) { return inquiry(request, null); }


    /**
     * Initiate a fund transfer — idempotent on referenceNumber + X-Simulate aware.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request, String simulate) {
        String mode = normalizeSimulate(simulate);
        if (mode != null) {
            switch (mode) {
                case "blocked" -> {
                    Transfer blocked = createTransfer(request);
                    blocked.fail("Destination account is blocked");
                    blocked.persist();
                    return TransferResponse.fromEntity(blocked);
                }
                case "timeout" -> { simulateTimeout(); Transfer t = createTransfer(request); t.status = TransferStatus.TIMEOUT; t.persist(); return TransferResponse.fromEntity(t); }
                case "rate-limit" -> { return new TransferResponse(request.referenceNumber(), request.sourceBankCode(), request.sourceAccountNumber(), request.destinationBankCode(), request.destinationAccountNumber(), null, request.amount(), request.currency(), "RATE_LIMIT", "42", "Rate limit exceeded", null, null); }
                case "5xx" -> { return TransferResponse.error("Simulated internal error"); }
                case "success" -> { /* fall through */ }
                default -> {}
            }
        }
        // Idempotency: duplicate referenceNumber returns original without second persist
        if (request.referenceNumber() != null && !request.referenceNumber().isBlank()) {
            Transfer existing = Transfer.findByReference(request.referenceNumber());
            if (existing != null) {
                Log.infof("Duplicate reference detected: %s -> returning existing", request.referenceNumber());
                return TransferResponse.fromEntity(existing);
            }
        }
        simulateLatency();
        if (!"success".equals(mode) && shouldSimulateFailure()) {
            Log.warn("Simulating random failure for transfer");
            Transfer failed = createTransfer(request);
            failed.fail("Simulated random failure");
            failed.persist();
            webhookDispatcher.dispatch(failed);
            return TransferResponse.fromEntity(failed);
        }
        BankAccount destAccount = BankAccount.findByBankAndAccount(request.destinationBankCode(), request.destinationAccountNumber());
        if (destAccount == null) {
            Transfer failed = createTransfer(request);
            failed.fail("Destination account not found");
            failed.persist();
            webhookDispatcher.dispatch(failed);
            return TransferResponse.fromEntity(failed);
        }
        if (destAccount.status == AccountStatus.BLOCKED) {
            Transfer failed = createTransfer(request);
            failed.fail("Destination account is blocked");
            failed.persist();
            webhookDispatcher.dispatch(failed);
            return TransferResponse.fromEntity(failed);
        }
        if (destAccount.status == AccountStatus.TIMEOUT) {
            simulateTimeout();
            Transfer timeout = createTransfer(request);
            timeout.status = TransferStatus.TIMEOUT;
            timeout.persist();
            return TransferResponse.fromEntity(timeout);
        }
        Transfer transfer = createTransfer(request);
        transfer.destinationAccountName = destAccount.accountName;
        transfer.complete();
        transfer.persist();
        webhookDispatcher.dispatch(transfer);
        Log.infof("Transfer completed: ref=%s", transfer.referenceNumber);
        return TransferResponse.fromEntity(transfer);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) { return transfer(request, null); }


    /**
     * Get transfer status by reference number.
     */
    public TransferResponse getStatus(String referenceNumber) {
        Log.infof("Getting status for reference=%s", referenceNumber);

        simulateLatency();

        Transfer transfer = Transfer.findByReference(referenceNumber);
        if (transfer == null) {
            return TransferResponse.error("Transfer not found: " + referenceNumber);
        }

        return TransferResponse.fromEntity(transfer);
    }

    private Transfer createTransfer(TransferRequest request) {
        Transfer transfer = new Transfer();
        transfer.referenceNumber = request.referenceNumber() != null && !request.referenceNumber().isBlank()
                ? request.referenceNumber()
                : generateReferenceNumber();
        transfer.sourceBankCode = request.sourceBankCode();
        transfer.sourceAccountNumber = request.sourceAccountNumber();
        transfer.sourceAccountName = request.sourceAccountName();
        transfer.destinationBankCode = request.destinationBankCode();
        transfer.destinationAccountNumber = request.destinationAccountNumber();
        transfer.amount = request.amount();
        transfer.currency = request.currency();
        transfer.description = request.description();
        transfer.webhookUrl = request.webhookUrl();
        return transfer;
    }

    private String generateReferenceNumber() {
        return "BIFAST-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void simulateLatency() {
        int minLatency = config.latency().min();
        int maxLatency = config.latency().max();
        int latency = minLatency + random.nextInt(maxLatency - minLatency + 1);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateTimeout() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < config.failureRate();
    }

    static String normalizeSimulate(String v) {
        if (v == null) return null;
        String s = v.trim().toLowerCase();
        return s.isEmpty() ? null : s;
    }
}
