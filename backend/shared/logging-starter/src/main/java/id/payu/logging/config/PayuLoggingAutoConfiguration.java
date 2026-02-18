package id.payu.logging.config;

import id.payu.logging.filter.CorrelationIdFilter;
import id.payu.logging.filter.TraceIdFilter;
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
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
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
}
