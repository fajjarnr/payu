package id.payu.account.adapter.persistence.repository;

import id.payu.account.adapter.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByExternalId(String externalId);
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmailHash(String emailHash);
    Optional<UserEntity> findByPhoneNumberHash(String phoneNumberHash);
    boolean existsByEmailHash(String emailHash);
    boolean existsByUsername(String username);
}
