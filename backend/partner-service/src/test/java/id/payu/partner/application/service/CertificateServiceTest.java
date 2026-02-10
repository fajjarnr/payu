package id.payu.partner.application.service;

import id.payu.partner.domain.Partner;
import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.adapter.persistence.repository.PartnerCertificateRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CertificateServiceTest {

    @InjectMocks
    private CertificateService certificateService;

    @Mock
    private PartnerCertificateRepository certificateRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private SnapBiSignatureService signatureService;

    private Partner testPartner;
    private PartnerCertificate testCertificate;
    private KeyPair testKeyPair;

    @BeforeEach
    public void setUp() throws Exception {
        testPartner = new Partner();
        testPartner.setId(1L);
        testPartner.setName("Test Partner");
        testPartner.setEmail("test@example.com");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        testKeyPair = keyPairGenerator.generateKeyPair();

        testCertificate = new PartnerCertificate();
        testCertificate.setId(1L);
        testCertificate.setPartner(testPartner);
        testCertificate.setPublicKeyFingerprint("test-fingerprint");
        testCertificate.setCertificateType("X.509");
        testCertificate.setKeyAlgorithm("RSA");
        testCertificate.setKeySize(2048);
        testCertificate.setValidFrom(LocalDateTime.now().minusDays(10));
        testCertificate.setValidTo(LocalDateTime.now().plusDays(350));
        testCertificate.setActive(true);
    }

    @Test
    public void testGenerateKeyPairAndStore() throws Exception {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));

        PartnerCertificate cert = certificateService.generateKeyPairAndStore(1L, 365);

        assertNotNull(cert);
        assertEquals(testPartner, cert.getPartner());
        assertNotNull(cert.getCertificatePem());
        assertNotNull(cert.getPrivateKeyPem());
        assertNotNull(cert.getPublicKeyFingerprint());
        assertEquals("RSA", cert.getKeyAlgorithm());
        assertEquals(2048, cert.getKeySize());
        assertTrue(cert.isActive());
    }

    @Test
    public void testGenerateKeyPairAndStore_PartnerNotFound() {
        when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            certificateService.generateKeyPairAndStore(999L, 365);
        });

        assertTrue(exception.getMessage().contains("Partner not found"));
    }

    @Test
    public void testGetActiveCertificate() {
        when(certificateRepository.findActiveByPartnerId(1L))
                .thenReturn(Optional.of(testCertificate));

        Optional<PartnerCertificate> cert = certificateService.getActiveCertificate(1L);

        assertTrue(cert.isPresent());
        assertEquals(testCertificate, cert.get());
    }

    @Test
    public void testGetActiveCertificate_NotFound() {
        when(certificateRepository.findActiveByPartnerId(1L))
                .thenReturn(Optional.empty());

        Optional<PartnerCertificate> cert = certificateService.getActiveCertificate(1L);

        assertFalse(cert.isPresent());
    }

    @Test
    public void testGetValidCertificate() {
        when(certificateRepository.findValidByPartnerId(1L))
                .thenReturn(Optional.of(testCertificate));

        Optional<PartnerCertificate> cert = certificateService.getValidCertificate(1L);

        assertTrue(cert.isPresent());
        assertEquals(testCertificate, cert.get());
    }

    @Test
    public void testGetCertificatesByPartner() {
        when(certificateRepository.findByPartnerId(1L))
                .thenReturn(List.of(testCertificate));

        List<PartnerCertificate> certs = certificateService.getCertificatesByPartner(1L);

        assertNotNull(certs);
        assertEquals(1, certs.size());
        assertEquals(testCertificate, certs.get(0));
    }

    @Test
    public void testGetExpiringCertificates() {
        PartnerCertificate expiringCert = new PartnerCertificate();
        expiringCert.setId(2L);
        expiringCert.setPartner(testPartner);
        expiringCert.setValidTo(LocalDateTime.now().plusDays(10));
        expiringCert.setActive(true);

        when(certificateRepository.findExpiringSoon(1L, 30))
                .thenReturn(List.of(expiringCert));

        List<PartnerCertificate> certs = certificateService.getExpiringCertificates(1L, 30);

        assertNotNull(certs);
        assertEquals(1, certs.size());
        assertEquals(expiringCert, certs.get(0));
    }

    @Test
    public void testDeactivateCertificate() {
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));

        boolean result = certificateService.deactivateCertificate(1L);

        assertTrue(result);
        assertFalse(testCertificate.isActive());
    }

    @Test
    public void testDeactivateCertificate_NotFound() {
        when(certificateRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = certificateService.deactivateCertificate(999L);

        assertFalse(result);
    }

    @Test
    public void testValidateCertificate_Valid() {
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));

        boolean isValid = certificateService.validateCertificate(1L);

        assertTrue(isValid);
    }

    @Test
    public void testValidateCertificate_Expired() {
        testCertificate.setValidTo(LocalDateTime.now().minusDays(1));
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));

        boolean isValid = certificateService.validateCertificate(1L);

        assertFalse(isValid);
    }

    @Test
    public void testValidateCertificate_NotActive() {
        testCertificate.setActive(false);
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));

        boolean isValid = certificateService.validateCertificate(1L);

        assertFalse(isValid);
    }

    @Test
    public void testVerifySignatureWithCertificate() throws Exception {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));

        PartnerCertificate cert = certificateService.generateKeyPairAndStore(1L, 365);
        cert.setValidFrom(LocalDateTime.now().minusDays(10));
        cert.setValidTo(LocalDateTime.now().plusDays(350));

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(cert));

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String testData = "test data to sign";
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(testData.getBytes());
        byte[] signatureBytes = sig.sign();
        String signatureB64 = Base64.getEncoder().encodeToString(signatureBytes);

        boolean isValid = certificateService.verifySignatureWithCertificate(1L, testData, signatureB64);

        assertFalse(isValid);
    }

    @Test
    public void testVerifySignatureWithCertificate_InvalidSignature() {
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));

        boolean isValid = certificateService.verifySignatureWithCertificate(1L, "test data", "invalid-signature");

        assertFalse(isValid);
    }
}
