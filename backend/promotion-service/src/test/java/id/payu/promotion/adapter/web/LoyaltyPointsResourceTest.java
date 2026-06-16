package id.payu.promotion.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import id.payu.promotion.config.SecurityConfig;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for LoyaltyPointsResource. Avoids RestAssured HTTPBuilder NPE on Java 25.
 */
@SpringBootTest
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class LoyaltyPointsResourceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_ACCOUNT_ID = "test-user";
    private static final String BASE_PATH = "/api/v1/loyalty-points";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        loyaltyPointsRepository.deleteAll();
    }

    private MvcResult earnPoints(CreateLoyaltyPointsRequest request) throws Exception {
        return mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    @Test
    void testAddPoints_Success() throws Exception {
        var request = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                .andExpect(jsonPath("$.points").value(100))
                .andExpect(jsonPath("$.balanceAfter").value(100))
                .andExpect(jsonPath("$.transactionType").value("EARNED"));
    }

    @Test
    void testAddPoints_InvalidRequest_Returns400() throws Exception {
        var request = new CreateLoyaltyPointsRequest(
                null, "txn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRedeemPoints_Success() throws Exception {
        var earnRequest = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-earn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );
        earnPoints(earnRequest);

        var redeemRequest = new RedeemLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, 50, "txn-redeem-001"
        );

        mockMvc.perform(post(BASE_PATH + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redeemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                .andExpect(jsonPath("$.points").value(-50))
                .andExpect(jsonPath("$.balanceAfter").value(50))
                .andExpect(jsonPath("$.transactionType").value("REDEEMED"));
    }

    @Test
    void testRedeemPoints_InsufficientBalance_Returns400() throws Exception {
        var redeemRequest = new RedeemLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, 1000, "txn-redeem-001"
        );

        mockMvc.perform(post(BASE_PATH + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redeemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Insufficient loyalty points balance")));
    }

    @Test
    void testGetLoyaltyPoints_Success() throws Exception {
        var request = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );

        MvcResult result = earnPoints(request);
        String id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get(BASE_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                .andExpect(jsonPath("$.points").value(100));
    }

    @Test
    void testGetLoyaltyPoints_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetLoyaltyPointsByAccount_Success() throws Exception {
        var request1 = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );
        var request2 = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-002", TransactionType.EARNED,
                50, LocalDateTime.now().plusMonths(6)
        );

        earnPoints(request1);
        earnPoints(request2);

        mockMvc.perform(get(BASE_PATH + "/account/{accountId}", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].accountId",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(TEST_ACCOUNT_ID))));
    }

    @Test
    void testGetBalance_Success() throws Exception {
        var request = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );

        earnPoints(request);

        mockMvc.perform(get(BASE_PATH + "/account/{accountId}/balance", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(100))
                .andExpect(jsonPath("$.totalEarned").value(100))
                .andExpect(jsonPath("$.totalRedeemed").value(0))
                .andExpect(jsonPath("$.expiredPoints").value(0));
    }

    @Test
    void testGetBalance_NoTransactions_ReturnsZero() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/account/{accountId}/balance", "test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(0))
                .andExpect(jsonPath("$.totalEarned").value(0))
                .andExpect(jsonPath("$.totalRedeemed").value(0))
                .andExpect(jsonPath("$.expiredPoints").value(0));
    }
}
