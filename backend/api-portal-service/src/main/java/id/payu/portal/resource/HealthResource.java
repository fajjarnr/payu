package id.payu.portal.resource;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

@ApplicationScoped
public class HealthResource {

    @Liveness
    public HealthCheckResponse liveness() {
        return HealthCheckResponse.up("api-portal-service");
    }

    @Readiness
    public HealthCheckResponse readiness() {
        return HealthCheckResponse.up("api-portal-service");
    }
}
