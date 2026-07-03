package id.payu.promotion.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.promotion.adapter.persistence.repository.ReferralRepository;
import id.payu.security.config.WebSecurityAutoConfiguration;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.CreateReferralRequest;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for ReferralResource. Avoids RestAssured HTTPBuilder NPE on Java 25.
 */
@SpringBootTest
@Import(WebSecurityAutoConfiguration.class)
@ActiveProfiles("test")
class ReferralResourceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    ReferralRepository referralRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String REFERRER_ACCOUNT_ID = "acc-referrer";
    private static final String REFEREE_ACCOUNT_ID = "acc-referee";
    private static final String BASE_PATH = "/api/v1/referrals";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        referralRepository.deleteAll();
    }

    private MvcResult createReferral(CreateReferralRequest request) throws Exception {
        return mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    @Test
    void testCreateReferral_Success() throws Exception {
        var request = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referrerAccountId").value(REFERRER_ACCOUNT_ID))
                .andExpect(jsonPath("$.referrerReward").value(50.00f))
                .andExpect(jsonPath("$.refereeReward").value(25.00f))
                .andExpect(jsonPath("$.rewardType").value("CASHBACK"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.referralCode").exists());
    }

    @Test
    void testCreateReferral_WithPointsRewardType() throws Exception {
        var request = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("1000.00"), new BigDecimal("500.00"),
                ReferralRewardType.POINTS, LocalDateTime.now().plusMonths(3)
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rewardType").value("POINTS"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testCreateReferral_InvalidRequest_Returns400() throws Exception {
        var request = new CreateReferralRequest(
                null, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCompleteReferral_Success() throws Exception {
        var createRequest = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        MvcResult result = createReferral(createRequest);
        String referralCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("referralCode").asText();

        var completeRequest = new CompleteReferralRequest(referralCode, REFEREE_ACCOUNT_ID);

        mockMvc.perform(post(BASE_PATH + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refereeAccountId").value(REFEREE_ACCOUNT_ID))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void testCompleteReferral_InvalidCode_Returns400() throws Exception {
        var request = new CompleteReferralRequest("INVALID_CODE", REFEREE_ACCOUNT_ID);

        mockMvc.perform(post(BASE_PATH + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid referral code"));
    }

    @Test
    void testCompleteReferral_AlreadyCompleted_Returns400() throws Exception {
        var createRequest = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        MvcResult result = createReferral(createRequest);
        String referralCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("referralCode").asText();

        var completeRequest = new CompleteReferralRequest(referralCode, REFEREE_ACCOUNT_ID);

        mockMvc.perform(post(BASE_PATH + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)));

        mockMvc.perform(post(BASE_PATH + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ReferralEntity already completed or expired"));
    }

    @Test
    void testGetReferral_Success() throws Exception {
        var createRequest = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        MvcResult result = createReferral(createRequest);
        String id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get(BASE_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.referrerAccountId").value(REFERRER_ACCOUNT_ID))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetReferral_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetReferralByCode_Success() throws Exception {
        var createRequest = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        MvcResult result = createReferral(createRequest);
        String referralCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("referralCode").asText();

        mockMvc.perform(get(BASE_PATH + "/code/{code}", referralCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referralCode").value(referralCode))
                .andExpect(jsonPath("$.referrerAccountId").value(REFERRER_ACCOUNT_ID))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetReferralByCode_NotFound_Returns404() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/code/{code}", "NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetReferralsByReferrer_Success() throws Exception {
        var request1 = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        var request2 = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );

        createReferral(request1);
        createReferral(request2);

        mockMvc.perform(get(BASE_PATH + "/referrer/{referrerId}", REFERRER_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].referrerAccountId",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(REFERRER_ACCOUNT_ID))));
    }

    @Test
    void testGetReferralSummary_Success() throws Exception {
        var request1 = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        var request2 = new CreateReferralRequest(
                REFERRER_ACCOUNT_ID, new BigDecimal("50.00"), new BigDecimal("25.00"),
                ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        );
        MvcResult result1 = createReferral(request1);
        String referralCode = objectMapper.readTree(result1.getResponse().getContentAsString())
                .path("referralCode").asText();
        createReferral(request2);

        var completeRequest = new CompleteReferralRequest(referralCode, REFEREE_ACCOUNT_ID);
        mockMvc.perform(post(BASE_PATH + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)));

        mockMvc.perform(get(BASE_PATH + "/referrer/{referrerId}/summary", REFERRER_ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReferrals").value(2))
                .andExpect(jsonPath("$.completedReferrals").value(1))
                .andExpect(jsonPath("$.pendingReferrals").value(1));
    }

    @Test
    void testGetReferralSummary_NoReferrals() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/referrer/{referrerId}/summary", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReferrals").value(0))
                .andExpect(jsonPath("$.completedReferrals").value(0))
                .andExpect(jsonPath("$.pendingReferrals").value(0));
    }
}
