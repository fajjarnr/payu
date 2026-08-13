package id.payu.account.adapter.web;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ACCOUNT-006: UserAccountController coverage.
 */
@DisplayName("UserAccountController")
class UserAccountControllerTest {

    private final UserPersistencePort userPort = mock(UserPersistencePort.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new UserAccountController(userPort))
            .build();

    @Test
    void getAccountIdsByUserIdReturnsEmptyWhenUserMissing() throws Exception {
        when(userPort.findByExternalId("missing")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/accounts/users/{userId}/account-ids", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAccountIdsByUserIdReturnsIds() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(userPort.findByExternalId("ext-1")).thenReturn(Optional.of(user));
        when(userPort.findAccountIdsByUserId(user.getId())).thenReturn(List.of(UUID.randomUUID()));

        mvc.perform(get("/api/v1/accounts/users/{userId}/account-ids", "ext-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    void getUserProfileNotFound() throws Exception {
        when(userPort.findByExternalId(anyString())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/accounts/users/{userId}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACC_404"));
    }

    @Test
    void getUserProfileFound() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("johndoe");
        user.setExternalId("ext-1");
        when(userPort.findByExternalId("ext-1")).thenReturn(Optional.of(user));

        mvc.perform(get("/api/v1/accounts/users/{userId}", "ext-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("johndoe"));
    }
}
