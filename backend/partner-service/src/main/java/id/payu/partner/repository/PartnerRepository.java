package id.payu.partner.repository;

import id.payu.partner.domain.Partner;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class PartnerRepository implements PanacheRepository<Partner> {
    
    public Optional<Partner> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<Partner> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
