package id.payu.fx.adapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpFxRateProviderAdapterTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void emptyProviderUrlUsesFailClosedAdapter() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.profiles.active=container",
                        "fx.provider.url=",
                        "fx.provider.timeout=3s",
                        "fx.provider.max-age=15m")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(ConversionService.class, ApplicationConversionService::getSharedInstance)
                .withUserConfiguration(HttpFxRateProviderAdapter.class, UnavailableFxRateProviderAdapter.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(UnavailableFxRateProviderAdapter.class)
                        .doesNotHaveBean(HttpFxRateProviderAdapter.class));
    }

    @Test
    void configuredProviderUrlUsesHttpAdapter() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.profiles.active=container",
                        "fx.provider.url=https://provider.example/rates",
                        "fx.provider.timeout=3s",
                        "fx.provider.max-age=15m")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(ConversionService.class, ApplicationConversionService::getSharedInstance)
                .withUserConfiguration(HttpFxRateProviderAdapter.class, UnavailableFxRateProviderAdapter.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(HttpFxRateProviderAdapter.class)
                        .doesNotHaveBean(UnavailableFxRateProviderAdapter.class));
    }

    @Test
    void applicationConfigurationBindsProviderUrlFromEnvironment() throws IOException {
        var source = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .get(0);

        assertThat(source.getProperty("fx.provider.url"))
                .isEqualTo("${FX_PROVIDER_URL:https://api.bi.go.id/fx}");
    }

    @Test
    void shouldParseAndAuditFreshProviderRate() {
        server.createContext("/latest", exchange -> {
            String response = "{\"base\":\"IDR\",\"rates\":{\"USD\":0.000061},"
                    + "\"timestamp\":\"" + Instant.now() + "\",\"source\":\"approved-provider\"}";
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();

        HttpFxRateProviderAdapter adapter = adapter();
        var result = adapter.fetchCurrentRate("IDR", "USD");

        // ponytail: 0.000061 HALF_EVEN scale 4 → 0.0001 (4 decimal financial precision)
        assertThat(result.getRate()).isEqualByComparingTo(new BigDecimal("0.0001"));
        assertThat(result.getSource()).isEqualTo("approved-provider");
        assertThat(result.getObservedAt()).isNotNull();
    }

    @Test
    void shouldRejectStaleProviderRate() {
        server.createContext("/latest", exchange -> {
            String response = "{\"base\":\"IDR\",\"rates\":{\"USD\":0.000061},"
                    + "\"timestamp\":\"" + Instant.now().minus(Duration.ofHours(1)) + "\"}";
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> adapter().fetchCurrentRate("IDR", "USD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FX provider rate is stale");
    }

    private HttpFxRateProviderAdapter adapter() {
        return new HttpFxRateProviderAdapter(
                new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/latest",
                "",
                "configured-source",
                Duration.ofSeconds(2),
                Duration.ofMinutes(15));
    }
}
