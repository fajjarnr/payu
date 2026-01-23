package id.payu.fx.application.service;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.in.FxConversionUseCase;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.domain.port.out.FxConversionRepositoryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FxConversionService implements FxConversionUseCase {

    private final FxConversionRepositoryPort conversionRepository;
    private final FxRateUseCase fxRateUseCase;

    public FxConversionService(FxConversionRepositoryPort conversionRepository, FxRateUseCase fxRateUseCase) {
        this.conversionRepository = conversionRepository;
        this.fxRateUseCase = fxRateUseCase;
    }

    @Override
    public FxConversion createConversion(FxConversion conversion) {
        FxRate rate = fxRateUseCase.getCurrentRate(conversion.getFromCurrency(), conversion.getToCurrency());
        
        BigDecimal convertedAmount = conversion.getFromAmount().multiply(rate.getRate());
        conversion.setToAmount(convertedAmount);
        conversion.setExchangeRate(rate.getRate());
        conversion.setStatus(FxConversion.ConversionStatus.PENDING);
        
        return conversionRepository.save(conversion);
    }

    @Override
    public FxConversion getConversion(UUID conversionId) {
        Optional<FxConversion> conversion = conversionRepository.findById(conversionId);
        if (conversion.isEmpty()) {
            throw new FxConversionNotFoundException("Conversion not found: " + conversionId);
        }
        return conversion.get();
    }

    @Override
    public List<FxConversion> getConversionsByAccount(String accountId) {
        return conversionRepository.findByAccountId(accountId);
    }

    @Override
    public void reverseConversion(UUID conversionId) {
        FxConversion conversion = getConversion(conversionId);
        
        if (conversion.getStatus() != FxConversion.ConversionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reverse conversion with status: " + conversion.getStatus());
        }
        
        conversion.markReversed();
        conversionRepository.save(conversion);
    }
}
