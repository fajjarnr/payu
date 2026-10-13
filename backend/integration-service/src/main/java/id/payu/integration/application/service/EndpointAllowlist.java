package id.payu.integration.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allowlist for server-side request destinations (SEC-004).
 *
 * <p>{@code sendSoapRequest}/{@code sendHttpRequest} accept operator-supplied
 * URLs that reach Camel dynamic {@code toD} producers. Only {@code http/https}
 * URLs whose host is in the configured allowlist are accepted; everything else
 * (including {@code file:}, {@code direct:}, and unlisted hosts) is rejected.
 * Fail-closed: an empty allowlist rejects every dynamic destination.
 */
@Slf4j
@Component
public class EndpointAllowlist {

    private final Set<String> allowedHosts;

    public EndpointAllowlist(
            @Value("${payu.integration.egress.allowed-hosts:}") String allowedHostsCsv) {
        this.allowedHosts = Arrays.stream(allowedHostsCsv.split(","))
                .map(h -> h.trim().toLowerCase(Locale.ROOT))
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void requireAllowed(String endpoint) {
        String host = hostOf(endpoint);
        if (host == null || !allowedHosts.contains(host)) {
            log.warn("Rejecting outbound integration request to non-allowlisted destination");
            throw new IllegalArgumentException("Endpoint host is not allowlisted");
        }
    }

    private static String hostOf(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        try {
            URI uri = URI.create(endpoint.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }
            String host = uri.getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
