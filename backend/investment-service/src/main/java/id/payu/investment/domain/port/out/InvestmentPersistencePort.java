package id.payu.investment.domain.port.out;

import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.domain.model.MutualFund;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for investment persistence operations.
 * Manages investment accounts, deposits, mutual funds, gold, and transactions.
 */
public interface InvestmentPersistencePort {

    // ========== Investment Account ==========

    /**
     * Save an investment account.
     *
     * @param account the account to save
     * @return the saved account
     */
    InvestmentAccount saveAccount(InvestmentAccount account);

    /**
     * Find investment account by ID.
     *
     * @param id the account ID
     * @return optional containing the account if found
     */
    Optional<InvestmentAccount> findAccountById(UUID id);

    /**
     * Find investment account by user ID.
     *
     * @param userId the user ID
     * @return optional containing the account if found
     */
    Optional<InvestmentAccount> findAccountByUserId(String userId);

    /**
     * Check if an account exists for the given user ID.
     *
     * @param userId the user ID
     * @return true if account exists, false otherwise
     */
    boolean existsAccountByUserId(String userId);

    /**
     * Update account balance by adding the specified amount.
     *
     * @param accountId the account ID
     * @param amount the amount to add (can be negative)
     */
    void updateAccountBalance(UUID accountId, BigDecimal amount);

    // ========== Deposit ==========

    /**
     * Save a deposit.
     *
     * @param deposit the deposit to save
     * @return the saved deposit
     */
    Deposit saveDeposit(Deposit deposit);

    /**
     * Find deposit by ID.
     *
     * @param id the deposit ID
     * @return optional containing the deposit if found
     */
    Optional<Deposit> findDepositById(UUID id);

    // ========== Mutual Fund ==========

    /**
     * Save a mutual fund.
     *
     * @param fund the mutual fund to save
     * @return the saved mutual fund
     */
    MutualFund saveMutualFund(MutualFund fund);

    /**
     * Find mutual fund by code.
     *
     * @param code the fund code
     * @return optional containing the mutual fund if found
     */
    Optional<MutualFund> findFundByCode(String code);

    /**
     * Get the latest NAV price for a mutual fund.
     *
     * @param code the fund code
     * @return the mutual fund with latest price, or null if not found
     */
    MutualFund getLatestFundPrice(String code);

    // ========== Gold ==========

    /**
     * Save gold holdings.
     *
     * @param gold the gold holdings to save
     * @return the saved gold holdings
     */
    Gold saveGold(Gold gold);

    /**
     * Find gold holdings by user ID.
     *
     * @param userId the user ID
     * @return optional containing the gold holdings if found
     */
    Optional<Gold> findGoldByUserId(String userId);

    /**
     * Get the latest gold price per gram.
     *
     * @return the current gold price
     */
    BigDecimal getLatestGoldPrice();

    // ========== Investment Transaction ==========

    /**
     * Save an investment transaction.
     *
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    InvestmentTransaction saveTransaction(InvestmentTransaction transaction);

    /**
     * Find transaction by ID.
     *
     * @param id the transaction ID
     * @return optional containing the transaction if found
     */
    Optional<InvestmentTransaction> findTransactionById(UUID id);
}
