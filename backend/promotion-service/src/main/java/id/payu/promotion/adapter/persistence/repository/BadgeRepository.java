package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    List<Badge> findByIsActiveTrue();

    Optional<Badge> findByName(String name);
}
