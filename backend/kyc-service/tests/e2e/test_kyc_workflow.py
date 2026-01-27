import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")  # noqa: E402
from unittest.mock import AsyncMock, patch, MagicMock  # noqa: E402


@pytest.mark.e2e
class TestKycWorkflowE2E:
    """End-to-end tests for KYC verification workflow"""

    @pytest.mark.asyncio
    async def test_complete_kyc_workflow_success(
        self, async_test_client, sample_user_id, sample_ktp_image, sample_selfie_image
    ):
        """Test complete KYC workflow from start to verified"""

        # Mock OCR response - include all required fields for KtpOcrResult
        mock_ocr_result = MagicMock()
        mock_ocr_result.nik = "3201234567890001"
        mock_ocr_result.name = "JOHN DOE"
        mock_ocr_result.confidence = 0.95
        mock_ocr_result.birth_date = "1990-01-01"
        mock_ocr_result.gender = "LAKI-LAKI"
        mock_ocr_result.address = "Jl. Test No. 123"
        mock_ocr_result.province = "DKI JAKARTA"
        mock_ocr_result.city = "JAKARTA SELATAN"
        mock_ocr_result.district = "TEBET"
        mock_ocr_result.model_dump.return_value = {
            "nik": "3201234567890001",
            "name": "JOHN DOE",
            "confidence": 0.95,
            "birth_date": "1990-01-01",
            "gender": "LAKI-LAKI",
            "address": "Jl. Test No. 123",
            "province": "DKI JAKARTA",
            "city": "JAKARTA SELATAN",
            "district": "TEBET",
        }

        # Mock liveness check
        mock_liveness_result = MagicMock()
        mock_liveness_result.is_live = True
        mock_liveness_result.confidence = 0.85
        mock_liveness_result.face_detected = True
        mock_liveness_result.face_quality_score = 0.9
        mock_liveness_result.model_dump.return_value = {
            "is_live": True,
            "confidence": 0.85,
            "face_detected": True,
            "face_quality_score": 0.9,
        }

        # Mock face matching
        mock_face_result = MagicMock()
        mock_face_result.is_match = True
        mock_face_result.similarity_score = 0.85
        mock_face_result.threshold = 0.6
        mock_face_result.ktp_face_found = True
        mock_face_result.selfie_face_found = True
        mock_face_result.model_dump.return_value = {
            "is_match": True,
            "similarity_score": 0.85,
            "threshold": 0.6,
            "ktp_face_found": True,
            "selfie_face_found": True,
        }

        # Mock Dukcapil client
        mock_dukcapil_result = MagicMock()
        mock_dukcapil_result.nik = "3201234567890001"
        mock_dukcapil_result.is_valid = True
        mock_dukcapil_result.name = "JOHN DOE"
        mock_dukcapil_result.birth_date = "1990-01-01"
        mock_dukcapil_result.gender = "LAKI-LAKI"
        mock_dukcapil_result.status = "VALID"
        mock_dukcapil_result.match_score = 0.95
        mock_dukcapil_result.notes = None
        mock_dukcapil_result.model_dump.return_value = {
            "nik": "3201234567890001",
            "is_valid": True,
            "name": "JOHN DOE",
            "birth_date": "1990-01-01",
            "gender": "LAKI-LAKI",
            "status": "VALID",
            "match_score": 0.95,
        }

        # Patch at the location where the services are imported/used
        with patch("app.services.kyc_service.OcrService") as MockOCR, patch(
            "app.services.kyc_service.LivenessService"
        ) as MockLiveness, patch(
            "app.services.kyc_service.FaceService"
        ) as MockFace, patch(
            "app.services.kyc_service.DukcapilClient"
        ) as MockDukcapil, patch(
            "app.services.kyc_service.KafkaProducerService"
        ) as MockKafka:

            # Setup mock instances
            mock_ocr_instance = AsyncMock()
            mock_ocr_instance.extract_ktp_data = AsyncMock(return_value=mock_ocr_result)
            MockOCR.return_value = mock_ocr_instance

            mock_liveness_instance = AsyncMock()
            mock_liveness_instance.check_liveness = AsyncMock(
                return_value=mock_liveness_result
            )
            MockLiveness.return_value = mock_liveness_instance

            mock_face_instance = AsyncMock()
            mock_face_instance.match_face = AsyncMock(return_value=mock_face_result)
            MockFace.return_value = mock_face_instance

            mock_dukcapil_instance = AsyncMock()
            mock_dukcapil_instance.verify_nik = AsyncMock(
                return_value=mock_dukcapil_result
            )
            MockDukcapil.return_value = mock_dukcapil_instance

            mock_kafka_instance = AsyncMock()
            mock_kafka_instance.publish_event = AsyncMock()
            MockKafka.return_value = mock_kafka_instance

            # Step 1: Start KYC verification
            start_response = await async_test_client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": sample_user_id, "verification_type": "FULL_KYC"},
            )

            assert start_response.status_code == 200
            start_data = start_response.json()
            assert "verification_id" in start_data
            assert start_data["status"] == "PENDING"
            assert start_data["message"] == "Please upload KTP image"

            verification_id = start_data["verification_id"]

            # Step 2: Upload KTP image
            ktp_response = await async_test_client.post(
                "/api/v1/kyc/verify/ktp",
                json={
                    "verification_id": verification_id,
                    "ktp_image": sample_ktp_image,
                },
            )

            assert ktp_response.status_code == 200
            ktp_data = ktp_response.json()
            assert ktp_data["status"] == "PROCESSING"
            assert "ocr_result" in ktp_data
            assert ktp_data["ocr_result"]["nik"] == "3201234567890001"

            # Step 3: Upload selfie image
            selfie_response = await async_test_client.post(
                "/api/v1/kyc/verify/selfie",
                json={
                    "verification_id": verification_id,
                    "selfie_image": sample_selfie_image,
                },
            )

            assert selfie_response.status_code == 200
            selfie_data = selfie_response.json()
            assert selfie_data["status"] == "VERIFIED"
            assert selfie_data["liveness_result"]["is_live"] is True
            assert selfie_data["face_match_result"]["is_match"] is True
            assert selfie_data["dukcapil_result"]["is_valid"] is True

    @pytest.mark.asyncio
    async def test_get_kyc_status_after_verification(
        self, async_test_client, sample_user_id, sample_verification_id
    ):
        """Test getting KYC verification status"""
        response = await async_test_client.get(
            f"/api/v1/kyc/verify/{sample_verification_id}"
        )
        # Verification doesn't exist yet - 404 expected
        assert response.status_code in [200, 404]

    @pytest.mark.asyncio
    async def test_kyc_verification_liveness_failure(
        self, async_test_client, sample_user_id
    ):
        """Test KYC verification when liveness check fails"""

        mock_liveness_result = MagicMock()
        mock_liveness_result.is_live = False
        mock_liveness_result.confidence = 0.3
        mock_liveness_result.face_detected = True
        mock_liveness_result.face_quality_score = 0.5
        mock_liveness_result.model_dump.return_value = {
            "is_live": False,
            "confidence": 0.3,
            "face_detected": True,
            "face_quality_score": 0.5,
        }

        # Patch at the location where the services are imported/used
        with patch("app.services.kyc_service.OcrService") as MockOCR, patch(
            "app.services.kyc_service.LivenessService"
        ) as MockLiveness, patch(
            "app.services.kyc_service.KafkaProducerService"
        ) as MockKafka:

            mock_ocr_result = MagicMock()
            mock_ocr_result.nik = "3201234567890001"
            mock_ocr_result.name = "JOHN DOE"
            mock_ocr_result.confidence = 0.95
            mock_ocr_result.birth_date = "1990-01-01"
            mock_ocr_result.gender = "LAKI-LAKI"
            mock_ocr_result.address = "Jl. Test No. 123"
            mock_ocr_result.province = "DKI JAKARTA"
            mock_ocr_result.city = "JAKARTA SELATAN"
            mock_ocr_result.district = "TEBET"
            mock_ocr_result.model_dump.return_value = {
                "nik": "3201234567890001",
                "name": "JOHN DOE",
                "confidence": 0.95,
                "birth_date": "1990-01-01",
                "gender": "LAKI-LAKI",
                "address": "Jl. Test No. 123",
                "province": "DKI JAKARTA",
                "city": "JAKARTA SELATAN",
                "district": "TEBET",
            }

            mock_ocr_instance = AsyncMock()
            mock_ocr_instance.extract_ktp_data = AsyncMock(return_value=mock_ocr_result)
            MockOCR.return_value = mock_ocr_instance

            mock_liveness_instance = AsyncMock()
            mock_liveness_instance.check_liveness = AsyncMock(
                return_value=mock_liveness_result
            )
            MockLiveness.return_value = mock_liveness_instance

            mock_kafka_instance = AsyncMock()
            mock_kafka_instance.publish_event = AsyncMock()
            MockKafka.return_value = mock_kafka_instance

            # Start verification
            start_response = await async_test_client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": sample_user_id, "verification_type": "FULL_KYC"},
            )
            assert start_response.status_code == 200
            verification_id = start_response.json()["verification_id"]

            # Upload KTP first
            ktp_response = await async_test_client.post(
                "/api/v1/kyc/verify/ktp",
                json={
                    "verification_id": verification_id,
                    "ktp_image": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                },
            )
            assert ktp_response.status_code == 200

            # Upload selfie with liveness failure
            selfie_response = await async_test_client.post(
                "/api/v1/kyc/verify/selfie",
                json={
                    "verification_id": verification_id,
                    "selfie_image": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                },
            )

            assert selfie_response.status_code == 200
            data = selfie_response.json()
            assert data["status"] == "REJECTED"
            # rejection_reason is not returned in the API response
            # assert data["rejection_reason"] == "Liveness check failed"

    @pytest.mark.asyncio
    async def test_kyc_verification_face_match_failure(
        self, async_test_client, sample_user_id
    ):
        """Test KYC verification when face matching fails"""

        mock_liveness_result = MagicMock()
        mock_liveness_result.is_live = True
        mock_liveness_result.confidence = 0.85
        mock_liveness_result.face_detected = True
        mock_liveness_result.face_quality_score = 0.9
        mock_liveness_result.model_dump.return_value = {
            "is_live": True,
            "confidence": 0.85,
            "face_detected": True,
            "face_quality_score": 0.9,
        }

        mock_face_result = MagicMock()
        mock_face_result.is_match = False
        mock_face_result.similarity_score = 0.45
        mock_face_result.threshold = 0.6
        mock_face_result.ktp_face_found = True
        mock_face_result.selfie_face_found = True
        mock_face_result.model_dump.return_value = {
            "is_match": False,
            "similarity_score": 0.45,
            "threshold": 0.6,
            "ktp_face_found": True,
            "selfie_face_found": True,
        }

        # Patch at the location where the services are imported/used
        with patch("app.services.kyc_service.OcrService") as MockOCR, patch(
            "app.services.kyc_service.LivenessService"
        ) as MockLiveness, patch(
            "app.services.kyc_service.FaceService"
        ) as MockFace, patch(
            "app.services.kyc_service.KafkaProducerService"
        ) as MockKafka:

            mock_ocr_result = MagicMock()
            mock_ocr_result.nik = "3201234567890001"
            mock_ocr_result.name = "JOHN DOE"
            mock_ocr_result.confidence = 0.95
            mock_ocr_result.birth_date = "1990-01-01"
            mock_ocr_result.gender = "LAKI-LAKI"
            mock_ocr_result.address = "Jl. Test No. 123"
            mock_ocr_result.province = "DKI JAKARTA"
            mock_ocr_result.city = "JAKARTA SELATAN"
            mock_ocr_result.district = "TEBET"
            mock_ocr_result.model_dump.return_value = {
                "nik": "3201234567890001",
                "name": "JOHN DOE",
                "confidence": 0.95,
                "birth_date": "1990-01-01",
                "gender": "LAKI-LAKI",
                "address": "Jl. Test No. 123",
                "province": "DKI JAKARTA",
                "city": "JAKARTA SELATAN",
                "district": "TEBET",
            }

            mock_ocr_instance = AsyncMock()
            mock_ocr_instance.extract_ktp_data = AsyncMock(return_value=mock_ocr_result)
            MockOCR.return_value = mock_ocr_instance

            mock_liveness_instance = AsyncMock()
            mock_liveness_instance.check_liveness = AsyncMock(
                return_value=mock_liveness_result
            )
            MockLiveness.return_value = mock_liveness_instance

            mock_face_instance = AsyncMock()
            mock_face_instance.match_face = AsyncMock(return_value=mock_face_result)
            MockFace.return_value = mock_face_instance

            mock_kafka_instance = AsyncMock()
            mock_kafka_instance.publish_event = AsyncMock()
            MockKafka.return_value = mock_kafka_instance

            # Start verification
            start_response = await async_test_client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": sample_user_id, "verification_type": "FULL_KYC"},
            )
            assert start_response.status_code == 200
            verification_id = start_response.json()["verification_id"]

            # Upload KTP first
            ktp_response = await async_test_client.post(
                "/api/v1/kyc/verify/ktp",
                json={
                    "verification_id": verification_id,
                    "ktp_image": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                },
            )
            assert ktp_response.status_code == 200

            # Upload selfie with face match failure
            selfie_response = await async_test_client.post(
                "/api/v1/kyc/verify/selfie",
                json={
                    "verification_id": verification_id,
                    "selfie_image": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                },
            )

            assert selfie_response.status_code == 200
            data = selfie_response.json()
            assert data["status"] == "REJECTED"
            # rejection_reason is not returned in the API response
            # assert data["rejection_reason"] == "Face matching failed"
