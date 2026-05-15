package id.payu.transaction.application.cqrs.query;

import id.payu.transaction.application.cqrs.QueryHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Handler for the GetAccountTransactionsQuery.
 * Implements the read side of CQRS for retrieving account transactions.
 */
@Component
public class GetAccountTransactionsQueryHandler implements QueryHandler<GetAccountTransactionsQuery, List<TransactionEntity>> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetAccountTransactionsQueryHandler.class);



    private final TransactionPersistencePort transactionPersistencePort;
    private final AuthorizationService authorizationService;

    public GetAccountTransactionsQueryHandler(TransactionPersistencePort transactionPersistencePort,
                                              AuthorizationService authorizationService) {
        this.transactionPersistencePort = transactionPersistencePort;
        this.authorizationService = authorizationService;
    }

    @Override
    public List<TransactionEntity> handle(GetAccountTransactionsQuery query) {
        log.info("Handling GetAccountTransactionsQuery for account: {}", query.accountId());

        // Verify user owns the account
        authorizationService.verifyAccountOwnership(UUID.fromString(query.accountId()), query.userId());

        return transactionPersistencePort.findByAccountId(
                UUID.fromString(query.accountId()),
                query.page(),
                query.size()
        );
    }
}
