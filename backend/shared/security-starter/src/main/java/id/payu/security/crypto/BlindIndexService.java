package id.payu.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic HMAC-SHA256 "blind index" over PII values that are encrypted at
 * rest with a random IV (AES-GCM). Equality lookups (findByEmail, unique checks)
 * cannot run against ciphertext, so this index is stored alongside and queried
 * instead. Values must be canonical (no leading/trailing whitespace); callers
 * normalize (e.g. lowercase emails) before indexing.
 * <p>
 * Key rotation: {@link #lookupIndexes(String)} hashes with the current key and
 * every previous key, so records written under an old key remain findable until
 * re-indexed (see {@link #currentVersion()}).
 */
public final class BlindIndexService {

    private final SecretKeySpec key;
    private final List<SecretKeySpec> lookupKeys;
    private final String currentVersion;

    public BlindIndexService(String key, String currentVersion, String previousKeys) {
        if (key == null || key.length() < 32) {
            throw new IllegalStateException("Blind index key must contain at least 32 characters");
        }
        this.key = validatedKey(key);
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new IllegalStateException("Blind index key version is required");
        }
        this.currentVersion = currentVersion.trim();
        List<SecretKeySpec> oldKeys = java.util.Arrays.stream(previousKeys == null ? new String[0]
                        : previousKeys.split(","))
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

    public String currentVersion() {
        return currentVersion;
    }

    /**
     * Deterministic index for the current key.
     */
    public String index(String value) {
        return calculate(value, key);
    }

    /**
     * Index candidates across the current and all previous keys (key rotation).
     */
    public List<String> lookupIndexes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return lookupKeys.stream().map(candidate -> calculate(value, candidate)).distinct().toList();
    }

    private SecretKeySpec parsePreviousKey(String configured) {
        int separator = configured.indexOf('=');
        if (separator <= 0 || separator == configured.length() - 1) {
            throw new IllegalStateException("Previous blind index keys must use version=key format");
        }
        return validatedKey(configured.substring(separator + 1));
    }

    private SecretKeySpec validatedKey(String value) {
        if (value.length() < 32) {
            throw new IllegalStateException("Every blind index key must contain at least 32 characters");
        }
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private String calculate(String value, SecretKeySpec candidate) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException("Blind indexed value must not contain leading or trailing whitespace");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(candidate);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to calculate blind index", exception);
        }
    }
}
