package id.payu.account.domain.port.out;

/**
 * Port for provisioning user identity credentials in the IAM (Keycloak).
 * Called during user registration to create Keycloak login credentials.
 */
public interface IdentityProviderPort {

    /**
     * Provision a new user in the identity provider with login credentials.
     *
     * @param username the username
     * @param email    the user email
     * @param password the password (will be set in Keycloak)
     * @param fullName the user's full name
     */
    String provisionUser(String username, String email, String password, String fullName);

    /**
     * ACCOUNT-005: remove a provisioned identity (saga compensation).
     * Called when local persistence fails after IAM provisioning, so no
     * orphan Keycloak user is left behind. Best-effort: a failure here is
     * logged loudly and requires manual cleanup.
     *
     * @param iamUserId the identity provider user id returned by {@link #provisionUser}
     */
    void deleteUser(String iamUserId);
}
