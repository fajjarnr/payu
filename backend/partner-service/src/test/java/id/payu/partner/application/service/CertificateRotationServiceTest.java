package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.PartnerCertificateEntity;
import id.payu.partner.adapter.persistence.repository.PartnerCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CertificateRotationServiceTest {

    @Mock
    private CertificateService certificateService;

    @Mock
    private PartnerCertificateRepository certificateRepository;

    @InjectMocks
    private CertificateRotationService rotationService;

    private PartnerEntity testPartner;
    private PartnerCertificateEntity testCertificate;

    @BeforeEach
    public void setUp() {
        testPartner = new PartnerEntity();
        testPartner.setId(1L);
        testPartner.setName("Test PartnerEntity");
        testPartner.setEmail("test@example.com");

        testCertificate = new PartnerCertificateEntity();
        testCertificate.setId(1L);
        testCertificate.setPartner(testPartner);
        testCertificate.setPublicKeyFingerprint("test-fingerprint");
        testCertificate.setCertificateType("X.509");
        testCertificate.setKeyAlgorithm("RSA");
        testCertificate.setKeySize(2048);
        testCertificate.setValidFrom(LocalDateTime.now().minusDays(10));
        testCertificate.setValidTo(LocalDateTime.now().plusDays(10));
        testCertificate.setActive(true);
    }

    @Test
    public void testRotateCertificate() throws Exception {
        PartnerCertificateEntity newCert = new PartnerCertificateEntity();
        newCert.setId(2L);
        newCert.setPartner(testPartner);
        newCert.setActive(true);

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificate(1L, 90);

        assertFalse(testCertificate.isActive());
        verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testRotateCertificate_CertificateNotFound() {
        when(certificateRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            rotationService.rotateCertificate(999L, 90);
        });

        assertTrue(exception.getMessage().contains("Certificate not found"));
    }

    @Test
    public void testRotateCertificate_DefaultValidityDays() throws Exception {
        PartnerCertificateEntity newCert = new PartnerCertificateEntity();
        newCert.setId(2L);
        newCert.setPartner(testPartner);
        newCert.setActive(true);

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificate(1L);

        assertFalse(testCertificate.isActive());
        verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testRotateExpiringCertificates() throws Exception {
        PartnerCertificateEntity newCert = new PartnerCertificateEntity();
        newCert.setId(2L);
        newCert.setPartner(testPartner);
        newCert.setActive(true);

        List<PartnerCertificateEntity> expiringCerts = List.of(testCertificate);
        when(certificateRepository.findExpiringSoon(null, 30)).thenReturn(expiringCerts);
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        int rotatedCount = rotationService.rotateExpiringCertificates(30);

        assertEquals(1, rotatedCount);
        assertFalse(testCertificate.isActive());
    }

    @Test
    public void testRotateExpiringCertificates_EmptyList() {
        List<PartnerCertificateEntity> emptyList = List.of();
        when(certificateRepository.findExpiringSoon(null, 30)).thenReturn(emptyList);

        int rotatedCount = rotationService.rotateExpiringCertificates(30);

        assertEquals(0, rotatedCount);
    }

    @Test
    public void testRotateAllExpiredCertificates() throws Exception {
        testCertificate.setValidTo(LocalDateTime.now().minusDays(1));

        PartnerCertificateEntity newCert = new PartnerCertificateEntity();
        newCert.setId(2L);
        newCert.setPartner(testPartner);
        newCert.setActive(true);

        List<PartnerCertificateEntity> expiredCerts = List.of(testCertificate);
        when(certificateRepository.findExpiredCertificates()).thenReturn(expiredCerts);
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        int rotatedCount = rotationService.rotateAllExpiredCertificates();

        assertEquals(1, rotatedCount);
        assertFalse(testCertificate.isActive());
    }

    @Test
    public void testRotateAllExpiredCertificates_EmptyList() {
        List<PartnerCertificateEntity> emptyList = List.of();
        when(certificateRepository.findExpiredCertificates()).thenReturn(emptyList);

        int rotatedCount = rotationService.rotateAllExpiredCertificates();

        assertEquals(0, rotatedCount);
    }

    @Test
    public void testRotateCertificateForPartner_WithActiveCert() throws Exception {
        PartnerCertificateEntity newCert = new PartnerCertificateEntity();
        newCert.setId(2L);
        newCert.setPartner(testPartner);
        newCert.setActive(true);

        when(certificateRepository.findByPartnerId(1L)).thenReturn(List.of(testCertificate));
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificateForPartner(1L, 90);

        assertFalse(testCertificate.isActive());
        verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testRotateCertificateForPartner_NoActiveCert() throws Exception {
        PartnerCertificateEntity newCert = new PartnerCertificateEntity();
        newCert.setId(2L);
        newCert.setPartner(testPartner);
        newCert.setActive(true);

        List<PartnerCertificateEntity> inactiveCerts = List.of();
        when(certificateRepository.findByPartnerId(1L)).thenReturn(inactiveCerts);
        when(certificateService.generateKeyPairAndStore(1L, 90)).thenReturn(newCert);

        rotationService.rotateCertificateForPartner(1L, 90);

        verify(certificateService).generateKeyPairAndStore(1L, 90);
    }

    @Test
    public void testShouldRotateCertificate_Expired() {
        testCertificate.setValidTo(LocalDateTime.now().minusDays(1));

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertTrue(shouldRotate);
    }

    @Test
    public void testShouldRotateCertificate_ExpiringSoon() {
        testCertificate.setValidTo(LocalDateTime.now().plusDays(20));

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertTrue(shouldRotate);
    }

    @Test
    public void testShouldRotateCertificate_NotExpiring() {
        testCertificate.setValidTo(LocalDateTime.now().plusDays(100));

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertFalse(shouldRotate);
    }

    @Test
    public void testShouldRotateCertificate_NotActive() {
        testCertificate.setActive(false);

        boolean shouldRotate = rotationService.shouldRotateCertificate(testCertificate, 30);

        assertFalse(shouldRotate);
    }
}
