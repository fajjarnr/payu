package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    List<XpTransaction> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<XpTransaction> findTop50ByAccountIdOrderByCreatedAtDesc(String accountId);
}
