package id.payu.transaction.application.cqrs.query;

import id.payu.api.common.exception.BusinessException;
import id.payu.transaction.application.cqrs.QueryHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Handler for the GetTransactionQuery.
 * Implements the read side of CQRS for retrieving a single transaction.
 */
@Component
public class GetTransactionQueryHandler implements QueryHandler<GetTransactionQuery, TransactionEntity> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetTransactionQueryHandler.class);



    private final TransactionPersistencePort transactionPersistencePort;
    private final AuthorizationService authorizationService;

    public GetTransactionQueryHandler(TransactionPersistencePort transactionPersistencePort,
                                      AuthorizationService authorizationService) {
        this.transactionPersistencePort = transactionPersistencePort;
        this.authorizationService = authorizationService;
    }

    @Override
    public TransactionEntity handle(GetTransactionQuery query) {
        log.info("Handling GetTransactionQuery for transaction: {}", query.transactionId());

        // Verify user has access to this transaction
        authorizationService.verifyTransactionAccess(query.transactionId(), query.userId());

        return transactionPersistencePort.findById(query.transactionId())
                .orElseThrow(() -> new BusinessException("TXN_404", "TransactionEntity not found: " + query.transactionId()));
    }
}
