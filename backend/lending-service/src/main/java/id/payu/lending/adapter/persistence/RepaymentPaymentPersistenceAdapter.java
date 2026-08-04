package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.RepaymentPayment;
import id.payu.lending.domain.port.out.RepaymentPaymentPersistencePort;
import id.payu.lending.entity.RepaymentPaymentEntity;
import id.payu.lending.repository.RepaymentPaymentRepository;
import id.payu.security.multitenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RepaymentPaymentPersistenceAdapter implements RepaymentPaymentPersistencePort {

    private final RepaymentPaymentRepository repository;

    public RepaymentPaymentPersistenceAdapter(RepaymentPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public RepaymentPayment save(RepaymentPayment payment) {
        RepaymentPaymentEntity entity = toEntity(payment);
        if (entity.getTenantId() == null || entity.getTenantId().isBlank()) {
            entity.setTenantId(TenantContext.getTenantId());
        }
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<RepaymentPayment> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<RepaymentPayment> findByStatusIn(List<id.payu.lending.domain.model.RepaymentPaymentStatus> statuses) {
        return repository.findByStatusIn(statuses).stream().map(this::toDomain).toList();
    }

    private RepaymentPaymentEntity toEntity(RepaymentPayment source) {
        RepaymentPaymentEntity target = source.getId() == null
                ? new RepaymentPaymentEntity()
                : repository.findById(source.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Repayment payment not found: " + source.getId()));
        target.setId(source.getId());
        target.setRepaymentScheduleId(source.getRepaymentScheduleId());
        target.setLoanId(source.getLoanId());
        target.setUserId(source.getUserId());
        target.setAmount(source.getAmount());
        target.setCurrency(source.getCurrency());
        target.setIdempotencyKey(source.getIdempotencyKey());
        target.setStatus(source.getStatus());
        target.setWalletTransactionId(source.getWalletTransactionId());
        target.setFailureReason(source.getFailureReason());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private RepaymentPayment toDomain(RepaymentPaymentEntity source) {
        RepaymentPayment target = new RepaymentPayment();
        target.setId(source.getId());
        target.setRepaymentScheduleId(source.getRepaymentScheduleId());
        target.setLoanId(source.getLoanId());
        target.setUserId(source.getUserId());
        target.setAmount(source.getAmount());
        target.setCurrency(source.getCurrency());
        target.setIdempotencyKey(source.getIdempotencyKey());
        target.setStatus(source.getStatus());
        target.setWalletTransactionId(source.getWalletTransactionId());
        target.setFailureReason(source.getFailureReason());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
