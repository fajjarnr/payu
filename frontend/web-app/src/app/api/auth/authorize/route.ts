import { NextResponse } from "next/server";
import { createHash, randomBytes } from "node:crypto";
import { getCorrelationId, withCorrelation } from "@/lib/logger";

/**
 * OIDC Authorization-Code + PKCE start (LOGIN-003).
 *
 * Browser → GET /api/auth/authorize → 302 to Keycloak's authorization
 * endpoint with an S256 code_challenge. The PKCE verifier and a CSRF state
 * token live in short-lived httpOnly cookies; the browser never sees them.
 * The user authenticates at Keycloak's own login page (password grant is
 * removed from the platform), and Keycloak redirects back to
 * /api/auth/callback with a one-time code.
 */
export async function GET(request: Request) {
  const correlationId = getCorrelationId(request);
  const log = withCorrelation(correlationId);

  const oidcIssuer = process.env.OIDC_ISSUER ?? "";
  const keycloakUrl =
    process.env.KEYCLOAK_URL ??
    (oidcIssuer.includes("/realms/") ? oidcIssuer.split("/realms/")[0] : "") ??
    "";
  const realm =
    process.env.KEYCLOAK_REALM ??
    (oidcIssuer.includes("/realms/") ? oidcIssuer.split("/realms/")[1] : "") ??
    "";
  const clientId = process.env.KEYCLOAK_CLIENT_ID ?? "payu-web-app";
  const baseUrl = process.env.NEXT_PUBLIC_BASE_URL ?? "http://localhost:3001";
  const redirectUri = `${baseUrl}/api/auth/callback`;

  if (!keycloakUrl || !realm) {
    log.error({ action: "authorize" }, "OIDC issuer not configured");
    return NextResponse.json(
      { success: false, message: "Authentication service unavailable" },
      { status: 503 },
    );
  }

  const state = randomBytes(16).toString("hex");
  // PKCE verifier: 64 random URL-safe chars (spec: 43–128).
  const codeVerifier = randomBytes(48).toString("base64url");
  const codeChallenge = createHash("sha256").update(codeVerifier).digest("base64url");

  const params = new URLSearchParams({
    client_id: clientId,
    response_type: "code",
    scope: "openid profile email",
    redirect_uri: redirectUri,
    state,
    code_challenge: codeChallenge,
    code_challenge_method: "S256",
  });
  const authorizeUrl = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/auth?${params}`;

  const response = NextResponse.redirect(authorizeUrl);
  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    maxAge: 600, // 10 minutes — the code flow must complete quickly
    path: "/",
  };
  response.cookies.set("oidc_state", state, cookieOptions);
  response.cookies.set("pkce_verifier", codeVerifier, cookieOptions);

  log.info({ action: "authorize" }, "Redirecting to Keycloak authorization endpoint");
  return response;
}
