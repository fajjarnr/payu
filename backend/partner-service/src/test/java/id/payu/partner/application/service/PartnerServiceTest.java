package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private PartnerService partnerService;

    private PartnerEntity testPartner;

    @BeforeEach
    public void setUp() {
        testPartner = new PartnerEntity();
        testPartner.setId(1L);
        testPartner.setName("Test PartnerEntity");
        testPartner.setType("MERCHANT");
        testPartner.setEmail("test@example.com");
        testPartner.setPhone("+62123456789");
        testPartner.setActive(true);
        testPartner.setApiKey("test-api-key");
        testPartner.setClientId("test-client-id");
        testPartner.setClientSecret("test-client-secret");
        testPartner.setPublicKey("test-public-key");
    }

    @Test
    public void testGetAllPartners() {
        when(partnerRepository.findAll()).thenReturn(List.of(testPartner));

        List<PartnerDTO> partners = partnerService.getAllPartners();

        assertNotNull(partners);
        assertEquals(1, partners.size());
        assertEquals("Test PartnerEntity", partners.get(0).name);
        assertEquals("test@example.com", partners.get(0).email);
    }

    @Test
    public void testGetAllPartners_Empty() {
        when(partnerRepository.findAll()).thenReturn(List.of());

        List<PartnerDTO> partners = partnerService.getAllPartners();

        assertNotNull(partners);
        assertTrue(partners.isEmpty());
    }

    @Test
    public void testGetPartnerById_Found() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));

        PartnerDTO partner = partnerService.getPartnerById(1L);

        assertNotNull(partner);
        assertEquals("Test PartnerEntity", partner.name);
        assertEquals("test@example.com", partner.email);
        assertEquals("MERCHANT", partner.type);
    }

    @Test
    public void testGetPartnerById_NotFound() {
        when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

        PartnerDTO partner = partnerService.getPartnerById(999L);

        assertNull(partner);
    }

    @Test
    public void testCreatePartner_Success() {
        PartnerDTO dto = new PartnerDTO(
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

        when(partnerRepository.findByEmail("newpartner@example.com")).thenReturn(Optional.empty());
        when(partnerRepository.save(any(PartnerEntity.class))).thenAnswer(invocation -> {
            PartnerEntity p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        PartnerDTO result = partnerService.createPartner(dto);

        assertNotNull(result);
        assertEquals("New PartnerEntity", result.name);
        assertEquals("PAYMENT_GATEWAY", result.type);
        assertEquals("newpartner@example.com", result.email);
        assertNotNull(result.clientId);
        assertNotNull(result.clientSecret);
    }

    @Test
    public void testCreatePartner_EmailAlreadyExists() {
        PartnerDTO dto = new PartnerDTO(
            null,
            "Duplicate PartnerEntity",
            "MERCHANT",
            "test@example.com",
            "+62812345678",
            true,
            null,
            null,
            "public-key"
        );

        when(partnerRepository.findByEmail("test@example.com"))
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
            "Updated PartnerEntity",
            "BANK",
            "updated@example.com",
            "+62898765432",
            true,
            "client-id",
            "client-secret",
            "updated-public-key"
        );

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(partnerRepository.save(any(PartnerEntity.class))).thenReturn(testPartner);

        PartnerDTO result = partnerService.updatePartner(1L, dto);

        assertNotNull(result);
        assertEquals("Updated PartnerEntity", result.name);
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

        when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

        PartnerDTO result = partnerService.updatePartner(999L, dto);

        assertNull(result);
    }

    @Test
    public void testRegenerateKeys_Success() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(partnerRepository.save(any(PartnerEntity.class))).thenReturn(testPartner);

        PartnerDTO result = partnerService.regenerateKeys(1L);

        assertNotNull(result);
        assertNotNull(result.clientId);
        assertNotNull(result.clientSecret);
    }

    @Test
    public void testRegenerateKeys_NotFound() {
        when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

        PartnerDTO result = partnerService.regenerateKeys(999L);

        assertNull(result);
    }

    @Test
    public void testDeletePartner_Success() {
        when(partnerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(partnerRepository).deleteById(1L);

        boolean result = partnerService.deletePartner(1L);

        assertTrue(result);
    }

    @Test
    public void testDeletePartner_NotFound() {
        when(partnerRepository.existsById(999L)).thenReturn(false);

        boolean result = partnerService.deletePartner(999L);

        assertFalse(result);
    }
}
