package id.payu.transaction.application.service;

import id.payu.transaction.domain.port.out.AggregateResultPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AggregateResultService {
    private final AggregateResultPort aggregatePort;

    public AggregateResultService(AggregateResultPort aggregatePort) {
        this.aggregatePort = aggregatePort;
    }

    public void saveResult(String referenceNo, String resultJson, int fanoutOrder) {
        aggregatePort.saveResult(referenceNo, resultJson, fanoutOrder);
    }

    public List<String> findByReferenceNo(String referenceNo) {
        return aggregatePort.findByReferenceNo(referenceNo);
    }
}
