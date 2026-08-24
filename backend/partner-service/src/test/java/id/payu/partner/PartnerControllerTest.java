package id.payu.partner;

import id.payu.partner.interfaces.dto.PartnerDTO;
import id.payu.partner.adapter.web.PartnerController;
import id.payu.partner.adapter.web.filter.SandboxFilter;
import id.payu.partner.application.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Boot test for PartnerController.
 * Uses MockMvc for testing REST endpoints without starting the full server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerService partnerService;

    @MockitoBean
    private SandboxFilter sandboxFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetAllPartners() throws Exception {
        PartnerDTO partner = new PartnerDTO(
            1L,
            "Test PartnerEntity",
            "MERCHANT",
            "test@example.com",
            "+62123456789",
            true,
            "client-id",
            "client-secret",
            "public-key"
        );

        when(partnerService.getAllPartners()).thenReturn(List.of(partner));

        mockMvc.perform(get("/partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test PartnerEntity"))
                .andExpect(jsonPath("$.data[0].email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetPartnerById_Found() throws Exception {
        PartnerDTO partner = new PartnerDTO(
            1L,
            "Test PartnerEntity",
            "MERCHANT",
            "test@example.com",
            "+62123456789",
            true,
            "client-id",
            "client-secret",
            "public-key"
        );

        when(partnerService.getPartnerById(1L)).thenReturn(partner);

        mockMvc.perform(get("/partners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test PartnerEntity"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetPartnerById_NotFound() throws Exception {
        when(partnerService.getPartnerById(999L)).thenReturn(null);

        mockMvc.perform(get("/partners/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreatePartner_Success() throws Exception {
        PartnerDTO newPartner = new PartnerDTO(
            null,
            "New PartnerEntity",
            "PAYMENT_GATEWAY",
            "newpartner@example.com",
            "+62812345678",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO createdPartner = new PartnerDTO(
            2L,
            "New PartnerEntity",
            "PAYMENT_GATEWAY",
            "newpartner@example.com",
            "+62812345678",
            true,
            "generated-client-id",
            "generated-client-secret",
            "public-key"
        );

        when(partnerService.createPartner(any(PartnerDTO.class), anyString())).thenReturn(createdPartner);
        String requestBody = """
            {
                "name": "New PartnerEntity",
                "type": "PAYMENT_GATEWAY",
                "email": "newpartner@example.com",
                "phone": "+62812345678",
                "active": true,
                "publicKey": "public-key"
            }
            """;

        mockMvc.perform(post("/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New PartnerEntity"))
                .andExpect(jsonPath("$.data.clientId").exists())
                .andExpect(jsonPath("$.data.clientSecret").exists());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "ADMIN")
    public void testGetMyPartner_Found() throws Exception {
        PartnerDTO partner = new PartnerDTO(
            1L, "Test PartnerEntity", "MERCHANT", "test@example.com",
            "+62123456789", true, "client-id", null, "public-key"
        );
        when(partnerService.findByEmail("test@example.com")).thenReturn(Optional.of(partner));

        mockMvc.perform(get("/partners/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "unknown@example.com", roles = "ADMIN")
    public void testGetMyPartner_NotFound() throws Exception {
        when(partnerService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/partners/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "ADMIN")
    public void testGetMyPartner_JwtFallback() throws Exception {
        // Tests fallback path where principal name contains email ( Covers Jwt email claim path via same fallback)
        PartnerDTO partner = new PartnerDTO(
            1L, "Test PartnerEntity", "MERCHANT", "test@example.com",
            "+62123456789", true, "client-id", null, "public-key"
        );
        when(partnerService.findByEmail("test@example.com")).thenReturn(Optional.of(partner));

        mockMvc.perform(get("/v1/partners/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
}
