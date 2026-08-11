import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/logger", () => ({
  getCorrelationId: vi.fn(() => "test-correlation-id"),
  withCorrelation: vi.fn(() => ({
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  })),
}));

import { GET as authorizeGet } from "@/app/api/auth/authorize/route";
import { GET as callbackGet } from "@/app/api/auth/callback/route";

describe("GET /api/auth/authorize (OIDC PKCE start)", () => {
  const originalEnv = { ...process.env };

  beforeEach(() => {
    vi.clearAllMocks();
    process.env = {
      ...originalEnv,
      OIDC_ISSUER: "http://keycloak:8080/realms/payu",
      NEXT_PUBLIC_BASE_URL: "http://localhost:3001",
    };
  });

  afterEach(() => {
    process.env = originalEnv;
  });

  it("redirects to the Keycloak authorization endpoint with an S256 code challenge", async () => {
    const response = await authorizeGet(new Request("http://localhost:3001/api/auth/authorize"));

    expect(response.status).toBe(307);
    const location = response.headers.get("location") ?? "";
    const url = new URL(location);
    expect(url.origin + url.pathname).toBe(
      "http://keycloak:8080/realms/payu/protocol/openid-connect/auth",
    );
    expect(url.searchParams.get("client_id")).toBe("payu-web-app");
    expect(url.searchParams.get("response_type")).toBe("code");
    expect(url.searchParams.get("scope")).toContain("openid");
    expect(url.searchParams.get("redirect_uri")).toBe(
      "http://localhost:3001/api/auth/callback",
    );
    expect(url.searchParams.get("code_challenge_method")).toBe("S256");
    expect(url.searchParams.get("state")).toHaveLength(32);
    const challenge = url.searchParams.get("code_challenge") ?? "";
    expect(challenge).toMatch(/^[A-Za-z0-9_-]{43}$/);
  });

  it("stores state and PKCE verifier in httpOnly cookies", async () => {
    const response = await authorizeGet(new Request("http://localhost:3001/api/auth/authorize"));
    const cookies = response.headers.getSetCookie().join(";");
    expect(cookies).toContain("oidc_state=");
    expect(cookies).toContain("pkce_verifier=");
    expect(cookies).toContain("HttpOnly");
  });

  it("fails closed with 503 when the OIDC issuer is not configured", async () => {
    process.env = { ...originalEnv, OIDC_ISSUER: "" };
    const response = await authorizeGet(new Request("http://localhost:3001/api/auth/authorize"));
    expect(response.status).toBe(503);
  });
});

describe("GET /api/auth/callback (OIDC PKCE completion)", () => {
  const originalEnv = { ...process.env };
  const gatewayUrl = "http://gateway:8080";

  beforeEach(() => {
    vi.clearAllMocks();
    process.env = { ...originalEnv, GATEWAY_URL: gatewayUrl, NEXT_PUBLIC_BASE_URL: "http://localhost:3001" };
  });

  afterEach(() => {
    process.env = originalEnv;
  });

  const buildRequest = (code = "auth-code-1", state = "s3cret-state-1234567890") =>
    new Request(`http://localhost:3001/api/auth/callback?code=${code}&state=${state}`);

  const withOidcCookies = (req: Request) => {
    req.headers.set(
      "cookie",
      "oidc_state=s3cret-state-1234567890; pkce_verifier=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
    );
    return req;
  };

  it("exchanges code+verifier at the gateway and sets httpOnly cookies, redirecting to /dashboard", async () => {
    let gatewayBody: unknown = null;
    vi.stubGlobal("fetch", vi.fn().mockImplementation(async (url: string, init: RequestInit) => {
      gatewayBody = JSON.parse(String(init.body));
      expect(url).toBe(`${gatewayUrl}/api/v1/auth/callback`);
      return new Response(
        JSON.stringify({
          data: {
            access_token: "at-123",
            refresh_token: "rt-456",
            expires_in: 900,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }));

    const response = await callbackGet(withOidcCookies(buildRequest()));

    expect(gatewayBody).toMatchObject({
      code: "auth-code-1",
      codeVerifier: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
      redirectUri: "http://localhost:3001/api/auth/callback",
    });
    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe("http://localhost:3001/dashboard");
    const cookies = response.headers.getSetCookie().join(";");
    expect(cookies).toContain("accessToken=at-123");
    expect(cookies).toContain("refreshToken=rt-456");
    expect(cookies).toContain("HttpOnly");
  });

  it("rejects a mismatched CSRF state without calling the gateway", async () => {
    const fetchSpy = vi.fn();
    vi.stubGlobal("fetch", fetchSpy);

    const req = withOidcCookies(buildRequest("auth-code-1", "attacker-state-1234567890"));
    const response = await callbackGet(req);

    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toContain("/login?error=invalid_state");
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("redirects to /login?error=authentication_failed when Keycloak rejects the code", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ error: { code: "AUTH_BUS_009" } }), { status: 400 }),
    ));

    const response = await callbackGet(withOidcCookies(buildRequest()));

    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toContain("/login?error=authentication_failed");
  });
});
