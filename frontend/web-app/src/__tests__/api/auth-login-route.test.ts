import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/logger", () => ({
  getCorrelationId: vi.fn(() => "test-correlation-id"),
  withCorrelation: vi.fn(() => ({
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  })),
}));

import { POST } from "@/app/api/auth/login/route";

describe("POST /api/auth/login", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    {
      name: "top-level snake_case",
      ip: "192.0.2.10",
      upstream: {
        access_token: "top-access",
        refresh_token: "top-refresh",
        expires_in: 900,
        user: { id: "user-1", username: "payu-user" },
      },
      accessToken: "top-access",
      refreshToken: "top-refresh",
    },
    {
      name: "nested camelCase",
      ip: "192.0.2.11",
      upstream: {
        data: {
          accessToken: "nested-access",
          refreshToken: "nested-refresh",
          expires_in: 900,
          user: { id: "user-1", username: "payu-user" },
        },
      },
      accessToken: "nested-access",
      refreshToken: "nested-refresh",
    },
  ])("keeps $name tokens out of JSON while setting httpOnly cookies", async ({
    upstream,
    ip,
    accessToken,
    refreshToken,
  }) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(JSON.stringify(upstream), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    ));

    const response = await POST(new Request("http://localhost/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Real-IP": ip,
      },
      body: JSON.stringify({ username: "payu-user", password: "secret" }),
    }));

    expect(await response.json()).toEqual({
      success: true,
      data: {
        user: upstream.user ?? upstream.data?.user,
        expiresIn: 900,
      },
    });
    const cookies = response.headers.getSetCookie().join(";");
    expect(cookies).toContain(`accessToken=${accessToken}`);
    expect(cookies).toContain(`refreshToken=${refreshToken}`);
    expect(cookies).toContain("HttpOnly");
  });

  it("allowlists upstream error data without exposing tokens", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        success: false,
        message: "Invalid credentials",
        code: "AUTH_001",
        access_token: "error-access",
        data: { refreshToken: "error-refresh" },
      }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      }),
    ));

    const response = await POST(new Request("http://localhost/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Real-IP": "192.0.2.12",
      },
      body: JSON.stringify({ username: "payu-user", password: "wrong" }),
    }));

    expect(response.status).toBe(401);
    expect(await response.json()).toEqual({
      success: false,
      message: "Invalid credentials",
      code: "AUTH_001",
    });
    expect(response.headers.getSetCookie()).toEqual([]);
  });

  it("calls gateway over plain http by default (WEB-002)", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        access_token: "access",
        refresh_token: "refresh",
        expires_in: 900,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await POST(new Request("http://localhost/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Real-IP": "192.0.2.13",
      },
      body: JSON.stringify({ username: "payu-user", password: "secret" }),
    }));

    const calledUrl = String(fetchMock.mock.calls[0][0]);
    expect(calledUrl).toBe("http://gateway-service:8080/api/v1/auth/login");
  });
});
