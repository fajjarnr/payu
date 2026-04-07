package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_003.
 * Thrown when a reservation is not found by its ID.
 */
public class ReservationNotFoundException extends BusinessException {
    public ReservationNotFoundException(String reservationId) {
        super("WAL_003", "Reservation not found with ID: " + reservationId);
    }
}
