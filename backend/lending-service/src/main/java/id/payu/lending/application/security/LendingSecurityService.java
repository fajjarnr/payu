package id.payu.lending.application.security;

import id.payu.lending.application.service.InstallmentService;
import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.application.service.LoanManagementService;
import id.payu.lending.application.service.LoanPreApprovalService;
import id.payu.lending.application.service.PayLaterTransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Security service for lending resource ownership validation.
 * Enforces RBAC policies for Loan, PayLater, and CreditScore resources.
 */
@Service
@RequiredArgsConstructor
public class LendingSecurityService {

    private static final Logger log = LoggerFactory.getLogger(LendingSecurityService.class);

    private final LendingApplicationService lendingApplicationService;
    private final PayLaterTransactionService payLaterTransactionService;
    private final LoanManagementService loanManagementService;
    private final LoanPreApprovalService preApprovalService;
    private final InstallmentService installmentService;

    /**
     * Verify loan belongs to authenticated user.
     * @param loanId Loan ID to check
     * @param userId Authenticated user ID
     * @throws AccessDeniedException if loan doesn't belong to user
     */
    public boolean isLoanOwner(UUID loanId, UUID userId) {
        log.debug("Checking loan ownership: loanId={}, userId={}", loanId, userId);
        return lendingApplicationService.getLoanById(loanId)
                .map(loan -> {
                    boolean isOwner = loan.getUserId().equals(userId);
                    if (!isOwner) {
                        log.warn("Access denied: User {} attempted to access loan {} belonging to user {}",
                                userId, loanId, loan.getUserId());
                    }
                    return isOwner;
                })
                .orElse(false);
    }

    /**
     * Verify PayLater account belongs to authenticated user.
     * @param paylaterUserId User ID from path
     * @param authenticatedUserId Authenticated user ID
     * @return true if IDs match
     */
    public boolean isPaylaterOwner(UUID paylaterUserId, UUID authenticatedUserId) {
        boolean isOwner = paylaterUserId.equals(authenticatedUserId);
        if (!isOwner) {
            log.warn("Access denied: User {} attempted to access PayLater for user {}",
                    authenticatedUserId, paylaterUserId);
        }
        return isOwner;
    }

    /**
     * Verify credit score belongs to authenticated user.
     * @param creditScoreUserId User ID from path
     * @param authenticatedUserId Authenticated user ID
     * @return true if IDs match
     */
    public boolean isCreditScoreOwner(UUID creditScoreUserId, UUID authenticatedUserId) {
        boolean isOwner = creditScoreUserId.equals(authenticatedUserId);
        if (!isOwner) {
            log.warn("Access denied: User {} attempted to access credit score for user {}",
                    authenticatedUserId, creditScoreUserId);
        }
        return isOwner;
    }

    /**
     * BUG-SECURITY-018 FIX: Verify repayment schedule belongs to loan owned by authenticated user.
     * @param scheduleId Schedule ID to check
     * @param userId Authenticated user ID
     * @return true if schedule's loan belongs to user
     */
    public boolean isRepaymentScheduleOwner(UUID scheduleId, UUID userId) {
        log.debug("Checking repayment schedule ownership: scheduleId={}, userId={}", scheduleId, userId);
        return loanManagementService.getRepaymentSchedule(scheduleId)
                .map(schedule -> isLoanOwner(schedule.getLoanId(), userId))
                .orElse(false);
    }
 
    /**
     * BUG-SECURITY-019 FIX: Verify pre-approval belongs to authenticated user.
     * @param preApprovalId Pre-approval ID to check
     * @param userId Authenticated user ID
     * @return true if IDs match
     */
    public boolean isPreApprovalOwnerById(UUID preApprovalId, UUID userId) {
        log.debug("Checking pre-approval ownership: preApprovalId={}, userId={}", preApprovalId, userId);
        return preApprovalService.getPreApprovalById(preApprovalId)
                .map(pre -> pre.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * BUG-SECURITY-021 FIX: Verify installment checkout belongs to authenticated user.
     * @param checkoutId Checkout ID to check
     * @param userId Authenticated user ID
     * @return true if IDs match
     */
    public boolean isInstallmentOwner(UUID checkoutId, UUID userId) {
        log.debug("Checking installment ownership: checkoutId={}, userId={}", checkoutId, userId);
        try {
            id.payu.lending.domain.model.InstallmentCheckout checkout = installmentService.getCheckout(checkoutId);
            return checkout.getUserId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify pre-approval belongs to authenticated user.
     * @param preApprovalUserId User ID from path
     * @param authenticatedUserId Authenticated user ID
     * @return true if IDs match
     */
    public boolean isPreApprovalOwner(UUID preApprovalUserId, UUID authenticatedUserId) {
        boolean isOwner = preApprovalUserId.equals(authenticatedUserId);
        if (!isOwner) {
            log.warn("Access denied: User {} attempted to access pre-approval for user {}",
                    authenticatedUserId, preApprovalUserId);
        }
        return isOwner;
    }
}
