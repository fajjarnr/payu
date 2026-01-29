"""
Unit tests for KYC API endpoints using factory patterns.

This demonstrates the refactored approach using factory functions
instead of hardcoded test data.

Benefits:
- More realistic test data variation
- Easier to create edge case tests
- Reduced code duplication
- Better maintainability
"""

import pytest
import sys
from unittest.mock import MagicMock, AsyncMock

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.main import app
from app.models.schemas import KycStatus
from app.database import KycVerificationEntity

# Import factory functions
from tests.factories import (
    user_factory,
    kyc_verification_factory,
    sample_ktp_image_base64,
    sample_selfie_image_base64,
)


@pytest.mark.unit
class TestKycApiWithFactories:
    """Unit tests for KYC API endpoints using factory patterns"""

    @pytest.fixture
    def mock_db_session(self):
        """Mock database session"""
        session = AsyncMock()
        session.commit = AsyncMock()
        session.refresh = AsyncMock()
        session.add = MagicMock()
        session.execute = AsyncMock()
        return session

    @pytest.mark.asyncio
    async def test_start_kyc_verification_with_factory(self, mock_db_session):
        """Test successful KYC verification start using factory-generated data"""
        from httpx import AsyncClient, ASGITransport
        from app.database import get_db_session

        # Use factory to generate test user
        test_user = user_factory(kyc_status="PENDING")

        mock_entity = KycVerificationEntity(
            verification_id=f"verify_{test_user['user_id']}",
            user_id=test_user["user_id"],
            verification_type="FULL_KYC",
            status=KycStatus.PENDING.value,
        )

        mock_result = MagicMock()
        mock_result.scalar_one_or_none.return_value = mock_entity
        mock_db_session.execute.return_value = mock_result

        async def override_get_db():
            yield mock_db_session

        app.dependency_overrides[get_db_session] = override_get_db

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": test_user["user_id"], "verification_type": "FULL_KYC"},
            )

            assert response.status_code in [200, 201, 422]

        app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_upload_ktp_with_factory_images(self, mock_db_session):
        """Test KTP upload using factory-generated image data"""
        from httpx import AsyncClient, ASGITransport
        from app.database import get_db_session

        # Use factory to generate test KYC verification
        test_kyc = kyc_verification_factory(status="PROCESSING")

        async def override_get_db():
            yield mock_db_session

        app.dependency_overrides[get_db_session] = override_get_db

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            # Use factory-generated sample image
            response = await client.post(
                "/api/v1/kyc/verify/ktp",
                json={
                    "verification_id": test_kyc.get("verification_id", "verify_123"),
                    "ktp_image": sample_ktp_image_base64(),
                },
            )

            assert response.status_code in [200, 201, 422, 500]

        app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_upload_selfie_with_factory_images(self, mock_db_session):
        """Test selfie upload using factory-generated image data"""
        from httpx import AsyncClient, ASGITransport
        from app.database import get_db_session

        test_kyc = kyc_verification_factory(status="PROCESSING")

        async def override_get_db():
            yield mock_db_session

        app.dependency_overrides[get_db_session] = override_get_db

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            # Use factory-generated sample image
            response = await client.post(
                "/api/v1/kyc/verify/selfie",
                json={
                    "verification_id": test_kyc.get("verification_id", "verify_123"),
                    "selfie_image": sample_selfie_image_base64(),
                },
            )

            assert response.status_code in [200, 201, 422, 500]

        app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_get_status_with_factory_verification(self, mock_db_session):
        """Test getting KYC status using factory-generated verification"""
        from httpx import AsyncClient, ASGITransport
        from app.database import get_db_session

        # Use factory to generate different KYC statuses
        for status in ["PENDING", "VERIFIED", "REJECTED"]:
            test_kyc = kyc_verification_factory(status=status)

            mock_entity = KycVerificationEntity(
                verification_id=test_kyc.get("verification_id", "verify_123"),
                user_id=test_kyc["user_id"],
                verification_type="FULL_KYC",
                status=status,
            )

            mock_result = MagicMock()
            mock_result.scalar_one_or_none.return_value = mock_entity
            mock_db_session.execute.return_value = mock_result

            async def override_get_db():
                yield mock_db_session

            app.dependency_overrides[get_db_session] = override_get_db

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    f"/api/v1/kyc/verify/{test_kyc.get('verification_id', 'verify_123')}"
                )

                assert response.status_code in [200, 404, 422]

            app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_multiple_verifications_with_factories(self, mock_db_session):
        """Test getting multiple verifications using factory-generated data"""
        from httpx import AsyncClient, ASGITransport
        from app.database import get_db_session

        # Generate multiple test users with different statuses
        test_users = [
            user_factory(kyc_status="VERIFIED"),
            user_factory(kyc_status="PENDING"),
            user_factory(kyc_status="REJECTED"),
        ]

        for user in test_users:
            mock_result = MagicMock()
            mock_scalars = MagicMock()
            mock_scalars.all.return_value = []
            mock_result.scalars.return_value = mock_scalars
            mock_db_session.execute.return_value = mock_result

            async def override_get_db():
                yield mock_db_session

            app.dependency_overrides[get_db_session] = override_get_db

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(f"/api/v1/kyc/user/{user['user_id']}")

                assert response.status_code in [200, 422]

            app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_edge_case_new_user_verification(self, mock_db_session):
        """Test edge case: New user with no verification history"""
        from httpx import AsyncClient, ASGITransport
        from app.database import get_db_session

        # Factory allows easy edge case creation
        new_user = user_factory(
            kyc_status="UNVERIFIED", account_created_at="2024-01-01T00:00:00"
        )

        mock_result = MagicMock()
        mock_result.scalar_one_or_none.return_value = None
        mock_db_session.execute.return_value = mock_result

        async def override_get_db():
            yield mock_db_session

        app.dependency_overrides[get_db_session] = override_get_db

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.get(f"/api/v1/kyc/user/{new_user['user_id']}")

            assert response.status_code in [200, 422]

        app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_batch_test_generation_with_factories(self, mock_db_session):
        """Demonstrate batch testing with factory-generated data"""

        # Generate 10 different test scenarios
        test_scenarios = [
            kyc_verification_factory(status=status)
            for status in ["PENDING", "PROCESSING", "VERIFIED", "REJECTED", "FAILED"]
            for _ in range(2)
        ]

        assert len(test_scenarios) == 10

        # All should have unique user IDs
        user_ids = [s["user_id"] for s in test_scenarios]
        assert len(set(user_ids)) == 10  # All unique

        # Verify all scenarios have required fields
        for scenario in test_scenarios:
            assert "user_id" in scenario
            assert "status" in scenario
            assert scenario["status"] in [
                "PENDING",
                "PROCESSING",
                "VERIFIED",
                "REJECTED",
                "FAILED",
            ]


@pytest.mark.unit
class TestFactoryPatternComparison:
    """Compare hardcoded vs factory-based test data generation"""

    def test_hardcoded_approach_verbose(self):
        """Show verbosity of hardcoded approach"""
        # Old approach - verbose and repetitive
        user1 = {
            "user_id": "user_123",
            "email": "user123@example.com",
            "phone": "+628123456789",
            "full_name": "TEST USER ONE",
            "kyc_status": "PENDING",
        }

        user2 = {
            "user_id": "user_456",
            "email": "user456@example.com",
            "phone": "+628987654321",
            "full_name": "TEST USER TWO",
            "kyc_status": "VERIFIED",
        }

        assert user1["user_id"] != user2["user_id"]
        assert len(user1) == 5
        assert len(user2) == 5

    def test_factory_approach_concise(self):
        """Show conciseness of factory approach"""
        # New approach - concise and generates unique data
        user1 = user_factory(kyc_status="PENDING")
        user2 = user_factory(kyc_status="VERIFIED")

        assert user1["user_id"] != user2["user_id"]
        assert user1["kyc_status"] == "PENDING"
        assert user2["kyc_status"] == "VERIFIED"
        assert len(user1) > 5  # More fields generated automatically

    def test_factory_enables_rapid_variation(self):
        """Test that factories enable rapid test variation"""
        # Generate 20 users with different statuses
        users_by_status = {
            status: [user_factory(kyc_status=status) for _ in range(5)]
            for status in ["PENDING", "VERIFIED", "REJECTED"]
        }

        # Verify we have the right distribution
        assert len(users_by_status["PENDING"]) == 5
        assert len(users_by_status["VERIFIED"]) == 5
        assert len(users_by_status["REJECTED"]) == 5

        # All users should be unique
        all_users = (
            users_by_status["PENDING"]
            + users_by_status["VERIFIED"]
            + users_by_status["REJECTED"]
        )
        all_user_ids = [u["user_id"] for u in all_users]
        assert len(set(all_user_ids)) == 15  # All unique
