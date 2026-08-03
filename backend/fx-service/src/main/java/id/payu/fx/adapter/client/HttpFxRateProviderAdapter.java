package id.payu.fx.adapter.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Configurable production FX provider.
 *
 * <p>Expected response: {@code {"base":"IDR","rates":{"USD":0.00006},
 * "timestamp":"2026-08-03T16:00:00Z","source":"approved-provider"}}.</p>
 */
@Component("realFxRateProvider")
@Profile("!local & !test")
@ConditionalOnProperty(name = "fx.provider.url")
public class HttpFxRateProviderAdapter implements FxRateProviderPort {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String providerUrl;
    private final String apiKey;
    private final String configuredSource;
    private final Duration requestTimeout;
    private final Duration maxAge;

    public HttpFxRateProviderAdapter(
            ObjectMapper objectMapper,
            @Value("${fx.provider.url}") String providerUrl,
            @Value("${fx.provider.api-key:}") String apiKey,
            @Value("${fx.provider.source:}") String configuredSource,
            @Value("${fx.provider.timeout:3s}") Duration requestTimeout,
            @Value("${fx.provider.max-age:15m}") Duration maxAge) {
        this.objectMapper = objectMapper;
        this.providerUrl = providerUrl;
        this.apiKey = apiKey;
        this.configuredSource = configuredSource;
        this.requestTimeout = requestTimeout;
        this.maxAge = maxAge;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .build();
    }

    @Override
    public FxRate fetchCurrentRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase(java.util.Locale.ROOT);
        String to = toCurrency.toUpperCase(java.util.Locale.ROOT);
        ProviderSnapshot snapshot = fetch(from, to);
        BigDecimal rate = snapshot.rates().get(to);
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalStateException("FX provider response missing a positive rate for " + to);
        }

        return FxRate.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .rate(rate)
                .inverseRate(BigDecimal.ONE.divide(rate, 12, RoundingMode.HALF_EVEN))
                .source(snapshot.source())
                .observedAt(toLocalDateTime(snapshot.observedAt()))
                .build();
    }

    @Override
    public Map<String, BigDecimal> fetchAllRates(String baseCurrency) {
        return fetch(baseCurrency.toUpperCase(java.util.Locale.ROOT), null).rates();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private ProviderSnapshot fetch(String baseCurrency, String targetCurrency) {
        URI uri = buildUri(baseCurrency, targetCurrency);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET();
        if (!apiKey.isBlank()) {
            request.header("X-Api-Key", apiKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("FX provider returned HTTP " + response.statusCode());
            }
            return parse(response.body(), baseCurrency);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FX provider request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("FX provider request failed", exception);
        }
    }

    private ProviderSnapshot parse(String body, String requestedBase) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String responseBase = root.path("base").asText("");
            if (!requestedBase.equalsIgnoreCase(responseBase)) {
                throw new IllegalStateException("FX provider response base mismatch");
            }

            String timestamp = root.path("timestamp").asText("");
            Instant observedAt = Instant.parse(timestamp);
            Instant now = Instant.now();
            if (observedAt.isAfter(now.plus(Duration.ofMinutes(2)))
                    || observedAt.isBefore(now.minus(maxAge))) {
                throw new IllegalStateException("FX provider rate is stale");
            }

            String source = root.path("source").asText(configuredSource);
            if (source == null || source.isBlank()) {
                throw new IllegalStateException("FX provider response is missing source");
            }

            Map<String, BigDecimal> rates = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.path("rates").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                BigDecimal rate = field.getValue().decimalValue();
                if (rate.signum() <= 0) {
                    throw new IllegalStateException("FX provider returned a non-positive rate");
                }
                rates.put(field.getKey().toUpperCase(java.util.Locale.ROOT), rate);
            }
            return new ProviderSnapshot(observedAt, source, rates);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalStateException) {
                throw (IllegalStateException) exception;
            }
            throw new IllegalStateException("Invalid FX provider response", exception);
        }
    }

    private URI buildUri(String baseCurrency, String targetCurrency) {
        String separator = providerUrl.contains("?") ? "&" : "?";
        StringBuilder url = new StringBuilder(providerUrl)
                .append(separator)
                .append("base=")
                .append(URLEncoder.encode(baseCurrency, StandardCharsets.UTF_8));
        if (targetCurrency != null) {
            url.append("&symbols=")
                    .append(URLEncoder.encode(targetCurrency, StandardCharsets.UTF_8));
        }
        return URI.create(url.toString());
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record ProviderSnapshot(Instant observedAt, String source, Map<String, BigDecimal> rates) {
    }
}
