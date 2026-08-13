package id.payu.account.application.service;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: AccountSecurityService coverage.
 */
@DisplayName("AccountSecurityService")
class AccountSecurityServiceTest {

    private final UserPersistencePort userPort = mock(UserPersistencePort.class);
    private final AccountSecurityService service =
            new AccountSecurityService(null, userPort);

    private Authentication auth(String subject) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(subject).build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void rejectsNullArguments() {
        assertThat(service.isAccountOwner(null, auth("ext-1"))).isFalse();
        assertThat(service.isAccountOwner(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    void rejectsBlankSubject() {
        assertThat(service.isAccountOwner(UUID.randomUUID(), auth(""))).isFalse();
    }

    @Test
    void ownerMatchesWhenAccountIdInUserAccounts() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userPort.findByExternalId("ext-1")).thenReturn(Optional.of(user));
        when(userPort.findAccountIdsByUserId(userId)).thenReturn(List.of(accountId));

        assertThat(service.isAccountOwner(accountId, auth("ext-1"))).isTrue();
    }

    @Test
    void ownerFailsWhenUserMissingOrAccountNotOwned() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userPort.findByExternalId("ext-1")).thenReturn(Optional.of(user));
        when(userPort.findAccountIdsByUserId(userId)).thenReturn(List.of(UUID.randomUUID()));

        assertThat(service.isAccountOwner(accountId, auth("ext-1"))).isFalse();

        when(userPort.findByExternalId("ext-missing")).thenReturn(Optional.empty());
        assertThat(service.isAccountOwner(accountId, auth("ext-missing"))).isFalse();
    }

    @Test
    void fallsBackToAuthenticationNameWhenNotJwt() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userPort.findByExternalId("principal-name")).thenReturn(Optional.of(user));
        when(userPort.findAccountIdsByUserId(userId)).thenReturn(List.of(accountId));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("principal-name");
        when(auth.getName()).thenReturn("principal-name");

        assertThat(service.isAccountOwner(accountId, auth)).isTrue();
    }

    @Test
    void swallowsExceptions() {
        when(userPort.findByExternalId("ext-1")).thenThrow(new RuntimeException("boom"));

        assertThat(service.isAccountOwner(UUID.randomUUID(), auth("ext-1"))).isFalse();
    }
}
