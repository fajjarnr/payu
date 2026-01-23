package id.payu.partner.service;

import id.payu.partner.domain.Partner;
import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.repository.PartnerCertificateRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

@QuarkusTest
public class CertificateRotationServiceTest {

    @Inject
    CertificateRotationService rotationService;

    @InjectMock
    CertificateService certificateService;

    @InjectMock
    PartnerCertificateRepository certificateRepository;

    private Partner testPartner;
    private PartnerCertificate testCertificate;

    @BeforeEach
    public void setUp() {
        testPartner = new Partner();
        testPartner.id = 1L;
        testPartner.name = "Test Partner";
        testPartner.email = "test@example.com";

        testCertificate = new PartnerCertificate();
        testCertificate.id = 1L;
        testCertificate.partner = testPartner;
        testCertificate.publicKeyFingerprint = "test-fingerprint";
        testCertificate.certificateType = "X.509";
        testCertificate.keyAlgorithm = "RSA";
        testCertificate.keySize = 2048;
        testCertificate.validFrom = LocalDateTime.now().minusDays(10);
        testCertificate.validTo = LocalDateTime.now().plusDays(10);
        testCertificate.active = true;
    }

    @Test
    public void testRotateCertificate() throws Exception {
        PartnerCertificate newCert = new PartnerCertificate();
        newCert.id = 2L;
        newCert.partner = testPartner;
        newCert.active = true;

        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);
        Mockito.when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificate(1L, 90);

        assertFalse(testCertificate.active);
        Mockito.verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testRotateCertificate_CertificateNotFound() {
        Mockito.when(certificateRepository.findById(999L)).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            rotationService.rotateCertificate(999L, 90);
        });

        assertTrue(exception.getMessage().contains("Certificate not found"));
    }

    @Test
    public void testRotateCertificate_DefaultValidityDays() throws Exception {
        PartnerCertificate newCert = new PartnerCertificate();
        newCert.id = 2L;
        newCert.partner = testPartner;
        newCert.active = true;

        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);
        Mockito.when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificate(1L);

        assertFalse(testCertificate.active);
        Mockito.verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testRotateExpiringCertificates() throws Exception {
        PartnerCertificate newCert = new PartnerCertificate();
        newCert.id = 2L;
        newCert.partner = testPartner;
        newCert.active = true;

        List<PartnerCertificate> expiringCerts = List.of(testCertificate);
        Mockito.when(certificateRepository.findExpiringSoon(null, 30)).thenReturn(expiringCerts);
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);
        Mockito.when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        int rotatedCount = rotationService.rotateExpiringCertificates(30);

        assertEquals(1, rotatedCount);
        assertFalse(testCertificate.active);
    }

    @Test
    public void testRotateExpiringCertificates_EmptyList() {
        List<PartnerCertificate> emptyList = List.of();
        Mockito.when(certificateRepository.findExpiringSoon(null, 30)).thenReturn(emptyList);

        int rotatedCount = rotationService.rotateExpiringCertificates(30);

        assertEquals(0, rotatedCount);
    }

    @Test
    public void testRotateAllExpiredCertificates() throws Exception {
        testCertificate.validTo = LocalDateTime.now().minusDays(1);

        PartnerCertificate newCert = new PartnerCertificate();
        newCert.id = 2L;
        newCert.partner = testPartner;
        newCert.active = true;

        List<PartnerCertificate> expiredCerts = List.of(testCertificate);
        Mockito.when(certificateRepository.findExpiredCertificates()).thenReturn(expiredCerts);
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);
        Mockito.when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        int rotatedCount = rotationService.rotateAllExpiredCertificates();

        assertEquals(1, rotatedCount);
        assertFalse(testCertificate.active);
    }

    @Test
    public void testRotateAllExpiredCertificates_EmptyList() {
        List<PartnerCertificate> emptyList = List.of();
        Mockito.when(certificateRepository.findExpiredCertificates()).thenReturn(emptyList);

        int rotatedCount = rotationService.rotateAllExpiredCertificates();

        assertEquals(0, rotatedCount);
    }

    @Test
    public void testRotateCertificateForPartner_WithActiveCert() throws Exception {
        PartnerCertificate newCert = new PartnerCertificate();
        newCert.id = 2L;
        newCert.partner = testPartner;
        newCert.active = true;

        Mockito.when(certificateRepository.findByPartnerId(1L)).thenReturn(List.of(testCertificate));
        Mockito.when(certificateRepository.findById(1L)).thenReturn(testCertificate);
        Mockito.when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificateForPartner(1L, 90);

        assertFalse(testCertificate.active);
        Mockito.verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testRotateCertificateForPartner_NoActiveCert() throws Exception {
        PartnerCertificate newCert = new PartnerCertificate();
        newCert.id = 2L;
        newCert.partner = testPartner;
        newCert.active = true;

        List<PartnerCertificate> inactiveCerts = List.of();
        Mockito.when(certificateRepository.findByPartnerId(1L)).thenReturn(inactiveCerts);
        Mockito.when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificateForPartner(1L, 90);

        Mockito.verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testShouldRotateCertificate_Expired() {
        testCertificate.validTo = LocalDateTime.now().minusDays(1);

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertTrue(shouldRotate);
    }

    @Test
    public void testShouldRotateCertificate_ExpiringSoon() {
        testCertificate.validTo = LocalDateTime.now().plusDays(20);

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertTrue(shouldRotate);
    }

    @Test
    public void testShouldRotateCertificate_NotExpiring() {
        testCertificate.validTo = LocalDateTime.now().plusDays(100);

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertFalse(shouldRotate);
    }

    @Test
    public void testShouldRotateCertificate_NotActive() {
        testCertificate.active = false;

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertFalse(shouldRotate);
    }
}
