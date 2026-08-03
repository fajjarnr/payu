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
});
