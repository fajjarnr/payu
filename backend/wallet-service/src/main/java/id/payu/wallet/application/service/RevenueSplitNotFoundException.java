package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_008.
 * Thrown when a revenue split is not found.
 */
public class RevenueSplitNotFoundException extends BusinessException {

    public RevenueSplitNotFoundException(String splitId) {
        super("WAL_008", "Revenue split not found: " + splitId);
    }
}
