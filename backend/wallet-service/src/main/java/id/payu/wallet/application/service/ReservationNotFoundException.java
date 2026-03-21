package id.payu.wallet.application.service;

// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String reservationId) {
        super("Reservation not found with ID: " + reservationId);
    }
}
