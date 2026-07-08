package id.payu.fx.adapter.persistence;

import id.payu.fx.adapter.persistence.entity.FxRateEntity;
import id.payu.fx.adapter.persistence.repository.FxRateJpaRepository;
import id.payu.fx.domain.model.FxRate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxRatePersistenceAdapterTest {

    @Mock
    private FxRateJpaRepository repository;

    @Test
    void saveShouldGenerateIdWhenDomainRateHasNoId() {
        FxRatePersistenceAdapter adapter = new FxRatePersistenceAdapter(repository);
        FxRate rate = FxRate.builder()
                .fromCurrency("IDR")
                .toCurrency("USD")
                .rate(new BigDecimal("0.000065"))
                .inverseRate(new BigDecimal("15384.62"))
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusMinutes(15))
                .build();

        when(repository.save(any(FxRateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FxRate saved = adapter.save(rate);

        ArgumentCaptor<FxRateEntity> entityCaptor = ArgumentCaptor.forClass(FxRateEntity.class);
        verify(repository).save(entityCaptor.capture());

        assertThat(entityCaptor.getValue().getId()).isNotNull();
        assertThat(entityCaptor.getValue().isNew()).isTrue();
        assertThat(saved.getId()).isEqualTo(entityCaptor.getValue().getId());
    }
}
