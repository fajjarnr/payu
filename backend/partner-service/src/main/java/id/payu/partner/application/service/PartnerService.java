package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.domain.PartnerStatus;
import id.payu.partner.domain.PartnerType;
import id.payu.partner.interfaces.dto.PartnerDTO;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.exception.ConflictException;
import id.payu.api.common.exception.ResourceNotFoundException;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final OutboxService outboxService;

    public List<PartnerDTO> getAllPartners() {
        return partnerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PartnerDTO getPartnerById(Long id) {
        return partnerRepository.findById(id).map(this::toDTO).orElse(null);
    }

    public java.util.Optional<PartnerDTO> findByEmail(String email) {
        return partnerRepository.findByEmail(email).map(this::toDTO);
    }

    public Optional<PartnerEntity> findByClientId(String clientId) {
        return partnerRepository.findByClientId(clientId);
    }

    private boolean isBypassType(String type) {
        if (type == null) return false;
        PartnerType pt = PartnerType.fromString(type);
        if (pt != null) return pt.isBypassDualControl();
        String upper = type.trim().toUpperCase();
        return "INTERNAL".equals(upper) || "SANDBOX".equals(upper);
    }

    @Transactional
    public PartnerDTO createPartner(PartnerDTO partnerDTO, String makerId) {
        if (partnerRepository.findByEmail(partnerDTO.email).isPresent()) {
            throw new IllegalArgumentException("PartnerEntity with email " + partnerDTO.email + " already exists");
        }

        PartnerEntity partner = new PartnerEntity();
        partner.setName(partnerDTO.name);
        partner.setType(partnerDTO.type);
        partner.setEmail(partnerDTO.email);
        partner.setPhone(partnerDTO.phone);
        partner.setApiKey(UUID.randomUUID().toString());
        partner.setClientId(UUID.randomUUID().toString());
        partner.setClientSecret(UUID.randomUUID().toString());
        partner.setPublicKey(partnerDTO.publicKey);

        if (isBypassType(partnerDTO.type)) {
            partner.setStatus(PartnerStatus.ACTIVE);
            partner.setActive(true);
        } else {
            partner.setStatus(PartnerStatus.PENDING_APPROVAL);
            partner.setActive(false);
            partner.setMakerId(makerId);
            partner.setRequestedAt(Instant.now());
        }

        partnerRepository.save(partner);

        // outbox: onboarding requested (only for non-bypass)
        if (partner.getStatus() == PartnerStatus.PENDING_APPROVAL) {
            try {
                outboxService.createEvent(
                        "Partner", partner.getId().toString(),
                        "PartnerOnboardingRequested",
                        Map.of("partnerId", partner.getId(), "makerId", makerId != null ? makerId : "unknown", "status", partner.getStatus().name()),
                        null, "payu.partner.onboarding-requested.v1");
            } catch (Exception e) {
                log.warn("Failed to publish onboarding-requested event for partner {}: {}", partner.getId(), e.getMessage());
            }
        }

        return toDTO(partner, true);
    }

    // Backward-compat overload (tests calling without makerId)
    @Transactional
    public PartnerDTO createPartner(PartnerDTO partnerDTO) {
        return createPartner(partnerDTO, "system-maker");
    }

    @Transactional
    public PartnerDTO approvePartner(Long id, String checkerId) {
        PartnerEntity partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PARTNER_NOT_FOUND", "Partner " + id + " not found"));
        if (partner.getStatus() != PartnerStatus.PENDING_APPROVAL) {
            throw new ConflictException("PARTNER_CONFLICT_STATUS", "Partner not in PENDING_APPROVAL (current=" + partner.getStatus() + ")");
        }
        if (checkerId != null && checkerId.equals(partner.getMakerId())) {
            throw new BusinessException("PARTNER_FORBIDDEN_SELF_APPROVAL", "Maker cannot approve own request");
        }
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setCheckerId(checkerId);
        partner.setDecidedAt(Instant.now());
        partnerRepository.save(partner);

        try {
            outboxService.createEvent(
                    "Partner", partner.getId().toString(),
                    "PartnerApproved",
                    Map.of("partnerId", partner.getId(), "checkerId", checkerId != null ? checkerId : "unknown", "status", "ACTIVE"),
                    null, "payu.partner.approved.v1");
        } catch (Exception e) {
            log.warn("Failed to publish approved event for partner {}: {}", partner.getId(), e.getMessage());
        }
        return toDTO(partner);
    }

    @Transactional
    public PartnerDTO rejectPartner(Long id, String checkerId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessException("PARTNER_REJECTION_REASON_REQUIRED", "rejection_reason is required");
        }
        PartnerEntity partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PARTNER_NOT_FOUND", "Partner " + id + " not found"));
        if (partner.getStatus() != PartnerStatus.PENDING_APPROVAL) {
            throw new ConflictException("PARTNER_CONFLICT_STATUS", "Partner not in PENDING_APPROVAL (current=" + partner.getStatus() + ")");
        }
        if (checkerId != null && checkerId.equals(partner.getMakerId())) {
            throw new BusinessException("PARTNER_FORBIDDEN_SELF_APPROVAL", "Maker cannot reject own request");
        }
        partner.setStatus(PartnerStatus.REJECTED);
        partner.setActive(false);
        partner.setCheckerId(checkerId);
        partner.setDecidedAt(Instant.now());
        partner.setRejectionReason(rejectionReason);
        partnerRepository.save(partner);

        try {
            outboxService.createEvent(
                    "Partner", partner.getId().toString(),
                    "PartnerRejected",
                    Map.of("partnerId", partner.getId(), "checkerId", checkerId != null ? checkerId : "unknown", "reason", rejectionReason),
                    null, "payu.partner.rejected.v1");
        } catch (Exception e) {
            log.warn("Failed to publish rejected event for partner {}: {}", partner.getId(), e.getMessage());
        }
        return toDTO(partner);
    }

    @Transactional
    public PartnerDTO resubmitPartner(Long id, String makerId) {
        PartnerEntity partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PARTNER_NOT_FOUND", "Partner " + id + " not found"));
        if (partner.getStatus() != PartnerStatus.REJECTED) {
            throw new ConflictException("PARTNER_CONFLICT_STATUS", "Only REJECTED can be resubmitted (current=" + partner.getStatus() + ")");
        }
        partner.setStatus(PartnerStatus.PENDING_APPROVAL);
        partner.setActive(false);
        partner.setMakerId(makerId);
        partner.setCheckerId(null);
        partner.setDecidedAt(null);
        partner.setRejectionReason(null);
        partner.setRequestedAt(Instant.now());
        partnerRepository.save(partner);

        try {
            outboxService.createEvent(
                    "Partner", partner.getId().toString(),
                    "PartnerOnboardingRequested",
                    Map.of("partnerId", partner.getId(), "makerId", makerId != null ? makerId : "unknown", "status", "PENDING_APPROVAL"),
                    null, "payu.partner.onboarding-requested.v1");
        } catch (Exception e) {
            log.warn("Failed to publish resubmit event for partner {}: {}", partner.getId(), e.getMessage());
        }
        return toDTO(partner);
    }

    @Transactional
    public PartnerDTO updatePartner(Long id, PartnerDTO partnerDTO) {
        PartnerEntity partner = partnerRepository.findById(id).orElse(null);
        if (partner == null) {
            return null;
        }

        partner.setName(partnerDTO.name);
        partner.setType(partnerDTO.type);
        partner.setPhone(partnerDTO.phone);
        partner.setPublicKey(partnerDTO.publicKey);
        
        partnerRepository.save(partner);
        return toDTO(partner);
    }

    @Transactional
    public PartnerDTO regenerateKeys(Long id) {
        PartnerEntity partner = partnerRepository.findById(id).orElse(null);
        if (partner == null) {
            return null;
        }
        
        partner.setApiKey(UUID.randomUUID().toString());
        partner.setClientId(UUID.randomUUID().toString());
        
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        partner.setClientSecret(Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes));

        partnerRepository.save(partner);
        return toDTO(partner, true);
    }

    @Transactional
    public boolean deletePartner(Long id) {
        PartnerEntity partner = partnerRepository.findById(id).orElse(null);
        if (partner == null) return false;
        // ADR-0035: only REJECTED deletable by maker
        if (partner.getStatus() != PartnerStatus.REJECTED) {
            throw new ConflictException("PARTNER_CONFLICT_STATUS", "Only REJECTED partners can be deleted");
        }
        partnerRepository.deleteById(id);
        return true;
    }

    // Overload for maker-scoped delete (controller enforces maker ownership via audit)
    @Transactional
    public boolean deletePartner(Long id, String requesterId) {
        return deletePartner(id);
    }

    private PartnerDTO toDTO(PartnerEntity partner) {
        return toDTO(partner, false);
    }

    private PartnerDTO toDTO(PartnerEntity partner, boolean includeSecret) {
        PartnerDTO dto = new PartnerDTO(partner.getId(), partner.getName(), partner.getType(),
                partner.getEmail(), partner.getPhone(), partner.isActive(),
                partner.getClientId(), includeSecret ? partner.getClientSecret() : null,
                partner.getPublicKey());
        dto.status = partner.getStatus() != null ? partner.getStatus().name() : null;
        dto.makerId = partner.getMakerId();
        dto.checkerId = partner.getCheckerId();
        dto.requestedAt = partner.getRequestedAt();
        dto.decidedAt = partner.getDecidedAt();
        dto.rejectionReason = partner.getRejectionReason();
        return dto;
    }
}
