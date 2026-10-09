import { beforeEach, describe, expect, it, vi } from "vitest";

const { getHeaders, getCookies } = vi.hoisted(() => ({
  getHeaders: vi.fn(() => new Headers({ "x-forwarded-for": "198.51.100.41" })),
  getCookies: vi.fn(() => ({
    get: (_name: string) => ({ value: "refresh-token" }),
  })),
}));

vi.mock("next/headers", () => ({
  headers: getHeaders,
  cookies: getCookies,
}));

vi.mock("@/lib/logger", () => ({
  default: { info: vi.fn(), warn: vi.fn(), error: vi.fn(), debug: vi.fn() },
}));

import { POST } from "@/app/api/auth/refresh/route";

const base64url = (value: unknown) =>
  Buffer.from(JSON.stringify(value)).toString("base64url");

/** JWT whose `exp` sits comfortably in the future. */
const freshAccessToken = [
  Buffer.from(JSON.stringify({ alg: "RS256" })).toString("base64url"),
  base64url({ exp: Math.floor(Date.now() / 1000) + 600, sub: "user-1" }),
  "signature",
].join(".");

/** JWT whose `exp` is already in the past. */
const expiredAccessToken = [
  Buffer.from(JSON.stringify({ alg: "RS256" })).toString("base64url"),
  base64url({ exp: Math.floor(Date.now() / 1000) - 600, sub: "user-1" }),
  "signature",
].join(".");

describe("POST /api/auth/refresh", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("coalesces concurrent refreshes sharing one single-use cookie into one upstream rotation (FE-AUDIT-001)", async () => {
    let releaseUpstream!: (value: Response) => void;
    const upstreamGate = new Promise<Response>((resolve) => {
      releaseUpstream = resolve;
    });
    const fetchMock = vi.fn().mockImplementation(() => upstreamGate);
    vi.stubGlobal("fetch", fetchMock);

    const pending = Promise.all(Array.from({ length: 6 }, () => POST()));
    await Promise.resolve();
    await Promise.resolve();
    releaseUpstream(
      new Response(
        JSON.stringify({
          access_token: "access-token",
          refresh_token: "refresh-token-2",
          expires_in: 900,
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    const responses = await pending;

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(responses.every((response) => response.status === 200)).toBe(true);
    for (const response of responses) {
      expect(response.cookies.get("refreshToken")?.value).toBe(
        "refresh-token-2",
      );
    }
  });

  it("rotates independently for different session cookies", async () => {
    const fetchMock = vi.fn().mockImplementation(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            access_token: "access-token",
            refresh_token: "refresh-token-2",
            expires_in: 900,
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        ),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    getCookies
      .mockReturnValueOnce({ get: vi.fn(() => ({ value: "session-A" })) })
      .mockReturnValueOnce({ get: vi.fn(() => ({ value: "session-B" })) });

    const responses = await Promise.all([POST(), POST()]);

    expect(responses.every((response) => response.status === 200)).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("preserves cookies when the gateway is unreachable (transient)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new Error("fetch failed")),
    );

    const response = await POST();

    expect(response.status).toBe(503);
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });

  it("preserves cookies on gateway 5xx without wiping the session", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        Promise.resolve(
          new Response(JSON.stringify({ success: false }), {
            status: 502,
            headers: { "Content-Type": "application/json" },
          }),
        ),
      ),
    );

    const response = await POST();

    expect(response.status).toBe(502);
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });
  it("preserves cookies even on definitive 401 rejection", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        Promise.resolve(
          new Response(JSON.stringify({ success: false }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          }),
        ),
      ),
    );

    const response = await POST();

    expect(response.status).toBe(401);
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });
  it("derives Secure from the request proto, not env (FE-AUDIT-002)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        Promise.resolve(
          new Response(
            JSON.stringify({
              access_token: "access-token",
              refresh_token: "refresh-token-2",
              expires_in: 900,
            }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        ),
      ),
    );
    const prevBaseUrl = process.env.NEXT_PUBLIC_BASE_URL;
    // Env says http (insecure) but the request arrived via https behind the LB.
    process.env.NEXT_PUBLIC_BASE_URL = "http://payu-dev.apps.fajjjar.my.id";
    try {
      const req = new Request(
        "https://payu-dev.apps.fajjjar.my.id/api/auth/refresh",
        {
          method: "POST",
          headers: { "x-forwarded-proto": "https" },
        },
      );
      const response = await POST(req);
      const setCookies = response.headers.getSetCookie();
      expect(
        setCookies.some(
          (c) => c.startsWith("accessToken=") && c.includes("Secure"),
        ),
      ).toBe(true);
    } finally {
      if (prevBaseUrl === undefined) delete process.env.NEXT_PUBLIC_BASE_URL;
      else process.env.NEXT_PUBLIC_BASE_URL = prevBaseUrl;
    }
  });

  it("skips the upstream rotation while the access token is still fresh (AUTH-REFRESH-001)", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    getCookies.mockReturnValueOnce({
      get: (name: string) =>
        name === "accessToken"
          ? { value: freshAccessToken }
          : { value: "refresh-token" },
    });

    const response = await POST();

    expect(fetchMock).not.toHaveBeenCalled();
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.expiresIn).toBeGreaterThan(30);
    // AUTH-REFRESH-001 regression guard: the early return must still
    // hydrate the auth store (SessionBootstrap reads data.user), or every
    // page load loses accountId.
    expect(body.user).toMatchObject({ id: "user-1" });
    // No rotation happened, so no cookies may be rewritten.
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });

  it("still rotates when the access token is missing or expired (AUTH-REFRESH-001)", async () => {
    const fetchMock = vi.fn().mockImplementation(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            access_token: "access-token-2",
            refresh_token: "refresh-token-2",
            expires_in: 900,
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        ),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    // Missing access token: nothing to compare expiry against.
    getCookies.mockReturnValueOnce({
      get: (name: string) =>
        name === "refreshToken" ? { value: "refresh-token" } : { value: "" },
    });
    await POST();

    // Expired access token: the guard must not suppress rotation.
    getCookies.mockReturnValueOnce({
      get: (name: string) =>
        name === "accessToken"
          ? { value: expiredAccessToken }
          : { value: "refresh-token" },
    });
    await POST();

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(getCookies).toHaveBeenCalled();
  });
});
