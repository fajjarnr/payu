package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {
    List<CardEntity> findByWalletId(UUID walletId);

    boolean existsByCardNumber(String cardNumber);
}
