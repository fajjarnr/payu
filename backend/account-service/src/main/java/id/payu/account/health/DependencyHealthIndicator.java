package id.payu.account.health;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Dependency health indicator that reports health of all dependencies.
 *
 * <p>This provides a consolidated view of all dependencies including:</p>
 * <ul>
 *   <li>Database connectivity and latency</li>
 *   <li>Redis connectivity and latency</li>
 *   <li>Kafka connectivity and listener status</li>
 *   <li>External service health</li>
 * </ul>
 */
@Component("dependencies")
@RequiredArgsConstructor
public class DependencyHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DependencyHealthIndicator.class);

    private final ApplicationAvailability availability;
    private final DeepHealthIndicator deepHealthIndicator;

    public Health health() {
        Map<String, Object> details = new HashMap<>();

        LivenessState livenessState = availability.getLivenessState();
        ReadinessState readinessState = availability.getReadinessState();

        details.put("liveness", livenessState.toString());
        details.put("readiness", readinessState.toString());

        Health deepHealth = deepHealthIndicator.health();
        details.put("deepHealth", deepHealth.getStatus().toString());

        Map<String, String> dependencySummary = new HashMap<>();

        if (deepHealth.getStatus() == Status.UP) {
            dependencySummary.put("overall", "HEALTHY");
        } else {
            dependencySummary.put("overall", "UNHEALTHY");
        }

        // DeepHealthIndicator stores each dependency's Health as a detail value,
        // so read status directly from the details map, not a fresh Health check.
        var deepDetails = deepHealth.getDetails();
        for (String dep : java.util.List.of("database", "redis", "kafka")) {
            if (deepDetails.get(dep) instanceof Health depHealth) {
                dependencySummary.put(dep, depHealth.getStatus().toString());
            }
        }

        details.put("dependencies", dependencySummary);

        return Health.status(deepHealth.getStatus())
            .withDetails(details)
            .build();
    }
}
