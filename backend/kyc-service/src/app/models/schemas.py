from pydantic import BaseModel, Field, field_validator
from typing import Optional
from datetime import datetime
from enum import Enum


def mask_nik(nik: str) -> str:
    """Mask NIK to show only first 4 and last 4 digits.

    Format: 1234********5678
    Compliant with UU PDP (Indonesian Personal Data Protection Law)
    and PCI-DSS requirements for PII masking.
    """
    if not nik or len(nik) < 8:
        return "****"
    return nik[:4] + "*" * (len(nik) - 8) + nik[-4:]


class KycStatus(str, Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    VERIFIED = "VERIFIED"
    REJECTED = "REJECTED"
    FAILED = "FAILED"


class KycVerificationType(str, Enum):
    FULL_KYC = "FULL_KYC"
    BASIC_KYC = "BASIC_KYC"


class KtpOcrResult(BaseModel):
    """
    KTP OCR result with masked NIK for API responses.

    <p><b>Security:</b> NIK is masked in API responses to show only
    first 4 and last 4 digits (e.g., 1234********5678). This prevents
    exposure of full NIK via API responses.</p>
    """

    nik: str = Field(..., description="Nomor Induk Kependudukan")
    name: str = Field(..., description="Nama sesuai KTP")
    birth_date: str = Field(..., description="Tanggal lahir")
    gender: str = Field(..., description="Jenis kelamin")
    address: str = Field(..., description="Alamat")
    province: str = Field(..., description="Provinsi")
    city: str = Field(..., description="Kota/Kabupaten")
    district: str = Field(..., description="Kecamatan")
    religion: Optional[str] = Field(None, description="Agama")
    marital_status: Optional[str] = Field(None, description="Status perkawinan")
    occupation: Optional[str] = Field(None, description="Pekerjaan")
    nationality: str = Field(default="WNI", description="Kewarganegaraan")
    valid_until: Optional[str] = Field(None, description="Berlaku hingga")
    confidence: float = Field(..., description="OCR confidence score")

    @property
    def masked_nik(self) -> str:
        """Returns the masked NIK showing only first 4 and last 4 digits."""
        return mask_nik(self.nik)

    @field_validator("nik")
    def validate_nik(cls, v):
        if len(v) != 16 or not v.isdigit():
            raise ValueError("NIK must be 16 digits")
        return v

    def safe_dump(self) -> dict:
        """Serialize with NIK masked for API responses.

        Use this instead of model_dump() when returning data to clients.
        model_dump() should only be used for internal/DB storage.
        """
        data = self.model_dump()
        data["nik"] = mask_nik(self.nik)
        return data


class LivenessCheckResult(BaseModel):
    is_live: bool = Field(..., description="Whether the selfie is live")
    confidence: float = Field(..., description="Liveness confidence score")
    face_detected: bool = Field(..., description="Face detected in image")
    face_quality_score: float = Field(..., description="Face image quality score")
    details: dict = Field(
        default_factory=dict, description="Additional liveness details"
    )


class FaceMatchResult(BaseModel):
    is_match: bool = Field(..., description="Whether KTP and selfie match")
    similarity_score: float = Field(..., description="Face similarity score")
    threshold: float = Field(..., description="Matching threshold used")
    ktp_face_found: bool = Field(..., description="Face detected on KTP")
    selfie_face_found: bool = Field(..., description="Face detected on selfie")


class DukcapilVerificationResult(BaseModel):
    """
    Dukcapil verification result with masked NIK.

    <p><b>Security:</b> NIK is masked in API responses to comply with
    Indonesian UU PDP (Personal Data Protection Law).</p>
    """

    nik: str
    is_valid: bool
    name: str
    birth_date: str
    gender: str
    status: str
    match_score: Optional[float] = None
    notes: Optional[str] = None

    @property
    def masked_nik(self) -> str:
        """Returns the masked NIK showing only first 4 and last 4 digits."""
        return mask_nik(self.nik)

    def safe_dump(self) -> dict:
        """Serialize with NIK masked for API responses.

        Use this instead of model_dump() when returning data to clients.
        model_dump() should only be used for internal/DB storage.
        """
        data = self.model_dump()
        data["nik"] = mask_nik(self.nik)
        return data


class KycVerification(BaseModel):
    user_id: str = Field(..., description="User ID")
    verification_type: KycVerificationType = Field(default=KycVerificationType.FULL_KYC)
    status: KycStatus = Field(default=KycStatus.PENDING)
    ktp_image_url: Optional[str] = None
    selfie_image_url: Optional[str] = None
    ktp_ocr_result: Optional[KtpOcrResult] = None
    liveness_result: Optional[LivenessCheckResult] = None
    face_match_result: Optional[FaceMatchResult] = None
    dukcapil_result: Optional[DukcapilVerificationResult] = None
    rejection_reason: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    completed_at: Optional[datetime] = None


class StartKycVerificationRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    verification_type: KycVerificationType = Field(default=KycVerificationType.FULL_KYC)


class UploadKtpRequest(BaseModel):
    verification_id: str = Field(..., description="KYC verification ID")
    ktp_image: str = Field(..., description="Base64 encoded KTP image")


class UploadSelfieRequest(BaseModel):
    verification_id: str = Field(..., description="KYC verification ID")
    selfie_image: str = Field(..., description="Base64 encoded selfie image")


class GetKycStatusResponse(BaseModel):
    verification_id: str
    user_id: str
    status: KycStatus
    ktp_ocr_result: Optional[KtpOcrResult] = None
    liveness_result: Optional[LivenessCheckResult] = None
    face_match_result: Optional[FaceMatchResult] = None
    dukcapil_result: Optional[DukcapilVerificationResult] = None
    rejection_reason: Optional[str] = None
    created_at: datetime
    completed_at: Optional[datetime] = None

    def safe_dump(self) -> dict:
        """Serialize with all NIK fields masked for API responses."""
        data = self.model_dump()
        if self.ktp_ocr_result:
            data["ktp_ocr_result"] = self.ktp_ocr_result.safe_dump()
        if self.dukcapil_result:
            data["dukcapil_result"] = self.dukcapil_result.safe_dump()
        return data


class ErrorResponse(BaseModel):
    detail: str
    error_code: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
