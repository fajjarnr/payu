package id.payu.logging.config;

import id.payu.logging.filter.CorrelationIdFilter;
import id.payu.logging.filter.CorrelationIdWebFilter;
import id.payu.logging.filter.RequestLoggingFilter;
import id.payu.logging.filter.TraceIdFilter;
import id.payu.logging.filter.TraceIdWebFilter;
import id.payu.logging.util.MdcUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.WebFilter;

/**
 * Auto-configuration for PayU standardized logging.
 * Provides:
 * <ul>
 *   <li>JSON logging via Logstash encoder (configured via logback-spring.xml)</li>
 *   <li>MDC propagation for correlation_id and trace_id</li>
 *   <li>OpenTelemetry integration for distributed tracing</li>
 *   <li>Request/response logging filters</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PayuLoggingProperties.class)
@ConditionalOnProperty(
    prefix = "payu.logging",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PayuLoggingAutoConfiguration {

    /**
     * Correlation ID filter for servlet-based applications.
     * Reads X-Correlation-Id header and sets in MDC.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "correlationIdFilter")
    @ConditionalOnProperty(
        prefix = "payu.logging.correlation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(
            PayuLoggingProperties properties) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("correlationIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Trace ID filter for OpenTelemetry integration.
     * Extracts trace_id and span_id from OTel context and sets in MDC.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "traceIdFilter")
    @ConditionalOnProperty(
        prefix = "payu.logging.tracing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter(
            PayuLoggingProperties properties) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * MDC utility bean for programmatic MDC manipulation.
     */
    @Bean
    @ConditionalOnMissingBean
    public MdcUtil mdcUtil() {
        return new MdcUtil();
    }

    /**
     * Request/response logging filter for servlet-based applications.
     * Logs HTTP method, URI, status, duration, and optionally payload.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "requestLoggingFilter")
    @ConditionalOnProperty(
        prefix = "payu.logging.request-logging",
        name = "enabled",
        havingValue = "true"
    )
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(
            PayuLoggingProperties properties) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLoggingFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("requestLoggingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    // ---- Reactive WebFlux Filters ----

    /**
     * Correlation ID filter for reactive (WebFlux) applications.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(name = "correlationIdWebFilter")
    @ConditionalOnProperty(
        prefix = "payu.logging.correlation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public WebFilter correlationIdWebFilter(PayuLoggingProperties properties) {
        return new CorrelationIdWebFilter(properties);
    }

    /**
     * Trace ID filter for reactive (WebFlux) applications with OpenTelemetry.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(name = "traceIdWebFilter")
    @ConditionalOnProperty(
        prefix = "payu.logging.tracing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
    public WebFilter traceIdWebFilter(PayuLoggingProperties properties) {
        return new TraceIdWebFilter(properties);
    }
}
