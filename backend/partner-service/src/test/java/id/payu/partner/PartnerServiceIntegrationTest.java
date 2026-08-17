package id.payu.partner;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.application.service.PartnerService;
import id.payu.partner.interfaces.dto.PartnerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PartnerService.
 * Uses H2 in-memory database for testing without requiring external dependencies.
 */
@SpringBootTest
@ActiveProfiles("test")
public class PartnerServiceIntegrationTest {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    public void setUp() {
        // Clean database before each test
        partnerRepository.deleteAll();
    }

    @Test
    public void testCreateAndRetrievePartner() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "Integration Test PartnerEntity",
            "MERCHANT",
            "integration@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO created = partnerService.createPartner(dto);

        assertNotNull(created);
        assertNotNull(created.id);
        assertEquals("Integration Test PartnerEntity", created.name);
        assertNotNull(created.clientId);
        assertNotNull(created.clientSecret);

        PartnerDTO retrieved = partnerService.getPartnerById(created.id);
        assertNotNull(retrieved);
        assertEquals("Integration Test PartnerEntity", retrieved.name);
    }

    @Test
    public void testGetAllPartners() {
        PartnerDTO dto1 = new PartnerDTO(
            null,
            "PartnerEntity 1",
            "MERCHANT",
            "partner1@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO dto2 = new PartnerDTO(
            null,
            "PartnerEntity 2",
            "PAYMENT_GATEWAY",
            "partner2@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        partnerService.createPartner(dto1);
        partnerService.createPartner(dto2);

        List<PartnerDTO> partners = partnerService.getAllPartners();

        assertEquals(2, partners.size());
    }

    @Test
    public void testUpdatePartner() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "Original Name",
            "MERCHANT",
            "original@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO created = partnerService.createPartner(dto);

        PartnerDTO updateDto = new PartnerDTO(
            created.id,
            "Updated Name",
            "BANK",
            "updated@example.com",
            "+62987654321",
            true,
            created.clientId,
            created.clientSecret,
            "updated-public-key"
        );

        PartnerDTO updated = partnerService.updatePartner(created.id, updateDto);

        assertNotNull(updated);
        assertEquals("Updated Name", updated.name);
        assertEquals("BANK", updated.type);
    }

    @Test
    public void testDeletePartner() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "To Delete",
            "MERCHANT",
            "delete@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO created = partnerService.createPartner(dto);

        assertTrue(partnerService.deletePartner(created.id));
        assertFalse(partnerService.deletePartner(999L));
    }

    @Test
    public void testRegenerateKeys() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "Key Test",
            "MERCHANT",
            "keytest@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO created = partnerService.createPartner(dto);
        String originalClientId = created.clientId;
        String originalClientSecret = created.clientSecret;

        PartnerDTO regenerated = partnerService.regenerateKeys(created.id);

        assertNotNull(regenerated);
        assertNotEquals(originalClientId, regenerated.clientId);
        assertNotEquals(originalClientSecret, regenerated.clientSecret);
    }

    @Test
    public void testClientSecretNotExposedOnReadPaths() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "Secret Leak Test",
            "MERCHANT",
            "secrettest@example.com",
            "+62123456789",
            true,
            null,
            null,
            "public-key"
        );

        PartnerDTO created = partnerService.createPartner(dto);
        assertNotNull(created.clientSecret, "create response carries the secret once");

        PartnerDTO byId = partnerService.getPartnerById(created.id);
        assertNull(byId.clientSecret, "getPartnerById must not expose the secret");

        assertTrue(partnerService.getAllPartners().stream()
                .noneMatch(p -> p.clientSecret != null),
                "partner list must not expose any client secret");

        PartnerDTO updateDto = new PartnerDTO(
            created.id,
            "Secret Leak Test Updated",
            "MERCHANT",
            "secrettest@example.com",
            "+62123456789",
            true,
            created.clientId,
            null,
            "public-key"
        );
        PartnerDTO updated = partnerService.updatePartner(created.id, updateDto);
        assertNull(updated.clientSecret, "update response must not expose the secret");
    }
}
