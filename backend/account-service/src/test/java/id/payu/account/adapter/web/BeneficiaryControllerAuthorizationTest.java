package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.domain.model.Beneficiary;
import id.payu.account.domain.model.BeneficiaryStatus;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.BeneficiaryPersistencePort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.interfaces.dto.BeneficiaryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ACCOUNT-002: every beneficiary mutation must be bound to the authenticated
 * principal. PUT/DELETE previously only checked {accountId} against the
 * beneficiary row, so any authenticated user could mutate another user's
 * beneficiary by guessing IDs.
 */
@DisplayName("BeneficiaryController authorization")
class BeneficiaryControllerAuthorizationTest {

    private static final UUID VICTIM_ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VICTIM_BENEFICIARY = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private MockMvc mockMvc;
    private BeneficiaryPersistencePort beneficiaryPersistencePort;
    private UserPersistencePort userPersistencePort;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        beneficiaryPersistencePort = mock(BeneficiaryPersistencePort.class);
        userPersistencePort = mock(UserPersistencePort.class);
        BeneficiaryController controller = new BeneficiaryController(beneficiaryPersistencePort, userPersistencePort);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        given(userPersistencePort.findByExternalId("victim-external-id"))
                .willReturn(Optional.of(User.builder().id(VICTIM_ACCOUNT).externalId("victim-external-id").build()));
        given(userPersistencePort.findByExternalId("attacker-external-id"))
                .willReturn(Optional.of(User.builder().id(UUID.randomUUID()).externalId("attacker-external-id").build()));

        given(beneficiaryPersistencePort.findById(VICTIM_BENEFICIARY))
                .willReturn(Optional.of(Beneficiary.builder()
                        .id(VICTIM_BENEFICIARY)
                        .userId(VICTIM_ACCOUNT)
                        .status(BeneficiaryStatus.ACTIVE)
                        .build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET returns 403 for an account that does not belong to the principal")
    void getForbiddenForForeignAccount() throws Exception {
        authenticateAs("attacker-external-id");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/beneficiaries", VICTIM_ACCOUNT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT rejects updating another user's beneficiary with 403")
    void putForbiddenForForeignAccount() throws Exception {
        authenticateAs("attacker-external-id");
        mockMvc.perform(put("/api/v1/accounts/{accountId}/beneficiaries/{beneficiaryId}", VICTIM_ACCOUNT, VICTIM_BENEFICIARY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BeneficiaryRequest("BANK001", "1234567890", "Evil"))))
                .andExpect(status().isForbidden());
        verify(beneficiaryPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("DELETE rejects deleting another user's beneficiary with 403")
    void deleteForbiddenForForeignAccount() throws Exception {
        authenticateAs("attacker-external-id");
        mockMvc.perform(delete("/api/v1/accounts/{accountId}/beneficiaries/{beneficiaryId}", VICTIM_ACCOUNT, VICTIM_BENEFICIARY))
                .andExpect(status().isForbidden());
        verify(beneficiaryPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("PUT succeeds for the owning principal")
    void putAllowedForOwner() throws Exception {
        authenticateAs("victim-external-id");
        mockMvc.perform(put("/api/v1/accounts/{accountId}/beneficiaries/{beneficiaryId}", VICTIM_ACCOUNT, VICTIM_BENEFICIARY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BeneficiaryRequest("BANK001", "1234567890", "Owner"))))
                .andExpect(status().isOk());
        verify(beneficiaryPersistencePort).save(any());
    }

    @Test
    @DisplayName("DELETE succeeds for the owning principal")
    void deleteAllowedForOwner() throws Exception {
        authenticateAs("victim-external-id");
        mockMvc.perform(delete("/api/v1/accounts/{accountId}/beneficiaries/{beneficiaryId}", VICTIM_ACCOUNT, VICTIM_BENEFICIARY))
                .andExpect(status().isOk());
        verify(beneficiaryPersistencePort).save(any());
    }

    private void authenticateAs(String externalId) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "none"), Map.of("sub", externalId));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
