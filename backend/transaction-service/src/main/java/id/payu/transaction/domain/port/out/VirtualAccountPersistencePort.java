package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VirtualAccountPersistencePort {
    VirtualAccountEntity save(VirtualAccountEntity virtualAccount);
    Optional<VirtualAccountEntity> findById(UUID id);
    Optional<VirtualAccountEntity> findByVaNumber(String vaNumber);
    List<VirtualAccountEntity> findExpiredPendingVAs(Instant now);
    List<VirtualAccountEntity> saveAll(Iterable<VirtualAccountEntity> virtualAccounts);
    boolean existsByVaNumber(String vaNumber);
}
