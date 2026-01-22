package id.payu.investment.adapter.persistence;

import id.payu.investment.adapter.persistence.repository.DepositRepository;
import id.payu.investment.adapter.persistence.repository.GoldRepository;
import id.payu.investment.adapter.persistence.repository.InvestmentAccountRepository;
import id.payu.investment.adapter.persistence.repository.InvestmentTransactionRepository;
import id.payu.investment.adapter.persistence.repository.MutualFundRepository;
import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.domain.model.MutualFund;
import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvestmentPersistenceAdapter implements InvestmentPersistencePort {

    private final InvestmentAccountRepository accountRepository;
    private final DepositRepository depositRepository;
    private final MutualFundRepository mutualFundRepository;
    private final GoldRepository goldRepository;
    private final InvestmentTransactionRepository transactionRepository;

    @Override
    @Transactional
    public InvestmentAccount saveAccount(InvestmentAccount account) {
        InvestmentAccountEntity entity = toAccountEntity(account);
        InvestmentAccountEntity savedEntity = accountRepository.save(entity);
        return toAccountDomain(savedEntity);
    }

    @Override
    public Optional<InvestmentAccount> findAccountById(UUID id) {
        return accountRepository.findById(id).map(this::toAccountDomain);
    }

    @Override
    public Optional<InvestmentAccount> findAccountByUserId(String userId) {
        return accountRepository.findByUserId(userId).map(this::toAccountDomain);
    }

    @Override
    public boolean existsAccountByUserId(String userId) {
        return accountRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public Deposit saveDeposit(Deposit deposit) {
        DepositEntity entity = toDepositEntity(deposit);
        DepositEntity savedEntity = depositRepository.save(entity);
        return toDepositDomain(savedEntity);
    }

    @Override
    public Optional<Deposit> findDepositById(UUID id) {
        return depositRepository.findById(id).map(this::toDepositDomain);
    }

    @Override
    @Transactional
    public MutualFund saveMutualFund(MutualFund fund) {
        MutualFundEntity entity = toMutualFundEntity(fund);
        MutualFundEntity savedEntity = mutualFundRepository.save(entity);
        return toMutualFundDomain(savedEntity);
    }

    @Override
    public Optional<MutualFund> findFundByCode(String code) {
        return mutualFundRepository.findByCode(code).map(this::toMutualFundDomain);
    }

    @Override
    @Transactional
    public Gold saveGold(Gold gold) {
        GoldEntity entity = toGoldEntity(gold);
        GoldEntity savedEntity = goldRepository.save(entity);
        return toGoldDomain(savedEntity);
    }

    @Override
    public Optional<Gold> findGoldByUserId(String userId) {
        return goldRepository.findByUserId(userId).map(this::toGoldDomain);
    }

    @Override
    @Transactional
    public InvestmentTransaction saveTransaction(InvestmentTransaction transaction) {
        InvestmentTransactionEntity entity = toTransactionEntity(transaction);
        InvestmentTransactionEntity savedEntity = transactionRepository.save(entity);
        return toTransactionDomain(savedEntity);
    }

    @Override
    public Optional<InvestmentTransaction> findTransactionById(UUID id) {
        return transactionRepository.findById(id).map(this::toTransactionDomain);
    }

    @Override
    @Transactional
    public void updateAccountBalance(UUID accountId, BigDecimal amount) {
        accountRepository.findById(accountId).ifPresent(account -> {
            BigDecimal newTotalBalance = account.getTotalBalance().add(amount);
            BigDecimal newAvailableBalance = account.getAvailableBalance().add(amount);
            account.setTotalBalance(newTotalBalance);
            account.setAvailableBalance(newAvailableBalance);
            accountRepository.save(account);
        });
    }

    @Override
    public MutualFund getLatestFundPrice(String code) {
        return mutualFundRepository.findByCode(code)
                .map(this::toMutualFundDomain)
                .orElse(null);
    }

    @Override
    public BigDecimal getLatestGoldPrice() {
        return BigDecimal.valueOf(1250000);
    }

    private InvestmentAccount toAccountDomain(InvestmentAccountEntity entity) {
        return InvestmentAccount.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .totalBalance(entity.getTotalBalance())
                .availableBalance(entity.getAvailableBalance())
                .lockedBalance(entity.getLockedBalance())
                .status(InvestmentAccount.AccountStatus.valueOf(entity.getStatus().name()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private InvestmentAccountEntity toAccountEntity(InvestmentAccount domain) {
        return InvestmentAccountEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .totalBalance(domain.getTotalBalance())
                .availableBalance(domain.getAvailableBalance())
                .lockedBalance(domain.getLockedBalance())
                .status(InvestmentAccountEntity.AccountStatus.valueOf(domain.getStatus().name()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private Deposit toDepositDomain(DepositEntity entity) {
        return Deposit.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .tenure(entity.getTenure())
                .interestRate(entity.getInterestRate())
                .maturityAmount(entity.getMaturityAmount())
                .startDate(entity.getStartDate())
                .maturityDate(entity.getMaturityDate())
                .status(Deposit.DepositStatus.valueOf(entity.getStatus().name()))
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DepositEntity toDepositEntity(Deposit domain) {
        return DepositEntity.builder()
                .id(domain.getId())
                .accountId(domain.getAccountId())
                .amount(domain.getAmount())
                .tenure(domain.getTenure())
                .interestRate(domain.getInterestRate())
                .maturityAmount(domain.getMaturityAmount())
                .startDate(domain.getStartDate())
                .maturityDate(domain.getMaturityDate())
                .status(DepositEntity.DepositStatus.valueOf(domain.getStatus().name()))
                .currency(domain.getCurrency())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private MutualFund toMutualFundDomain(MutualFundEntity entity) {
        return MutualFund.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .type(MutualFund.FundType.valueOf(entity.getType().name()))
                .navPerUnit(entity.getNavPerUnit())
                .minimumInvestment(entity.getMinimumInvestment())
                .managementFee(entity.getManagementFee())
                .redemptionFee(entity.getRedemptionFee())
                .status(MutualFund.FundStatus.valueOf(entity.getStatus().name()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MutualFundEntity toMutualFundEntity(MutualFund domain) {
        return MutualFundEntity.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .name(domain.getName())
                .type(MutualFundEntity.FundType.valueOf(domain.getType().name()))
                .navPerUnit(domain.getNavPerUnit())
                .minimumInvestment(domain.getMinimumInvestment())
                .managementFee(domain.getManagementFee())
                .redemptionFee(domain.getRedemptionFee())
                .status(MutualFundEntity.FundStatus.valueOf(domain.getStatus().name()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private Gold toGoldDomain(GoldEntity entity) {
        return Gold.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .amount(entity.getAmount())
                .averageBuyPrice(entity.getAverageBuyPrice())
                .currentPrice(entity.getCurrentPrice())
                .currentValue(entity.getCurrentValue())
                .unrealizedProfitLoss(entity.getUnrealizedProfitLoss())
                .lastPriceUpdate(entity.getLastPriceUpdate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private GoldEntity toGoldEntity(Gold domain) {
        return GoldEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .amount(domain.getAmount())
                .averageBuyPrice(domain.getAverageBuyPrice())
                .currentPrice(domain.getCurrentPrice())
                .currentValue(domain.getCurrentValue())
                .unrealizedProfitLoss(domain.getUnrealizedProfitLoss())
                .lastPriceUpdate(domain.getLastPriceUpdate())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private InvestmentTransaction toTransactionDomain(InvestmentTransactionEntity entity) {
        return InvestmentTransaction.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .type(InvestmentTransaction.TransactionType.valueOf(entity.getType().name()))
                .investmentType(InvestmentTransaction.InvestmentType.valueOf(entity.getInvestmentType().name()))
                .investmentId(entity.getInvestmentId())
                .amount(entity.getAmount())
                .price(entity.getPrice())
                .units(entity.getUnits())
                .fee(entity.getFee())
                .currency(entity.getCurrency())
                .status(InvestmentTransaction.TransactionStatus.valueOf(entity.getStatus().name()))
                .referenceNumber(entity.getReferenceNumber())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private InvestmentTransactionEntity toTransactionEntity(InvestmentTransaction domain) {
        return InvestmentTransactionEntity.builder()
                .id(domain.getId())
                .accountId(domain.getAccountId())
                .type(InvestmentTransactionEntity.TransactionType.valueOf(domain.getType().name()))
                .investmentType(InvestmentTransactionEntity.InvestmentType.valueOf(domain.getInvestmentType().name()))
                .investmentId(domain.getInvestmentId())
                .amount(domain.getAmount())
                .price(domain.getPrice())
                .units(domain.getUnits())
                .fee(domain.getFee())
                .currency(domain.getCurrency())
                .status(InvestmentTransactionEntity.TransactionStatus.valueOf(domain.getStatus().name()))
                .referenceNumber(domain.getReferenceNumber())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
