package id.payu.auth.adapter.persistence.repository;

import id.payu.auth.adapter.persistence.entity.UserRiskProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRiskProfileRepository extends JpaRepository<UserRiskProfileEntity, String> {
}
