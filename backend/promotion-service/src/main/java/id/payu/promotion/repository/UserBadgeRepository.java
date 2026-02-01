package id.payu.promotion.repository;

import id.payu.promotion.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    List<UserBadge> findByAccountId(String accountId);

    List<UserBadge> findByAccountIdAndBadgeId(String accountId, UUID badgeId);

    boolean existsByAccountIdAndBadgeId(String accountId, UUID badgeId);
}
