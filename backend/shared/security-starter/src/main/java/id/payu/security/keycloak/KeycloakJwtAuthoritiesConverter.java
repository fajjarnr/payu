package id.payu.security.keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Keycloak JWT claims to Spring Security {@link GrantedAuthority} instances.
 *
 * <p>Extracts authorities from three sources:</p>
 * <ol>
 *   <li>{@code realm_access.roles} → {@code ROLE_*}</li>
 *   <li>{@code resource_access.{client}.roles} → {@code ROLE_*}</li>
 *   <li>{@code scope} → {@code SCOPE_*}</li>
 * </ol>
 *
 * <p>When {@code derivePermissions} is enabled (default), also derives fine-grained
 * permissions from coarse Keycloak roles:</p>
 * <ul>
 *   <li>{@code default-roles-payu} or {@code user} → ROLE_USER + read/write permissions</li>
 *   <li>{@code admin} → ROLE_ADMIN + all user permissions</li>
 *   <li>{@code backoffice} → ROLE_BACKOFFICE</li>
 *   <li>{@code partner} → ROLE_PARTNER</li>
 *   <li>{@code system} → ROLE_SYSTEM</li>
 * </ul>
 *
 * <p>This class is a centralized replacement for the previously duplicated
 * {@code keycloakGrantedAuthoritiesConverter()} and {@code keycloakRealmRolesConverter()}
 * methods that were copy-pasted across 7+ services.</p>
 *
 * @see KeycloakJwtAutoConfiguration
 */
public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_SCOPE = "scope";

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_PREFIX = "SCOPE_";

    /** Standard user permissions granted to consumer-facing roles. */
    private static final List<String> USER_PERMISSIONS = List.of(
            "read:wallet", "write:wallet",
            "read:transaction", "write:transaction",
            "write:payment",
            "read:account", "write:account"
    );

    private final boolean derivePermissions;

    /**
     * Creates a converter with derived permissions enabled.
     */
    public KeycloakJwtAuthoritiesConverter() {
        this(true);
    }

    /**
     * Creates a converter with configurable permission derivation.
     *
     * @param derivePermissions if {@code true}, coarse Keycloak roles (user, admin,
     *                          backoffice, partner, system) are expanded into
     *                          fine-grained Spring Security authorities
     */
    public KeycloakJwtAuthoritiesConverter(boolean derivePermissions) {
        this.derivePermissions = derivePermissions;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        List<String> realmRoles = extractRealmRoles(jwt);

        // 1. Map realm_access.roles → ROLE_*
        for (String role : realmRoles) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
        }

        // 2. Map resource_access.*.roles → ROLE_*
        for (String role : extractResourceRoles(jwt)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
        }

        // 3. Map scope → SCOPE_*
        String scope = jwt.getClaimAsString(CLAIM_SCOPE);
        if (scope != null) {
            for (String s : scope.split(" ")) {
                if (!s.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(SCOPE_PREFIX + s));
                }
            }
        }

        // 4. Derive fine-grained permissions from coarse Keycloak roles
        if (derivePermissions) {
            derivePermissions(realmRoles, authorities);
        }

        return authorities;
    }

    private void derivePermissions(List<String> realmRoles, Set<GrantedAuthority> authorities) {
        boolean isUser = realmRoles.contains("default-roles-payu")
                || realmRoles.stream().anyMatch(r -> r.equalsIgnoreCase("user"));
        boolean isAdmin = realmRoles.contains("admin");
        boolean isBackoffice = realmRoles.contains("backoffice");
        boolean isPartner = realmRoles.contains("partner");
        boolean isSystem = realmRoles.contains("system");

        if (isUser) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            USER_PERMISSIONS.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }

        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            // Admin inherits all user permissions
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            USER_PERMISSIONS.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }

        if (isBackoffice) {
            authorities.add(new SimpleGrantedAuthority("ROLE_BACKOFFICE"));
        }

        if (isPartner) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PARTNER"));
        }

        if (isSystem) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(CLAIM_REALM_ACCESS);
        if (realmAccess != null && realmAccess.containsKey(CLAIM_ROLES)) {
            Object roles = realmAccess.get(CLAIM_ROLES);
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        }
        // Fallback to flat "roles" claim
        List<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);
        return roles != null ? roles : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractResourceRoles(Jwt jwt) {
        List<String> allRoles = new ArrayList<>();
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(CLAIM_RESOURCE_ACCESS);
        if (resourceAccess != null) {
            for (Object value : resourceAccess.values()) {
                if (value instanceof Map) {
                    Map<String, Object> resource = (Map<String, Object>) value;
                    Object roles = resource.get(CLAIM_ROLES);
                    if (roles instanceof List) {
                        allRoles.addAll((List<String>) roles);
                    }
                }
            }
        }
        return allRoles;
    }
}
