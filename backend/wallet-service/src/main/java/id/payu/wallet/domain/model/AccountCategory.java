package id.payu.wallet.domain.model;

public enum AccountCategory {
        // Asset categories
        USER_WALLET,
        BANK_ACCOUNT,
        ESCROW_RECEIVABLE,
        SETTLEMENT_RECEIVABLE,

        // Liability categories
        ESCROW_HOLDING,
        MERCHANT_PAYABLE,
        PARTNER_PAYABLE,
        FEE_PAYABLE,

        // Equity categories
        CAPITAL,
        RETAINED_EARNINGS,

        // Revenue categories
        TRANSACTION_FEE,
        INTEREST_INCOME,
        FX_SPREAD,
        SERVICE_FEE,

        // Expense categories
        OPERATIONAL_COST,
        SETTLEMENT_COST,
        REFUND_COST
    }
