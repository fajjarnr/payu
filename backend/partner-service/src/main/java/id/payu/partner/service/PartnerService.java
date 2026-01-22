package id.payu.partner.service;

import id.payu.partner.domain.Partner;
import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.repository.PartnerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PartnerService {

    @Inject
    PartnerRepository partnerRepository;

    public List<PartnerDTO> getAllPartners() {
        return partnerRepository.listAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PartnerDTO getPartnerById(Long id) {
        Partner partner = partnerRepository.findById(id);
        if (partner == null) {
            return null;
        }
        return toDTO(partner);
    }

    @Transactional
    public PartnerDTO createPartner(PartnerDTO partnerDTO) {
        if (partnerRepository.findByEmail(partnerDTO.email).isPresent()) {
            throw new IllegalArgumentException("Partner with email " + partnerDTO.email + " already exists");
        }

        Partner partner = new Partner();
        partner.name = partnerDTO.name;
        partner.type = partnerDTO.type;
        partner.email = partnerDTO.email;
        partner.phone = partnerDTO.phone;
        partner.active = true;
        partner.apiKey = UUID.randomUUID().toString(); // Generate API Key

        partnerRepository.persist(partner);
        return toDTO(partner);
    }

    @Transactional
    public PartnerDTO updatePartner(Long id, PartnerDTO partnerDTO) {
        Partner partner = partnerRepository.findById(id);
        if (partner == null) {
            return null;
        }

        partner.name = partnerDTO.name;
        partner.type = partnerDTO.type;
        partner.phone = partnerDTO.phone;
        // Email update logic check could be added here
        
        return toDTO(partner);
    }

    @Transactional
    public boolean deletePartner(Long id) {
        return partnerRepository.deleteById(id);
    }

    private PartnerDTO toDTO(Partner partner) {
        return new PartnerDTO(partner.id, partner.name, partner.type, partner.email, partner.phone, partner.active);
    }
}
