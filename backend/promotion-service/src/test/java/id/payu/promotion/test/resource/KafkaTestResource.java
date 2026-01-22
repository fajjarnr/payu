package id.payu.promotion.test.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;

import java.util.Collections;
import java.util.Map;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    static KafkaContainer kafka = new KafkaContainer();

    @Override
    public Map<String, String> start() {
        kafka.start();
        return Map.of(
                "kafka.bootstrap.servers", kafka.getBootstrapServers()
        );
    }

    @Override
    public void stop() {
        kafka.stop();
    }
}
