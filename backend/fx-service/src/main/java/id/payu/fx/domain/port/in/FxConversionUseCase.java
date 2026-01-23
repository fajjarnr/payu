package id.payu.fx.domain.port.in;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FxConversionUseCase {

    FxConversion createConversion(FxConversion conversion);

    FxConversion getConversion(UUID conversionId);

    List<FxConversion> getConversionsByAccount(String accountId);

    void reverseConversion(UUID conversionId);
}
