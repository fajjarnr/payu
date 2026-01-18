package id.payu.simulator.bifast.service;

import id.payu.simulator.bifast.config.SimulatorConfig;
import id.payu.simulator.bifast.dto.*;
import id.payu.simulator.bifast.entity.BankAccount;
import id.payu.simulator.bifast.entity.Transfer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Random;
import java.util.UUID;

/**
 * Service for BI-FAST simulation operations.
 */
@ApplicationScoped
public class BiFastService {

    private final Random random = new Random();

    @Inject
    SimulatorConfig config;

    /**
     * Simulate account inquiry.
     */
    @Transactional
    public InquiryResponse inquiry(InquiryRequest request) {
        Log.infof("Processing inquiry for bank=%s, account=%s", 
                  request.bankCode(), request.accountNumber());

        // Simulate network latency
        simulateLatency();

        // Check for random failure
        if (shouldSimulateFailure()) {
            Log.warn("Simulating random failure for inquiry");
            return InquiryResponse.error("Simulated random failure");
        }

        // Find account
        BankAccount account = BankAccount.findByBankAndAccount(
            request.bankCode(), 
            request.accountNumber()
        );

        if (account == null) {
            Log.infof("Account not found: %s-%s", request.bankCode(), request.accountNumber());
            return InquiryResponse.notFound(request.bankCode(), request.accountNumber());
        }

        // Handle special statuses
        return switch (account.status) {
            case ACTIVE -> InquiryResponse.success(account);
            case BLOCKED, DORMANT -> InquiryResponse.blocked(account);
            case TIMEOUT -> {
                simulateTimeout();
                yield InquiryResponse.timeout();
            }
        };
    }

    /**
     * Initiate a fund transfer.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        Log.infof("Processing transfer from %s-%s to %s-%s, amount=%s",
                  request.sourceBankCode(), request.sourceAccountNumber(),
                  request.destinationBankCode(), request.destinationAccountNumber(),
                  request.amount());

        // Simulate network latency
        simulateLatency();

        // Check for random failure
        if (shouldSimulateFailure()) {
            Log.warn("Simulating random failure for transfer");
            return TransferResponse.error("Simulated random failure");
        }

        // Validate destination account
        BankAccount destAccount = BankAccount.findByBankAndAccount(
            request.destinationBankCode(),
            request.destinationAccountNumber()
        );

        if (destAccount == null) {
            Transfer failed = createTransfer(request);
            failed.fail("Destination account not found");
            failed.persist();
            return TransferResponse.fromEntity(failed);
        }

        if (destAccount.status == BankAccount.AccountStatus.BLOCKED) {
            Transfer failed = createTransfer(request);
            failed.fail("Destination account is blocked");
            failed.persist();
            return TransferResponse.fromEntity(failed);
        }

        if (destAccount.status == BankAccount.AccountStatus.TIMEOUT) {
            simulateTimeout();
            Transfer timeout = createTransfer(request);
            timeout.status = Transfer.TransferStatus.TIMEOUT;
            timeout.persist();
            return TransferResponse.fromEntity(timeout);
        }

        // Create successful transfer
        Transfer transfer = createTransfer(request);
        transfer.destinationAccountName = destAccount.accountName;
        transfer.complete();
        transfer.persist();

        Log.infof("Transfer completed: ref=%s", transfer.referenceNumber);
        return TransferResponse.fromEntity(transfer);
    }

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
        transfer.referenceNumber = generateReferenceNumber();
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
        return "BIFAST-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
            Thread.sleep(5000); // 5 second timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < config.failureRate();
    }
}
