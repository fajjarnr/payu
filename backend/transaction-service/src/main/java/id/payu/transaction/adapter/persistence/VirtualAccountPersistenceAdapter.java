package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.entity.VaPaymentRecordEntity;
import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.adapter.persistence.repository.VaPaymentRecordRepository;
import id.payu.transaction.adapter.persistence.repository.VirtualAccountRepository;
import id.payu.transaction.domain.port.out.VirtualAccountPersistencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VirtualAccountPersistenceAdapter implements VirtualAccountPersistencePort {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VirtualAccountPersistenceAdapter.class);

    private final VirtualAccountRepository virtualAccountRepository;
    private final VaPaymentRecordRepository vaPaymentRecordRepository;

    public VirtualAccountPersistenceAdapter(VirtualAccountRepository virtualAccountRepository,
                                            VaPaymentRecordRepository vaPaymentRecordRepository) {
        this.virtualAccountRepository = virtualAccountRepository;
        this.vaPaymentRecordRepository = vaPaymentRecordRepository;
    }

    @Override
    @Transactional
    public VirtualAccountEntity save(VirtualAccountEntity virtualAccount) {
        return virtualAccountRepository.save(virtualAccount);
    }

    @Override
    public Optional<VirtualAccountEntity> findById(UUID id) {
        return virtualAccountRepository.findById(id);
    }

    @Override
    public Optional<VirtualAccountEntity> findByVaNumber(String vaNumber) {
        return virtualAccountRepository.findByVaNumber(vaNumber);
    }

    @Override
    public List<VirtualAccountEntity> findExpiredPendingVAs(Instant now) {
        return virtualAccountRepository.findExpiredPendingVAs(now);
    }

    @Override
    @Transactional
    public List<VirtualAccountEntity> saveAll(Iterable<VirtualAccountEntity> virtualAccounts) {
        return virtualAccountRepository.saveAll(virtualAccounts);
    }

    @Override
    public boolean existsByVaNumber(String vaNumber) {
        return virtualAccountRepository.existsByVaNumber(vaNumber);
    }

    @Override
    public Optional<VirtualAccountEntity> findWithLockByVaNumber(String vaNumber) {
        return virtualAccountRepository.findWithLockByVaNumber(vaNumber);
    }

    @Override
    @Transactional
    public VaPaymentRecordEntity savePaymentRecord(VaPaymentRecordEntity record) {
        return vaPaymentRecordRepository.save(record);
    }

    @Override
    public int markExpiredIfPending(UUID id) {
        return virtualAccountRepository.markExpiredIfPending(id);
    }
}
