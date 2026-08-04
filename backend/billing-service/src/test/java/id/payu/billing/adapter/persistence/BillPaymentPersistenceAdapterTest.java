package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.BillPaymentRepository;
import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.model.PaymentStatus;
import id.payu.billing.infrastructure.persistence.entity.BillPaymentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillPaymentPersistenceAdapterTest {

    @Mock
    private BillPaymentRepository repository;

    @Test
    void updatesExistingEntityWithoutDroppingOptimisticVersion() {
        UUID id = UUID.randomUUID();
        BillPaymentEntity existing = new BillPaymentEntity();
        existing.setId(id);
        existing.setVersion(7L);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(BillPaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BillPayment payment = new BillPayment();
        payment.setId(id);
        payment.setAccountId("account-123");
        payment.setReferenceNumber("BILL-123");
        payment.setBillerType(BillerType.PLN);
        payment.setCustomerId("12345678901234");
        payment.setAmount(new BigDecimal("100000"));
        payment.setAdminFee(new BigDecimal("2500"));
        payment.setTotalAmount(new BigDecimal("102500"));
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setVersion(7L);

        new BillPaymentPersistenceAdapter(repository).save(payment);

        assertEquals(7L, existing.getVersion());
        verify(repository).save(existing);
    }
}
