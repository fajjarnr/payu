package id.payu.fx.application.service;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.in.FxConversionUseCase;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.domain.port.out.FxConversionRepositoryPort;
import id.payu.fx.domain.port.out.WalletServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FX Conversion application service.
 * BUG-BE-024 FIX: Now integrates with wallet-service to debit source currency
 * and credit target currency upon conversion.
 */
@Slf4j
@Service
public class FxConversionService implements FxConversionUseCase {

    private final FxConversionRepositoryPort conversionRepository;
    private final FxRateUseCase fxRateUseCase;
    private final WalletServicePort walletServicePort;

    public FxConversionService(FxConversionRepositoryPort conversionRepository,
                               FxRateUseCase fxRateUseCase,
                               WalletServicePort walletServicePort) {
        this.conversionRepository = conversionRepository;
        this.fxRateUseCase = fxRateUseCase;
        this.walletServicePort = walletServicePort;
    }

    @Override
    public FxConversion createConversion(FxConversion conversion) {
        FxRate rate = fxRateUseCase.getCurrentRate(conversion.getFromCurrency(), conversion.getToCurrency());
        
        BigDecimal convertedAmount = conversion.getFromAmount().multiply(rate.getRate());
        conversion.setToAmount(convertedAmount);
        conversion.setExchangeRate(rate.getRate());
        conversion.setStatus(FxConversion.ConversionStatus.PENDING);
        
        FxConversion saved = conversionRepository.save(conversion);
        String txId = saved.getId().toString();

        // BUG-BE-024 FIX: Debit source currency from wallet
        boolean debited = walletServicePort.debit(
                saved.getAccountId(), txId, saved.getFromAmount(), saved.getFromCurrency());

        if (!debited) {
            log.warn("FX conversion {} failed: unable to debit {} {} from account {}",
                    txId, saved.getFromAmount(), saved.getFromCurrency(), saved.getAccountId());
            saved.markFailed();
            conversionRepository.save(saved);
            throw new IllegalStateException("Insufficient balance or wallet debit failed");
        }

        // Credit target currency to wallet
        boolean credited = walletServicePort.credit(
                saved.getAccountId(), txId, saved.getToAmount(), saved.getToCurrency());

        if (!credited) {
            // Compensate: reverse the debit since credit failed
            log.error("FX conversion {} credit failed, reversing debit of {} {}",
                    txId, saved.getFromAmount(), saved.getFromCurrency());
            walletServicePort.reverseDebit(
                    saved.getAccountId(), txId, saved.getFromAmount(), saved.getFromCurrency());
            saved.markFailed();
            conversionRepository.save(saved);
            throw new IllegalStateException("Wallet credit failed for target currency");
        }

        saved.markCompleted();
        return conversionRepository.save(saved);
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
        
        String txId = conversion.getId().toString();

        // BUG-BE-171 FIX: Perform actual wallet operations (inverse of createConversion).
        // Debit the target currency that was credited during conversion
        boolean debited = walletServicePort.debit(
                conversion.getAccountId(), txId + "-REV",
                conversion.getToAmount(), conversion.getToCurrency());

        if (!debited) {
            log.error("FX reversal {} failed: unable to debit {} {} (target currency) from account {}",
                    txId, conversion.getToAmount(), conversion.getToCurrency(), conversion.getAccountId());
            throw new IllegalStateException("Cannot reverse conversion: insufficient target currency balance");
        }

        // Credit back the source currency that was debited during conversion
        boolean credited = walletServicePort.credit(
                conversion.getAccountId(), txId + "-REV",
                conversion.getFromAmount(), conversion.getFromCurrency());

        if (!credited) {
            // Compensate: reverse the debit we just did
            log.error("FX reversal {} credit failed, reversing debit of {} {}",
                    txId, conversion.getToAmount(), conversion.getToCurrency());
            walletServicePort.reverseDebit(
                    conversion.getAccountId(), txId + "-REV",
                    conversion.getToAmount(), conversion.getToCurrency());
            throw new IllegalStateException("Cannot reverse conversion: source currency credit failed");
        }

        conversion.markReversed();
        conversionRepository.save(conversion);
        
        log.info("FX conversion {} reversed: debited {} {}, credited {} {}",
                txId, conversion.getToAmount(), conversion.getToCurrency(),
                conversion.getFromAmount(), conversion.getFromCurrency());
    }
}
