package id.payu.wallet.domain.model;

public enum EscrowStatus {
        CREATED,    // initial state, funds not yet held
        HELD,       // buyer funds reserved in escrow
        RELEASED,   // released to merchant (pending settlement)
        SETTLED,    // merchant has received funds
        REFUNDED,   // funds returned to buyer
        EXPIRED     // transitional — auto-refund triggered
    }
