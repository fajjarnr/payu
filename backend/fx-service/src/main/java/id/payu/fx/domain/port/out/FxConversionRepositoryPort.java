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

    /**
     * Save an FX conversion.
     *
     * @param conversion the conversion to save
     * @return the saved conversion
     */
    FxConversion save(FxConversion conversion);

    /**
     * Find conversion by ID.
     *
     * @param conversionId the conversion ID
     * @return optional containing the conversion if found
     */
    Optional<FxConversion> findById(UUID conversionId);

    /**
     * Find all conversions for an account.
     *
     * @param accountId the account ID
     * @return list of conversions for the account
     */
    List<FxConversion> findByAccountId(String accountId);

    /**
     * Delete conversion by ID.
     *
     * @param conversionId the conversion ID
     */
    void deleteById(UUID conversionId);
}
