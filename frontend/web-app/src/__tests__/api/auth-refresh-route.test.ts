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

  it("does not keep a process-local attempt counter", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(
      new Response(JSON.stringify({
        access_token: "access-token",
        refresh_token: "refresh-token",
        expires_in: 900,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    ));
    vi.stubGlobal("fetch", fetchMock);

    const responses = await Promise.all(Array.from({ length: 6 }, () => POST()));

    expect(responses.every(response => response.status === 200)).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(6);
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
});
