package id.payu.dispute.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.dispute.domain.port.in.RefundUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefundController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RefundControllerSecurityTest.TestSecurityConfiguration.class)
class RefundControllerSecurityTest {

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfiguration {

        @org.springframework.context.annotation.Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .build();
        }
    }

    @MockitoBean
    private RefundUseCase refundUseCase;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void anonymousCannotReadRefund() throws Exception {
        mockMvc.perform(get("/api/v1/refunds/{refundId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "user")
    void customerCannotProcessRefund() throws Exception {
        mockMvc.perform(post("/api/v1/refunds/{refundId}/process", UUID.randomUUID())
                        .header("X-Idempotency-Key", "security-test-process"))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyRefundMutationRequiresExternalIdempotencyKey() {
        for (String methodName : List.of("createFullRefund", "createPartialRefund", "processRefund",
                "completeRefund", "failRefund", "cancelRefund")) {
            Method method = Arrays.stream(RefundController.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            Idempotent idempotent = method.getAnnotation(Idempotent.class);

            assertThat(idempotent)
                    .as("%s must require idempotency", methodName)
                    .isNotNull();
            assertThat(idempotent.required()).isTrue();
            assertThat(idempotent.headerName()).isEqualTo("X-Idempotency-Key");
        }
    }
}
