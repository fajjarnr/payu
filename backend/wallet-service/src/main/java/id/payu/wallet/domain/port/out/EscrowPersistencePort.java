package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.EscrowTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscrowPersistencePort {
    EscrowTransaction save(EscrowTransaction escrow);
    Optional<EscrowTransaction> findById(UUID id);
    Optional<EscrowTransaction> findByExternalReferenceId(String externalReferenceId);
    List<EscrowTransaction> findByBuyerAccountId(String buyerAccountId);
    List<EscrowTransaction> findBySellerAccountId(String sellerAccountId);
    List<EscrowTransaction> findByPartnerId(String partnerId);
    List<EscrowTransaction> findExpiredHeldEscrows(LocalDateTime now);
}
