-- MVP-004: enforce SNAP-BI idempotency via the natural key partner_reference_no (per partner).
-- SNAP-BI integrators send partnerReferenceNo as their request id. A replayed createPayment
-- must return the existing payment instead of minting a duplicate (guarded in
-- SnapBiPaymentService.createPayment). Unique index backs that guard against races.

BEGIN;

-- Replace the legacy non-unique index with a unique one (dedup residual rows first).
DROP INDEX IF EXISTS idx_snap_payment_partner_ref;

DELETE FROM snap_bi_payments a
 USING snap_bi_payments b
 WHERE a.id > b.id
   AND a.partner_id = b.partner_id
   AND a.partner_reference_no IS NOT DISTINCT FROM b.partner_reference_no;

CREATE UNIQUE INDEX uq_snap_payment_partner_ref
    ON snap_bi_payments (partner_id, partner_reference_no);

-- Refund idempotency: one refund per partner_refund_no (per partner/payment).
DELETE FROM snap_bi_refunds a
 USING snap_bi_refunds b
 WHERE a.id > b.id
   AND a.partner_id = b.partner_id
   AND a.payu_reference_no = b.payu_reference_no
   AND a.partner_refund_no IS NOT DISTINCT FROM b.partner_refund_no;

CREATE UNIQUE INDEX uq_snap_refund_partner_ref
    ON snap_bi_refunds (partner_id, payu_reference_no, partner_refund_no);

COMMIT;
