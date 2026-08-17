package id.payu.transaction.application.security;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.domain.port.in.SplitBillUseCase;
import id.payu.transaction.interfaces.dto.SplitBillResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Security service for split bill ownership validation.
 * Enforces RBAC policies for split bill resources.
 */
@Service
public class SplitBillSecurityService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SplitBillSecurityService.class);



    private final SplitBillUseCase splitBillUseCase;

    public SplitBillSecurityService(SplitBillUseCase splitBillUseCase) {
        this.splitBillUseCase = splitBillUseCase;
    }

    /**
     * Verify user is the creator/owner of the split bill.
     * @param splitBillId Split bill ID to check
     * @param userId Authenticated user ID
     * @return true if user is the creator
     */
    public boolean isOwner(UUID splitBillId, UUID userId) {
        log.debug("Checking split bill ownership: splitBillId={}, userId={}", splitBillId, userId);
        try {
            SplitBillResponse response = splitBillUseCase.getSplitBill(splitBillId);
            boolean isOwner = response.getCreatorAccountId().equals(userId);
            if (!isOwner) {
                log.warn("Access denied: User {} attempted to access split bill {} belonging to user {}",
                        userId, splitBillId, response.getCreatorAccountId());
            }
            return isOwner;
        } catch (Exception e) {
            log.error("Error checking split bill ownership", e);
            return false;
        }
    }

    /**
     * Verify user is a participant in the split bill.
     * @param splitBillId Split bill ID to check
     * @param userId Authenticated user ID
     * @return true if user is a participant
     */
    public boolean isParticipant(UUID splitBillId, UUID userId) {
        log.debug("Checking split bill participation: splitBillId={}, userId={}", splitBillId, userId);
        try {
            SplitBillResponse response = splitBillUseCase.getSplitBill(splitBillId);
            boolean isCreator = response.getCreatorAccountId().equals(userId);
            boolean isParticipant = response.getParticipants() != null &&
                    response.getParticipants().stream()
                            .anyMatch(p -> p.getAccountId().equals(userId));
            boolean hasAccess = isCreator || isParticipant;
            if (!hasAccess) {
                log.warn("Access denied: User {} is not a participant in split bill {}",
                        userId, splitBillId);
            }
            return hasAccess;
        } catch (Exception e) {
            log.error("Error checking split bill participation", e);
            return false;
        }
    }

    /**
     * Verify user is either creator or participant for read operations.
     * @param splitBillId Split bill ID to check
     * @param userId Authenticated user ID
     * @return true if user has read access
     */
    public boolean hasReadAccess(UUID splitBillId, UUID userId) {
        return isParticipant(splitBillId, userId);
    }

    /**
     * Verify user can accept/decline the split bill invitation.
     * @param splitBillId Split bill ID
     * @param participantId Participant ID from path
     * @param userId Authenticated user ID
     * @return true if user is the participant
     */
    public boolean canRespondToInvitation(UUID splitBillId, UUID participantId, UUID userId) {
        log.debug("Checking invitation response permission: splitBillId={}, participantId={}, userId={}",
                splitBillId, participantId, userId);
        try {
            SplitBillResponse response = splitBillUseCase.getSplitBill(splitBillId);
            return response.getParticipants() != null &&
                    response.getParticipants().stream()
                            .filter(p -> p.getId().equals(participantId))
                            .findFirst()
                            .map(p -> p.getAccountId().equals(userId))
                            .orElse(false);
        } catch (Exception e) {
            log.error("Error checking invitation response permission", e);
            return false;
        }
    }

    /**
     * Verify user can make payment for a participant.
     * @param splitBillId Split bill ID
     * @param participantId Participant ID from path
     * @param userId Authenticated user ID
     * @return true if user is the participant
     */
    public boolean canMakePayment(UUID splitBillId, UUID participantId, UUID userId) {
        return canRespondToInvitation(splitBillId, participantId, userId);
    }
}
