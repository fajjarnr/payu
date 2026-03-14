package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.RevenueSplit;
import id.payu.wallet.domain.model.SettlementBatch;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementPersistencePort {
    SettlementBatch saveSettlementBatch(SettlementBatch batch);
    Optional<SettlementBatch> findSettlementBatchById(UUID id);
    List<SettlementBatch> findSettlementBatchesByPartner(String partnerId, LocalDate from, LocalDate to);
    List<SettlementBatch> findSettlementBatchesByDate(LocalDate settlementDate);
    List<SettlementBatch> findPendingSettlementBatches();

    RevenueSplit saveRevenueSplit(RevenueSplit revenueSplit);
    Optional<RevenueSplit> findRevenueSplitById(UUID id);
    List<RevenueSplit> findRevenueSplitsByPartner(String partnerId);
    List<RevenueSplit> findActiveRevenueSplitsByPartner(String partnerId);
}
