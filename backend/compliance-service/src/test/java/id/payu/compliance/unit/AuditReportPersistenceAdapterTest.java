package id.payu.compliance.unit;

import id.payu.compliance.adapter.persistence.AuditReportPersistenceAdapter;
import id.payu.compliance.adapter.persistence.repository.AuditReportRepository;
import id.payu.compliance.adapter.persistence.entity.AuditReportEntity;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditReportPersistenceAdapterTest {

    @Mock
    private AuditReportRepository repository;

    private AuditReportPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuditReportPersistenceAdapter(repository);
    }

    @Test
    void shouldSaveAuditReport() {
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        AuditReport report = AuditReport.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(List.of())
                .overallStatus(ComplianceCheckResult.PASS)
                .createdAt(LocalDateTime.now())
                .build();

        AuditReportEntity persisted = AuditReportEntity.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(List.of())
                .overallStatus(ComplianceCheckResult.PASS)
                .createdAt(report.getCreatedAt())
                .build();
        when(repository.save(any(AuditReportEntity.class))).thenReturn(persisted);

        AuditReport result = adapter.save(report);

        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        verify(repository, times(1)).save(any(AuditReportEntity.class));
    }

    @Test
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        AuditReportEntity report = AuditReportEntity.builder()
                .id(id)
                .transactionId(UUID.randomUUID())
                .merchantId("MERCHANT_001")
                .standard(ComplianceStandard.PCI_DSS)
                .checks(List.of())
                .overallStatus(ComplianceCheckResult.PASS)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(report));

        Optional<AuditReport> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(repository, times(1)).findById(id);
    }

    @Test
    void shouldReturnEmptyWhenNotFoundById() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<AuditReport> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(id);
    }

    @Test
    void shouldFindByTransactionId() {
        UUID transactionId = UUID.randomUUID();
        List<AuditReportEntity> reports = List.of(
                AuditReportEntity.builder()
                        .id(UUID.randomUUID())
                        .transactionId(transactionId)
                        .merchantId("MERCHANT_001")
                        .standard(ComplianceStandard.PCI_DSS)
                        .checks(List.of())
                        .overallStatus(ComplianceCheckResult.PASS)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(repository.findByTransactionId(transactionId)).thenReturn(reports);

        List<AuditReport> result = adapter.findByTransactionId(transactionId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transactionId, result.get(0).getTransactionId());
        verify(repository, times(1)).findByTransactionId(transactionId);
    }

    @Test
    void shouldFindByMerchantId() {
        String merchantId = "MERCHANT_001";
        List<AuditReportEntity> reports = List.of(
                AuditReportEntity.builder()
                        .id(UUID.randomUUID())
                        .transactionId(UUID.randomUUID())
                        .merchantId(merchantId)
                        .standard(ComplianceStandard.PCI_DSS)
                        .checks(List.of())
                        .overallStatus(ComplianceCheckResult.PASS)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(repository.findByMerchantId(merchantId)).thenReturn(reports);

        List<AuditReport> result = adapter.findByMerchantId(merchantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(merchantId, result.get(0).getMerchantId());
        verify(repository, times(1)).findByMerchantId(merchantId);
    }
}
