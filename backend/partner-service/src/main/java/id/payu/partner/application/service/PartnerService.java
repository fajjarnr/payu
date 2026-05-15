package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;

    public List<PartnerDTO> getAllPartners() {
        return partnerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PartnerDTO getPartnerById(Long id) {
        return partnerRepository.findById(id).map(this::toDTO).orElse(null);
    }

    /**
     * BUG-BE-138 FIX: Lookup partner by clientId via service layer.
     * Allows SnapBiController to use service instead of direct repository access.
     */
    public Optional<PartnerEntity> findByClientId(String clientId) {
        return partnerRepository.findByClientId(clientId);
    }

    @Transactional
    public PartnerDTO createPartner(PartnerDTO partnerDTO) {
        if (partnerRepository.findByEmail(partnerDTO.email).isPresent()) {
            throw new IllegalArgumentException("PartnerEntity with email " + partnerDTO.email + " already exists");
        }

        PartnerEntity partner = new PartnerEntity();
        partner.setName(partnerDTO.name);
        partner.setType(partnerDTO.type);
        partner.setEmail(partnerDTO.email);
        partner.setPhone(partnerDTO.phone);
        partner.setActive(true);
        partner.setApiKey(UUID.randomUUID().toString());
        partner.setClientId(UUID.randomUUID().toString());
        partner.setClientSecret(UUID.randomUUID().toString());
        partner.setPublicKey(partnerDTO.publicKey);

        partnerRepository.save(partner);
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
        return toDTO(partner);
    }

    @Transactional
    public boolean deletePartner(Long id) {
        if (partnerRepository.existsById(id)) {
            partnerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PartnerDTO toDTO(PartnerEntity partner) {
        return new PartnerDTO(partner.getId(), partner.getName(), partner.getType(), partner.getEmail(), partner.getPhone(), partner.isActive(), partner.getClientId(), partner.getClientSecret(), partner.getPublicKey());
    }
}
