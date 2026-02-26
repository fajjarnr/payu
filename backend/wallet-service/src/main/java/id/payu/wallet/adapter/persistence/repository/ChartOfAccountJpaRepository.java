package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.ChartOfAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChartOfAccountJpaRepository extends JpaRepository<ChartOfAccountEntity, UUID> {

    Optional<ChartOfAccountEntity> findByCode(String code);

    List<ChartOfAccountEntity> findByAccountType(String accountType);

    List<ChartOfAccountEntity> findByCategory(String category);

    List<ChartOfAccountEntity> findByParentId(UUID parentId);

    @Query("SELECT c FROM ChartOfAccountEntity c WHERE c.active = true ORDER BY c.code")
    List<ChartOfAccountEntity> findAllActive();

    @Query("SELECT c FROM ChartOfAccountEntity c WHERE c.level = :level AND c.active = true ORDER BY c.code")
    List<ChartOfAccountEntity> findByLevel(@Param("level") int level);

    boolean existsByCode(String code);
}
