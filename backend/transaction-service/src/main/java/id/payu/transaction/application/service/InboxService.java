package id.payu.transaction.application.service;

import id.payu.transaction.domain.port.out.InboxPersistencePort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxService {
    private final InboxPersistencePort inboxPort;

    public InboxService(InboxPersistencePort inboxPort) {
        this.inboxPort = inboxPort;
    }

    /**
     * Try to mark referenceNo as processed. Returns true if first time, false if duplicate.
     * Uses DB unique constraint on reference_no as final guard (race-free).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkProcessed(String referenceNo, String payload) {
        if (inboxPort.existsByReferenceNo(referenceNo)) {
            return false;
        }
        try {
            inboxPort.save(referenceNo, payload);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    public boolean isDuplicate(String referenceNo) {
        return inboxPort.existsByReferenceNo(referenceNo);
    }
}
