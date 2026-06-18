package id.payu.billing.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Biller Resource Tests")
class BillerResourceTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_TOKEN = "Bearer test-token";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        Jwt mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test-user")
                .claim("scope", "openid")
                .build();
        org.mockito.Mockito.when(jwtDecoder.decode("test-token")).thenReturn(mockJwt);
    }

    @Test
    @DisplayName("GET /api/v1/billers - should return all billers")
    void shouldReturnAllBillers() throws Exception {
        mockMvc.perform(get("/api/v1/billers")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].code").value(notNullValue()))
                .andExpect(jsonPath("$.data[0].displayName").value(notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=mobile - should filter by category")
    void shouldFilterByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/billers")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .param("category", "mobile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].category", everyItem(equalTo("mobile"))));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return specific biller")
    void shouldReturnSpecificBiller() throws Exception {
        mockMvc.perform(get("/api/v1/billers/PLN")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("PLN"))
                .andExpect(jsonPath("$.data.displayName").value(containsString("PLN")))
                .andExpect(jsonPath("$.data.category").value("electricity"));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return 404 for unknown biller")
    void shouldReturn404ForUnknownBiller() throws Exception {
        mockMvc.perform(get("/api/v1/billers/UNKNOWN")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/billers/categories - should return all categories")
    void shouldReturnAllCategories() throws Exception {
        mockMvc.perform(get("/api/v1/billers/categories")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItems("electricity", "water", "mobile")));
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=tv_cable - should return TV Cable billers")
    void shouldReturnTVCableBillers() throws Exception {
        mockMvc.perform(get("/api/v1/billers")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .param("category", "tv_cable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].category", everyItem(equalTo("tv_cable"))))
                .andExpect(jsonPath("$.data[*].code", hasItems("INDOVISION", "TRANSTV", "KVISION", "MNC_VISION")));
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=multifinance - should return Multifinance billers")
    void shouldReturnMultifinanceBillers() throws Exception {
        mockMvc.perform(get("/api/v1/billers")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .param("category", "multifinance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].category", everyItem(equalTo("multifinance"))))
                .andExpect(jsonPath("$.data[*].code", hasItems("FIFASTRA", "BFI", "ADIRA", "WOM", "MEGA_FINANCE")));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return TV Cable biller with correct admin fee")
    void shouldReturnTVCableBillerWithCorrectAdminFee() throws Exception {
        mockMvc.perform(get("/api/v1/billers/INDOVISION")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("INDOVISION"))
                .andExpect(jsonPath("$.data.displayName").value(containsString("Indovision")))
                .andExpect(jsonPath("$.data.category").value("tv_cable"))
                .andExpect(jsonPath("$.data.adminFee").value(2500));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return Multifinance biller with correct admin fee")
    void shouldReturnMultifinanceBillerWithCorrectAdminFee() throws Exception {
        mockMvc.perform(get("/api/v1/billers/FIFASTRA")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("FIFASTRA"))
                .andExpect(jsonPath("$.data.displayName").value(containsString("FIFASTRA")))
                .andExpect(jsonPath("$.data.category").value("multifinance"))
                .andExpect(jsonPath("$.data.adminFee").value(5000));
    }

    @Test
    @DisplayName("GET /api/v1/billers/categories - should include tv_cable and multifinance categories")
    void shouldIncludeNewCategories() throws Exception {
        mockMvc.perform(get("/api/v1/billers/categories")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItems("electricity", "water", "mobile", "tv_cable", "multifinance")));
    }
}
