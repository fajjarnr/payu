package id.payu.partner.application.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PARTNER-PROD-003: webhook URL trust-boundary validation.
 * <p>All tests resolve through an injected fake resolver so no live DNS is
 * needed; the production resolver uses {@code InetAddress.getAllByName}.
 */
class WebhookUrlValidatorServiceTest {

    private static final byte[] PUBLIC_V4 = {(byte) 8, (byte) 8, (byte) 8, (byte) 8};

    private WebhookUrlValidatorService validator(byte[]... addresses) {
        return new WebhookUrlValidatorService(host -> Arrays.stream(addresses)
                .map(ip -> {
                    try {
                        return InetAddress.getByAddress(host, ip);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(InetAddress[]::new));
    }

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("accepts a public IPv4 endpoint")
        void acceptsPublicIpv4() {
            assertDoesNotThrow(() -> validator(PUBLIC_V4).validate("https://hooks.example.com/payu"));
        }

        @Test
        @DisplayName("accepts a public IPv6 endpoint")
        void acceptsPublicIpv6() {
            byte[] ipv6 = new byte[16];
            ipv6[0] = 0x20;
            ipv6[1] = 0x01;
            ipv6[2] = 0x48;
            ipv6[3] = 0x60;
            assertDoesNotThrow(() -> validator(ipv6).validate("https://hooks.example.com/payu"));
        }

        @Test
        @DisplayName("rejects when any resolved address is non-public")
        void rejectsWhenAnyAddressIsNonPublic() {
            byte[] privateIp = {10, 0, 0, 1};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(PUBLIC_V4, privateIp).validate("https://hooks.example.com/payu"));
        }
    }

    @Nested
    @DisplayName("Scheme and URI shape")
    class SchemeAndUri {

        @Test
        @DisplayName("rejects non-HTTPS scheme")
        void rejectsNonHttps() {
            assertThrows(IllegalArgumentException.class,
                    () -> validator(PUBLIC_V4).validate("http://hooks.example.com/payu"));
        }

        @Test
        @DisplayName("rejects userinfo in URL")
        void rejectsUserinfo() {
            assertThrows(IllegalArgumentException.class,
                    () -> validator(PUBLIC_V4).validate("https://user:pass@hooks.example.com/payu") // trufflehog:ignore -- test userinfo rejection);
        }

        @Test
        @DisplayName("rejects URL without a host")
        void rejectsMissingHost() {
            assertThrows(IllegalArgumentException.class,
                    () -> validator(PUBLIC_V4).validate("https:///path"));
        }
    }

    @Nested
    @DisplayName("Non-public resolved addresses")
    class NonPublicAddresses {

        @Test
        @DisplayName("rejects IPv4 loopback")
        void rejectsLoopback() {
            byte[] ip = {127, 0, 0, 1};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://localhost/webhook"));
        }

        @Test
        @DisplayName("rejects IPv6 loopback")
        void rejectsIpv6Loopback() {
            byte[] ip = new byte[16];
            ip[15] = 1;
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://[::1]/webhook"));
        }

        @Test
        @DisplayName("rejects RFC1918 10.0.0.0/8")
        void rejectsPrivate10() {
            byte[] ip = {10, 1, 2, 3};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://internal.example.com/webhook"));
        }

        @Test
        @DisplayName("rejects RFC1918 172.16.0.0/12")
        void rejectsPrivate172() {
            byte[] ip = {(byte) 172, 20, 0, 1};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://internal.example.com/webhook"));
        }

        @Test
        @DisplayName("rejects RFC1918 192.168.0.0/16")
        void rejectsPrivate192() {
            byte[] ip = {(byte) 192, (byte) 168, 1, 1};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://internal.example.com/webhook"));
        }

        @Test
        @DisplayName("rejects link-local metadata endpoint 169.254.169.254")
        void rejectsMetadataEndpoint() {
            byte[] ip = {(byte) 169, (byte) 254, (byte) 169, (byte) 254};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://169.254.169.254/latest/meta-data"));
        }

        @Test
        @DisplayName("rejects link-local 169.254.0.0/16")
        void rejectsLinkLocal() {
            byte[] ip = {(byte) 169, (byte) 254, 1, 1};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://link.local/x"));
        }

        @Test
        @DisplayName("rejects CGNAT/shared 100.100.100.200 (cloud metadata)")
        void rejectsCgnatMetadata() {
            byte[] ip = {100, 100, 100, (byte) 200};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://metadata.example.com/x"));
        }

        @Test
        @DisplayName("rejects 0.0.0.0")
        void rejectsAnyAddress() {
            byte[] ip = {0, 0, 0, 0};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://x.example.com/"));
        }

        @Test
        @DisplayName("rejects 255.255.255.255")
        void rejectsBroadcast() {
            byte[] ip = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://x.example.com/"));
        }

        @Test
        @DisplayName("rejects IPv6 ULA fc00::/7")
        void rejectsIpv6Ula() {
            byte[] ip = new byte[16];
            ip[0] = (byte) 0xfd;
            ip[1] = 0x00;
            assertThrows(IllegalArgumentException.class,
                    () -> validator(ip).validate("https://[fd00::1]/webhook"));
        }

        @Test
        @DisplayName("rejects when any resolved address is non-public")
        void rejectsWhenAnyAddressIsNonPublic() {
            byte[] privateIp = {10, 0, 0, 1};
            assertThrows(IllegalArgumentException.class,
                    () -> validator(PUBLIC_V4, privateIp).validate("https://hooks.example.com/payu"));
        }
    }

    @Nested
    @DisplayName("DNS failures")
    class DnsFailures {

        @Test
        @DisplayName("rejects a host that does not resolve")
        void rejectsUnresolvableHost() {
            WebhookUrlValidatorService validator = new WebhookUrlValidatorService(host -> {
                throw new UnknownHostException(host);
            });
            assertThrows(IllegalArgumentException.class,
                    () -> validator.validate("https://no-such-host.invalid/webhook"));
        }
    }
}
