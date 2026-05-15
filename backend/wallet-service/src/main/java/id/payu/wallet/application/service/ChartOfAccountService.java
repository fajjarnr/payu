package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.port.in.ChartOfAccountUseCase;
import id.payu.wallet.domain.port.out.JournalPersistencePort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.wallet.domain.model.AccountType;

/**
 * Application service for Chart of Accounts operations (IMP-002).
 */
@Service
public class ChartOfAccountService implements ChartOfAccountUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChartOfAccountService.class);

    private final JournalPersistencePort journalPersistencePort;

    public ChartOfAccountService(JournalPersistencePort journalPersistencePort) {
        this.journalPersistencePort = journalPersistencePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccount> getAllActiveAccounts() {
        log.debug("Getting all active chart of accounts");
        return journalPersistencePort.findAllActiveChartOfAccounts();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChartOfAccount> getByCode(String code) {
        log.debug("Getting chart of account by code: {}", code);
        return journalPersistencePort.findChartOfAccountByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccount> getByType(AccountType type) {
        log.debug("Getting chart of accounts by type: {}", type);
        return journalPersistencePort.findChartOfAccountsByType(type.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccount> getChildren(UUID parentId) {
        log.debug("Getting children of account: {}", parentId);
        return journalPersistencePort.findChartOfAccountsByParentId(parentId);
    }

    @Override
    @Transactional
    public ChartOfAccount createAccount(ChartOfAccount account) {
        log.info("Creating chart of account: code={}, name={}", account.getCode(), account.getName());

        if (journalPersistencePort.chartOfAccountExistsByCode(account.getCode())) {
            throw new IllegalArgumentException("Chart of account with code " + account.getCode() + " already exists");
        }

        if (account.getId() == null) {
            account.setId(UUID.randomUUID());
        }
        if (account.getCreatedAt() == null) {
            account.setCreatedAt(LocalDateTime.now());
        }

        ChartOfAccount saved = journalPersistencePort.saveChartOfAccount(account);
        log.info("Chart of account created: {} - {}", saved.getCode(), saved.getName());
        return saved;
    }
}
