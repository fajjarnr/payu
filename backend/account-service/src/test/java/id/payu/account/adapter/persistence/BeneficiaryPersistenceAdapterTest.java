package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.entity.BeneficiaryEntity;
import id.payu.account.domain.model.BeneficiaryStatus;
import id.payu.account.adapter.persistence.entity.UserEntity;
import id.payu.account.adapter.persistence.repository.BeneficiaryRepository;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.domain.model.Beneficiary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: BeneficiaryPersistenceAdapter coverage.
 */
@DisplayName("BeneficiaryPersistenceAdapter")
class BeneficiaryPersistenceAdapterTest {

    private final BeneficiaryRepository benRepo = mock(BeneficiaryRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final BeneficiaryPersistenceAdapter adapter =
            new BeneficiaryPersistenceAdapter(benRepo, userRepo);

    private BeneficiaryEntity entity() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        BeneficiaryEntity e = new BeneficiaryEntity();
        e.setId(UUID.randomUUID());
        e.setUser(user);
        e.setBankCode("011");
        e.setAccountNumber("1234567890");
        e.setAccountName("Ali");
        e.setStatus(BeneficiaryStatus.ACTIVE);
        return e;
    }

    @Test
    void savePersistsAndReturnsDomain() {
        Beneficiary b = Beneficiary.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .bankCode("011")
                .accountNumber("1234567890")
                .accountName("Ali")
                .status(id.payu.account.domain.model.BeneficiaryStatus.ACTIVE)
                .build();
        when(benRepo.save(any(BeneficiaryEntity.class))).thenAnswer(i -> i.getArgument(0));

        Beneficiary saved = adapter.save(b);

        assertThat(saved.getAccountName()).isEqualTo("Ali");
    }

    @Test
    void findByIdMapsEntity() {
        BeneficiaryEntity e = entity();
        when(benRepo.findById(e.getId())).thenReturn(Optional.of(e));

        Optional<Beneficiary> found = adapter.findById(e.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(e.getUser().getId());
        assertThat(found.get().getBankCode()).isEqualTo("011");
    }

    @Test
    void findActiveByUserIdAndCount() {
        UUID userId = UUID.randomUUID();
        when(benRepo.findActiveByUserId(userId)).thenReturn(List.of(entity()));
        when(benRepo.countActiveByUserId(userId)).thenReturn(1L);

        assertThat(adapter.findActiveByUserId(userId)).hasSize(1);
        assertThat(adapter.countActiveByUserId(userId)).isEqualTo(1L);
    }

    @Test
    void duplicateChecks() {
        UUID userId = UUID.randomUUID();
        BeneficiaryEntity e = entity();
        when(benRepo.findByUserIdAndBankCodeAndAccountNumber(userId, "011", "1234567890"))
                .thenReturn(Optional.of(e));
        when(benRepo.existsByUserIdAndBankCodeAndAccountNumber(userId, "011", "1234567890"))
                .thenReturn(true);

        assertThat(adapter.findByUserIdAndBankCodeAndAccountNumber(userId, "011", "1234567890")).isPresent();
        assertThat(adapter.existsByUserIdAndBankCodeAndAccountNumber(userId, "011", "1234567890")).isTrue();
    }
}
