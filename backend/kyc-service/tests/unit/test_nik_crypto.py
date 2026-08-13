import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")  # noqa: E402

from unittest.mock import patch
from app.crypto import encrypt_nik, decrypt_nik


@pytest.mark.unit
class TestNikCrypto:
    """AES-GCM NIK encryption at-rest (ARCH-KYC-001)"""

    def test_roundtrip(self):
        original = "3201234567890001"
        encrypted = encrypt_nik(original)
        assert encrypted.startswith("enc:v1:")
        assert original not in encrypted
        assert decrypt_nik(encrypted) == original

    def test_nonce_makes_ciphertext_unique(self):
        nik = "3201234567890001"
        assert encrypt_nik(nik) != encrypt_nik(nik)

    def test_plaintext_value_passes_through(self):
        assert decrypt_nik("3201234567890001") == "3201234567890001"

    def test_fails_closed_without_key(self):
        with patch("app.crypto.settings", autospec=True) as mock_settings:
            mock_settings.nik_encryption_key = ""
            with pytest.raises(RuntimeError, match="KYC_NIK_ENCRYPTION_KEY"):
                encrypt_nik("3201234567890001")
