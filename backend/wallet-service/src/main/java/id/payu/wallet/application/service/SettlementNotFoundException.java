package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_005.
 * Thrown when a settlement batch is not found.
 */
public class SettlementNotFoundException extends BusinessException {

    public SettlementNotFoundException(String batchId) {
        super("WAL_005", "Settlement batch not found: " + batchId);
    }
}
