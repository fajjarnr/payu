"""
ARCH-LOG-001: analytics structlog must never emit PII (NIK/phone/email/account
number/tokens) — the masking processor scrubs them before rendering.
"""

import pytest


@pytest.mark.unit
class TestPiiMasking:
    def test_pii_fields_are_masked(self):
        from app.logging_config import _mask_pii

        event = {
            "event": "search executed",
            "nik": "3201010101010001",
            "phone_number": "08123456789",
            "email": "user@payu.id",
            "account_number": "1234567890",
            "access_token": "eyJhbGciOi...",
            "amount": 10000,
        }

        result = _mask_pii(None, None, event)

        assert result["nik"] == "***"
        assert result["phone_number"] == "***"
        assert result["email"] == "***"
        assert result["account_number"] == "***"
        assert result["access_token"] == "***"
        assert result["amount"] == 10000  # non-PII untouched
        assert result["event"] == "search executed"

    def test_masking_is_a_processor_used_in_configured_chain(self):
        from app import logging_config as lc
        import inspect

        source = inspect.getsource(lc.configure_logging)
        assert "_mask_pii" in source
        assert "_mask_pii" in dir(lc)
