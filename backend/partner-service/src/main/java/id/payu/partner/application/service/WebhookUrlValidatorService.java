package id.payu.partner.application.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * PARTNER-PROD-003: webhook endpoint trust boundary.
 * <p>Only HTTPS endpoints whose <em>resolved</em> addresses are all public
 * Internet addresses are accepted. Validation runs at subscription create/update
 * and again before every delivery attempt, so a DNS rebinding between
 * registration and dispatch (or a DB-written URL) is still blocked at the last
 * check before the socket is opened. Redirects are already disabled on the
 * delivery client ({@code HttpClient.Redirect.NEVER}).
 */
@Component
public class WebhookUrlValidatorService {

    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final HostResolver resolver;

    public WebhookUrlValidatorService() {
        this(InetAddress::getAllByName);
    }

    // Visible for testing
    WebhookUrlValidatorService(HostResolver resolver) {
        this.resolver = resolver;
    }

    public void validate(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Webhook URL is not a valid URI");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Webhook URL must use HTTPS");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Webhook URL must not contain userinfo");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must include a host");
        }

        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Webhook URL host does not resolve: " + host);
        }

        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("Webhook URL host does not resolve: " + host);
        }

        for (InetAddress address : addresses) {
            if (!isPublic(address)) {
                throw new IllegalArgumentException(
                        "Webhook URL resolves to a non-public address: " + address.getHostAddress());
            }
        }
    }

    private static boolean isPublic(InetAddress address) {
        if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()
                || address.isAnyLocalAddress()) {
            return false;
        }
        byte[] raw = address.getAddress();
        int first = raw[0] & 0xFF;
        if (raw.length == 4) {
            // 100.64.0.0/10 CGNAT (shared address space, used by cloud metadata endpoints)
            if (first == 100) {
                return false;
            }
            // 0.0.0.0 / 255.255.255.255
            if (first == 0 || first == 255) {
                return false;
            }
        } else {
            // fc00::/7 IPv6 unique local (isSiteLocalAddress only covers fec0::/10)
            if ((first & 0xFE) == 0xFC) {
                return false;
            }
        }
        return true;
    }
}
