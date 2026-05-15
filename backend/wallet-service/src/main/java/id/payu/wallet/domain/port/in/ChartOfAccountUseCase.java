package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.ChartOfAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.wallet.domain.model.AccountType;

/**
 * Input port for Chart of Accounts use cases.
 */
public interface ChartOfAccountUseCase {

    /**
     * Get all active chart of accounts.
     */
    List<ChartOfAccount> getAllActiveAccounts();

    /**
     * Get chart of account by code.
     */
    Optional<ChartOfAccount> getByCode(String code);

    /**
     * Get chart of accounts by type.
     */
    List<ChartOfAccount> getByType(AccountType type);

    /**
     * Get children of a parent account.
     */
    List<ChartOfAccount> getChildren(UUID parentId);

    /**
     * Create a new chart of account entry.
     */
    ChartOfAccount createAccount(ChartOfAccount account);
}
