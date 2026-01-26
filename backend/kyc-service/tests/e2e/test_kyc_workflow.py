import pytest
from httpx import AsyncClient, ASGITransport
from unittest.mock import AsyncMock, patch, MagicMock
from uuid import uuid4
import base64

import sys
sys.path.insert(0, '/home/ubuntu/payu/backend/kyc-service/src')
from app.main import app


@pytest.mark.e2e
class TestKycWorkflowE2E:
    """End-to-end tests for KYC verification workflow"""

    @pytest.mark.asyncio
    async def test_complete_kyc_workflow_success(self, sample_user_id, sample_ktp_image, sample_selfie_image):
        """Test complete KYC workflow from start to verified"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Step 1: Start KYC verification
            start_response = await client.post(
                "/api/v1/kyc/verify/start",
                json={
                    "user_id": sample_user_id,
                    "verification_type": "FULL_KYC"
                }
            )

            assert start_response.status_code == 200
            start_data = start_response.json()
            assert "verification_id" in start_data
            assert start_data["status"] == "PENDING"
            assert start_data["message"] == "Please upload KTP image"

            verification_id = start_data["verification_id"]

            # Step 2: Upload KTP image
            with patch('app.services.kyc_service.OcrService') as MockOCR, \
                 patch('app.services.kyc_service.LivenessService') as MockLiveness, \
                 patch('app.services.kyc_service.FaceService') as MockFace, \
                 patch('app.services.kyc_service.DukcapilClient') as MockDukcapil:
                
                # Mock OCR response
                mock_ocr_instance = AsyncMock()
                mock_ocr_instance.extract_ktp_data = AsyncMock(return_value=MagicMock(
                    nik="3201234567890001",
                    name="JOHN DOE",
                    confidence=0.95,
                    model_dump=lambda: {
                        "nik": "3201234567890001",
                        "name": "JOHN DOE",
                        "confidence": 0.95
                    }
                ))
                MockOCR.return_value = mock_ocr_instance

                # Mock liveness check
                mock_liveness_instance = AsyncMock()
                mock_liveness_instance.check_liveness = AsyncMock(return_value=MagicMock(
                    is_live=True,
                    confidence=0.85,
                    face_detected=True,
                    face_quality_score=0.9,
                    model_dump=lambda: {
                        "is_live": True,
                        "confidence": 0.85,
                        "face_detected": True,
                        "face_quality_score": 0.9
                    }
                ))
                MockLiveness.return_value = mock_liveness_instance

                # Mock face matching
                mock_face_instance = AsyncMock()
                mock_face_instance.match_face = AsyncMock(return_value=MagicMock(
                    is_match=True,
                    similarity_score=0.85,
                    threshold=0.6,
                    ktp_face_found=True,
                    selfie_face_found=True,
                    model_dump=lambda: {
                        "is_match": True,
                        "similarity_score": 0.85,
                        "threshold": 0.6,
                        "ktp_face_found": True,
                        "selfie_face_found": True
                    }
                ))
                MockFace.return_value = mock_face_instance

                # Mock Dukcapil client
                mock_dukcapil_instance = AsyncMock()
                mock_dukcapil_instance.verify_nik = AsyncMock(return_value=MagicMock(
                    nik="3201234567890001",
                    is_valid=True,
                    name="JOHN DOE",
                    birth_date="1990-01-01",
                    gender="LAKI-LAKI",
                    status="VALID",
                    match_score=0.95,
                    notes=None,
                    model_dump=lambda: {
                        "nik": "3201234567890001",
                        "is_valid": True,
                        "name": "JOHN DOE",
                        "birth_date": "1990-01-01",
                        "gender": "LAKI-LAKI",
                        "status": "VALID",
                        "match_score": 0.95
                    }
                ))
                MockDukcapil.return_value = mock_dukcapil_instance

                # Mock Kafka producer
                with patch('app.services.kyc_service.KafkaProducerService') as MockKafka:
                    mock_kafka_instance = AsyncMock()
                    mock_kafka_instance.publish_event = AsyncMock()
                    MockKafka.return_value = mock_kafka_instance

                    ktp_response = await client.post(
                        "/api/v1/kyc/verify/ktp",
                        json={
                            "verification_id": verification_id,
                            "ktp_image": sample_ktp_image
                        }
                    )

                    assert ktp_response.status_code == 200
                    ktp_data = ktp_response.json()
                    assert ktp_data["status"] == "PROCESSING"
                    assert "ocr_result" in ktp_data
                    assert ktp_data["ocr_result"]["nik"] == "3201234567890001"

                    # Step 3: Upload selfie image
                    selfie_response = await client.post(
                        "/api/v1/kyc/verify/selfie",
                        json={
                            "verification_id": verification_id,
                            "selfie_image": sample_selfie_image
                        }
                    )

                    assert selfie_response.status_code == 200
                    selfie_data = selfie_response.json()
                    assert selfie_data["status"] == "VERIFIED"
                    assert "liveness_result" in selfie_data
                    assert selfie_data["liveness_result"]["is_live"] == True
                    assert "face_match_result" in selfie_data
                    assert selfie_data["face_match_result"]["is_match"] == True
                    assert "dukcapil_result" in selfie_data
                    assert selfie_data["dukcapil_result"]["is_valid"] == True

    @pytest.mark.asyncio
    async def test_get_kyc_status_after_verification(self, sample_user_id, sample_verification_id):
        """Test getting KYC verification status"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Mock database query
            with patch('app.services.kyc_service.KycService.get_verification') as mock_get:
                mock_entity = MagicMock()
                mock_entity.verification_id = sample_verification_id
                mock_entity.user_id = sample_user_id
                mock_entity.status = "VERIFIED"
                mock_entity.ktp_ocr_result = {
                    "nik": "3201234567890001",
                    "name": "JOHN DOE",
                    "confidence": 0.95
                }
                mock_entity.liveness_result = {
                    "is_live": True,
                    "confidence": 0.85
                }
                mock_entity.face_match_result = {
                    "is_match": True,
                    "similarity_score": 0.85
                }
                mock_entity.dukcapil_result = {
                    "is_valid": True,
                    "name": "JOHN DOE"
                }
                mock_entity.rejection_reason = None
                mock_entity.created_at = "2026-01-20T10:00:00Z"
                mock_entity.completed_at = "2026-01-20T10:05:00Z"

                mock_get.return_value = mock_entity

                response = await client.get(f"/api/v1/kyc/verify/{sample_verification_id}")

                assert response.status_code == 200
                data = response.json()
                assert data["verification_id"] == sample_verification_id
                assert data["user_id"] == sample_user_id
                assert data["status"] == "VERIFIED"
                assert data["ktp_ocr_result"]["nik"] == "3201234567890001"
                assert data["liveness_result"]["is_live"] == True
                assert data["face_match_result"]["is_match"] == True
                assert data["dukcapil_result"]["is_valid"] == True

    @pytest.mark.asyncio
    async def test_kyc_verification_liveness_failure(self, sample_user_id):
        """Test KYC verification when liveness check fails"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Start verification
            start_response = await client.post(
                "/api/v1/kyc/verify/start",
                json={
                    "user_id": sample_user_id,
                    "verification_type": "FULL_KYC"
                }
            )
            assert start_response.status_code == 200
            verification_id = start_response.json()["verification_id"]

            # Mock liveness failure
            with patch('app.services.kyc_service.LivenessService') as MockLiveness:
                mock_liveness_instance = AsyncMock()
                mock_liveness_instance.check_liveness = AsyncMock(return_value=MagicMock(
                    is_live=False,
                    confidence=0.3,
                    face_detected=True,
                    face_quality_score=0.5,
                    model_dump=lambda: {
                        "is_live": False,
                        "confidence": 0.3,
                        "face_detected": True,
                        "face_quality_score": 0.5
                    }
                ))
                MockLiveness.return_value = mock_liveness_instance

                with patch('app.services.kyc_service.KafkaProducerService') as MockKafka:
                    mock_kafka_instance = AsyncMock()
                    mock_kafka_instance.publish_event = AsyncMock()
                    MockKafka.return_value = mock_kafka_instance

                    selfie_response = await client.post(
                        "/api/v1/kyc/verify/selfie",
                        json={
                            "verification_id": verification_id,
                            "selfie_image": "fake_image_data"
                        }
                    )

                    assert selfie_response.status_code == 200
                    data = selfie_response.json()
                    assert data["status"] == "REJECTED"
                    assert data["rejection_reason"] == "Liveness check failed"

    @pytest.mark.asyncio
    async def test_kyc_verification_face_match_failure(self, sample_user_id):
        """Test KYC verification when face matching fails"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Start verification
            start_response = await client.post(
                "/api/v1/kyc/verify/start",
                json={
                    "user_id": sample_user_id,
                    "verification_type": "FULL_KYC"
                }
            )
            assert start_response.status_code == 200
            verification_id = start_response.json()["verification_id"]

            # Mock face match failure
            with patch('app.services.kyc_service.FaceService') as MockFace:
                mock_face_instance = AsyncMock()
                mock_face_instance.match_face = AsyncMock(return_value=MagicMock(
                    is_match=False,
                    similarity_score=0.45,
                    threshold=0.6,
                    ktp_face_found=True,
                    selfie_face_found=True,
                    model_dump=lambda: {
                        "is_match": False,
                        "similarity_score": 0.45,
                        "threshold": 0.6,
                        "ktp_face_found": True,
                        "selfie_face_found": True
                    }
                ))
                MockFace.return_value = mock_face_instance

                # Mock liveness pass
                with patch('app.services.kyc_service.LivenessService') as MockLiveness:
                    mock_liveness_instance = AsyncMock()
                    mock_liveness_instance.check_liveness = AsyncMock(return_value=MagicMock(
                        is_live=True,
                        confidence=0.85,
                        face_detected=True,
                        face_quality_score=0.9,
                        model_dump=lambda: {
                            "is_live": True,
                            "confidence": 0.85,
                            "face_detected": True,
                            "face_quality_score": 0.9
                        }
                    ))
                    MockLiveness.return_value = mock_liveness_instance

                    with patch('app.services.kyc_service.KafkaProducerService') as MockKafka:
                        mock_kafka_instance = AsyncMock()
                        mock_kafka_instance.publish_event = AsyncMock()
                        MockKafka.return_value = mock_kafka_instance

                        selfie_response = await client.post(
                            "/api/v1/kyc/verify/selfie",
                            json={
                                "verification_id": verification_id,
                                "selfie_image": "fake_image_data"
                            }
                        )

                        assert selfie_response.status_code == 200
                        data = selfie_response.json()
                        assert data["status"] == "REJECTED"
                        assert data["rejection_reason"] == "Face matching failed"
