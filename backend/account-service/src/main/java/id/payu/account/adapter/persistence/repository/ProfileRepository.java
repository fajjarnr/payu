package id.payu.account.adapter.persistence.repository;

import id.payu.account.adapter.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {
    Optional<ProfileEntity> findByNik(String nik);
}
