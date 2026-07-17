package id.payu.promotion.domain.port.out; import java.util.Map;
public interface PromotionEventPublisher {void publish(String aggregate,String id,String type,Map<String,Object> payload,String topic);}
