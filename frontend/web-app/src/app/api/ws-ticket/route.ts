import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import logger from "@/lib/logger";
import { issueWsTicket } from "@/lib/ws-ticket";

/**
 * BFF WebSocket ticket route (WEB-OS-008).
 *
 * The analytics real-time stream authenticates with a short-lived HMAC ticket
 * because page JS cannot read the httpOnly session cookie to attach a JWT to
 * the WebSocket handshake. The ticket is signed with WS_TICKET_SECRET and
 * verified by analytics-service (same secret via env).
 *
 * POST /api/ws-ticket -> { ticket, accountId } | 401 | 500 (misconfigured)
 */
export async function POST() {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("accessToken")?.value;
  if (!accessToken) {
    return NextResponse.json({ error: "UNAUTHENTICATED" }, { status: 401 });
  }

  let claims: Record<string, unknown>;
  try {
    const payload = JSON.parse(
      Buffer.from(accessToken.split(".")[1] ?? "", "base64url").toString("utf-8")
    ) as Record<string, unknown>;
    claims = payload;
  } catch {
    return NextResponse.json({ error: "INVALID_TOKEN" }, { status: 401 });
  }

  const sub = claims.sub as string | undefined;
  if (!sub) {
    return NextResponse.json({ error: "INVALID_TOKEN" }, { status: 401 });
  }
  // Fallback mirrors the OIDC callback mapping (account_id claim or `account-<sub>`).
  const accountId = (claims.account_id as string | undefined) ?? `account-${sub}`;

  const secret = process.env.WS_TICKET_SECRET;
  if (!secret) {
    logger.error({ action: "ws-ticket" }, "WS_TICKET_SECRET not configured");
    return NextResponse.json({ error: "TICKET_SECRET_MISSING" }, { status: 500 });
  }

  const ticket = issueWsTicket(secret, accountId, sub);
  logger.info({ action: "ws-ticket", accountId }, "WebSocket ticket issued");

  return NextResponse.json({ ticket, accountId });
}
