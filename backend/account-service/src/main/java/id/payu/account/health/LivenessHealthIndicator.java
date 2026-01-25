package id.payu.account.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Liveness probe - checks if the application is running.
 *
 * <p>This is a lightweight check that should always return UP if the JVM is alive.
 * Use this for Kubernetes liveness probes.</p>
 */
@Component("customLiveness")
public class LivenessHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(LivenessHealthIndicator.class);

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Health health() {
        try {
            // Basic JVM checks
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            int threadCount = threadMXBean.getThreadCount();

            return Health.up()
                .withDetail("heapUsed", heapUsed)
                .withDetail("heapMax", heapMax)
                .withDetail("heapUtilization", (heapUsed * 100.0 / heapMax) + "%")
                .withDetail("threadCount", threadCount)
                .withDetail("uptime", ManagementFactory.getRuntimeMXBean().getUptime() + "ms")
                .build();
        } catch (Exception e) {
            log.error("Liveness check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
