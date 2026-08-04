package id.payu.dispute.adapter.persistence;

import id.payu.dispute.adapter.persistence.entity.RefundEntity;
import id.payu.dispute.adapter.persistence.repository.RefundJpaRepository;
import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPersistenceAdapterTest {

    @Mock
    private RefundJpaRepository refundJpaRepository;

    @Test
    void updatesManagedEntityWhenSavingExistingRefund() {
        UUID refundId = UUID.randomUUID();
        RefundEntity managedEntity = RefundEntity.builder()
                .id(refundId)
                .status(RefundStatus.PENDING)
                .build();
        Refund refund = Refund.builder()
                .id(refundId)
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("IDR")
                .reason("test")
                .status(RefundStatus.PROCESSING)
                .build();
        when(refundJpaRepository.findById(refundId)).thenReturn(Optional.of(managedEntity));
        when(refundJpaRepository.save(any(RefundEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund saved = new RefundPersistenceAdapter(refundJpaRepository).save(refund);

        assertThat(managedEntity.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        assertThat(saved.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        verify(refundJpaRepository).save(same(managedEntity));
    }
}
