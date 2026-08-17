package id.payu.promotion.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.promotion.adapter.persistence.repository.CashbackRepository;
import id.payu.security.config.WebSecurityAutoConfiguration;
import id.payu.promotion.interfaces.dto.CreateCashbackRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for CashbackResource. Avoids RestAssured HTTPBuilder NPE on Java 25.
 * Pattern: webAppContextSetup + springSecurity() preserves Spring Security filter chain.
 * Uses TestSecurityConfig (permitAll) to bypass JWT in test.
 */
@SpringBootTest
@Import(WebSecurityAutoConfiguration.class)
@ActiveProfiles("test")
class CashbackResourceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    CashbackRepository cashbackRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_ACCOUNT_ID = "acc-test-456";
    private static final String BASE_PATH = "/api/v1/cashbacks";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        cashbackRepository.deleteAll();
    }

    private MvcResult createCashback(CreateCashbackRequest request) throws Exception {
        return mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    @Test
    void testCreateCashback_Success() throws Exception {
        var request = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-001", new BigDecimal("1000.00"),
                "MERCHANT001", "GROCERY", "PROMO2024"
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                .andExpect(jsonPath("$.transactionId").value("txn-001"))
                .andExpect(jsonPath("$.merchantCode").value("MERCHANT001"))
                .andExpect(jsonPath("$.categoryCode").value("GROCERY"))
                .andExpect(jsonPath("$.cashbackCode").value("PROMO2024"))
                .andExpect(jsonPath("$.status").value("CREDITED"));
    }

    @Test
    void testCreateCashback_DiningCategory_3Percent() throws Exception {
        var request = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-002", new BigDecimal("500.00"),
                "MERCHANT002", "DINING", null
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashbackAmount").value(15.00f))
                .andExpect(jsonPath("$.percentage").value(3.0000f))
                .andExpect(jsonPath("$.status").value("CREDITED"));
    }

    @Test
    void testCreateCashback_InvalidRequest_Returns400() throws Exception {
        var request = new CreateCashbackRequest(
                null, "txn-003", new BigDecimal("1000.00"),
                "MERCHANT001", "GROCERY", "PROMO2024"
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCashback_Success() throws Exception {
        var request = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-001", new BigDecimal("1000.00"),
                "MERCHANT001", "GROCERY", null
        );

        MvcResult result = createCashback(request);
        String id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get(BASE_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                .andExpect(jsonPath("$.cashbackAmount").value(20.00f));
    }

    @Test
    void testGetCashback_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCashbacksByAccount_Success() throws Exception {
        var request1 = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-001", new BigDecimal("1000.00"),
                "MERCHANT001", "GROCERY", null
        );
        var request2 = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-002", new BigDecimal("500.00"),
                "MERCHANT002", "DINING", null
        );

        createCashback(request1);
        createCashback(request2);

        mockMvc.perform(get(BASE_PATH + "/account/{accountId}", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].accountId", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(TEST_ACCOUNT_ID))));
    }

    @Test
    void testGetCashbackSummary_Success() throws Exception {
        var request1 = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-001", new BigDecimal("1000.00"),
                "MERCHANT001", "GROCERY", null
        );
        var request2 = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-002", new BigDecimal("500.00"),
                "MERCHANT002", "DINING", null
        );

        createCashback(request1);
        createCashback(request2);

        mockMvc.perform(get(BASE_PATH + "/account/{accountId}/summary", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCashback").value(35.00f))
                .andExpect(jsonPath("$.pendingCashback").value(0))
                .andExpect(jsonPath("$.creditedCashback").value(35.00f))
                .andExpect(jsonPath("$.transactionCount").value(2));
    }

    @Test
    void testGetCashbackSummary_NoTransactions() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/account/{accountId}/summary", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCashback").value(0))
                .andExpect(jsonPath("$.pendingCashback").value(0))
                .andExpect(jsonPath("$.creditedCashback").value(0))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }
}
