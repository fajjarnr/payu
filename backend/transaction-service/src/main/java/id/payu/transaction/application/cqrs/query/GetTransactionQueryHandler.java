package id.payu.transaction.application.cqrs.query;

import id.payu.transaction.application.cqrs.QueryHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Handler for the GetTransactionQuery.
 * Implements the read side of CQRS for retrieving a single transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetTransactionQueryHandler implements QueryHandler<GetTransactionQuery, Transaction> {

    private final TransactionPersistencePort transactionPersistencePort;
    private final AuthorizationService authorizationService;

    @Override
    public Transaction handle(GetTransactionQuery query) {
        log.info("Handling GetTransactionQuery for transaction: {}", query.transactionId());

        // Verify user has access to this transaction
        authorizationService.verifyTransactionAccess(query.transactionId(), query.userId());

        return transactionPersistencePort.findById(query.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + query.transactionId()));
    }
}
