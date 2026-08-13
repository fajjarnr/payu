"""AES-GCM field encryption for NIK at-rest (ARCH-KYC-001 / UU PDP).

Stored format: ``enc:v1:<base64(nonce + ciphertext + tag)>``.
A plaintext value (no prefix) passes through unchanged so reads are
backward-compatible and a backfill can run without downtime.
"""
import base64
import os

from structlog import get_logger

from app.config import get_settings

logger = get_logger(__name__)
settings = get_settings()

_PREFIX = "enc:v1:"
_KEY_LEN = 32


def _cipher():
    key_hex = settings.nik_encryption_key
    if not key_hex:
        raise RuntimeError(
            "KYC_NIK_ENCRYPTION_KEY is not set — refusing to store NIK plaintext. "
            "Set a 64-char hex key (32 bytes) in the environment."
        )
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM

    key = bytes.fromhex(key_hex)
    if len(key) != _KEY_LEN:
        raise RuntimeError("KYC_NIK_ENCRYPTION_KEY must be a 64-char hex string (32 bytes)")
    return AESGCM(key)


def encrypt_nik(nik: str) -> str:
    if not nik:
        return nik
    aesgcm = _cipher()
    nonce = os.urandom(12)
    ct = aesgcm.encrypt(nonce, nik.encode("utf-8"), None)
    return _PREFIX + base64.b64encode(nonce + ct).decode("ascii")


def decrypt_nik(value: str) -> str:
    if not value or not value.startswith(_PREFIX):
        return value
    try:
        raw = base64.b64decode(value[len(_PREFIX):])
        nonce, ct = raw[:12], raw[12:]
        return _cipher().decrypt(nonce, ct, None).decode("utf-8")
    except Exception as e:  # noqa: BLE001 — never crash the flow on a bad blob
        logger.error("Failed to decrypt NIK value", error=str(e))
        raise


def encrypt_json_nik(blob: dict) -> dict:
    """Returns a copy of the JSON dict with the ``nik`` field encrypted."""
    if not blob or not isinstance(blob.get("nik"), str):
        return blob
    out = dict(blob)
    out["nik"] = encrypt_nik(blob["nik"])
    return out


def decrypt_json_nik(blob: dict) -> dict:
    """Returns a copy of the JSON dict with the ``nik`` field decrypted."""
    if not blob or not isinstance(blob.get("nik"), str):
        return blob
    out = dict(blob)
    out["nik"] = decrypt_nik(blob["nik"])
    return out
