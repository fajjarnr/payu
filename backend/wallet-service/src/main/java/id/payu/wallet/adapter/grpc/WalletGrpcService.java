package id.payu.wallet.adapter.grpc;

import id.payu.wallet.application.service.WalletService;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.grpc.*;
import id.payu.grpc.common.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * gRPC service implementation for Wallet operations.
 * Implements the WalletService proto contract.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Service
@GrpcService
@RequiredArgsConstructor
public class WalletGrpcService extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletService walletService;

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        try {
            log.debug("gRPC getBalance - walletId: {}, accountId: {}",
                    request.getWalletId(), request.getAccountId());

            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                // Try to get accountId from walletId
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            BigDecimal balance = walletService.getBalance(accountId);
            BigDecimal availableBalance = walletService.getAvailableBalance(accountId);

            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            BalanceResponse response = BalanceResponse.newBuilder()
                    .setWalletId(wallet.getId().toString())
                    .setAccountId(accountId)
                    .setBalance(toMoney(balance, wallet.getCurrency()))
                    .setAvailableBalance(toMoney(availableBalance, wallet.getCurrency()))
                    .setReservedBalance(toMoney(wallet.getReservedBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getBalance: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getAvailableBalance(GetBalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        try {
            log.debug("gRPC getAvailableBalance - walletId: {}, accountId: {}",
                    request.getWalletId(), request.getAccountId());

            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            BigDecimal availableBalance = walletService.getAvailableBalance(accountId);
            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            BalanceResponse response = BalanceResponse.newBuilder()
                    .setWalletId(wallet.getId().toString())
                    .setAccountId(accountId)
                    .setBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                    .setAvailableBalance(toMoney(availableBalance, wallet.getCurrency()))
                    .setReservedBalance(toMoney(wallet.getReservedBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getAvailableBalance: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void debit(DebitRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            log.info("gRPC debit - accountId: {}, amount: {}",
                    request.getAccountId(), request.getAmount().getAmount());

            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            BigDecimal amount = new BigDecimal(request.getAmount().getAmount());
            String transactionId = walletService.reserveBalance(
                    accountId,
                    amount,
                    request.getReferenceId()
            );

            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            TransactionResponse response = TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(transactionId)
                    .setNewBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid debit request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in debit: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void repayLoan(LoanRepaymentRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            BigDecimal amount = new BigDecimal(request.getAmount().getAmount());
            String transactionId = walletService.repayLoan(
                    accountId,
                    request.getLoanId(),
                    amount,
                    request.getAmount().getCurrency(),
                    request.getReferenceId(),
                    request.getDescription());

            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(transactionId)
                    .setNewBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid repayLoan request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in repayLoan: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void credit(CreditRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            log.info("gRPC credit - accountId: {}, amount: {}",
                    request.getAccountId(), request.getAmount().getAmount());

            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            BigDecimal amount = new BigDecimal(request.getAmount().getAmount());
            String transactionId = walletService.credit(
                    accountId,
                    amount,
                    request.getReferenceId(),
                    request.getDescription()
            );

            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            TransactionResponse response = TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(transactionId)
                    .setNewBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid credit request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in credit: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void transfer(TransferRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            log.info("gRPC transfer - from: {}, to: {}, amount: {}",
                    request.getFromAccountId(), request.getToAccountId(), request.getAmount().getAmount());

            // Debit from source
            String fromAccountId = request.getFromAccountId();
            if (fromAccountId == null || fromAccountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getFromWalletId()));
                fromAccountId = wallet.getAccountId();
            }

            String toAccountId = request.getToAccountId();
            if (toAccountId == null || toAccountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getToWalletId()));
                toAccountId = wallet.getAccountId();
            }

            BigDecimal amount = new BigDecimal(request.getAmount().getAmount());

            // Reserve and commit debit
            String reservationId = walletService.reserveBalance(
                    fromAccountId,
                    amount,
                    request.getReferenceId()
            );
            walletService.commitReservation(reservationId);

            // Credit to destination
            String creditTransactionId = walletService.credit(
                    toAccountId,
                    amount,
                    request.getReferenceId(),
                    request.getDescription()
            );

            Wallet fromWallet = walletService.getWalletByAccountId(fromAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));

            TransactionResponse response = TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(creditTransactionId)
                    .setNewBalance(toMoney(fromWallet.getBalance(), fromWallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid transfer request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in transfer: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getHistory(GetHistoryRequest request, StreamObserver<id.payu.wallet.grpc.LedgerEntry> responseObserver) {
        try {
            log.debug("gRPC getHistory - walletId: {}, accountId: {}",
                    request.getWalletId(), request.getAccountId());

            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            List<LedgerEntry> entries = walletService.getLedgerEntriesByAccountId(accountId);

            for (LedgerEntry entry : entries) {
                id.payu.wallet.grpc.LedgerEntry grpcEntry = toGrpcLedgerEntry(entry);
                responseObserver.onNext(grpcEntry);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getHistory: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getWallet(GetWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            log.debug("gRPC getWallet - walletId: {}, accountId: {}",
                    request.getWalletId(), request.getAccountId());

            Wallet wallet;
            if (request.getWalletId() != null && !request.getWalletId().isEmpty()) {
                wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
            } else if (request.getAccountId() != null && !request.getAccountId().isEmpty()) {
                wallet = walletService.getWalletByAccountId(request.getAccountId())
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
            } else {
                throw new IllegalArgumentException("Either walletId or accountId must be provided");
            }

            WalletResponse response = toWalletResponse(wallet);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid getWallet request: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in getWallet: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void reserveBalance(ReserveBalanceRequest request, StreamObserver<ReservationResponse> responseObserver) {
        try {
            log.info("gRPC reserveBalance - accountId: {}, amount: {}",
                    request.getAccountId(), request.getAmount().getAmount());

            String accountId = request.getAccountId();
            if (accountId == null || accountId.isEmpty()) {
                Wallet wallet = walletService.getWallet(UUID.fromString(request.getWalletId()));
                accountId = wallet.getAccountId();
            }

            BigDecimal amount = new BigDecimal(request.getAmount().getAmount());
            String reservationId = walletService.reserveBalance(
                    accountId,
                    amount,
                    request.getReferenceId()
            );

            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            ReservationResponse response = ReservationResponse.newBuilder()
                    .setSuccess(true)
                    .setReservationId(reservationId)
                    .setReservedAmount(toMoney(amount, wallet.getCurrency()))
                    .setAvailableBalance(toMoney(wallet.getAvailableBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid reserveBalance request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in reserveBalance: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void commitReservation(CommitReservationRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            log.info("gRPC commitReservation - reservationId: {}", request.getReservationId());

            walletService.commitReservation(request.getReservationId());

            String accountId = walletService.getAccountIdByReservationId(request.getReservationId());
            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            TransactionResponse response = TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(request.getReservationId())
                    .setNewBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid commitReservation request: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in commitReservation: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void releaseReservation(ReleaseReservationRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            log.info("gRPC releaseReservation - reservationId: {}", request.getReservationId());

            walletService.releaseReservation(request.getReservationId());

            String accountId = walletService.getAccountIdByReservationId(request.getReservationId());
            Wallet wallet = walletService.getWalletByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            TransactionResponse response = TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(request.getReservationId())
                    .setNewBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                    .setTimestamp(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid releaseReservation request: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in releaseReservation: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // Helper methods

    private Money toMoney(BigDecimal amount, String currency) {
        return Money.newBuilder()
                .setAmount(amount.toPlainString())
                .setCurrency(currency != null ? currency : "IDR")
                .build();
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private id.payu.wallet.grpc.LedgerEntry toGrpcLedgerEntry(LedgerEntry entry) {
        return id.payu.wallet.grpc.LedgerEntry.newBuilder()
                .setEntryId(entry.getId().toString())
                .setWalletId(entry.getAccountId())
                .setAccountId(entry.getAccountId())
                .setTransactionId(entry.getTransactionId().toString())
                .setType(entry.getEntryType() == id.payu.wallet.domain.model.EntryType.DEBIT ? id.payu.wallet.grpc.EntryType.DEBIT : id.payu.wallet.grpc.EntryType.CREDIT)
                .setAmount(toMoney(entry.getAmount(), entry.getCurrency()))
                .setBalanceAfter(toMoney(entry.getBalanceAfter(), entry.getCurrency()))
                .setReferenceType(entry.getReferenceType())
                .setReferenceId(entry.getReferenceId())
                .setTimestamp(toTimestamp(entry.getCreatedAt()))
                .build();
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.newBuilder()
                .setWalletId(wallet.getId().toString())
                .setAccountId(wallet.getAccountId())
                .setAccountNumber(wallet.getAccountId())
                .setBalance(toMoney(wallet.getBalance(), wallet.getCurrency()))
                .setAvailableBalance(toMoney(wallet.getAvailableBalance(), wallet.getCurrency()))
                .setReservedBalance(toMoney(wallet.getReservedBalance(), wallet.getCurrency()))
                .setCurrency(wallet.getCurrency())
                .setStatus(toWalletStatus(wallet.getStatus()))
                .setCreatedAt(toTimestamp(wallet.getCreatedAt()))
                .setUpdatedAt(toTimestamp(wallet.getUpdatedAt()))
                .build();
    }

    private WalletStatus toWalletStatus(id.payu.wallet.domain.model.WalletStatus status) {
        return switch (status) {
            case ACTIVE -> WalletStatus.ACTIVE;
            case FROZEN -> WalletStatus.FROZEN;
            case CLOSED -> WalletStatus.CLOSED;
            default -> WalletStatus.WALLET_STATUS_UNSPECIFIED;
        };
    }
}
