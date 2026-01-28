package id.payu.transaction.application.cqrs.query;

import id.payu.transaction.application.cqrs.QueryHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Handler for the GetAccountTransactionsQuery.
 * Implements the read side of CQRS for retrieving account transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetAccountTransactionsQueryHandler implements QueryHandler<GetAccountTransactionsQuery, List<Transaction>> {

    private final TransactionPersistencePort transactionPersistencePort;
    private final AuthorizationService authorizationService;

    @Override
    public List<Transaction> handle(GetAccountTransactionsQuery query) {
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
