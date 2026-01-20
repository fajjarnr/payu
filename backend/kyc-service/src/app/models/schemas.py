from pydantic import BaseModel, Field, field_validator
from typing import Optional, List
from datetime import datetime
from enum import Enum


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

    @field_validator("nik")
    def validate_nik(cls, v):
        if len(v) != 16 or not v.isdigit():
            raise ValueError("NIK must be 16 digits")
        return v


class LivenessCheckResult(BaseModel):
    is_live: bool = Field(..., description="Whether the selfie is live")
    confidence: float = Field(..., description="Liveness confidence score")
    face_detected: bool = Field(..., description="Face detected in image")
    face_quality_score: float = Field(..., description="Face image quality score")
    details: dict = Field(default_factory=dict, description="Additional liveness details")


class FaceMatchResult(BaseModel):
    is_match: bool = Field(..., description="Whether KTP and selfie match")
    similarity_score: float = Field(..., description="Face similarity score")
    threshold: float = Field(..., description="Matching threshold used")
    ktp_face_found: bool = Field(..., description="Face detected on KTP")
    selfie_face_found: bool = Field(..., description="Face detected on selfie")


class DukcapilVerificationResult(BaseModel):
    nik: str
    is_valid: bool
    name: str
    birth_date: str
    gender: str
    status: str
    match_score: Optional[float] = None
    notes: Optional[str] = None


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


class ErrorResponse(BaseModel):
    detail: str
    error_code: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
