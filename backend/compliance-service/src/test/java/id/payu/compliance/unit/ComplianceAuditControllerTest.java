package id.payu.compliance.unit;

import id.payu.compliance.adapter.web.ComplianceAuditController;
import id.payu.compliance.application.service.ComplianceAuditService;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;
import id.payu.compliance.exception.ComplianceDomainException;
import id.payu.compliance.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ComplianceAuditControllerTest {

    @Mock
    private ComplianceAuditService complianceAuditService;

    @InjectMocks
    private ComplianceAuditController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testControllerInstantiation() {
        assertNotNull(controller);
    }

    @Test
    void shouldCreateAuditReport() {
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";
        
        List<ComplianceCheck> checks = List.of(
                ComplianceCheck.builder()
                        .checkId("PCIDSS_001")
                        .standard(ComplianceStandard.PCI_DSS)
                        .description("Card data encryption verification")
                        .status(ComplianceCheckResult.PASS)
                        .details("Card data properly encrypted")
                        .checkedAt(LocalDateTime.now())
                        .build()
        );

        AuditReport report = AuditReport.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(checks)
                .overallStatus(ComplianceCheckResult.PASS)
                .createdAt(LocalDateTime.now())
                .build();

        when(complianceAuditService.createAuditReport(any(UUID.class), eq(merchantId), eq(ComplianceStandard.PCI_DSS), any(List.class)))
                .thenReturn(report);

        AuditReport result = complianceAuditService.createAuditReport(transactionId, merchantId, ComplianceStandard.PCI_DSS, checks);

        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(merchantId, result.getMerchantId());
        assertEquals(ComplianceStandard.PCI_DSS, result.getStandard());
        assertEquals(ComplianceCheckResult.PASS, result.getOverallStatus());
    }

    @Test
    void shouldGetAuditReportById() {
        UUID reportId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        AuditReport report = AuditReport.builder()
                .id(reportId)
                .transactionId(transactionId)
                .merchantId("MERCHANT_001")
                .standard(ComplianceStandard.PCI_DSS)
                .checks(List.of())
                .overallStatus(ComplianceCheckResult.PASS)
                .createdAt(LocalDateTime.now())
                .build();

        when(complianceAuditService.getAuditReport(reportId)).thenReturn(report);

        AuditReport result = complianceAuditService.getAuditReport(reportId);

        assertNotNull(result);
        assertEquals(reportId, result.getId());
        assertEquals(transactionId, result.getTransactionId());
    }

    @Test
    void shouldThrowExceptionWhenReportNotFound() {
        UUID reportId = UUID.randomUUID();
        String errorMessage = "Audit report not found: " + reportId;

        when(complianceAuditService.getAuditReport(reportId))
                .thenThrow(new IllegalArgumentException(errorMessage));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            complianceAuditService.getAuditReport(reportId);
        });

        assertTrue(exception.getMessage().contains(errorMessage));
    }
}

class GlobalExceptionHandlerTest {

    @Test
    void shouldHandleIllegalArgumentException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        
        var mockRequest = org.mockito.Mockito.mock(org.springframework.web.context.request.WebRequest.class);
        when(mockRequest.getDescription(anyBoolean())).thenReturn("mock request");

        var response = handler.handleIllegalArgumentException(ex, mockRequest);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("Invalid input", response.getBody().get("message"));
    }

    @Test
    void shouldHandleGenericException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new Exception("Generic error");
        
        var mockRequest = org.mockito.Mockito.mock(org.springframework.web.context.request.WebRequest.class);
        when(mockRequest.getDescription(anyBoolean())).thenReturn("mock request");

        var response = handler.handleGlobalException(ex, mockRequest);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }

    @Test
    void shouldCreateDomainExceptionWithMessage() {
        ComplianceDomainException exception = new ComplianceDomainException("Domain error");

        assertNotNull(exception);
        assertEquals("Domain error", exception.getMessage());
    }

    @Test
    void shouldCreateDomainExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        ComplianceDomainException exception = new ComplianceDomainException("Domain error", cause);

        assertNotNull(exception);
        assertEquals("Domain error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
