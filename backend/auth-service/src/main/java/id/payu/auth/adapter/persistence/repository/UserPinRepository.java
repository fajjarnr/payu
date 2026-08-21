package id.payu.auth.adapter.persistence.repository;

import id.payu.auth.adapter.persistence.entity.UserPinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPinRepository extends JpaRepository<UserPinEntity, String> {}
