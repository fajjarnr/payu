package id.payu.promotion.domain.port.out;
import java.util.Map;
public interface CashbackEventPublisher {
 void publish(String aggregateType,String aggregateId,String eventType,Map<String,Object> payload,String topic);
}
