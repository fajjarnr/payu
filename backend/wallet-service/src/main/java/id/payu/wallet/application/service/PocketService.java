package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.port.in.PocketUseCase;
import id.payu.wallet.domain.port.out.FxRateProviderPort;
import id.payu.wallet.domain.port.out.PocketPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PocketService implements PocketUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PocketService.class);

    private final PocketPersistencePort pocketPersistencePort;
    private final FxRateProviderPort fxRateProviderPort;

    public PocketService(PocketPersistencePort pocketPersistencePort, FxRateProviderPort fxRateProviderPort) {
        this.pocketPersistencePort = pocketPersistencePort;
        this.fxRateProviderPort = fxRateProviderPort;
    }

    @Override
    @Transactional
    public Pocket createPocket(String accountId, String name, String description, String currency) {
        log.info("Creating pocket {} for account {} with currency {}", name, accountId, currency);

        Pocket pocket = Pocket.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .name(name)
                .description(description)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .status(Pocket.PocketStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Pocket saved = pocketPersistencePort.save(pocket);
        log.info("Pocket created successfully: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Pocket> getPocketById(UUID pocketId) {
        log.debug("Getting pocket by ID: {}", pocketId);
        return pocketPersistencePort.findById(pocketId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pocket> getPocketsByAccountId(String accountId) {
        log.debug("Getting pockets for account: {}", accountId);
        return pocketPersistencePort.findByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pocket> getPocketsByAccountIdAndCurrency(String accountId, String currency) {
        log.debug("Getting pockets for account {} with currency {}", accountId, currency);
        return pocketPersistencePort.findByAccountIdAndCurrency(accountId, currency);
    }

    @Override
    @Transactional
    public void creditPocket(UUID pocketId, BigDecimal amount, String referenceId) {
        log.info("Crediting {} to pocket {} with reference {}", amount, pocketId, referenceId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.credit(amount);
        pocketPersistencePort.save(pocket);

        log.info("Credited {} to pocket {}, amount: {}", pocketId, amount);
    }

    @Override
    @Transactional
    public void debitPocket(UUID pocketId, BigDecimal amount, String referenceId) {
        log.info("Debiting {} from pocket {} with reference {}", amount, pocketId, referenceId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.debit(amount);
        pocketPersistencePort.save(pocket);

        log.info("Debited {} from pocket {}, amount: {}", pocketId, amount);
    }

    @Override
    @Transactional
    public void freezePocket(UUID pocketId) {
        log.info("Freezing pocket: {}", pocketId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.freeze();
        pocketPersistencePort.save(pocket);

        log.info("Pocket frozen successfully: {}", pocketId);
    }

    @Override
    @Transactional
    public void unfreezePocket(UUID pocketId) {
        log.info("Unfreezing pocket: {}", pocketId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.unfreeze();
        pocketPersistencePort.save(pocket);

        log.info("Pocket unfrozen successfully: {}", pocketId);
    }

    @Override
    @Transactional
    public void closePocket(UUID pocketId) {
        log.info("Closing pocket: {}", pocketId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.close();
        pocketPersistencePort.save(pocket);

        log.info("Pocket closed successfully: {}", pocketId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceInCurrency(String accountId, String targetCurrency) {
        log.debug("Calculating total balance for account {} in currency {}", accountId, targetCurrency);

        List<Pocket> pockets = pocketPersistencePort.findByAccountId(accountId);
        BigDecimal total = BigDecimal.ZERO;

        for (Pocket pocket : pockets) {
            BigDecimal pocketBalance = pocket.getBalance();

            if (pocket.getCurrency().equals(targetCurrency)) {
                total = total.add(pocketBalance);
            } else {
                Optional<?> rateOptional = fxRateProviderPort.getCurrentRate(
                        pocket.getCurrency(), targetCurrency);
                if (rateOptional.isEmpty()) {
                    throw new FxRateNotFoundException(
                                "No FX rate available for " + pocket.getCurrency() + " to " + targetCurrency);
                }
                var rate = rateOptional.get();
                
                try {
                    java.lang.reflect.Field rateField = rate.getClass().getDeclaredField("rate");
                    rateField.setAccessible(true);
                    BigDecimal rateValue = (BigDecimal) rateField.get(rate);
                    BigDecimal convertedAmount = pocketBalance.multiply(rateValue);
                    total = total.add(convertedAmount);
                } catch (Exception e) {
                    throw new FxRateNotFoundException("Failed to get FX rate value", e);
                }
            }
        }

        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pocket> getAllActivePockets() {
        log.debug("Getting all active pockets");
        return pocketPersistencePort.findAllActive();
    }
}
