package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.Command;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import id.payu.transaction.domain.model.Money;

import java.util.UUID;

/**
 * Command to process a QRIS payment.
 * This is a write operation that modifies system state.
 */
public record ProcessQrisPaymentCommand(
        String qrisCode,
        Money amount,
        String userId
) implements Command<Void> {

    /**
     * Factory method to create command from DTO.
     */
    public static ProcessQrisPaymentCommand from(ProcessQrisPaymentRequest request, String userId) {
        Money money = request.getCurrency() != null
                ? Money.of(request.getAmount(), request.getCurrency())
                : Money.idr(request.getAmount());

        return new ProcessQrisPaymentCommand(
                request.getQrisCode(),
                money,
                userId
        );
    }
}
