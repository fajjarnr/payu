"""
Example test demonstrating factory pattern usage for KYC Service.

This file shows how to use the factory functions to generate test data
instead of hardcoded values. Factories provide:
- Realistic test data variation
- Easier edge case testing
- Reduced test code duplication
"""

import pytest
import sys
from unittest.mock import AsyncMock

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from tests.factories import (
    user_factory,
    ktp_ocr_factory,
    liveness_result_factory,
    face_match_factory,
    kyc_verification_factory,
)


@pytest.mark.unit
class TestFactoryUsage:
    """Demonstrate factory pattern usage in KYC tests"""

    def test_user_factory_generates_unique_users(self):
        """Test that user_factory generates unique data each time"""
        user1 = user_factory()
        user2 = user_factory()

        # Each call should generate unique data
        assert user1["user_id"] != user2["user_id"]
        assert user1["email"] != user2["email"]
        assert user1["phone"] != user2["phone"]

    def test_user_factory_with_overrides(self):
        """Test that user_factory accepts kwargs for overrides"""
        # Create user with specific KYC status
        verified_user = user_factory(kyc_status="VERIFIED")
        assert verified_user["kyc_status"] == "VERIFIED"

        # Create user with specific email
        custom_user = user_factory(email="test@example.com")
        assert custom_user["email"] == "test@example.com"

    def test_ktp_ocr_factory_generates_valid_nik(self):
        """Test that KTP factory generates valid 16-digit NIK"""
        ktp_data = ktp_ocr_factory()

        # NIK must be 16 digits
        assert len(ktp_data["nik"]) == 16
        assert ktp_data["nik"].isdigit()

        # Required fields should be present
        assert ktp_data["name"]
        assert ktp_data["birth_date"]
        assert ktp_data["gender"] in ["LAKI-LAKI", "PEREMPUAN"]

    def test_ktp_ocr_factory_with_edge_cases(self):
        """Test creating KTP data for edge case scenarios"""
        # Create KTP with low confidence (poor image quality scenario)
        poor_quality_ktp = ktp_ocr_factory(confidence=0.65)
        assert poor_quality_ktp["confidence"] == 0.65

        # Create KTP with specific gender
        female_ktp = ktp_ocr_factory(gender="PEREMPUAN")
        assert female_ktp["gender"] == "PEREMPUAN"

    def test_liveness_result_factory_scenarios(self):
        """Test liveness result factory for different scenarios"""
        # Successful liveness check
        live_result = liveness_result_factory(is_live=True)
        assert live_result["is_live"] is True
        assert live_result["face_detected"] is True
        assert live_result["confidence"] >= 0.70

        # Failed liveness check (spoof attempt)
        spoof_result = liveness_result_factory(is_live=False)
        assert spoof_result["is_live"] is False

    def test_face_match_factory_scenarios(self):
        """Test face match factory for different scenarios"""
        # Matching faces
        match_result = face_match_factory(is_match=True)
        assert match_result["is_match"] is True
        assert match_result["similarity_score"] >= 0.85

        # Non-matching faces
        no_match_result = face_match_factory(is_match=False)
        assert no_match_result["is_match"] is False
        assert no_match_result["similarity_score"] < 0.75

    def test_kyc_verification_factory_status_transitions(self):
        """Test KYC verification factory for different status scenarios"""
        # Pending verification
        pending_kyc = kyc_verification_factory(status="PENDING")
        assert pending_kyc["status"] == "PENDING"
        assert pending_kyc["completed_at"] is None

        # Verified KYC
        verified_kyc = kyc_verification_factory(status="VERIFIED")
        assert verified_kyc["status"] == "VERIFIED"
        assert verified_kyc["completed_at"] is not None

        # Rejected KYC
        rejected_kyc = kyc_verification_factory(status="REJECTED")
        assert rejected_kyc["status"] == "REJECTED"
        assert rejected_kyc["rejection_reason"] is not None

    def test_factory_generates_realistic_indonesian_data(self):
        """Test that factories generate realistic Indonesian data"""
        ktp = ktp_ocr_factory()

        # Province should be valid Indonesian province
        # Common provinces include DKI JAKARTA, JAWA BARAT, etc.
        assert len(ktp["province"]) > 0
        assert len(ktp["city"]) > 0

        # Religion should be one of recognized religions in Indonesia
        valid_religions = ["ISLAM", "KRISTEN", "KATOLIK", "HINDU", "BUDDHA", "KONGHUCU"]
        assert ktp["religion"] in valid_religions

    def test_factory_pattern_simplifies_edge_case_testing(self):
        """Demonstrate how factories simplify testing edge cases"""
        # Edge case 1: New user with minimal history
        new_user = user_factory(
            kyc_status="PENDING", account_created_at="2024-01-01T00:00:00"
        )
        assert new_user["kyc_status"] == "PENDING"

        # Edge case 2: High confidence OCR result
        perfect_ktp = ktp_ocr_factory(confidence=0.99)
        assert perfect_ktp["confidence"] == 0.99

        # Edge case 3: Face not detected in KTP
        no_face_ktp = ktp_ocr_factory()
        # Simulate scenario where face detection would fail
        face_result = face_match_factory(ktp_face_found=False)
        assert face_result["ktp_face_found"] is False

    @pytest.mark.asyncio
    async def test_factory_with_mock_service(self):
        """Test using factory data with mocked services"""
        from app.services.kyc_service import KycService

        # Create mock session
        mock_session = AsyncMock()
        mock_session.commit = AsyncMock()
        mock_session.refresh = AsyncMock()

        # Use factory to generate test data
        test_user = user_factory(user_id="test_user_123")
        test_kyc = kyc_verification_factory(
            user_id=test_user["user_id"], status="PENDING"
        )

        # Verify factory data can be used with service
        service = KycService(mock_session)
        assert test_kyc["user_id"] == test_user["user_id"]
        assert test_kyc["status"] == "PENDING"

    def test_factory_reduces_test_code_duplication(self):
        """Demonstrate how factories reduce code duplication"""
        # Without factory - hardcoded values (verbose)
        user1 = {
            "user_id": "user_123",
            "email": "user123@example.com",
            "phone": "+628123456789",
            "full_name": "TEST USER",
            "kyc_status": "PENDING",
        }

        # With factory - one line, generates unique data
        user2 = user_factory()

        # Both produce valid test data, but factory is:
        # - More concise (1 line vs 5 lines)
        # - Generates unique data each time
        # - Easier to maintain
        assert "user_id" in user1
        assert "user_id" in user2
        assert len(user2["user_id"]) > 0

    def test_batch_data_generation_with_factories(self):
        """Test generating multiple test data instances easily"""
        # Generate 10 unique users with one line
        users = [user_factory() for _ in range(10)]

        # All users should be unique
        user_ids = [u["user_id"] for u in users]
        assert len(set(user_ids)) == 10  # All unique

        # Generate 5 KTP OCR results
        ktp_results = [ktp_ocr_factory() for _ in range(5)]

        # All should have valid NIKs
        for ktp in ktp_results:
            assert len(ktp["nik"]) == 16
            assert ktp["nik"].isdigit()
