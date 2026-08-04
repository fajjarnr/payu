package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.RepaymentPayment;
import id.payu.lending.domain.model.RepaymentPaymentStatus;
import id.payu.lending.entity.RepaymentPaymentEntity;
import id.payu.lending.repository.RepaymentPaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepaymentPaymentPersistenceAdapterTest {

    private final RepaymentPaymentRepository repository = mock(RepaymentPaymentRepository.class);
    private final RepaymentPaymentPersistenceAdapter adapter =
            new RepaymentPaymentPersistenceAdapter(repository);

    @Test
    void updatesExistingGeneratedIdEntityWithoutRecreatingDetachedInstance() {
        UUID paymentId = UUID.randomUUID();
        RepaymentPayment payment = new RepaymentPayment();
        payment.setId(paymentId);
        payment.setRepaymentScheduleId(UUID.randomUUID());
        payment.setLoanId(UUID.randomUUID());
        payment.setUserId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("1078000.0000"));
        payment.setCurrency("IDR");
        payment.setIdempotencyKey("repayment-key");
        payment.setStatus(RepaymentPaymentStatus.COMPLETED);

        RepaymentPaymentEntity managedEntity = new RepaymentPaymentEntity();
        managedEntity.setId(paymentId);
        when(repository.findById(paymentId)).thenReturn(Optional.of(managedEntity));
        when(repository.save(any(RepaymentPaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RepaymentPayment saved = adapter.save(payment);

        verify(repository).findById(paymentId);
        verify(repository).save(same(managedEntity));
        assertThat(saved.getId()).isEqualTo(paymentId);
        assertThat(managedEntity.getStatus()).isEqualTo(RepaymentPaymentStatus.COMPLETED);
    }
}
