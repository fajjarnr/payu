import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const GATEWAY_URL = process.env.GATEWAY_URL || "http://gateway-service:8080";

/**
 * Decode JWT payload without verifying signature (BFF already trusts the token from the gateway).
 * Extracts user claims from the Keycloak access token.
 */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = Buffer.from(parts[1], "base64url").toString("utf-8");
    return JSON.parse(payload);
  } catch {
    return null;
  }
}

/**
 * BFF Login Route — Authenticates user and stores tokens in httpOnly cookies.
 *
 * Flow:
 *   Browser → POST /api/auth/login → this route → gateway /api/v1/auth/login
 *   Returns user data only; tokens are NEVER exposed to JavaScript.
 *
 * Security:
 *   - httpOnly: JS cannot read tokens (prevents XSS token theft)
 *   - secure: HTTPS-only in production
 *   - sameSite=strict: prevents CSRF
 *   - PCI-DSS 8.2.4 compliant
 */
export async function POST(request: Request) {
  try {
    const body = await request.json();

    const res = await fetch(`${GATEWAY_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await res.json();

    if (!res.ok) {
      return NextResponse.json(data, { status: res.status });
    }

    // Backend may return tokens at top-level or nested in data
    const accessToken =
      data.access_token ?? data.data?.access_token ?? data.data?.accessToken;
    const refreshToken =
      data.refresh_token ?? data.data?.refresh_token ?? data.data?.refreshToken;

    // Extract user info from JWT payload (auth-service returns Keycloak tokens directly)
    let user = data.user ?? data.data?.user;
    if (!user && accessToken) {
      const claims = decodeJwtPayload(accessToken);
      if (claims) {
        user = {
          id: claims.sub as string,
          username: claims.preferred_username as string,
          fullName: (claims.name as string) || "",
          email: (claims.email as string) || "",
          roles:
            ((claims.realm_access as Record<string, unknown>)
              ?.roles as string[]) || [],
        };
      }
    }

    const isProduction = process.env.NODE_ENV === "production";
    const cookieStore = await cookies();

    if (accessToken) {
      cookieStore.set("accessToken", accessToken, {
        httpOnly: true,
        secure: isProduction,
        sameSite: "strict",
        maxAge: 900, // 15 minutes
        path: "/",
      });
    }

    if (refreshToken) {
      cookieStore.set("refreshToken", refreshToken, {
        httpOnly: true,
        secure: isProduction,
        sameSite: "strict",
        maxAge: 604_800, // 7 days
        path: "/",
      });
    }

    // Return user data WITHOUT tokens — browser never sees JWT
    return NextResponse.json({ success: true, data: { user } });
  } catch (error) {
    console.error("[BFF] Login proxy error:", error);
    return NextResponse.json(
      { success: false, message: "Authentication service unavailable" },
      { status: 503 },
    );
  }
}
