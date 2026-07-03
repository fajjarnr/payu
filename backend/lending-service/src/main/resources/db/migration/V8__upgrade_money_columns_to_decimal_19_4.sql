-- AUDIT-042 / GAP-25: Upgrade monetary columns from DECIMAL/NUMERIC(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision in wallet-service.
--
-- Affected columns (lending-service):
--   - loans.principal_amount                  (V1__Create_schema.sql:6, V1_1__Fix_Loans_Table.sql:6)
--   - loans.monthly_installment               (V1__Create_schema.sql:9, V1_1__Fix_Loans_Table.sql:9)
--   - loans.outstanding_balance               (V1__Create_schema.sql:10, V1_1__Fix_Loans_Table.sql:10)
--   - paylater_accounts.credit_limit          (V1__Create_schema.sql:23)
--   - paylater_accounts.used_credit           (V1__Create_schema.sql:24)
--   - paylater_accounts.available_credit      (V1__Create_schema.sql:25)
--   - loan_repayments.installment_amount      (V2__Add_repayment_and_paylater_transactions.sql:5)
--   - loan_repayments.principal_amount        (V2__Add_repayment_and_paylater_transactions.sql:6)
--   - loan_repayments.interest_amount         (V2__Add_repayment_and_paylater_transactions.sql:7)
--   - loan_repayments.outstanding_principal   (V2__Add_repayment_and_paylater_transactions.sql:8)
--   - loan_repayments.paid_amount             (V2__Add_repayment_and_paylater_transactions.sql:12) -- nullable
--   - paylater_transactions.amount            (V2__Add_repayment_and_paylater_transactions.sql:27)
--   - installment_checkouts.purchase_amount   (V5__create_installment_checkouts_table.sql:11)
--   - installment_checkouts.monthly_payment   (V5__create_installment_checkouts_table.sql:14)
--
-- Note: loan_pre_approvals.requested_amount, max_approved_amount, estimated_monthly_payment also
-- declared precision=19, scale=2 in LoanPreApprovalEntity.java -- included below for consistency.
--
-- USAGE: DECIMAL(19,2) -> DECIMAL(19,4) is a widening cast (no data loss; trailing zeros appended).

BEGIN;

ALTER TABLE loans
    ALTER COLUMN principal_amount TYPE DECIMAL(19,4) USING principal_amount::DECIMAL(19,4),
    ALTER COLUMN monthly_installment TYPE DECIMAL(19,4) USING monthly_installment::DECIMAL(19,4),
    ALTER COLUMN outstanding_balance TYPE DECIMAL(19,4) USING outstanding_balance::DECIMAL(19,4);

ALTER TABLE paylater_accounts
    ALTER COLUMN credit_limit TYPE DECIMAL(19,4) USING credit_limit::DECIMAL(19,4),
    ALTER COLUMN used_credit TYPE DECIMAL(19,4) USING used_credit::DECIMAL(19,4),
    ALTER COLUMN available_credit TYPE DECIMAL(19,4) USING available_credit::DECIMAL(19,4);

ALTER TABLE repayment_schedules
    ALTER COLUMN installment_amount TYPE DECIMAL(19,4) USING installment_amount::DECIMAL(19,4),
    ALTER COLUMN principal_amount TYPE DECIMAL(19,4) USING principal_amount::DECIMAL(19,4),
    ALTER COLUMN interest_amount TYPE DECIMAL(19,4) USING interest_amount::DECIMAL(19,4),
    ALTER COLUMN outstanding_principal TYPE DECIMAL(19,4) USING outstanding_principal::DECIMAL(19,4),
    ALTER COLUMN paid_amount TYPE DECIMAL(19,4) USING paid_amount::DECIMAL(19,4);

ALTER TABLE paylater_transactions
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

ALTER TABLE installment_checkouts
    ALTER COLUMN purchase_amount TYPE DECIMAL(19,4) USING purchase_amount::DECIMAL(19,4),
    ALTER COLUMN monthly_payment TYPE DECIMAL(19,4) USING monthly_payment::DECIMAL(19,4);

-- If loan_pre_approvals table exists (created by some V*), upgrade its money columns too.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'loan_pre_approvals') THEN
        ALTER TABLE loan_pre_approvals
            ALTER COLUMN requested_amount TYPE DECIMAL(19,4) USING requested_amount::DECIMAL(19,4),
            ALTER COLUMN max_approved_amount TYPE DECIMAL(19,4) USING max_approved_amount::DECIMAL(19,4),
            ALTER COLUMN estimated_monthly_payment TYPE DECIMAL(19,4) USING estimated_monthly_payment::DECIMAL(19,4);
    END IF;
END
$$;

COMMIT;
