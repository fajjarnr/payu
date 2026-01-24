package id.payu.gateway.filter;

import id.payu.gateway.service.ApiAnalyticsService;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.time.Duration;

/**
 * Filter to track API analytics.
 * Records request counts, response times, and error rates.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.USER)
public class ApiAnalyticsFilter implements ContainerResponseFilter {

    private static final String START_TIME_PROPERTY = "analytics-start-time";

    @Inject
    ApiAnalyticsService analyticsService;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Record start time for later use
        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);

        if (startTime == null) {
            startTime = System.currentTimeMillis();
            requestContext.setProperty(START_TIME_PROPERTY, startTime);
        }

        long duration = System.currentTimeMillis() - startTime;

        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        int statusCode = responseContext.getStatus();

        // Record metrics asynchronously
        try {
            analyticsService.recordRequest(path, method, statusCode, duration);
            Log.debugf("Recorded analytics: %s %s -> %d (%dms)", method, path, statusCode, duration);
        } catch (Exception e) {
            // Don't fail requests if analytics fails
            Log.warnf(e, "Failed to record analytics for %s %s", method, path);
        }
    }
}
