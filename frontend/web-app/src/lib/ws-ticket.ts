import { createHmac } from "crypto";

/**
 * WEB-OS-008: WebSocket ticket issuance for the analytics real-time stream.
 *
 * The browser cannot read the httpOnly session cookie, so it cannot attach a
 * JWT to the WebSocket handshake. The BFF instead decodes the accessToken
 * cookie server-side and signs a short-lived ticket that the analytics
 * service validates with the same shared secret:
 *
 *   ticket = base64url(json{"sub", "account_id", "exp"}) + "." + hex(hmac_sha256(secret, payload))
 */
export const WS_TICKET_TTL_SECONDS = 60;

export interface WsTicketPayload {
  sub: string;
  account_id: string;
  exp: number;
}

export function issueWsTicket(
  secret: string,
  accountId: string,
  sub: string,
  ttlSeconds: number = WS_TICKET_TTL_SECONDS
): string {
  const payload = Buffer.from(
    JSON.stringify({
      sub,
      account_id: accountId,
      exp: Math.floor(Date.now() / 1000) + ttlSeconds,
    } satisfies WsTicketPayload)
  ).toString("base64url");
  const signature = createHmac("sha256", secret).update(payload).digest("hex");
  return `${payload}.${signature}`;
}

/** Symmetric verification (single source for tests; the service re-implements it in Python). */
export function verifyWsTicket(
  secret: string,
  ticket: string,
  accountId: string,
  nowSeconds: number = Math.floor(Date.now() / 1000)
): WsTicketPayload | null {
  if (!secret || !ticket) return null;
  const sep = ticket.lastIndexOf(".");
  if (sep <= 0) return null;
  const payloadB64 = ticket.slice(0, sep);
  const signature = ticket.slice(sep + 1);
  const expected = createHmac("sha256", secret).update(payloadB64).digest("hex");
  if (expected.length !== signature.length) return null;
  // constant-time-ish comparison without timingSafeEqual length pitfalls
  let diff = 0;
  for (let i = 0; i < expected.length; i += 1) {
    diff |= expected.charCodeAt(i) ^ signature.charCodeAt(i);
  }
  if (diff !== 0) return null;
  try {
    const payload = JSON.parse(Buffer.from(payloadB64, "base64url").toString("utf-8")) as WsTicketPayload;
    if (payload.exp <= nowSeconds) return null;
    if (payload.account_id !== accountId) return null;
    return payload;
  } catch {
    return null;
  }
}
