package id.payu.wallet.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

    @Autowired
    protected MockMvc mockMvc;

    @BeforeEach
    void setUpSecurityContext() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }
}
