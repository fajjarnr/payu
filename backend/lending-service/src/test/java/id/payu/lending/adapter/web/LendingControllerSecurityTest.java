package id.payu.lending.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.lending.application.security.LendingSecurityService;
import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.application.service.LoanManagementService;
import id.payu.lending.application.service.LoanPreApprovalService;
import id.payu.lending.application.service.PayLaterTransactionService;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.dto.LoanApplicationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LendingController security - BUG-BE-147 fix.
 * Verifies that loan application endpoint:
 * 1. Requires authentication (@PreAuthorize)
 * 2. Extracts userId from JWT, not request body
 * 3. Prevents applying loan on behalf of other users
 */
@ExtendWith(MockitoExtension.class)
class LendingControllerSecurityTest {

    @Mock
    private LendingApplicationService lendingApplicationService;

    @Mock
    private LoanManagementService loanManagementService;

    @Mock
    private PayLaterTransactionService payLaterTransactionService;

    @Mock
    private LoanPreApprovalService preApprovalService;

    @Mock
    private LendingSecurityService lendingSecurityService;

    private LendingController lendingController;

    @BeforeEach
    void setUp() {
        lendingController = new LendingController(
                lendingApplicationService,
                loanManagementService,
                payLaterTransactionService,
                preApprovalService,
                lendingSecurityService
        );
    }

    @Test
    void applyLoan_WithAuthenticatedUser_ShouldUsePrincipalUserId() {
        // Given: Authenticated user with UUID
        UUID authenticatedUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Principal principal = () -> authenticatedUserId.toString();

        LoanApplicationCommand command = new LoanApplicationCommand(
                "EXT-001",
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Emergency"
        );

        Loan expectedLoan = new Loan();
        expectedLoan.setId(UUID.randomUUID());
        expectedLoan.setUserId(authenticatedUserId);
        expectedLoan.setStatus(Loan.LoanStatus.APPROVED);

        when(lendingApplicationService.applyLoan(any()))
                .thenReturn(CompletableFuture.completedFuture(expectedLoan));

        // When: Apply for loan
        // Note: This test verifies the service is called with correct userId
        // Full HTTP integration test requires MockMvc with servlet context
        CompletableFuture<ResponseEntity<ApiResponse<Loan>>> result = lendingController.applyLoan(command, principal);

        // Then: Should use authenticated user's ID (service call verification)
        assertNotNull(result);

        // Verify the service was called with authenticated user's ID
        verify(lendingApplicationService).applyLoan(argThat(request ->
                request.userId().equals(authenticatedUserId) &&
                        request.externalId().equals("EXT-001") &&
                        request.principalAmount().equals(new BigDecimal("10000000"))
        ));
    }

    @Test
    void applyLoan_WithDifferentUserIdInBody_ShouldIgnoreAndUsePrincipal() {
        // Given: Authenticated user
        UUID authenticatedUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID maliciousUserId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        Principal principal = () -> authenticatedUserId.toString();

        // Command doesn't contain userId - it's extracted from Principal
        LoanApplicationCommand command = new LoanApplicationCommand(
                "EXT-002",
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("5000000"),
                6,
                "Business"
        );

        Loan expectedLoan = new Loan();
        expectedLoan.setId(UUID.randomUUID());
        expectedLoan.setUserId(authenticatedUserId);
        expectedLoan.setStatus(Loan.LoanStatus.APPROVED);

        when(lendingApplicationService.applyLoan(any()))
                .thenReturn(CompletableFuture.completedFuture(expectedLoan));

        // When: Apply for loan
        CompletableFuture<ResponseEntity<ApiResponse<Loan>>> result = lendingController.applyLoan(command, principal);

        // Then: Should use authenticated user's ID, not any potential malicious ID
        result.join();

        verify(lendingApplicationService).applyLoan(argThat(request ->
                request.userId().equals(authenticatedUserId) &&
                        !request.userId().equals(maliciousUserId)
        ));
    }

    @Test
    void applyLoan_WithoutPrincipal_ShouldFail() {
        // Given: No principal (unauthenticated)
        LoanApplicationCommand command = new LoanApplicationCommand(
                "EXT-003",
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Test"
        );

        // When/Then: Should throw exception when principal is null
        assertThrows(NullPointerException.class, () -> {
            lendingController.applyLoan(command, null);
        });

        verifyNoInteractions(lendingApplicationService);
    }

    @Test
    void applyLoan_WithInvalidPrincipalFormat_ShouldFail() {
        // Given: Invalid principal format
        Principal invalidPrincipal = () -> "not-a-valid-uuid";

        LoanApplicationCommand command = new LoanApplicationCommand(
                "EXT-004",
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Test"
        );

        // When/Then: Should throw IllegalArgumentException for invalid UUID
        assertThrows(IllegalArgumentException.class, () -> {
            lendingController.applyLoan(command, invalidPrincipal);
        });

        verifyNoInteractions(lendingApplicationService);
    }

    @Test
    void verifyApplyLoanEndpoint_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        // Verify that the applyLoan method has @PreAuthorize annotation
        var method = LendingController.class.getMethod("applyLoan",
                LoanApplicationCommand.class,
                Principal.class);

        var preAuthorizeAnnotation = method.getAnnotation(
                org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorizeAnnotation, "applyLoan should have @PreAuthorize annotation");
        assertEquals("isAuthenticated()", preAuthorizeAnnotation.value(),
                "@PreAuthorize should require authentication");
    }

    @Test
    void verifyApplyLoanEndpoint_AcceptsLoanApplicationCommand() throws NoSuchMethodException {
        // Verify that the applyLoan method accepts LoanApplicationCommand (without userId)
        var method = LendingController.class.getMethod("applyLoan",
                LoanApplicationCommand.class,
                Principal.class);

        var requestBodyAnnotation = method.getParameters()[0].getAnnotation(
                org.springframework.web.bind.annotation.RequestBody.class);

        assertNotNull(requestBodyAnnotation,
                "First parameter should have @RequestBody annotation");

        // Verify the parameter type is LoanApplicationCommand (not LoanApplicationRequest)
        assertEquals(LoanApplicationCommand.class, method.getParameterTypes()[0],
                "Request body should be LoanApplicationCommand (without userId field)");
    }
}
