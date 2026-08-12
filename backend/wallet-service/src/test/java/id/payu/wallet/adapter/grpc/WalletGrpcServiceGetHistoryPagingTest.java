package id.payu.wallet.adapter.grpc;

import id.payu.wallet.application.service.WalletService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LedgerEntry;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GRPC-015: getHistory must honor PageRequest — not stream the full history.
 */
class WalletGrpcServiceGetHistoryPagingTest {

    private WalletService walletService;
    private WalletGrpcService service;

    @BeforeEach
    void setUp() {
        walletService = mock(WalletService.class);
        service = new WalletGrpcService(walletService);
    }

    private List<LedgerEntry> entries(int count) {
        List<LedgerEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(LedgerEntry.builder()
                    .id(UUID.randomUUID())
                    .transactionId(UUID.randomUUID())
                    .accountId("ACC-1")
                    .entryType(EntryType.DEBIT)
                    .amount(new BigDecimal("10.0000"))
                    .currency("IDR")
                    .balanceAfter(new BigDecimal("100.0000"))
                    .referenceType("TRANSFER")
                    .referenceId("REF-" + i)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return list;
    }

    private static class RecordingObserver implements StreamObserver<id.payu.wallet.grpc.LedgerEntry> {
        final List<id.payu.wallet.grpc.LedgerEntry> received = new ArrayList<>();
        Throwable error;

        @Override
        public void onNext(id.payu.wallet.grpc.LedgerEntry value) {
            received.add(value);
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
        }
    }

    @Test
    void getHistoryReturnsOnlyRequestedPage() {
        when(walletService.getLedgerEntriesByAccountId("ACC-1")).thenReturn(entries(25));

        id.payu.wallet.grpc.GetHistoryRequest request = id.payu.wallet.grpc.GetHistoryRequest.newBuilder()
                .setAccountId("ACC-1")
                .setPage(id.payu.grpc.common.PageRequest.newBuilder().setPage(1).setSize(10).build())
                .build();
        RecordingObserver observer = new RecordingObserver();

        service.getHistory(request, observer);

        assertThat(observer.error).isNull();
        assertThat(observer.received).hasSize(10);
        assertThat(observer.received.get(0).getReferenceId()).isEqualTo("REF-10");
        assertThat(observer.received.get(9).getReferenceId()).isEqualTo("REF-19");
    }

    @Test
    void getHistoryWithoutPageStreamsEverything() {
        when(walletService.getLedgerEntriesByAccountId("ACC-1")).thenReturn(entries(5));

        RecordingObserver observer = new RecordingObserver();
        service.getHistory(id.payu.wallet.grpc.GetHistoryRequest.newBuilder()
                .setAccountId("ACC-1").build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.received).hasSize(5);
    }

    @Test
    void getHistoryPageBeyondRangeStreamsNothing() {
        when(walletService.getLedgerEntriesByAccountId("ACC-1")).thenReturn(entries(5));

        RecordingObserver observer = new RecordingObserver();
        service.getHistory(id.payu.wallet.grpc.GetHistoryRequest.newBuilder()
                .setAccountId("ACC-1")
                .setPage(id.payu.grpc.common.PageRequest.newBuilder().setPage(9).setSize(10).build())
                .build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.received).isEmpty();
    }
}
