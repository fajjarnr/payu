package id.payu.transaction.domain.port.out;

import java.util.List;

public interface AggregateResultPort {
    void saveResult(String referenceNo, String resultJson, int fanoutOrder);
    List<String> findByReferenceNo(String referenceNo);
}
