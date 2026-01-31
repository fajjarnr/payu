package id.payu.auth.repository;

import id.payu.auth.entity.BiometricRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricRegistrationRepository extends JpaRepository<BiometricRegistrationEntity, String> {
    
    Optional<BiometricRegistrationEntity> findByUsernameAndDeviceIdAndActiveTrue(String username, String deviceId);
    
    List<BiometricRegistrationEntity> findByUsernameAndActiveTrue(String username);
}
