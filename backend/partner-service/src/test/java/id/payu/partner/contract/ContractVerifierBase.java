package id.payu.partner.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

/**
 * QAMVP-003: base for partner-service Spring Cloud Contract verifier tests.
 * SNAP-BI happy paths require a live HMAC + within-5-minutes timestamp, so the
 * deterministic contracts cover the request/error wire shape (e.g. expired
 * timestamp → 4002508) which is stable forever.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "partner.jwt.secret=test-jwt-secret")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
@Import(ContractVerifierBase.TestLockProviderConfig.class)
public abstract class ContractVerifierBase {

    @TestConfiguration
    static class TestLockProviderConfig {
        @Bean
        LockProvider lockProvider() {
            return new LockProvider() {
                @Override
                public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
                    return Optional.of(new SimpleLock() {
                        @Override
                        public void unlock() {
                            // no-op
                        }
                    });
                }
            };
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }
}
