package id.payu.partner;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.resource.PartnerController;
import id.payu.partner.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Boot test for PartnerController.
 * Uses MockMvc for testing REST endpoints without starting the full server.
 */
@WebMvcTest(controllers = PartnerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerService partnerService;

    @Test
    public void testGetAllPartners() throws Exception {
        PartnerDTO partner = new PartnerDTO(
            1L,
            "Test Partner",
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
                .andExpect(jsonPath("$.data[0].name").value("Test Partner"))
                .andExpect(jsonPath("$.data[0].email").value("test@example.com"));
    }

    @Test
    public void testGetPartnerById_Found() throws Exception {
        PartnerDTO partner = new PartnerDTO(
            1L,
            "Test Partner",
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
                .andExpect(jsonPath("$.data.name").value("Test Partner"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    public void testGetPartnerById_NotFound() throws Exception {
        when(partnerService.getPartnerById(999L)).thenReturn(null);

        mockMvc.perform(get("/partners/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreatePartner_Success() throws Exception {
        PartnerDTO newPartner = new PartnerDTO(
            null,
            "New Partner",
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
            "New Partner",
            "PAYMENT_GATEWAY",
            "newpartner@example.com",
            "+62812345678",
            true,
            "generated-client-id",
            "generated-client-secret",
            "public-key"
        );

        when(partnerService.createPartner(any(PartnerDTO.class))).thenReturn(createdPartner);

        String requestBody = """
            {
                "name": "New Partner",
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
                .andExpect(jsonPath("$.data.name").value("New Partner"))
                .andExpect(jsonPath("$.data.clientId").exists())
                .andExpect(jsonPath("$.data.clientSecret").exists());
    }
}
