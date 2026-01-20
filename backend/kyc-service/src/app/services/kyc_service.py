from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from uuid import uuid4
from datetime import datetime
from base64 import b64decode
from structlog import get_logger

from app.database import KycVerificationEntity
from app.models.schemas import KycVerification, KycStatus, KtpOcrResult, LivenessCheckResult, FaceMatchResult, DukcapilVerificationResult
from app.ml.ocr_service import OcrService
from app.ml.liveness_service import LivenessService
from app.ml.face_service import FaceService
from app.adapters.dukcapil_client import DukcapilClient
from app.messaging.kafka_producer import KafkaProducerService

logger = get_logger(__name__)


class KycService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.ocr_service = OcrService()
        self.liveness_service = LivenessService()
        self.face_service = FaceService()
        self.dukcapil_client = DukcapilClient()
        self.kafka_producer = KafkaProducerService()

    async def create_verification(
        self,
        user_id: str,
        verification_type: str = "FULL_KYC"
    ) -> KycVerificationEntity:
        verification_id = str(uuid4())

        entity = KycVerificationEntity(
            verification_id=verification_id,
            user_id=user_id,
            verification_type=verification_type,
            status=KycStatus.PENDING.value,
            created_at=datetime.utcnow()
        )

        self.db.add(entity)
        await self.db.commit()
        await self.db.refresh(entity)

        logger.info("KYC verification created", verification_id=verification_id, user_id=user_id)

        return entity

    async def process_ktp_upload(
        self,
        verification_id: str,
        ktp_image_base64: str
    ) -> dict:
        verification = await self._get_verification(verification_id)

        if verification.status != KycStatus.PENDING.value:
            raise ValueError(f"Invalid status: {verification.status}")

        image_data = b64decode(ktp_image_base64)

        ocr_result = await self.ocr_service.extract_ktp_data(image_data)

        if ocr_result.confidence < 0.7:
            await self._reject_verification(
                verification_id,
                "KTP OCR confidence too low"
            )
            raise ValueError("KTP image quality too low. Please upload a clearer image.")

        verification.ktp_image_url = f"/uploads/ktp/{verification_id}.jpg"
        verification.ktp_ocr_result = ocr_result.model_dump()
        verification.status = KycStatus.PROCESSING.value
        verification.updated_at = datetime.utcnow()

        await self.db.commit()

        await self.kafka_producer.publish_event(
            topic="payu.kyc.ktp_uploaded",
            event={
                "verification_id": verification_id,
                "user_id": verification.user_id,
                "nik": ocr_result.nik,
                "name": ocr_result.name
            }
        )

        return {
            "status": verification.status,
            "ocr_result": ocr_result.model_dump()
        }

    async def process_selfie_upload(
        self,
        verification_id: str,
        selfie_image_base64: str
    ) -> dict:
        verification = await self._get_verification(verification_id)

        if verification.status != KycStatus.PROCESSING.value:
            raise ValueError(f"Invalid status: {verification.status}")

        if not verification.ktp_ocr_result:
            raise ValueError("KTP not processed. Please upload KTP first.")

        selfie_image_data = b64decode(selfie_image_base64)

        liveness_result = await self.liveness_service.check_liveness(selfie_image_data)

        if not liveness_result.is_live:
            await self._reject_verification(
                verification_id,
                "Liveness check failed"
            )
            return {
                "status": KycStatus.REJECTED.value,
                "rejection_reason": "Liveness check failed",
                "liveness_result": liveness_result.model_dump()
            }

        ocr_data = KtpOcrResult(**verification.ktp_ocr_result)

        face_match_result = await self.face_service.match_face(
            ktp_image_path=verification.ktp_image_url,
            selfie_image_data=selfie_image_data
        )

        if not face_match_result.is_match:
            await self._reject_verification(
                verification_id,
                "Face matching failed"
            )
            return {
                "status": KycStatus.REJECTED.value,
                "rejection_reason": "Face matching failed",
                "liveness_result": liveness_result.model_dump(),
                "face_match_result": face_match_result.model_dump()
            }

        dukcapil_result = await self.dukcapil_client.verify_nik(ocr_data.nik)

        if not dukcapil_result.is_valid:
            await self._reject_verification(
                verification_id,
                "NIK verification failed with Dukcapil"
            )
            return {
                "status": KycStatus.REJECTED.value,
                "rejection_reason": "NIK verification failed",
                "liveness_result": liveness_result.model_dump(),
                "face_match_result": face_match_result.model_dump(),
                "dukcapil_result": dukcapil_result.model_dump()
            }

        verification.selfie_image_url = f"/uploads/selfie/{verification_id}.jpg"
        verification.liveness_result = liveness_result.model_dump()
        verification.face_match_result = face_match_result.model_dump()
        verification.dukcapil_result = dukcapil_result.model_dump()
        verification.status = KycStatus.VERIFIED.value
        verification.completed_at = datetime.utcnow()
        verification.updated_at = datetime.utcnow()

        await self.db.commit()

        await self.kafka_producer.publish_event(
            topic="payu.kyc.verified",
            event={
                "verification_id": verification_id,
                "user_id": verification.user_id,
                "nik": ocr_data.nik,
                "name": ocr_data.name,
                "status": "VERIFIED"
            }
        )

        logger.info(
            "KYC verification completed successfully",
            verification_id=verification_id,
            user_id=verification.user_id
        )

        return {
            "status": KycStatus.VERIFIED.value,
            "liveness_result": liveness_result.model_dump(),
            "face_match_result": face_match_result.model_dump(),
            "dukcapil_result": dukcapil_result.model_dump()
        }

    async def get_verification(self, verification_id: str) -> KycVerificationEntity | None:
        result = await self.db.execute(
            select(KycVerificationEntity).where(
                KycVerificationEntity.verification_id == verification_id
            )
        )
        return result.scalar_one_or_none()

    async def get_user_verifications(self, user_id: str) -> list[KycVerificationEntity]:
        result = await self.db.execute(
            select(KycVerificationEntity)
            .where(KycVerificationEntity.user_id == user_id)
            .order_by(KycVerificationEntity.created_at.desc())
        )
        return result.scalars().all()

    async def _get_verification(self, verification_id: str) -> KycVerificationEntity:
        verification = await self.get_verification(verification_id)
        if not verification:
            raise ValueError("Verification not found")
        return verification

    async def _reject_verification(self, verification_id: str, reason: str):
        verification = await self._get_verification(verification_id)
        verification.status = KycStatus.REJECTED.value
        verification.rejection_reason = reason
        verification.completed_at = datetime.utcnow()
        verification.updated_at = datetime.utcnow()

        await self.db.commit()

        await self.kafka_producer.publish_event(
            topic="payu.kyc.failed",
            event={
                "verification_id": verification_id,
                "user_id": verification.user_id,
                "status": "REJECTED",
                "reason": reason
            }
        )

        logger.warning(
            "KYC verification rejected",
            verification_id=verification_id,
            reason=reason
        )
