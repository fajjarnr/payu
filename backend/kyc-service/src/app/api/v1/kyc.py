from fastapi import APIRouter, Depends, Header, Request, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.ext.asyncio import AsyncSession
from structlog import get_logger
from typing import Optional
from slowapi.util import get_remote_address

from app.database import get_db_session
from app.models.schemas import (
    StartKycVerificationRequest,
    UploadKtpRequest,
    UploadSelfieRequest,
    GetKycStatusResponse,
)
from app.services.kyc_service import KycService
from app.api.responses import ApiResponse
from app.api.idempotency import get_cached_result, cache_result
from app.config import get_settings

logger = get_logger(__name__)
kyc_router = APIRouter(prefix="/kyc", tags=["KYC Verification"])


@kyc_router.get("/")
async def get_kyc_status():
    """Return KYC service health and available endpoints."""
    return ApiResponse.create_success(
        data={
            "service": "kyc-service",
            "status": "UP",
            "version": "1.0.0",
        })


# BUG-AUTH-022: JWT authentication dependency for KYC endpoints
_bearer_scheme = HTTPBearer(auto_error=False)
_settings = get_settings()


async def require_auth(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer_scheme),
) -> dict:
    """
    BUG-AUTH-022: Validate JWT token from Authorization header.
    All KYC endpoints require authentication.
    """
    import base64
    import json
    import time

    if credentials is None:
        raise HTTPException(status_code=401, detail="Authentication required")

    try:
        token = credentials.credentials
        parts = token.split('.')
        if len(parts) != 3:
            raise HTTPException(status_code=401, detail="Invalid token")

        # Base64url decode the payload
        payload_b64 = parts[1]
        payload_b64 += '=' * (4 - len(payload_b64) % 4)
        payload_json = base64.urlsafe_b64decode(payload_b64.encode('utf-8')).decode('utf-8')
        payload = json.loads(payload_json)

        # Validate expiration if present
        if "exp" in payload and payload["exp"] < time.time():
            raise HTTPException(status_code=401, detail="Token expired")

        return payload
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")


@kyc_router.post("/verify/start")
async def start_kyc_verification(
    request: Request,
    request_data: StartKycVerificationRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key"),
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth),
):
    """
    BUG-SECURITY-017 FIX: Start a new KYC verification with IDOR check.
    Supports idempotency for safe retries.
    Rate limit: 10 requests per minute per IP.
    """
    # Validate ownership
    if request_data.user_id != auth.get("sub"):
         raise HTTPException(status_code=403, detail="Forbidden: You can only start KYC for yourself")
    log = logger.bind(
        user_id=request_data.user_id,
        request_id=getattr(request.state, "request_id", None),
        idempotency_key=idempotency_key,
    )
    log.info("Starting KYC verification")

    # Apply rate limiting
    limiter = request.app.state.limiter
    try:
        await limiter.check(request, get_remote_address(request), "10/minute")
    except Exception:
        return ApiResponse.create_error(
            code="KYC_RAT_001",
            message="Rate limit exceeded. Please try again later.",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

    # Check idempotency cache
    if idempotency_key:
        cached = await get_cached_result(
            idempotency_key=idempotency_key,
            request_path=str(request.url.path),
            request_body=await request.body() if hasattr(request, "body") else None,
        )
        if cached:
            log.info("Returning cached KYC start result")
            return ApiResponse.create_success(
                data=cached, request_id=getattr(request.state, "request_id", None)
            ).model_dump()

    try:
        service = KycService(db)
        verification = await service.create_verification(
            user_id=request_data.user_id,
            verification_type=request_data.verification_type,
        )

        log.info(
            "KYC verification started",
            verification_id=verification.verification_id,
            status=verification.status,
        )

        response_data = {
            "verification_id": verification.verification_id,
            "status": verification.status,
            "message": "Please upload KTP image",
        }

        # Store result for idempotency
        if idempotency_key:
            await cache_result(
                idempotency_key=idempotency_key,
                request_path=str(request.url.path),
                result=response_data,
            )

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to start KYC verification", exc_info=e)
        return ApiResponse.create_error(
            code="KYC_SYS_001",
            message="Failed to start KYC verification",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@kyc_router.post("/verify/ktp")
async def upload_ktp(
    request: Request,
    request_data: UploadKtpRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key"),
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth),
):
    """
    Upload and process KTP image for OCR.
    Supports idempotency for safe retries.
    Rate limit: 5 requests per minute per IP.
    """
    log = logger.bind(
        verification_id=request_data.verification_id,
        request_id=getattr(request.state, "request_id", None),
        idempotency_key=idempotency_key,
    )
    log.info("Processing KTP image upload")

    # Apply rate limiting
    limiter = request.app.state.limiter
    try:
        await limiter.check(request, get_remote_address(request), "5/minute")
    except Exception:
        return ApiResponse.create_error(
            code="KYC_RAT_001",
            message="Rate limit exceeded. Please try again later.",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

    # Check idempotency cache
    if idempotency_key:
        cached = await get_cached_result(
            idempotency_key=idempotency_key,
            request_path=str(request.url.path),
            request_body=await request.body() if hasattr(request, "body") else None,
        )
        if cached:
            log.info("Returning cached KTP upload result")
            return ApiResponse.create_success(
                data=cached, request_id=getattr(request.state, "request_id", None)
            ).model_dump()

    try:
        service = KycService(db)

        result = await service.process_ktp_upload(
            verification_id=request_data.verification_id,
            ktp_image_base64=request_data.ktp_image,
        )

        log.info("KTP OCR completed", status=result.get("status"))

        response_data = {
            "verification_id": request_data.verification_id,
            "status": result.get("status"),
            "ocr_result": result.get("ocr_result"),
            "next_step": "Please upload selfie image",
        }

        # Store result for idempotency
        if idempotency_key:
            await cache_result(
                idempotency_key=idempotency_key,
                request_path=str(request.url.path),
                result=response_data,
            )

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except ValueError as e:
        log.warning("KTP validation failed", error=str(e))
        return ApiResponse.create_error(
            code="KYC_VAL_001",
            message=str(e),
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
    except Exception as e:
        log.error("Failed to process KTP upload", exc_info=e)
        return ApiResponse.create_error(
            code="KYC_SYS_002",
            message="Failed to process KTP upload",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@kyc_router.post("/verify/selfie")
async def upload_selfie(
    request: Request,
    request_data: UploadSelfieRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key"),
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth),
):
    """
    Upload and process selfie image for liveness and face matching.
    Supports idempotency for safe retries.
    Rate limit: 5 requests per minute per IP.
    """
    log = logger.bind(
        verification_id=request_data.verification_id,
        request_id=getattr(request.state, "request_id", None),
        idempotency_key=idempotency_key,
    )
    log.info("Processing selfie image upload")

    # Apply rate limiting
    limiter = request.app.state.limiter
    try:
        await limiter.check(request, get_remote_address(request), "5/minute")
    except Exception:
        return ApiResponse.create_error(
            code="KYC_RAT_001",
            message="Rate limit exceeded. Please try again later.",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

    # Check idempotency cache
    if idempotency_key:
        cached = await get_cached_result(
            idempotency_key=idempotency_key,
            request_path=str(request.url.path),
            request_body=await request.body() if hasattr(request, "body") else None,
        )
        if cached:
            log.info("Returning cached selfie upload result")
            return ApiResponse.create_success(
                data=cached, request_id=getattr(request.state, "request_id", None)
            ).model_dump()

    try:
        service = KycService(db)

        result = await service.process_selfie_upload(
            verification_id=request_data.verification_id,
            selfie_image_base64=request_data.selfie_image,
        )

        log.info("KYC verification completed", status=result.get("status"))

        response_data = {
            "verification_id": request_data.verification_id,
            "status": result.get("status"),
            "liveness_result": result.get("liveness_result"),
            "face_match_result": result.get("face_match_result"),
            "dukcapil_result": result.get("dukcapil_result"),
        }

        # Store result for idempotency
        if idempotency_key:
            await cache_result(
                idempotency_key=idempotency_key,
                request_path=str(request.url.path),
                result=response_data,
            )

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except ValueError as e:
        log.warning("Selfie validation failed", error=str(e))
        return ApiResponse.create_error(
            code="KYC_VAL_002",
            message=str(e),
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
    except Exception as e:
        log.error("Failed to process selfie upload", exc_info=e)
        return ApiResponse.create_error(
            code="KYC_SYS_003",
            message="Failed to process selfie upload",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@kyc_router.get("/verify/{verification_id}")
async def get_kyc_status(
    request: Request, verification_id: str, db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth),
):
    """Get KYC verification status by ID."""
    log = logger.bind(
        verification_id=verification_id,
        request_id=getattr(request.state, "request_id", None),
    )
    log.info("Fetching KYC verification status")

    try:
        service = KycService(db)
        verification = await service.get_verification(verification_id)

        if not verification:
            log.warning("Verification not found")
            return ApiResponse.create_error(
                code="KYC_VAL_003",
                message="Verification not found",
                request_id=getattr(request.state, "request_id", None),
            ).model_dump()

        # BUG-SECURITY-017 FIX: Validate ownership
        if verification.user_id != auth.get("sub"):
            raise HTTPException(status_code=403, detail="Forbidden: You can only access your own KYC status")

        response_data = GetKycStatusResponse(
            verification_id=verification.verification_id,
            user_id=verification.user_id,
            status=verification.status,
            ktp_ocr_result=verification.ktp_ocr_result,
            liveness_result=verification.liveness_result,
            face_match_result=verification.face_match_result,
            dukcapil_result=verification.dukcapil_result,
            rejection_reason=verification.rejection_reason,
            created_at=verification.created_at,
            completed_at=verification.completed_at,
        )

        return ApiResponse.success(
            data=response_data.safe_dump(),
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch verification status", exc_info=e)
        return ApiResponse.create_error(
            code="KYC_SYS_004",
            message="Failed to fetch verification status",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@kyc_router.get("/user/{user_id}")
async def get_user_kyc_history(
    request: Request, user_id: str, db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth),
):
    """BUG-SECURITY-017 FIX: Get KYC verification history with IDOR check."""
    # Validate ownership
    if user_id != auth.get("sub"):
         raise HTTPException(status_code=403, detail="Forbidden: You can only access your own KYC history")
    log = logger.bind(
        user_id=user_id, request_id=getattr(request.state, "request_id", None)
    )
    log.info("Fetching user KYC history")

    try:
        service = KycService(db)
        verifications = await service.get_user_verifications(user_id)

        response_data = [
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
                completed_at=v.completed_at,
            ).safe_dump()
            for v in verifications
        ]

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch user KYC history", exc_info=e)
        return ApiResponse.create_error(
            code="KYC_SYS_005",
            message="Failed to fetch user KYC history",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
