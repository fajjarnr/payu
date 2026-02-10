package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.LevelReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LevelRewardRepository extends JpaRepository<LevelReward, UUID> {

    List<LevelReward> findByLevel(Integer level);
}
