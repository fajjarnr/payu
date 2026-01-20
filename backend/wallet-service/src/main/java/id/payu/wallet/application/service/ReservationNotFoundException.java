package id.payu.wallet.application.service;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String reservationId) {
        super("Reservation not found with ID: " + reservationId);
    }
}
