"""
QAMVP-016 — KycService unit tests: the upload/verification state machine
(ktp upload, selfie liveness/face/dukcapil gates, rejections, verified path).

Heavy ML deps are stubbed so the service logic is testable without inference.
"""

import base64
import json
import sys
import types
from unittest.mock import AsyncMock, MagicMock, patch

for mod in ("cv2", "stomp", "paddle", "paddleocr", "paddle2onnx", "torch", "torchvision"):
    if mod not in sys.modules:
        sys.modules[mod] = MagicMock()

sys.modules.setdefault("paddle.ocr", MagicMock())
sys.modules.setdefault("paddleocr.ocr", MagicMock())

import pytest  # noqa: E402

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.models.schemas import (  # noqa: E402
    KycStatus,
    KtpOcrResult,
    LivenessCheckResult,
    FaceMatchResult,
    DukcapilVerificationResult,
)
from app.database import KycVerificationEntity  # noqa: E402


def base64_image() -> str:
    return base64.b64encode(b"\x89PNG\r\n\x1a\nfakepixel").decode()


def make_verification(verification_id="v-1", user_id="u-1", status=KycStatus.PENDING.value):
    entity = KycVerificationEntity(
        verification_id=verification_id,
        user_id=user_id,
        verification_type="FULL_KYC",
        status=status,
    )
    return entity


def make_ocr(confidence=0.95):
    return KtpOcrResult(
        nik="3201010101010001",
        name="TEST USER",
        birth_date="01-01-1990",
        gender="LAKI-LAKI",
        address="JAKARTA",
        province="DKI JAKARTA",
        city="JAKARTA",
        district="MENTENG",
        nationality="WNI",
        confidence=confidence,
    )


def make_live(quality=0.9):
    return LivenessCheckResult(
        is_live=True, confidence=0.99, face_detected=True, face_quality_score=quality,
        details={"model": "stub"},
    )


def make_face():
    return FaceMatchResult(
        is_match=True, similarity_score=0.95, threshold=0.8,
        ktp_face_found=True, selfie_face_found=True,
    )


def make_dukcapil():
    return DukcapilVerificationResult(
        nik="3201010101010001", is_valid=True, name="TEST USER",
        birth_date="01-01-1990", gender="LAKI-LAKI", status="VALID",
    )


@pytest.mark.unit
class TestKycService:
    @pytest.fixture
    def mock_session(self):
        session = AsyncMock()
        session.add = MagicMock()
        session.commit = AsyncMock()
        session.refresh = AsyncMock()
        return session

    async def test_create_verification_persists(self, mock_session):
        from app.services.kyc_service import KycService

        service = KycService(mock_session)
        entity = await service.create_verification("u-1", "FULL_KYC")

        assert entity.user_id == "u-1"
        assert entity.status == KycStatus.PENDING.value
        mock_session.add.assert_called_once()
        mock_session.commit.assert_awaited()
        mock_session.refresh.assert_awaited()

    async def test_get_verification_not_found(self, mock_session):
        from app.services.kyc_service import KycService

        result = MagicMock()
        result.scalar_one_or_none.return_value = None
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        assert await service.get_verification("missing") is None

        with pytest.raises(ValueError, match="Verification not found"):
            await service.process_ktp_upload("missing", base64_image(), user_id="u-1")

    async def test_ktp_upload_success(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PENDING.value)
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with patch("app.ml.ocr_service.OcrService") as ocr_cls:
            ocr_cls.return_value.extract_ktp_data = AsyncMock(return_value=make_ocr())
            outcome = await service.process_ktp_upload("v-1", base64_image(), user_id="u-1")

        assert outcome["status"] == KycStatus.PROCESSING.value
        assert verification.status == KycStatus.PROCESSING.value
        assert verification.ktp_image_url == "/uploads/ktp/v-1.jpg"
        assert verification.ktp_image_data == base64.b64decode(base64_image())
        mock_session.commit.assert_awaited()

    async def test_ktp_upload_wrong_status(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.VERIFIED.value)
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with pytest.raises(ValueError, match="Invalid status"):
            await service.process_ktp_upload("v-1", base64_image(), user_id="u-1")

    async def test_ktp_upload_low_confidence_rejects(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PENDING.value)
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with patch("app.ml.ocr_service.OcrService") as ocr_cls:
            ocr_cls.return_value.extract_ktp_data = AsyncMock(return_value=make_ocr(confidence=0.5))
            with pytest.raises(ValueError, match="too low"):
                await service.process_ktp_upload("v-1", base64_image(), user_id="u-1")

        assert verification.status == KycStatus.REJECTED.value
        assert verification.rejection_reason == "KTP OCR confidence too low"

    async def test_upload_wrong_owner_rejected(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PENDING.value)
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with pytest.raises(PermissionError, match="another user"):
            await service.process_ktp_upload("v-1", base64_image(), user_id="attacker")

    async def test_selfie_requires_processing_state(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PENDING.value)
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with pytest.raises(ValueError, match="Invalid status"):
            await service.process_selfie_upload("v-1", base64_image(), user_id="u-1")

    async def test_selfie_requires_ktp_first(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PROCESSING.value)
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with pytest.raises(ValueError, match="KTP not processed"):
            await service.process_selfie_upload("v-1", base64_image(), user_id="u-1")

    async def test_selfie_liveness_fail_rejects(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PROCESSING.value)
        verification.ktp_ocr_result = {"enc": "stub-ktp"}
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with patch.object(service, "liveness_service") as live_mock:
            live_mock.check_liveness = AsyncMock(
                return_value=LivenessCheckResult(
                    is_live=False, confidence=0.1, face_detected=True,
                    face_quality_score=0.2, details={},
                )
            )
            outcome = await service.process_selfie_upload("v-1", base64_image(), user_id="u-1")

        assert outcome["status"] == KycStatus.REJECTED.value
        assert verification.rejection_reason == "Liveness check failed"

    async def test_selfie_face_fail_rejects(self, mock_session):
        from app.services.kyc_service import KycService
        from app.crypto import encrypt_json_nik

        verification = make_verification(status=KycStatus.PROCESSING.value)
        verification.ktp_ocr_result = encrypt_json_nik(make_ocr().model_dump())
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with patch.object(service, "liveness_service") as live_mock, \
             patch.object(service, "face_service") as face_mock:
            live_mock.check_liveness = AsyncMock(return_value=make_live())
            face_mock.match_face = AsyncMock(
                return_value=FaceMatchResult(
                    is_match=False, similarity_score=0.3, threshold=0.8,
                    ktp_face_found=True, selfie_face_found=True,
                )
            )
            outcome = await service.process_selfie_upload("v-1", base64_image(), user_id="u-1")

        assert outcome["status"] == KycStatus.REJECTED.value
        assert verification.rejection_reason == "Face matching failed"

    async def test_selfie_dukcapil_fail_rejects(self, mock_session):
        from app.services.kyc_service import KycService
        from app.crypto import encrypt_json_nik

        verification = make_verification(status=KycStatus.PROCESSING.value)
        verification.ktp_ocr_result = encrypt_json_nik(make_ocr().model_dump())
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with patch.object(service, "liveness_service") as live_mock, \
             patch.object(service, "face_service") as face_mock, \
             patch.object(service, "dukcapil_client") as dukcapil_mock:
            live_mock.check_liveness = AsyncMock(return_value=make_live())
            face_mock.match_face = AsyncMock(return_value=make_face())
            dukcapil_mock.verify_nik = AsyncMock(
                return_value=DukcapilVerificationResult(
                    nik="3201010101010001", is_valid=False, name="TEST USER",
                    birth_date="01-01-1990", gender="LAKI-LAKI", status="INVALID",
                )
            )
            outcome = await service.process_selfie_upload("v-1", base64_image(), user_id="u-1")

        assert outcome["status"] == KycStatus.REJECTED.value
        assert verification.rejection_reason == "NIK verification failed with Dukcapil"

    async def test_selfie_verified_path(self, mock_session):
        from app.services.kyc_service import KycService
        from app.crypto import encrypt_json_nik

        verification = make_verification(status=KycStatus.PROCESSING.value)
        verification.ktp_ocr_result = encrypt_json_nik(make_ocr().model_dump())
        verification.ktp_image_data = b"ktp-bytes"
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with patch.object(service, "liveness_service") as live_mock, \
             patch.object(service, "face_service") as face_mock, \
             patch.object(service, "dukcapil_client") as dukcapil_mock:
            live_mock.check_liveness = AsyncMock(return_value=make_live())
            face_mock.match_face = AsyncMock(return_value=make_face())
            dukcapil_mock.verify_nik = AsyncMock(return_value=make_dukcapil())
            outcome = await service.process_selfie_upload("v-1", base64_image(), user_id="u-1")

        face_mock.match_face.assert_awaited_once_with(
            ktp_image_data=b"ktp-bytes",
            selfie_image_data=base64.b64decode(base64_image()),
        )
        assert outcome["status"] == KycStatus.VERIFIED.value
        assert verification.status == KycStatus.VERIFIED.value
        assert verification.completed_at is not None
        mock_session.commit.assert_awaited()

    async def test_selfie_wrong_owner_rejected(self, mock_session):
        from app.services.kyc_service import KycService

        verification = make_verification(status=KycStatus.PROCESSING.value)
        verification.ktp_ocr_result = {"enc": "stub-ktp"}
        result = MagicMock()
        result.scalar_one_or_none.return_value = verification
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        with pytest.raises(PermissionError, match="another user"):
            await service.process_selfie_upload("v-1", base64_image(), user_id="attacker")

    async def test_get_user_verifications_ordered(self, mock_session):
        from app.services.kyc_service import KycService

        scalars = MagicMock()
        scalars.all.return_value = [make_verification("v-2"), make_verification("v-1")]
        result = MagicMock()
        result.scalars.return_value = scalars
        mock_session.execute.return_value = result

        service = KycService(mock_session)
        rows = await service.get_user_verifications("u-1")
        assert len(rows) == 2
