package id.payu.auth.repository;

import id.payu.auth.entity.UserRiskProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRiskProfileRepository extends JpaRepository<UserRiskProfileEntity, String> {
}
