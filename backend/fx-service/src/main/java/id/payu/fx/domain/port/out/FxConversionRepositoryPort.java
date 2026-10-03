package id.payu.fx.domain.port.out;

import id.payu.fx.domain.model.FxConversion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for FX conversion persistence operations.
 * Manages foreign exchange transaction records.
 */
public interface FxConversionRepositoryPort {

    FxConversion save(FxConversion conversion);

    Optional<FxConversion> findById(UUID conversionId);

    List<FxConversion> findByAccountId(String accountId);

    void deleteById(UUID conversionId);
}
