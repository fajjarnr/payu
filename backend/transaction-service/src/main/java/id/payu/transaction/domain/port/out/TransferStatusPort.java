package id.payu.transaction.domain.port.out;

public interface TransferStatusPort {
    /**
     * Query BI-FAST rail for latest status of a transfer.
     * @param referenceNo referenceNumber propagated to rail metadata
     * @return latest status: 00=SUCCESS, 01=PENDING, 03=FAILED, 06=REJECTED etc (BRIAPI SNAP)
     */
    String getLatestTransactionStatus(String referenceNo);
}
