"""ARCH-KYC-001 backfill: encrypt any plaintext NIK left in JSON columns.

Scans kyc_verifications and encrypts ``nik`` values that are still plaintext
(16 digits) inside ``ktp_ocr_result`` / ``dukcapil_result``. Idempotent —
encrypted values (``enc:v1:`` prefix) are left untouched. Run against the
service DB with the same KYC_NIK_ENCRYPTION_KEY the service uses.

Usage:
    KYC_NIK_ENCRYPTION_KEY=<hex> DATABASE_URL=postgresql+asyncpg://... \
        python scripts/backfill_nik_encryption.py
"""
import asyncio
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from sqlalchemy import select  # noqa: E402

from app.config import get_settings  # noqa: E402
from app.crypto import encrypt_json_nik, decrypt_json_nik  # noqa: E402
from app.database import KycVerificationEntity, init_db, close_db, async_session_maker  # noqa: E402

_NIK_PLAINTEXT = re.compile(r"^\d{16}$")


async def main():
    settings = get_settings()
    if not settings.nik_encryption_key:
        print("KYC_NIK_ENCRYPTION_KEY must be set")
        return 1

    await init_db()
    updated = 0
    async with async_session_maker() as session:
        result = await session.execute(select(KycVerificationEntity))
        for entity in result.scalars().all():
            changed = False
            for column_name in ("ktp_ocr_result", "dukcapil_result"):
                blob = getattr(entity, column_name)
                if blob and isinstance(blob.get("nik"), str) and _NIK_PLAINTEXT.match(blob["nik"]):
                    setattr(entity, column_name, encrypt_json_nik(blob))
                    changed = True
                    print(f"encrypted nik in {column_name} for verification_id={entity.verification_id}")
            if changed:
                updated += 1
        await session.commit()
    await close_db()
    print(f"Backfill complete: {updated} rows updated")
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
