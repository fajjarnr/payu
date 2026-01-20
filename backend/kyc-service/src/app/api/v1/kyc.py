from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from structlog import get_logger
from typing import List

from app.database import get_db_session
from app.models.schemas import (
    StartKycVerificationRequest,
    UploadKtpRequest,
    UploadSelfieRequest,
    GetKycStatusResponse,
    ErrorResponse
)
from app.services.kyc_service import KycService

logger = get_logger(__name__)
kyc_router = APIRouter(prefix="/kyc", tags=["KYC Verification"])


@kyc_router.post("/verify/start", response_model=dict)
async def start_kyc_verification(
    request: StartKycVerificationRequest,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=request.user_id)
    log.info("Starting KYC verification")

    try:
        service = KycService(db)
        verification = await service.create_verification(
            user_id=request.user_id,
            verification_type=request.verification_type
        )

        log.info(
            "KYC verification started",
            verification_id=verification.verification_id,
            status=verification.status
        )

        return {
            "verification_id": verification.verification_id,
            "status": verification.status,
            "message": "Please upload KTP image"
        }
    except Exception as e:
        log.error("Failed to start KYC verification", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "KYC_SYS_001", "detail": str(e)}
        )


@kyc_router.post("/verify/ktp", response_model=dict)
async def upload_ktp(
    request: UploadKtpRequest,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(verification_id=request.verification_id)
    log.info("Processing KTP image upload")

    try:
        service = KycService(db)

        result = await service.process_ktp_upload(
            verification_id=request.verification_id,
            ktp_image_base64=request.ktp_image
        )

        log.info("KTP OCR completed", result=result)

        return {
            "verification_id": request.verification_id,
            "status": result.get("status"),
            "ocr_result": result.get("ocr_result"),
            "next_step": "Please upload selfie image"
        }
    except ValueError as e:
        log.warning("KTP validation failed", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error_code": "KYC_VAL_001", "detail": str(e)}
        )
    except Exception as e:
        log.error("Failed to process KTP upload", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "KYC_SYS_002", "detail": str(e)}
        )


@kyc_router.post("/verify/selfie", response_model=dict)
async def upload_selfie(
    request: UploadSelfieRequest,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(verification_id=request.verification_id)
    log.info("Processing selfie image upload")

    try:
        service = KycService(db)

        result = await service.process_selfie_upload(
            verification_id=request.verification_id,
            selfie_image_base64=request.selfie_image
        )

        log.info("KYC verification completed", result=result)

        return {
            "verification_id": request.verification_id,
            "status": result.get("status"),
            "liveness_result": result.get("liveness_result"),
            "face_match_result": result.get("face_match_result"),
            "dukcapil_result": result.get("dukcapil_result")
        }
    except ValueError as e:
        log.warning("Selfie validation failed", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error_code": "KYC_VAL_002", "detail": str(e)}
        )
    except Exception as e:
        log.error("Failed to process selfie upload", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "KYC_SYS_003", "detail": str(e)}
        )


@kyc_router.get("/verify/{verification_id}", response_model=GetKycStatusResponse)
async def get_kyc_status(
    verification_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(verification_id=verification_id)
    log.info("Fetching KYC verification status")

    try:
        service = KycService(db)
        verification = await service.get_verification(verification_id)

        if not verification:
            log.warning("Verification not found")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error_code": "KYC_VAL_003", "detail": "Verification not found"}
            )

        return GetKycStatusResponse(
            verification_id=verification.verification_id,
            user_id=verification.user_id,
            status=verification.status,
            ktp_ocr_result=verification.ktp_ocr_result,
            liveness_result=verification.liveness_result,
            face_match_result=verification.face_match_result,
            dukcapil_result=verification.dukcapil_result,
            rejection_reason=verification.rejection_reason,
            created_at=verification.created_at,
            completed_at=verification.completed_at
        )
    except HTTPException:
        raise
    except Exception as e:
        log.error("Failed to fetch verification status", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "KYC_SYS_004", "detail": str(e)}
        )


@kyc_router.get("/user/{user_id}", response_model=List[GetKycStatusResponse])
async def get_user_kyc_history(
    user_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=user_id)
    log.info("Fetching user KYC history")

    try:
        service = KycService(db)
        verifications = await service.get_user_verifications(user_id)

        return [
            GetKycStatusResponse(
                verification_id=v.verification_id,
                user_id=v.user_id,
                status=v.status,
                ktp_ocr_result=v.ktp_ocr_result,
                liveness_result=v.liveness_result,
                face_match_result=v.face_match_result,
                dukcapil_result=v.dukcapil_result,
                rejection_reason=v.rejection_reason,
                created_at=v.created_at,
                completed_at=v.completed_at
            )
            for v in verifications
        ]
    except Exception as e:
        log.error("Failed to fetch user KYC history", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "KYC_SYS_005", "detail": str(e)}
        )
