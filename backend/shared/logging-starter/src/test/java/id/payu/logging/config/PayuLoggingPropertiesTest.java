package id.payu.logging.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayuLoggingPropertiesTest {

    @Test
    @DisplayName("should have correct default values")
    void shouldHaveCorrectDefaults() {
        PayuLoggingProperties props = new PayuLoggingProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getServiceVersion()).isEqualTo("1.0.0");
        assertThat(props.getEnvironment()).isEqualTo("dev");
        assertThat(props.getServiceName()).isNull();
    }

    @Test
    @DisplayName("correlation properties should have correct defaults")
    void correlationDefaults() {
        var correlation = new PayuLoggingProperties().getCorrelation();

        assertThat(correlation.isEnabled()).isTrue();
        assertThat(correlation.getHeaderName()).isEqualTo("X-Correlation-Id");
        assertThat(correlation.getMdcKey()).isEqualTo("correlation_id");
    }

    @Test
    @DisplayName("tracing properties should have correct defaults")
    void tracingDefaults() {
        var tracing = new PayuLoggingProperties().getTracing();

        assertThat(tracing.isEnabled()).isTrue();
        assertThat(tracing.getTraceIdMdcKey()).isEqualTo("trace_id");
        assertThat(tracing.getSpanIdMdcKey()).isEqualTo("span_id");
    }

    @Test
    @DisplayName("request logging should be disabled by default")
    void requestLoggingDefaults() {
        var reqLog = new PayuLoggingProperties().getRequestLogging();

        assertThat(reqLog.isEnabled()).isFalse();
        assertThat(reqLog.isIncludePayload()).isFalse();
        assertThat(reqLog.getMaxPayloadLength()).isEqualTo(1000);
    }

    @Test
    @DisplayName("should allow setting all properties")
    void shouldAllowSettingAll() {
        PayuLoggingProperties props = new PayuLoggingProperties();
        props.setEnabled(false);
        props.setServiceName("test-svc");
        props.setServiceVersion("2.0.0");
        props.setEnvironment("prod");

        props.getCorrelation().setEnabled(false);
        props.getCorrelation().setHeaderName("X-Custom");
        props.getCorrelation().setMdcKey("custom_id");

        props.getTracing().setEnabled(false);
        props.getTracing().setTraceIdMdcKey("t_id");
        props.getTracing().setSpanIdMdcKey("s_id");

        props.getRequestLogging().setEnabled(true);
        props.getRequestLogging().setIncludePayload(true);
        props.getRequestLogging().setMaxPayloadLength(5000);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getServiceName()).isEqualTo("test-svc");
        assertThat(props.getServiceVersion()).isEqualTo("2.0.0");
        assertThat(props.getEnvironment()).isEqualTo("prod");
        assertThat(props.getCorrelation().isEnabled()).isFalse();
        assertThat(props.getCorrelation().getHeaderName()).isEqualTo("X-Custom");
        assertThat(props.getCorrelation().getMdcKey()).isEqualTo("custom_id");
        assertThat(props.getTracing().isEnabled()).isFalse();
        assertThat(props.getTracing().getTraceIdMdcKey()).isEqualTo("t_id");
        assertThat(props.getTracing().getSpanIdMdcKey()).isEqualTo("s_id");
        assertThat(props.getRequestLogging().isEnabled()).isTrue();
        assertThat(props.getRequestLogging().isIncludePayload()).isTrue();
        assertThat(props.getRequestLogging().getMaxPayloadLength()).isEqualTo(5000);
    }
}
