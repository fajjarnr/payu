"""AI-AUTH-001: cryptographically verify JWTs against Keycloak JWKS.

Never authorize by base64-decoding a JWT payload. The gateway forwards the
original Keycloak RS256 token to backend services, so signature verification
must use Keycloak's public keys (mirror of gateway AuthorizationFilter).
Fails closed: no KEYCLOAK_URL, unknown kid, or bad signature => JWTError.
"""
import os
import time

import httpx
from jose import jwt
from jose.exceptions import JWTError
from structlog import get_logger

logger = get_logger(__name__)

KEYCLOAK_URL = os.getenv("KEYCLOAK_URL", "").rstrip("/")
KEYCLOAK_REALM = os.getenv("KEYCLOAK_REALM", "payu")
JWKS_URI = f"{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/certs"
JWKS_TTL_SECONDS = 300

_jwks: dict = {"keys": [], "fetched_at": 0.0}


async def _fetch_jwks() -> dict:
    async with httpx.AsyncClient(timeout=5.0) as client:
        response = await client.get(JWKS_URI)
        response.raise_for_status()
        return response.json()


async def _public_key(kid: str) -> dict | None:
    now = time.time()
    if not _jwks["keys"] or now - _jwks["fetched_at"] > JWKS_TTL_SECONDS:
        _jwks["keys"] = (await _fetch_jwks()).get("keys", [])
        _jwks["fetched_at"] = now
    for key in _jwks["keys"]:
        if key.get("kid") == kid:
            return key
    _jwks["keys"] = (await _fetch_jwks()).get("keys", [])  # key rotation: refetch once
    _jwks["fetched_at"] = time.time()
    for key in _jwks["keys"]:
        if key.get("kid") == kid:
            return key
    return None


async def verify_jwt(token: str) -> dict:
    if not KEYCLOAK_URL:
        raise JWTError("KEYCLOAK_URL not configured")
    header = jwt.get_unverified_header(token)
    kid = header.get("kid")
    if not kid:
        raise JWTError("Token missing kid")
    key = await _public_key(kid)
    if key is None:
        raise JWTError("Unknown key id")
    payload = jwt.decode(
        token,
        key,
        algorithms=["RS256"],
        options={"verify_aud": False},
    )
    if not payload.get("sub"):
        raise JWTError("Token missing sub")
    return payload