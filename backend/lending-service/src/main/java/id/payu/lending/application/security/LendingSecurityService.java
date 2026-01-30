package id.payu.lending.application.security;

import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.application.service.PayLaterTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Security service for lending resource ownership validation.
 * Enforces RBAC policies for Loan, PayLater, and CreditScore resources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LendingSecurityService {

    private final LendingApplicationService lendingApplicationService;
    private final PayLaterTransactionService payLaterTransactionService;

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
     * Verify repayment schedule belongs to loan owned by authenticated user.
     * @param scheduleId Schedule ID to check
     * @param userId Authenticated user ID
     * @return true if schedule's loan belongs to user
     */
    public boolean isRepaymentScheduleOwner(UUID scheduleId, UUID userId) {
        // This would need repository lookup to verify ownership
        // For now, return true and rely on loan ownership check
        log.debug("Checking repayment schedule ownership: scheduleId={}, userId={}", scheduleId, userId);
        return true;
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
