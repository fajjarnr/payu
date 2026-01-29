package id.payu.partner.service;

import id.payu.partner.domain.Partner;
import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.repository.PartnerRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PartnerServiceTest {

    @Inject
    PartnerService partnerService;

    @InjectMock
    PartnerRepository partnerRepository;

    private Partner testPartner;

    @BeforeEach
    public void setUp() {
        testPartner = new Partner();
        testPartner.id = 1L;
        testPartner.name = "Test Partner";
        testPartner.type = "MERCHANT";
        testPartner.email = "test@example.com";
        testPartner.phone = "+62123456789";
        testPartner.active = true;
        testPartner.apiKey = "test-api-key";  // pragma: allowlist secret
        testPartner.clientId = "test-client-id";  // pragma: allowlist secret
        testPartner.clientSecret = "test-client-secret";  // pragma: allowlist secret
        testPartner.publicKey = "test-public-key";
    }

    @Test
    public void testGetAllPartners() {
        Mockito.when(partnerRepository.listAll()).thenReturn(List.of(testPartner));

        List<PartnerDTO> partners = partnerService.getAllPartners();

        assertNotNull(partners);
        assertEquals(1, partners.size());
        assertEquals("Test Partner", partners.get(0).name);
        assertEquals("test@example.com", partners.get(0).email);
    }

    @Test
    public void testGetAllPartners_Empty() {
        Mockito.when(partnerRepository.listAll()).thenReturn(List.of());

        List<PartnerDTO> partners = partnerService.getAllPartners();

        assertNotNull(partners);
        assertTrue(partners.isEmpty());
    }

    @Test
    public void testGetPartnerById_Found() {
        Mockito.when(partnerRepository.findById(1L)).thenReturn(testPartner);

        PartnerDTO partner = partnerService.getPartnerById(1L);

        assertNotNull(partner);
        assertEquals("Test Partner", partner.name);
        assertEquals("test@example.com", partner.email);
        assertEquals("MERCHANT", partner.type);
    }

    @Test
    public void testGetPartnerById_NotFound() {
        Mockito.when(partnerRepository.findById(999L)).thenReturn(null);

        PartnerDTO partner = partnerService.getPartnerById(999L);

        assertNull(partner);
    }

    @Test
    public void testCreatePartner_Success() {
        PartnerDTO dto = new PartnerDTO(
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

        Mockito.when(partnerRepository.findByEmail("newpartner@example.com")).thenReturn(Optional.empty());
        Mockito.doAnswer(invocation -> {
            Partner p = invocation.getArgument(0);
            p.id = 2L;
            return null;
        }).when(partnerRepository).persist(Mockito.any(Partner.class));

        PartnerDTO result = partnerService.createPartner(dto);

        assertNotNull(result);
        assertEquals("New Partner", result.name);
        assertEquals("PAYMENT_GATEWAY", result.type);
        assertEquals("newpartner@example.com", result.email);
        assertNotNull(result.clientId);
        assertNotNull(result.clientSecret);
    }

    @Test
    public void testCreatePartner_EmailAlreadyExists() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "Duplicate Partner",
            "MERCHANT",
            "test@example.com",
            "+62812345678",
            true,
            null,
            null,
            "public-key"
        );

        Mockito.when(partnerRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testPartner));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            partnerService.createPartner(dto);
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    public void testUpdatePartner_Success() {
        PartnerDTO dto = new PartnerDTO(
            1L,
            "Updated Partner",
            "BANK",
            "updated@example.com",
            "+62898765432",
            true,
            "client-id",
            "client-secret",
            "updated-public-key"
        );

        Mockito.when(partnerRepository.findById(1L)).thenReturn(testPartner);

        PartnerDTO result = partnerService.updatePartner(1L, dto);

        assertNotNull(result);
        assertEquals("Updated Partner", result.name);
        assertEquals("BANK", result.type);
    }

    @Test
    public void testUpdatePartner_NotFound() {
        PartnerDTO dto = new PartnerDTO(
            999L,
            "Non-existent",
            "MERCHANT",
            "nonexistent@example.com",
            "+62812345678",
            true,
            null,
            null,
            "public-key"
        );

        Mockito.when(partnerRepository.findById(999L)).thenReturn(null);

        PartnerDTO result = partnerService.updatePartner(999L, dto);

        assertNull(result);
    }

    @Test
    public void testRegenerateKeys_Success() {
        Mockito.when(partnerRepository.findById(1L)).thenReturn(testPartner);

        PartnerDTO result = partnerService.regenerateKeys(1L);

        assertNotNull(result);
        assertNotNull(result.clientId);
        assertNotNull(result.clientSecret);
        assertNotEquals("test-client-id", result.clientId);
    }

    @Test
    public void testRegenerateKeys_NotFound() {
        Mockito.when(partnerRepository.findById(999L)).thenReturn(null);

        PartnerDTO result = partnerService.regenerateKeys(999L);

        assertNull(result);
    }

    @Test
    public void testDeletePartner_Success() {
        Mockito.when(partnerRepository.deleteById(1L)).thenReturn(true);

        boolean result = partnerService.deletePartner(1L);

        assertTrue(result);
    }

    @Test
    public void testDeletePartner_NotFound() {
        Mockito.when(partnerRepository.deleteById(999L)).thenReturn(false);

        boolean result = partnerService.deletePartner(999L);

        assertFalse(result);
    }
}
