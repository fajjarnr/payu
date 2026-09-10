import { beforeEach, describe, expect, it, vi } from "vitest";

const { getHeaders, getCookies } = vi.hoisted(() => ({
  getHeaders: vi.fn(() => new Headers({ "x-forwarded-for": "198.51.100.41" })),
  getCookies: vi.fn(() => ({ get: vi.fn(() => ({ value: "refresh-token" })) })),
}));

vi.mock("next/headers", () => ({
  headers: getHeaders,
  cookies: getCookies,
}));

vi.mock("@/lib/logger", () => ({
  default: { info: vi.fn(), warn: vi.fn(), error: vi.fn() },
}));

import { POST } from "@/app/api/auth/refresh/route";

describe("POST /api/auth/refresh", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("coalesces concurrent refreshes sharing one single-use cookie into one upstream rotation (FE-AUDIT-001)", async () => {
    let releaseUpstream!: (value: Response) => void;
    const upstreamGate = new Promise<Response>((resolve) => { releaseUpstream = resolve; });
    const fetchMock = vi.fn().mockImplementation(() => upstreamGate);
    vi.stubGlobal("fetch", fetchMock);

    const pending = Promise.all(Array.from({ length: 6 }, () => POST()));
    await Promise.resolve();
    await Promise.resolve();
    releaseUpstream(new Response(JSON.stringify({
      access_token: "access-token",
      refresh_token: "refresh-token-2",
      expires_in: 900,
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    const responses = await pending;

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(responses.every(response => response.status === 200)).toBe(true);
    for (const response of responses) {
      expect(response.cookies.get("refreshToken")?.value).toBe("refresh-token-2");
    }
  });

  it("rotates independently for different session cookies", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(
      new Response(JSON.stringify({
        access_token: "access-token",
        refresh_token: "refresh-token-2",
        expires_in: 900,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    ));
    vi.stubGlobal("fetch", fetchMock);
    getCookies
      .mockReturnValueOnce({ get: vi.fn(() => ({ value: "session-A" })) })
      .mockReturnValueOnce({ get: vi.fn(() => ({ value: "session-B" })) });

    const responses = await Promise.all([POST(), POST()]);

    expect(responses.every(response => response.status === 200)).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("preserves cookies when the gateway is unreachable (transient)", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("fetch failed")));

    const response = await POST();

    expect(response.status).toBe(503);
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });

  it("preserves cookies on gateway 5xx without wiping the session", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(
      new Response(JSON.stringify({ success: false }), {
        status: 502,
        headers: { "Content-Type": "application/json" },
      }),
    )));

    const response = await POST();

    expect(response.status).toBe(502);
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });
  it('preserves cookies even on definitive 401 rejection', async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(
      new Response(JSON.stringify({ success: false }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      }),
    )));

    const response = await POST();

    expect(response.status).toBe(401);
    expect(response.cookies.get("accessToken")).toBeUndefined();
    expect(response.cookies.get("refreshToken")).toBeUndefined();
  });
  it('derives Secure from the request proto, not env (FE-AUDIT-002)', async () => {

    vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(
      new Response(JSON.stringify({
        access_token: "access-token",
        refresh_token: "refresh-token-2",
        expires_in: 900,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    )));
    const prevBaseUrl = process.env.NEXT_PUBLIC_BASE_URL;
    // Env says http (insecure) but the request arrived via https behind the LB.
    process.env.NEXT_PUBLIC_BASE_URL = "http://payu-dev.apps.fajjjar.my.id";
    try {
      const req = new Request("https://payu-dev.apps.fajjjar.my.id/api/auth/refresh", {
        method: "POST",
        headers: { "x-forwarded-proto": "https" },
      });
      const response = await POST(req);
      const setCookies = response.headers.getSetCookie();
      expect(setCookies.some((c) => c.startsWith("accessToken=") && c.includes("Secure"))).toBe(true);
    } finally {
      if (prevBaseUrl === undefined) delete process.env.NEXT_PUBLIC_BASE_URL;
      else process.env.NEXT_PUBLIC_BASE_URL = prevBaseUrl;
    }
  });
});
