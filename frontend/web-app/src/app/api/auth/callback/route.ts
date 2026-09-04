import { NextResponse } from "next/server";
import { getCorrelationId, withCorrelation } from "@/lib/logger";

const DEFAULT_GATEWAY_URL = "http://gateway-service:8080";

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = Buffer.from(parts[1], "base64url").toString("utf-8");
    return JSON.parse(payload);
  } catch (err) {
    console.error('[callback] JWT decode failed:', err);
    return null;
  }
}

function parseCookies(header: string | null): Record<string, string> {
  const result: Record<string, string> = {};
  if (!header) return result;
  for (const part of header.split(";")) {
    const eq = part.indexOf("=");
    if (eq < 0) continue;
    result[part.slice(0, eq).trim()] = decodeURIComponent(part.slice(eq + 1).trim());
  }
  return result;
}

/**
 * OIDC callback (LOGIN-003) — Keycloak redirects here with ?code&state.
 * Verifies the CSRF state cookie, exchanges code + PKCE verifier at the
 * gateway (→ auth-service → Keycloak token endpoint), then stores the tokens
 * in httpOnly cookies and sends the browser to the dashboard.
 *
 * Security:
 *   - state mismatch or missing verifier → 302 /login?error=... (no exchange)
 *   - tokens never reach the browser's JS (httpOnly, secure in prod)
 *   - the verifier cookie is consumed (deleted) on every callback
 */
export async function GET(request: Request) {
  const correlationId = getCorrelationId(request);
  const log = withCorrelation(correlationId);

  const url = new URL(request.url);
  const host = request.headers.get("host") ?? url.host;
  const proto = request.headers.get("x-forwarded-proto") ?? url.protocol.replace(":", "");
  // Browser-facing origin: derive from Host header so 18.143.199.84:3001 and localhost:3001 both work without hardcode
  const baseUrl = `${proto}://${host}`;
  const origin = new URL(baseUrl).origin;
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");

  const incomingCookies = parseCookies(request.headers.get("cookie"));
  const expectedState = incomingCookies["oidc_state"];
  const codeVerifier = incomingCookies["pkce_verifier"];

  const redirectToLogin = (error: string) => {
    const response = NextResponse.redirect(
      new URL(`/login?error=${encodeURIComponent(error)}`, origin),
    );
    response.cookies.delete("oidc_state");
    response.cookies.delete("pkce_verifier");
    return response;
  };

  if (!code || !state || !expectedState || !codeVerifier || state !== expectedState) {
    log.warn({ action: "callback" }, "OIDC callback rejected: missing or mismatched state/code");
    return redirectToLogin("invalid_state");
  }

  const redirectUri = `${origin}/api/auth/callback`;
  const gatewayUrl = process.env.GATEWAY_URL || DEFAULT_GATEWAY_URL;

  try {
    const res = await fetch(`${gatewayUrl}/api/v1/auth/callback`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code, codeVerifier, redirectUri }),
      signal: AbortSignal.timeout(10_000),
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
      log.warn({ action: "callback", status: res.status }, "Code exchange failed");
      return redirectToLogin("authentication_failed");
    }

    const accessToken =
      data.access_token ?? data.data?.access_token ?? data.data?.accessToken;
    const refreshToken =
      data.refresh_token ?? data.data?.refresh_token ?? data.data?.refreshToken;

    // User profile from the JWT claims (same logic as the removed login route).
    let user = data.user ?? data.data?.user;
    if (!user && accessToken) {
      const claims = decodeJwtPayload(accessToken);
      if (claims) {
        const accountId = (claims.account_id as string) || `account-${claims.sub}`;
        user = {
          id: claims.sub as string,
          accountId,
          username: claims.preferred_username as string,
          fullName: (claims.name as string) || "",
          email: (claims.email as string) || "",
          roles: ((claims.realm_access as Record<string, unknown>)?.roles as string[]) || [],
        };
      }
    }

    const isSecure = origin.startsWith("https://");
    const ACCESS_TOKEN_MAX_AGE = data.expires_in ?? data.data?.expires_in ?? 900;

    const response = NextResponse.redirect(new URL("/dashboard", origin));
    response.headers.set("X-Correlation-Id", correlationId);

    if (accessToken) {
      response.cookies.set("accessToken", accessToken, {
        httpOnly: true,
        secure: isSecure,
        sameSite: "lax", // FE-AUDIT-005: session must survive top-level navigation (SSO callback is cross-site-initiated); Lax still blocks cross-site POST CSRF
        maxAge: ACCESS_TOKEN_MAX_AGE,
        path: "/",
      });
    }
    if (refreshToken) {
      response.cookies.set("refreshToken", refreshToken, {
        httpOnly: true,
        secure: isSecure,
        sameSite: "lax", // FE-AUDIT-005: session must survive top-level navigation (SSO callback is cross-site-initiated); Lax still blocks cross-site POST CSRF
        maxAge: 604_800, // 7 days
        path: "/",
      });
    }
    response.cookies.delete("oidc_state");
    response.cookies.delete("pkce_verifier");

    log.info({ action: "callback", userId: user?.id }, "OIDC login completed");
    return response;
  } catch (error) {
    log.error(
      { action: "callback", err: error instanceof Error ? error : { message: String(error) } },
      "Code exchange proxy error",
    );
    return redirectToLogin("authentication_failed");
  }
}
