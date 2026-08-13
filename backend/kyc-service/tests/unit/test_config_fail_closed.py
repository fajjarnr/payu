"""
ARCH-SECRET-001: kyc config must be fail-closed — ARTEMIS credentials cannot
default to 'admin'; get_settings() rejects a missing password.
"""

import os
import pytest


@pytest.mark.unit
class TestConfigFailClosed:
    def test_missing_artemis_password_is_rejected(self, monkeypatch):
        monkeypatch.setenv("SECRET_KEY", "test-secret")
        monkeypatch.setenv("ENCRYPTION_KEY", "test-encryption-key")
        monkeypatch.setenv("ARTEMIS_USERNAME", "test-user")
        monkeypatch.setenv("ARTEMIS_PASSWORD", "")

        from app import config as cfg
        cfg.get_settings.cache_clear()
        try:
            with pytest.raises(ValueError, match="ARTEMIS_USERNAME/ARTEMIS_PASSWORD"):
                cfg.get_settings()
        finally:
            cfg.get_settings.cache_clear()

    def test_empty_artemis_username_is_rejected(self, monkeypatch):
        monkeypatch.setenv("SECRET_KEY", "test-secret")
        monkeypatch.setenv("ENCRYPTION_KEY", "test-encryption-key")
        monkeypatch.setenv("ARTEMIS_PASSWORD", "pw")
        monkeypatch.setenv("ARTEMIS_USERNAME", "")

        from app import config as cfg
        cfg.get_settings.cache_clear()
        try:
            with pytest.raises(ValueError, match="ARTEMIS_USERNAME/ARTEMIS_PASSWORD"):
                cfg.get_settings()
        finally:
            cfg.get_settings.cache_clear()

    def test_valid_credentials_pass(self, monkeypatch):
        monkeypatch.setenv("SECRET_KEY", "test-secret")
        monkeypatch.setenv("ENCRYPTION_KEY", "test-encryption-key")
        monkeypatch.setenv("ARTEMIS_USERNAME", "test-user")
        monkeypatch.setenv("ARTEMIS_PASSWORD", "test-password")

        from app import config as cfg
        cfg.get_settings.cache_clear()
        try:
            settings = cfg.get_settings()
            assert settings.artemis_username == "test-user"
            assert settings.artemis_password == "test-password"
        finally:
            cfg.get_settings.cache_clear()
