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
}
