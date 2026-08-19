# PayU Domain Glossary

## Web Identity and BFF

- **BFF (Backend for Frontend)**: A server-side boundary that adapts authenticated backend capabilities for one frontend channel without exposing backend credentials to browser code.
- **Browser Session**: The authenticated relationship between a browser and PayU web application. Its client-visible identifier is not a user identity, role, or access token.
- **Access Token**: A short-lived credential used by a trusted server component to call an authorized backend service.
- **Refresh Token**: A longer-lived credential used only by the trusted authentication boundary to obtain a new access token or rotate a session.
- **Token Relay**: Server-side forwarding of a validated access token to an upstream service; browser code does not perform the relay.
- **CSRF Token**: A value submitted explicitly by browser code on state-changing requests to prove same-origin intent when authentication uses cookies.
- **OIDC Authorization Callback**: The one-time browser return from the identity provider after authorization-code and PKCE processing.
- **Route Handler**: A public HTTP endpoint in the web application that must perform its own authentication, authorization, input validation, and response policy.
- **Proxy**: An early request decision point used for routing or optimistic navigation checks. It is not the final authorization boundary.
- **Server Component**: A server-rendered UI component that can access server-only session context and should fetch protected data without an unnecessary browser round trip.
