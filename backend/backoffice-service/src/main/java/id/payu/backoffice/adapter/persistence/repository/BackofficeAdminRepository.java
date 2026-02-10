package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.domain.BackofficeAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackofficeAdminRepository extends JpaRepository<BackofficeAdmin, UUID> {
    Optional<BackofficeAdmin> findByUsername(String username);
    Optional<BackofficeAdmin> findByEmail(String email);
}
