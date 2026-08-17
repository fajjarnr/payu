package id.payu.statement.domain.port.out;

import id.payu.statement.interfaces.dto.TransactionRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * Output port for querying transactions from Transaction Service.
 */
public interface TransactionServicePort {

    List<TransactionRecord> getTransactions(String accountId, LocalDate startDate, LocalDate endDate);

    TransactionRecord getTransaction(String transactionId);
}
