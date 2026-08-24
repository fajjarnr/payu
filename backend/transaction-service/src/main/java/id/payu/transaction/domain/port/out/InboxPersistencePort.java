package id.payu.transaction.domain.port.out;

import java.util.Optional;

public interface InboxPersistencePort {
    boolean existsByReferenceNo(String referenceNo);
    Optional<String> findPayloadByReferenceNo(String referenceNo);
    void save(String referenceNo, String payload);
}
