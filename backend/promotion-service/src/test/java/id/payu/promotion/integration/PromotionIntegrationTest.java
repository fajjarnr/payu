package id.payu.promotion.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.adapter.persistence.repository.*;
import id.payu.security.config.WebSecurityAutoConfiguration;
import id.payu.promotion.domain.PromotionRewardType;
import id.payu.promotion.domain.PromotionType;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.RewardType;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.interfaces.dto.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for PromotionEntity Service rewards engine. Avoids RestAssured HTTPBuilder NPE on Java 25.
 * Pattern: webAppContextSetup + springSecurity() preserves Spring Security filter chain.
 */
@SpringBootTest
@Import(WebSecurityAutoConfiguration.class)
@ActiveProfiles("test")
class PromotionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    PromotionRepository promotionRepository;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    @Autowired
    CashbackRepository cashbackRepository;

    @Autowired
    ReferralRepository referralRepository;

    @Autowired
    RewardRepository rewardRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_ACCOUNT_ID = "test-user";
    private static final String TEST_REFERRER_ID = "acc-referrer-test";
    private static final String TEST_REFEREE_ID = "acc-referee-test";

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Clean up database (in correct order to respect FKs)
        rewardRepository.deleteAll();
        loyaltyPointsRepository.deleteAll();
        cashbackRepository.deleteAll();
        referralRepository.deleteAll();
        promotionRepository.deleteAll();
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    private MvcResult getUrl(String url) throws Exception {
        return mockMvc.perform(get(url)).andReturn();
    }

    // ==================== LOYALTY POINTS TESTS ====================

    @Test
    void testPointsCalculationAndAwarding() throws Exception {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-earn-001", TransactionType.EARNED,
                100, LocalDateTime.now().plusMonths(6)
        );

        MvcResult result = postJson("/api/v1/loyalty-points", earnRequest);
        String loyaltyId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();

        Assertions.assertEquals(201, result.getResponse().getStatus());
        mockMvc.perform(get("/api/v1/loyalty-points/account/{accountId}/balance", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(100))
                .andExpect(jsonPath("$.totalEarned").value(100));

        mockMvc.perform(get("/api/v1/loyalty-points/{id}", loyaltyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(100));

        CreateLoyaltyPointsRequest earnRequest2 = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-earn-002", TransactionType.EARNED,
                50, LocalDateTime.now().plusMonths(6)
        );
        postJson("/api/v1/loyalty-points", earnRequest2);

        mockMvc.perform(get("/api/v1/loyalty-points/account/{accountId}/balance", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(150))
                .andExpect(jsonPath("$.totalEarned").value(150));
    }

    @Test
    void testPointsRedemption() throws Exception {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, "txn-earn-redeem", TransactionType.EARNED,
                200, LocalDateTime.now().plusMonths(6)
        );
        postJson("/api/v1/loyalty-points", earnRequest);

        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
                TEST_ACCOUNT_ID, 75, "txn-redeem-001"
        );

        mockMvc.perform(post("/api/v1/loyalty-points/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redeemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(-75))
                .andExpect(jsonPath("$.balanceAfter").value(125))
                .andExpect(jsonPath("$.transactionType").value("REDEEMED"))
                .andExpect(jsonPath("$.redeemedAt").exists());

        mockMvc.perform(get("/api/v1/loyalty-points/account/{accountId}/balance", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(125))
                .andExpect(jsonPath("$.totalRedeemed").value(75));
    }

    // ==================== CASHBACK PROCESSING TESTS ====================

    @Test
    void testCashbackProcessing() throws Exception {
        CreateCashbackRequest cashbackRequest = new CreateCashbackRequest(
                TEST_ACCOUNT_ID, "txn-cashback-001", new BigDecimal("100000"),
                "MERCHANT-001", "DINING", "CASHBACK-10"
        );

        MvcResult result = postJson("/api/v1/cashbacks", cashbackRequest);
        String cashbackId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get("/api/v1/cashbacks/{id}", cashbackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashbackAmount").value(3000.0F))
                .andExpect(jsonPath("$.percentage").value(3.0F));

        mockMvc.perform(get("/api/v1/cashbacks/account/{accountId}/summary", TEST_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCashback").value(3000.0F))
                .andExpect(jsonPath("$.creditedCashback").value(3000.0F))
                .andExpect(jsonPath("$.transactionCount").value(1));
    }

    @Test
    void testCashbackByCategory() throws Exception {
        CreateCashbackRequest groceryRequest = new CreateCashbackRequest(
                TEST_ACCOUNT_ID + "-1", "txn-grocery", new BigDecimal("50000"),
                null, "GROCERY", null
        );
        postJson("/api/v1/cashbacks", groceryRequest);

        CreateCashbackRequest shoppingRequest = new CreateCashbackRequest(
                TEST_ACCOUNT_ID + "-2", "txn-shopping", new BigDecimal("100000"),
                null, "SHOPPING", null
        );
        postJson("/api/v1/cashbacks", shoppingRequest);
    }

    // ==================== REFERRAL PROGRAM TESTS ====================

    @Test
    void testReferralProgramRewards() throws Exception {
        CreateReferralRequest referralRequest = new CreateReferralRequest(
                TEST_REFERRER_ID, new BigDecimal("50000"), new BigDecimal("25000"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );

        MvcResult result = postJson("/api/v1/referrals", referralRequest);
        String referralCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("referralCode").asText();

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(referralCode, TEST_REFEREE_ID);
        mockMvc.perform(post("/api/v1/referrals/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.refereeAccountId").value(TEST_REFEREE_ID))
                .andExpect(jsonPath("$.completedAt").exists());

        List<RewardEntity> referrerRewards = rewardRepository.findByAccountId(TEST_REFERRER_ID);
        Assertions.assertTrue(referrerRewards.stream()
                .anyMatch(r -> r.getType() == RewardType.REFERRAL_BONUS
                        && r.getAmount().compareTo(new BigDecimal("50000")) == 0));

        List<RewardEntity> refereeRewards = rewardRepository.findByAccountId(TEST_REFEREE_ID);
        Assertions.assertTrue(refereeRewards.stream()
                .anyMatch(r -> r.getType() == RewardType.REFERRAL_BONUS
                        && r.getAmount().compareTo(new BigDecimal("25000")) == 0));

        mockMvc.perform(get("/api/v1/referrals/referrer/{referrerId}/summary", TEST_REFERRER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReferrals").value(1))
                .andExpect(jsonPath("$.completedReferrals").value(1))
                .andExpect(jsonPath("$.pendingReferrals").value(0));
    }

    @Test
    void testReferralProgramWithPointsReward() throws Exception {
        CreateReferralRequest referralRequest = new CreateReferralRequest(
                TEST_REFERRER_ID + "-points", new BigDecimal("100"), new BigDecimal("50"),
                ReferralRewardType.POINTS, LocalDateTime.now().plusMonths(3)
        );
        MvcResult result = postJson("/api/v1/referrals", referralRequest);
        String referralCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("referralCode").asText();

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
                referralCode, TEST_REFEREE_ID + "-points"
        );
        mockMvc.perform(post("/api/v1/referrals/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        List<LoyaltyPointsEntity> referrerPoints = loyaltyPointsRepository.findByAccountId(TEST_REFERRER_ID + "-points");
        Assertions.assertTrue(referrerPoints.stream()
                .anyMatch(p -> p.getTransactionType() == TransactionType.REFERRAL_BONUS && p.getPoints() == 100));

        List<LoyaltyPointsEntity> refereePoints = loyaltyPointsRepository.findByAccountId(TEST_REFEREE_ID + "-points");
        Assertions.assertTrue(refereePoints.stream()
                .anyMatch(p -> p.getTransactionType() == TransactionType.REFERRAL_BONUS && p.getPoints() == 50));
    }

    // ==================== KAFKA EVENT TESTS ====================

    @Test
    void testCashbackCreation_HttpPostSucceeds() throws Exception {
        CreateCashbackRequest cashbackRequest = new CreateCashbackRequest(
                TEST_ACCOUNT_ID + "-kafka", "txn-kafka-001", new BigDecimal("100000"),
                "MERCHANT-KAFKA", "DINING", "CASHBACK-KAFKA"
        );

        mockMvc.perform(post("/api/v1/cashbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashbackRequest)))
                .andExpect(status().isCreated());
    }
}
