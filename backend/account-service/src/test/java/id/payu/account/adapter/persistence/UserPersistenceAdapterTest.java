package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.entity.ProfileEntity;
import id.payu.account.adapter.persistence.entity.UserEntity;
import id.payu.account.adapter.persistence.repository.AccountRepository;
import id.payu.account.adapter.persistence.repository.ProfileRepository;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.domain.model.User;
import id.payu.security.crypto.BlindIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: UserPersistenceAdapter coverage.
 */
@DisplayName("UserPersistenceAdapter")
class UserPersistenceAdapterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final BlindIndexService blindIndexService = mock(BlindIndexService.class);

    private final UserPersistenceAdapter adapter =
            new UserPersistenceAdapter(userRepository, profileRepository, accountRepository, blindIndexService);

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .externalId("ext-1")
                .email("User@Payu.Id")
                .phoneNumber(" 08123456789 ")
                .username("johndoe")
                .status(id.payu.account.domain.model.UserStatus.ACTIVE)
                .kycStatus(id.payu.account.domain.model.KycStatus.VERIFIED)
                .build();
    }

    private UserEntity entity() {
        UserEntity e = new UserEntity();
        e.setId(UUID.randomUUID());
        e.setExternalId("ext-1");
        e.setStatus(id.payu.account.adapter.persistence.entity.UserStatus.ACTIVE);
        e.setKycStatus(id.payu.account.adapter.persistence.entity.KycStatus.VERIFIED);
        return e;
    }

    @Test
    void saveIndexesEmailAndPhone() {
        User u = user();
        UserEntity e = entity();
        when(blindIndexService.index("user@payu.id")).thenReturn("email-hash");
        when(blindIndexService.index("08123456789")).thenReturn("phone-hash");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        adapter.save(u);

        verify(blindIndexService).index("user@payu.id");
        verify(blindIndexService).index("08123456789");
    }

    @Test
    void saveSavesProfileWhenFullNamePresent() {
        User u = user();
        u.setFullName("John Doe");
        UserEntity e = entity();
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findById(e.getId())).thenReturn(Optional.empty());
        when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(i -> i.getArgument(0));

        adapter.save(u);

        verify(profileRepository).save(any(ProfileEntity.class));
    }

    @Test
    void findByIdMapsEntity() {
        UserEntity e = entity();
        when(userRepository.findById(e.getId())).thenReturn(Optional.of(e));
        assertThat(adapter.findById(e.getId())).isPresent();
    }

    @Test
    void findByEmailUsesHash() {
        when(blindIndexService.index("a@b.id")).thenReturn("hash");
        when(userRepository.findByEmailHash("hash")).thenReturn(Optional.of(entity()));
        assertThat(adapter.findByEmail("a@b.id")).isPresent();

        assertThat(adapter.findByEmail("  ")).isEmpty();
        when(blindIndexService.index("c@d.id")).thenReturn("h2");
        when(userRepository.findByEmailHash("h2")).thenReturn(Optional.empty());
        assertThat(adapter.findByEmail("c@d.id")).isEmpty();
    }

    @Test
    void existsByEmailAndUsername() {
        when(blindIndexService.index("a@b.id")).thenReturn("h");
        when(userRepository.existsByEmailHash("h")).thenReturn(true);
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThat(adapter.existsByEmail("a@b.id")).isTrue();
        assertThat(adapter.existsByUsername("user")).isFalse();
    }

    @Test
    void findByPhoneNumberHandlesNullHash() {
        assertThat(adapter.findByPhoneNumber(null)).isEmpty();
        assertThat(adapter.findByPhoneNumber("  ")).isEmpty();

        when(blindIndexService.index("0811")).thenReturn("ph");
        when(userRepository.findByPhoneNumberHash("ph")).thenReturn(Optional.of(entity()));
        assertThat(adapter.findByPhoneNumber("0811")).isPresent();
    }

    @Test
    void findAccountIdsByUserIdMapsAccountIds() {
        UUID userId = UUID.randomUUID();
        id.payu.account.adapter.persistence.entity.AccountEntity a = new id.payu.account.adapter.persistence.entity.AccountEntity();
        a.setId(UUID.randomUUID());
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(a));

        assertThat(adapter.findAccountIdsByUserId(userId)).containsExactly(a.getId());
    }
}
