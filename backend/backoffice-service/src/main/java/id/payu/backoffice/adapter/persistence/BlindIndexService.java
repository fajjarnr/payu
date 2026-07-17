package id.payu.backoffice.adapter.persistence;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlindIndexService {
    private final SecretKeySpec key;
    private final List<SecretKeySpec> lookupKeys;
    private final String currentVersion;

    @org.springframework.beans.factory.annotation.Autowired
    public BlindIndexService(
            @Value("${payu.security.blind-index-key}") String key,
            @Value("${payu.security.blind-index-key-version}") String currentVersion,
            @Value("${payu.security.blind-index-previous-keys:}") String previousKeys) {
        if (key == null || key.length() < 32) {
            throw new IllegalStateException("payu.security.blind-index-key must contain at least 32 characters");
        }
        this.key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new IllegalStateException("payu.security.blind-index-key-version is required");
        }
        this.currentVersion = currentVersion.trim();
        List<SecretKeySpec> oldKeys = java.util.Arrays.stream(previousKeys.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::parsePreviousKey)
                .toList();
        this.lookupKeys = java.util.stream.Stream.concat(java.util.stream.Stream.of(this.key), oldKeys.stream())
                .toList();
    }

    public BlindIndexService(String key, String previousKeys) {
        this(key, "v1", previousKeys.isBlank() ? "" : "legacy=" + previousKeys);
    }

    public String currentVersion() { return currentVersion; }

    public String index(String value) {
        return calculate(value, key);
    }

    public List<String> lookupIndexes(String value) {
        if (value == null || value.isBlank()) return List.of();
        return lookupKeys.stream().map(candidate -> calculate(value, candidate)).distinct().toList();
    }

    private SecretKeySpec parsePreviousKey(String configured) {
        int separator = configured.indexOf('=');
        if (separator <= 0 || separator == configured.length() - 1) {
            throw new IllegalStateException("Previous blind-index keys must use version=key format");
        }
        return validatedKey(configured.substring(separator + 1));
    }

    private SecretKeySpec validatedKey(String value) {
        if (value.length() < 32) {
            throw new IllegalStateException("Every blind-index key must contain at least 32 characters");
        }
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private String calculate(String value, SecretKeySpec candidate) {
        if (value == null || value.isBlank()) return null;
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException("User ID must not contain leading or trailing whitespace");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(candidate);
            return HexFormat.of().formatHex(mac.doFinal(value
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to calculate blind index", exception);
        }
    }
}
