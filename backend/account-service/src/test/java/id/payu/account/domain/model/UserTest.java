package id.payu.account.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACCOUNT-006: exhaustive core-domain coverage for {@link User}.
 */
@DisplayName("User domain")
class UserTest {

    private User fullUser() {
        return new User(UUID.randomUUID(), "ext-1", "user1", "user1@payu.id",
                "0812", "User One", "3201010101010001", UserStatus.ACTIVE,
                KycStatus.VERIFIED, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void constructorAndGetters() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User u = new User(id, "ext-1", "user1", "u@payu.id", "0812", "User One",
                "3201", UserStatus.LOCKED, KycStatus.PENDING, now, now);

        assertThat(u.getId()).isEqualTo(id);
        assertThat(u.getExternalId()).isEqualTo("ext-1");
        assertThat(u.getUsername()).isEqualTo("user1");
        assertThat(u.getEmail()).isEqualTo("u@payu.id");
        assertThat(u.getPhoneNumber()).isEqualTo("0812");
        assertThat(u.getFullName()).isEqualTo("User One");
        assertThat(u.getNik()).isEqualTo("3201");
        assertThat(u.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(u.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(u.getCreatedAt()).isEqualTo(now);
        assertThat(u.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void setters() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setExternalId("ext-2");
        u.setUsername("user2");
        u.setEmail("user2@payu.id");
        u.setPhoneNumber("0813");
        u.setFullName("User Two");
        u.setNik("3202");
        u.setStatus(UserStatus.SUSPENDED);
        u.setKycStatus(KycStatus.REJECTED);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        assertThat(u.getExternalId()).isEqualTo("ext-2");
        assertThat(u.getUsername()).isEqualTo("user2");
        assertThat(u.getEmail()).isEqualTo("user2@payu.id");
        assertThat(u.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(u.getKycStatus()).isEqualTo(KycStatus.REJECTED);
    }

    @Test
    void builder() {
        User u = User.builder()
                .id(UUID.randomUUID())
                .externalId("ext-3")
                .username("user3")
                .email("user3@payu.id")
                .phoneNumber("0814")
                .fullName("User Three")
                .nik("3203")
                .status(UserStatus.PENDING_VERIFICATION)
                .kycStatus(KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThat(u.getUsername()).isEqualTo("user3");
        assertThat(u.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void mutators() {
        User u = fullUser();
        u.setStatus(UserStatus.ACTIVE);
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        u.setKycStatus(KycStatus.VERIFIED);
        assertThat(u.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    }
}
