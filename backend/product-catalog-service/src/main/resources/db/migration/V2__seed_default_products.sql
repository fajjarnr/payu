-- Seed default product definitions for PayU platform
-- These replace previously hardcoded values in wallet-service, lending-service, and investment-service

-- SAVINGS products
INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'SAVINGS_BASIC',
    'SAVINGS',
    'Basic Savings',
    'Standard savings account with basic features',
    true,
    '{
        "minimumBalance": 10000,
        "monthlyFee": 0,
        "interestRate": 0.015,
        "interestCalculationMethod": "DAILY_BALANCE",
        "withdrawalLimitPerDay": 100000000,
        "transferLimitPerDay": 100000000,
        "currency": "IDR"
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'SAVINGS_PREMIUM',
    'SAVINGS',
    'Premium Savings',
    'High-yield savings account for premium customers',
    true,
    '{
        "minimumBalance": 1000000,
        "monthlyFee": 0,
        "interestRate": 0.035,
        "interestCalculationMethod": "DAILY_BALANCE",
        "withdrawalLimitPerDay": 500000000,
        "transferLimitPerDay": 500000000,
        "currency": "IDR",
        "bonusInterestRate": 0.005,
        "bonusMinimumBalance": 50000000
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

-- LOAN products
INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'LOAN_PERSONAL',
    'LOAN',
    'Personal Loan',
    'Unsecured personal loan for various purposes',
    true,
    '{
        "minAmount": 5000000,
        "maxAmount": 300000000,
        "minTenureMonths": 6,
        "maxTenureMonths": 60,
        "baseInterestRate": 0.12,
        "maxInterestRate": 0.24,
        "processingFeeRate": 0.03,
        "latePaymentFee": 150000,
        "earlyRepaymentFee": 0.02,
        "requiredDocuments": ["KTP", "NPWP", "SALARY_SLIP"],
        "minCreditScore": 600
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'LOAN_MICRO',
    'LOAN',
    'Micro Loan',
    'Small loan for micro businesses and entrepreneurs',
    true,
    '{
        "minAmount": 1000000,
        "maxAmount": 50000000,
        "minTenureMonths": 3,
        "maxTenureMonths": 24,
        "baseInterestRate": 0.18,
        "maxInterestRate": 0.36,
        "processingFeeRate": 0.02,
        "latePaymentFee": 50000,
        "earlyRepaymentFee": 0.01,
        "requiredDocuments": ["KTP", "BUSINESS_PROOF"],
        "minCreditScore": 500
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

-- PAYLATER product
INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'PAYLATER_STANDARD',
    'PAYLATER',
    'PayLater',
    'Buy now, pay later with flexible installments',
    true,
    '{
        "creditLimitMin": 1000000,
        "creditLimitMax": 50000000,
        "billingCycleDays": 30,
        "gracePeriodDays": 7,
        "interestRate": 0.025,
        "lateFee": 100000,
        "minCreditScore": 550,
        "maxUtilizationRatio": 0.9,
        "installmentOptions": [3, 6, 12],
        "installmentInterestRate": 0.015
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

-- INVESTMENT products
INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'INVESTMENT_DEPOSIT',
    'INVESTMENT',
    'Time Deposit',
    'Fixed-term deposit with guaranteed returns',
    true,
    '{
        "minAmount": 10000000,
        "maxAmount": 10000000000,
        "tenureOptions": [1, 3, 6, 12, 24],
        "interestRates": {
            "1": 0.045,
            "3": 0.055,
            "6": 0.065,
            "12": 0.075,
            "24": 0.085
        },
        "earlyWithdrawalPenalty": 0.02,
        "autoRollover": true,
        "taxRate": 0.20
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'INVESTMENT_MUTUAL_FUND',
    'INVESTMENT',
    'Mutual Fund',
    'Diversified mutual fund investment',
    true,
    '{
        "minInitialAmount": 100000,
        "minSubsequentAmount": 100000,
        "subscriptionFeeRate": 0.02,
        "redemptionFeeRate": 0.01,
        "managementFeeRate": 0.015,
        "settlementDays": 2,
        "riskProfiles": ["CONSERVATIVE", "MODERATE", "AGGRESSIVE"]
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'INVESTMENT_GOLD',
    'INVESTMENT',
    'Digital Gold',
    'Invest in gold digitally with small amounts',
    true,
    '{
        "minAmount": 10000,
        "minQuantityGrams": 0.01,
        "buySpreadRate": 0.005,
        "sellSpreadRate": 0.005,
        "storageFeeRate": 0.001,
        "settlementDays": 0,
        "physicalRedemptionMinGrams": 1.0,
        "physicalRedemptionFee": 50000
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

-- INSURANCE products
INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'INSURANCE_LIFE_BASIC',
    'INSURANCE',
    'Basic Life Insurance',
    'Term life insurance coverage',
    true,
    '{
        "minCoverage": 100000000,
        "maxCoverage": 2000000000,
        "minEntryAge": 18,
        "maxEntryAge": 60,
        "termOptions": [5, 10, 20],
        "premiumPaymentModes": ["MONTHLY", "QUARTERLY", "ANNUALLY"],
        "medicalCheckupThreshold": 500000000
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;

-- CREDIT_CARD product
INSERT INTO product_definitions (product_code, product_type, name, description, active, parameters, created_at, updated_at)
VALUES (
    'CREDIT_CARD_CLASSIC',
    'CREDIT_CARD',
    'Classic Credit Card',
    'Standard credit card with basic rewards',
    true,
    '{
        "creditLimitMin": 3000000,
        "creditLimitMax": 50000000,
        "annualFee": 250000,
        "interestRate": 0.029,
        "cashAdvanceFeeRate": 0.06,
        "latePaymentFee": 150000,
        "overLimitFee": 100000,
        "minPaymentRate": 0.10,
        "rewardPointsRate": 0.001,
        "freeSupplementaryCards": 2
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (product_code) DO NOTHING;
