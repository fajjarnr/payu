package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.adapter.persistence.entity.BackofficeAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackofficeAdminRepository extends JpaRepository<BackofficeAdminEntity, UUID> {
    Optional<BackofficeAdminEntity> findByUsername(String username);
    Optional<BackofficeAdminEntity> findByEmail(String email);
}
