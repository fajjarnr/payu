package id.payu.transaction.application.scheduler;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.TransferStatusPort;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"payu.grpc.server.port=0"})
@ActiveProfiles("test")
@DirtiesContext
class ReconciliationSchedulerTest {

    @Autowired ReconciliationScheduler scheduler;
    @Autowired TransactionJpaRepository txRepo;

    @MockitoBean TransferStatusPort transferStatusPort;
    @MockitoBean id.payu.transaction.domain.port.out.WalletServicePort walletServicePort;

    @Test
    void schedulerHasShedLockWithDbTimeAndCorrectName() throws Exception {
        Method m = ReconciliationScheduler.class.getMethod("reconcilePendingTransfers");
        assertThat(m.isAnnotationPresent(SchedulerLock.class)).isTrue();
        SchedulerLock lock = m.getAnnotation(SchedulerLock.class);
        assertThat(lock.name()).isEqualTo("biFastReconciliation");
        assertThat(lock.lockAtMostFor()).isEqualTo("9m");
        assertThat(lock.lockAtLeastFor()).isEqualTo("30s");
        // verify ShedLockConfig uses usingDbTime + UTC
        String src = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/main/java/id/payu/transaction/config/ShedLockConfig.java")));
        assertThat(src).contains("usingDbTime()");
        assertThat(src).contains("withTimeZone");
    }

    @Test
    void reconciliationPollsTransferStatusAndFixesMismatch() {
        String ref = "TXN-RECON-" + UUID.randomUUID().toString().substring(0,8);
        TransactionEntity tx = TransactionEntity.builder()
                .referenceNumber(ref)
                .senderAccountId(UUID.randomUUID())
                .amount(Money.idr("75000"))
                .type(TransactionType.BIFAST_TRANSFER)
                .status(TransactionStatus.PENDING)
                .reservationId("res-recon-001")
                .createdAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .updatedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();
        tx = txRepo.save(tx);
        // rail says SUCCESS (00) but local still PENDING -> reconciler should heal to COMPLETED
        when(transferStatusPort.getLatestTransactionStatus(ref)).thenReturn("00");

        scheduler.reconcilePendingTransfers();

        TransactionEntity healed = txRepo.findByReferenceNumber(ref).orElseThrow();
        assertThat(healed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void transferStatusEndpointReturnsSnapMapping() throws Exception {
        // verify controller exists and maps GET /snap/v1.0/transfer/status
        Class<?> ctrl = Class.forName("id.payu.transaction.adapter.web.TransferStatusController");
        var method = ctrl.getMethod("getTransferStatus", String.class);
        assertThat(method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)).isTrue();
        var mapping = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertThat(mapping.value()).contains("/status");
        var classMapping = ctrl.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertThat(String.join("", classMapping.value())).contains("/snap/v1.0/transfer");
    }
}
