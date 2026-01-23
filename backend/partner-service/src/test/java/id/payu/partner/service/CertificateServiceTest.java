package id.payu.partner.service;

import id.payu.partner.domain.Partner;
import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.repository.PartnerCertificateRepository;
import id.payu.partner.repository.PartnerRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class CertificateServiceTest {

    @Inject
    CertificateService certificateService;

    @InjectMock
    PartnerCertificateRepository certificateRepository;

    @InjectMock
    PartnerRepository partnerRepository;

    @InjectMock
    SnapBiSignatureService signatureService;

    private Partner testPartner;
    private PartnerCertificate testCertificate;
    private KeyPair testKeyPair;

    @BeforeEach
    public void setUp() throws Exception {
        testPartner = new Partner();
        testPartner.id = 1L;
        testPartner.name = "Test Partner";
        testPartner.email = "test@example.com";

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        testKeyPair = keyPairGenerator.generateKeyPair();

        testCertificate = new PartnerCertificate();
        testCertificate.id = 1L;
        testCertificate.partner = testPartner;
        testCertificate.publicKeyFingerprint = "test-fingerprint";
        testCertificate.certificateType = "X.509";
        testCertificate.keyAlgorithm = "RSA";
        testCertificate.keySize = 2048;
        testCertificate.validFrom = LocalDateTime.now().minusDays(10);
        testCertificate.validTo = LocalDateTime.now().plusDays(350);
        testCertificate.active = true;
    }

    @Test
    public void testGenerateKeyPairAndStore() throws Exception {
        Mockito.when(partnerRepository.findById(1L)).thenReturn(testPartner);

        PartnerCertificate cert = certificateService.generateKeyPairAndStore(1L, 365);

        assertNotNull(cert);
        assertEquals(testPartner, cert.partner);
        assertNotNull(cert.certificatePem);
        assertNotNull(cert.privateKeyPem);
        assertNotNull(cert.publicKeyFingerprint);
        assertEquals("RSA", cert.keyAlgorithm);
        assertEquals(2048, cert.keySize);
        assertTrue(cert.active);
    }

    @Test
    public void testGenerateKeyPairAndStore_PartnerNotFound() {
        Mockito.when(partnerRepository.findById(999L)).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            certificateService.generateKeyPairAndStore(999L, 365);
        });

        assertTrue(exception.getMessage().contains("Partner not found"));
    }

    @Test
    public void testGetActiveCertificate() {
        Mockito.when(certificateRepository.findActiveByPartnerId(1L))
                .thenReturn(Optional.of(testCertificate));

        Optional<PartnerCertificate> cert = certificateService.getActiveCertificate(1L);

        assertTrue(cert.isPresent());
        assertEquals(testCertificate, cert.get());
    }

    @Test
    public void testGetActiveCertificate_NotFound() {
        Mockito.when(certificateRepository.findActiveByPartnerId(1L))
                .thenReturn(Optional.empty());

        Optional<PartnerCertificate> cert = certificateService.getActiveCertificate(1L);

        assertFalse(cert.isPresent());
    }

    @Test
    public void testGetValidCertificate() {
        Mockito.when(certificateRepository.findValidByPartnerId(1L))
                .thenReturn(Optional.of(testCertificate));

        Optional<PartnerCertificate> cert = certificateService.getValidCertificate(1L);

        assertTrue(cert.isPresent());
        assertEquals(testCertificate, cert.get());
    }

    @Test
    public void testGetCertificatesByPartner() {
        Mockito.when(certificateRepository.findByPartnerId(1L))
                .thenReturn(List.of(testCertificate));

        List<PartnerCertificate> certs = certificateService.getCertificatesByPartner(1L);

        assertNotNull(certs);
        assertEquals(1, certs.size());
        assertEquals(testCertificate, certs.get(0));
    }

    @Test
    public void testGetExpiringCertificates() {
        PartnerCertificate expiringCert = new PartnerCertificate();
        expiringCert.id = 2L;
        expiringCert.partner = testPartner;
        expiringCert.validTo = LocalDateTime.now().plusDays(10);
        expiringCert.active = true;

        Mockito.when(certificateRepository.findExpiringSoon(1L, 30))
                .thenReturn(List.of(expiringCert));

        List<PartnerCertificate> certs = certificateService.getExpiringCertificates(1L, 30);

        assertNotNull(certs);
        assertEquals(1, certs.size());
        assertEquals(expiringCert, certs.get(0));
    }

    @Test
    public void testDeactivateCertificate() {
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);

        boolean result = certificateService.deactivateCertificate(1L);

        assertTrue(result);
        assertFalse(testCertificate.active);
    }

    @Test
    public void testDeactivateCertificate_NotFound() {
        Mockito.when(certificateRepository.findById(999L)).thenReturn(null);

        boolean result = certificateService.deactivateCertificate(999L);

        assertFalse(result);
    }

    @Test
    public void testDeleteCertificate() {
        Mockito.when(certificateRepository.deleteById(1L)).thenReturn(true);

        boolean result = certificateService.deleteCertificate(1L);

        assertTrue(result);
    }

    @Test
    public void testDeleteCertificate_NotFound() {
        Mockito.when(certificateRepository.deleteById(999L)).thenReturn(false);

        boolean result = certificateService.deleteCertificate(999L);

        assertFalse(result);
    }

    @Test
    public void testValidateCertificate_Valid() {
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);

        boolean isValid = certificateService.validateCertificate(1L);

        assertTrue(isValid);
    }

    @Test
    public void testValidateCertificate_Expired() {
        testCertificate.validTo = LocalDateTime.now().minusDays(1);
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);

        boolean isValid = certificateService.validateCertificate(1L);

        assertFalse(isValid);
    }

    @Test
    public void testValidateCertificate_NotActive() {
        testCertificate.active = false;
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);

        boolean isValid = certificateService.validateCertificate(1L);

        assertFalse(isValid);
    }

    @Test
    public void testVerifySignatureWithCertificate() throws Exception {
        Mockito.when(partnerRepository.findById(1L)).thenReturn(testPartner);

        PartnerCertificate cert = certificateService.generateKeyPairAndStore(1L, 365);
        cert.validFrom = LocalDateTime.now().minusDays(10);
        cert.validTo = LocalDateTime.now().plusDays(350);

        Mockito.when(certificateRepository.findById(1L)).thenReturn(cert);

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
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);

        boolean isValid = certificateService.verifySignatureWithCertificate(1L, "test data", "invalid-signature");

        assertFalse(isValid);
    }
}
