package id.payu.account.adapter.web;

import id.payu.account.domain.model.Account;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.domain.port.out.UserPersistencePort;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ACCOUNT-006: web controller coverage (Health + AccountLookup).
 */
@DisplayName("Account web controllers")
class AccountWebControllersTest {

    private DataSource dataSource;
    private RemoteCacheManager rcm;
    private ListenerContainerRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(mock(java.sql.Statement.class));
        when(dataSource.getConnection()).thenReturn(conn);

        @SuppressWarnings("unchecked")
        RemoteCache<String, String> cache = mock(RemoteCache.class);
        when(cache.containsKey("__payu_health__")).thenReturn(false);
        org.infinispan.client.hotrod.configuration.Configuration config =
                mock(org.infinispan.client.hotrod.configuration.Configuration.class);
        when(config.remoteCaches()).thenReturn(java.util.Collections.emptyMap());
        rcm = mock(RemoteCacheManager.class);
        when(rcm.getConfiguration()).thenReturn(config);
        doReturn(cache).when(rcm).getCache();

        registry = mock(ListenerContainerRegistry.class);
    }

    private MockMvc healthMvc(DataSource ds, RemoteCacheManager mgr, ListenerContainerRegistry reg) {
        return MockMvcBuilders.standaloneSetup(new HealthController(ds, mgr, reg)).build();
    }

    @Test
    void healthUpWhenAllDependenciesReady() throws Exception {
        when(registry.getListenerContainerIds()).thenReturn(java.util.Set.of());
        healthMvc(dataSource, rcm, registry).perform(get("/api/v1/accounts/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthDownWhenDatabaseFails() throws Exception {
        DataSource bad = mock(DataSource.class);
        when(bad.getConnection()).thenThrow(new java.sql.SQLException("db down"));
        when(registry.getListenerContainerIds()).thenReturn(java.util.Set.of());

        healthMvc(bad, rcm, registry).perform(get("/api/v1/accounts/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void healthNotConfiguredWhenDataGridAndKafkaAbsent() throws Exception {
        healthMvc(dataSource, null, null).perform(get("/api/v1/accounts/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.datagrid").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.details.kafka").value("NOT_CONFIGURED"));
    }

    @Test
    void lookupByPhoneFoundMasksAccountNumber() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("johndoe");
        Account account = new Account();
        account.setAccountNumber("1234567890");
        UserPersistencePort userPort = mock(UserPersistencePort.class);
        AccountPersistencePort accountPort = mock(AccountPersistencePort.class);
        when(userPort.findByPhoneNumber("08123456789")).thenReturn(Optional.of(user));
        when(accountPort.findByUserIdAndAllowPhoneLookupTrue(user.getId())).thenReturn(Optional.of(account));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AccountLookupController(userPort, accountPort)).build();

        mvc.perform(get("/api/v1/accounts/lookup").param("phone", "08123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.found").value(true))
                .andExpect(jsonPath("$.data.accountName").value("johndoe"))
                .andExpect(jsonPath("$.data.maskedAccountNumber").value("******7890"));
    }

    @Test
    void lookupByPhoneNotFound() throws Exception {
        UserPersistencePort userPort = mock(UserPersistencePort.class);
        AccountPersistencePort accountPort = mock(AccountPersistencePort.class);
        when(userPort.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AccountLookupController(userPort, accountPort)).build();

        mvc.perform(get("/api/v1/accounts/lookup").param("phone", "08120000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.found").value(false));
    }

    @Test
    void lookupByPhoneFoundButNoLookupAccount() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        UserPersistencePort userPort = mock(UserPersistencePort.class);
        AccountPersistencePort accountPort = mock(AccountPersistencePort.class);
        when(userPort.findByPhoneNumber(anyString())).thenReturn(Optional.of(user));
        when(accountPort.findByUserIdAndAllowPhoneLookupTrue(any())).thenReturn(Optional.empty());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AccountLookupController(userPort, accountPort)).build();

        mvc.perform(get("/api/v1/accounts/lookup").param("phone", "08123333333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.found").value(false));
    }
}
