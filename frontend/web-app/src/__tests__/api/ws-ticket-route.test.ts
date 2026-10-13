import { beforeEach, describe, expect, it, vi } from "vitest";
import { createHmac } from "crypto";
import { issueWsTicket, verifyWsTicket, WS_TICKET_TTL_SECONDS } from "@/lib/ws-ticket";

const { getCookies } = vi.hoisted(() => ({
  getCookies: vi.fn(),
}));

vi.mock("next/headers", () => ({
  cookies: getCookies,
}));

vi.mock("@/lib/logger", () => ({
  default: { info: vi.fn(), warn: vi.fn(), error: vi.fn(), debug: vi.fn() },
}));

import { POST } from "@/app/api/ws-ticket/route";

const SECRET = "test-ws-secret";
const ACCOUNT = "acc-750e8400-0001";

const b64url = (value: unknown) => Buffer.from(JSON.stringify(value)).toString("base64url");

function accessTokenWith(claims: Record<string, unknown>): string {
  return [
    Buffer.from(JSON.stringify({ alg: "RS256" })).toString("base64url"),
    b64url({ exp: Math.floor(Date.now() / 1000) + 600, ...claims }),
    "signature",
  ].join(".");
}

describe("lib/ws-ticket", () => {
  it("issues a ticket that verifies for the same account and secret", () => {
    const ticket = issueWsTicket(SECRET, ACCOUNT, "sub-1");
    const parts = ticket.split(".");
    expect(parts).toHaveLength(2);

    const payload = verifyWsTicket(SECRET, ticket, ACCOUNT);
    expect(payload?.sub).toBe("sub-1");
    expect(payload?.account_id).toBe(ACCOUNT);
    expect(payload?.exp).toBeGreaterThan(Math.floor(Date.now() / 1000));
  });

  it("rejects another account, another secret, a tampered payload, and expiry", () => {
    const ticket = issueWsTicket(SECRET, ACCOUNT, "sub-1");
    expect(verifyWsTicket(SECRET, ticket, "acc-other")).toBeNull();
    expect(verifyWsTicket("other-secret", ticket, ACCOUNT)).toBeNull();
    expect(verifyWsTicket(SECRET, `${ticket.split(".")[0]}.${"0".repeat(64)}`, ACCOUNT)).toBeNull();
    const expired = issueWsTicket(SECRET, ACCOUNT, "sub-1", -10);
    expect(verifyWsTicket(SECRET, expired, ACCOUNT)).toBeNull();
    expect(verifyWsTicket(SECRET, "garbage", ACCOUNT)).toBeNull();
    expect(verifyWsTicket(SECRET, "", ACCOUNT)).toBeNull();
  });

  it("uses an HMAC-SHA256 signature over the base64url payload", () => {
    const ticket = issueWsTicket(SECRET, ACCOUNT, "sub-1");
    const [payload, sig] = ticket.split(".");
    const expected = createHmac("sha256", SECRET).update(payload).digest("hex");
    expect(sig).toBe(expected);
    expect(Number.isInteger(WS_TICKET_TTL_SECONDS)).toBe(true);
  });
});

describe("POST /api/ws-ticket", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv("WS_TICKET_SECRET", SECRET);
  });

  it("401 when no session cookie", async () => {
    getCookies.mockResolvedValue({ get: () => undefined });
    const res = await POST();
    expect(res.status).toBe(401);
  });

  it("401 when the cookie is not a decodable JWT", async () => {
    getCookies.mockResolvedValue({ get: () => ({ value: "not-a-jwt" }) });
    const res = await POST();
    expect(res.status).toBe(401);
  });

  it("500 when WS_TICKET_SECRET is missing", async () => {
    vi.stubEnv("WS_TICKET_SECRET", "");
    getCookies.mockResolvedValue({
      get: () => ({ value: accessTokenWith({ sub: "sub-1", account_id: ACCOUNT }) }),
    });
    const res = await POST();
    expect(res.status).toBe(500);
  });

  it("issues a ticket bound to the session accountId", async () => {
    getCookies.mockResolvedValue({
      get: () => ({ value: accessTokenWith({ sub: "sub-1", account_id: ACCOUNT }) }),
    });
    const res = await POST();
    expect(res.status).toBe(200);

    const body = await res.json();
    expect(body.accountId).toBe(ACCOUNT);

    const payload = verifyWsTicket(SECRET, body.ticket, ACCOUNT);
    expect(payload?.sub).toBe("sub-1");
  });

  it("falls back to account-<sub> when the claim is missing", async () => {
    getCookies.mockResolvedValue({
      get: () => ({ value: accessTokenWith({ sub: "sub-9" }) }),
    });
    const res = await POST();
    const body = await res.json();
    expect(body.accountId).toBe("account-sub-9");
    expect(verifyWsTicket(SECRET, body.ticket, "account-sub-9")?.sub).toBe("sub-9");
  });
});
