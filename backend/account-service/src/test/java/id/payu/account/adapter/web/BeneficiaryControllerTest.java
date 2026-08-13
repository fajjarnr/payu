package id.payu.account.adapter.web;

import id.payu.account.domain.model.Beneficiary;
import id.payu.account.domain.model.BeneficiaryStatus;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.BeneficiaryPersistencePort;
import id.payu.account.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ACCOUNT-006: BeneficiaryController coverage.
 */
@DisplayName("BeneficiaryController")
class BeneficiaryControllerTest {

    private final BeneficiaryPersistencePort benPort = mock(BeneficiaryPersistencePort.class);
    private final UserPersistencePort userPort = mock(UserPersistencePort.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new BeneficiaryController(benPort, userPort))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    private void authAs(UUID accountId) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("ext-" + accountId).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        User user = new User();
        user.setId(accountId);
        when(userPort.findByExternalId("ext-" + accountId)).thenReturn(Optional.of(user));
        when(userPort.findById(accountId)).thenReturn(Optional.of(user));
    }

    private Beneficiary beneficiary(UUID accountId) {
        return Beneficiary.builder()
                .id(UUID.randomUUID())
                .userId(accountId)
                .bankCode("011")
                .accountNumber("1234567890")
                .accountName("Ali")
                .status(BeneficiaryStatus.ACTIVE)
                .build();
    }

    @Test
    void getBeneficiariesOwned() throws Exception {
        UUID accountId = UUID.randomUUID();
        authAs(accountId);
        when(benPort.findActiveByUserId(accountId)).thenReturn(List.of(beneficiary(accountId)));

        mvc.perform(get("/api/v1/accounts/{accountId}/beneficiaries", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountName").value("Ali"));
    }

    @Test
    void getBeneficiariesForbiddenWhenNotOwner() throws Exception {
        UUID accountId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("other-user").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(userPort.findByExternalId("other-user")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/accounts/{accountId}/beneficiaries", accountId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BEN_004"));
    }

    @Test
    void createBeneficiarySuccess() throws Exception {
        UUID accountId = UUID.randomUUID();
        authAs(accountId);
        when(benPort.countActiveByUserId(accountId)).thenReturn(0L);
        when(benPort.existsByUserIdAndBankCodeAndAccountNumber(eq(accountId), eq("011"), eq("1234567890")))
                .thenReturn(false);
        when(benPort.save(any(Beneficiary.class))).thenAnswer(i -> i.getArgument(0));

        mvc.perform(post("/api/v1/accounts/{accountId}/beneficiaries", accountId)
                        .contentType("application/json")
                        .content("{\"bankCode\":\"011\",\"accountNumber\":\"1234567890\",\"nickname\":\"Ali\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));
    }

    @Test
    void createBeneficiaryConflictOnDuplicate() throws Exception {
        UUID accountId = UUID.randomUUID();
        authAs(accountId);
        when(benPort.countActiveByUserId(accountId)).thenReturn(0L);
        when(benPort.existsByUserIdAndBankCodeAndAccountNumber(eq(accountId), eq("011"), eq("1234567890")))
                .thenReturn(true);

        mvc.perform(post("/api/v1/accounts/{accountId}/beneficiaries", accountId)
                        .contentType("application/json")
                        .content("{\"bankCode\":\"011\",\"accountNumber\":\"1234567890\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BEN_002"));
    }

    @Test
    void createBeneficiaryLimitReached() throws Exception {
        UUID accountId = UUID.randomUUID();
        authAs(accountId);
        when(benPort.countActiveByUserId(accountId)).thenReturn(50L);

        mvc.perform(post("/api/v1/accounts/{accountId}/beneficiaries", accountId)
                        .contentType("application/json")
                        .content("{\"bankCode\":\"011\",\"accountNumber\":\"1234567890\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BEN_001"));
    }

    @Test
    void deleteBeneficiaryNotFoundOrWrongOwner() throws Exception {
        UUID accountId = UUID.randomUUID();
        authAs(accountId);
        when(benPort.findById(UUID.randomUUID())).thenReturn(Optional.empty());

        mvc.perform(delete("/api/v1/accounts/{accountId}/beneficiaries/{beneficiaryId}",
                        accountId, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BEN_003"));
    }
}
