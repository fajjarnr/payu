package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    
    Optional<Partner> findByEmail(String email);

    Optional<Partner> findByClientId(String clientId);
}
