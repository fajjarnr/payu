"""
KYC Test Data Factory

Factory functions for generating KYC-related test data using Faker.
Provides realistic variations for user data, KTP information, and verification results.

Patterns:
- Use Faker for realistic data (names, addresses, dates)
- Support kwargs for overrides
- Return Pydantic models when applicable
- Use Indonesian locale for localized data (names, addresses)
"""

import random
from datetime import datetime
from typing import Dict, Any
from faker import Faker

# Indonesian locale for realistic Indonesian names and addresses
fake = Faker("id_ID")


def user_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate test user data with realistic Indonesian information.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with user data including:
            - user_id: Unique identifier
            - email: Valid email address
            - phone: Indonesian phone number
            - full_name: Indonesian name
            - kyc_status: One of the KYC status values

    Example:
        >>> user = user_factory()
        >>> user['kyc_status']
        'PENDING'
        >>> custom_user = user_factory(kyc_status="VERIFIED")
    """
    defaults = {
        "user_id": f"user_{fake.uuid4()[:8]}",
        "email": fake.email(),
        "phone": f"+62{fake.msisdn()[3:]}",  # Indonesian format
        "full_name": fake.name().upper(),
        "kyc_status": random.choice(
            ["PENDING", "PROCESSING", "VERIFIED", "REJECTED", "FAILED"]
        ),
        "account_created_at": fake.date_time_between(start_date="-2y", end_date="now"),
        "date_of_birth": fake.date_of_birth(minimum_age=17, maximum_age=80),
    }
    defaults.update(kwargs)
    return defaults


def ktp_ocr_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate KTP OCR result data with realistic Indonesian KTP information.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with KTP data matching KtpOcrResult schema:
            - nik: 16-digit Indonesian NIK
            - name: Full name in uppercase
            - birth_date: Date in DD-MM-YYYY format
            - gender: LAKI-LAKI or PEREMPUAN
            - address: Street address
            - province: Indonesian province
            - city: Indonesian city/kabupaten
            - district: Indonesian kecamatan
            - confidence: OCR confidence score (0.0 to 1.0)

    Example:
        >>> ktp = ktp_ocr_factory()
        >>> ktp['nik']
        '3201012345678901'
        >>> len(ktp['nik'])
        16
    """
    # Generate realistic 16-digit NIK (Indonesian ID number)
    # Format: [Province Code(2)][Regency Code(2)][District Code(2)][Sequence(8)][Checksum(2)]
    province_code = fake.random_int(min=11, max=64)  # Valid province codes
    regency_code = fake.random_int(min=1, max=99)
    district_code = fake.random_int(min=1, max=99)
    sequence = fake.random_int(min=1, max=99999999)
    checksum = fake.random_int(min=1, max=99)

    nik = f"{province_code:02d}{regency_code:02d}{district_code:02d}{sequence:08d}{checksum:02d}"

    # Generate birth date in DD-MM-YYYY format (standard KTP format)
    birth_date_obj = fake.date_of_birth(minimum_age=17, maximum_age=80)
    birth_date = birth_date_obj.strftime("%d-%m-%Y")

    defaults = {
        "nik": nik,
        "name": fake.name().upper(),
        "birth_date": birth_date,
        "gender": random.choice(["LAKI-LAKI", "PEREMPUAN"]),
        "address": f"JL. {fake.street_name().upper()} NO. {fake.random_int(min=1, max=200)}",
        "province": random.choice(
            [
                "DKI JAKARTA",
                "JAWA BARAT",
                "JAWA TENGAH",
                "JAWA TIMUR",
                "BANTEN",
                "DAERAH ISTIMEWA YOGYAKARTA",
                "SUMATERA UTARA",
                "SUMATERA BARAT",
                "RIAU",
                "LAMPUNG",
                "BALI",
                "SULAWESI SELATAN",
            ]
        ),
        "city": fake.city().upper(),
        "district": f"KEC. {fake.street_name().upper()}",
        "religion": random.choice(
            ["ISLAM", "KRISTEN", "KATOLIK", "HINDU", "BUDDHA", "KONGHUCU"]
        ),
        "marital_status": random.choice(
            ["BELUM KAWIN", "KAWIN", "CERAI HIDUP", "CERAI MATI"]
        ),
        "occupation": random.choice(
            [
                "KARYAWAN SWASTA",
                "PEGAWAI NEGERI",
                "WIRASWASTA",
                "PELAJAR/MAHASISWA",
                "IBU RUMAH TANGGA",
                "PENSIUNAN",
                "PETANI",
                "BURUH",
                "PEDAGANG",
            ]
        ),
        "nationality": "WNI",
        "valid_until": random.choice(
            [
                "SEUMUR HIDUP",
                fake.date_time_between(start_date="+1y", end_date="+10y").strftime(
                    "%d-%m-%Y"
                ),
            ]
        ),
        "confidence": fake.pyfloat(
            left_digits=1, right_digits=2, positive=True, min_value=0.80, max_value=1.0
        ),
    }
    defaults.update(kwargs)
    return defaults


def liveness_result_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate liveness check result data.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with liveness data matching LivenessCheckResult schema:
            - is_live: Whether the selfie is live
            - confidence: Liveness confidence score (0.0 to 1.0)
            - face_detected: Whether face was detected
            - face_quality_score: Face image quality score (0.0 to 1.0)
            - details: Additional liveness details

    Example:
        >>> result = liveness_result_factory(is_live=True)
        >>> result['is_live']
        True
    """
    is_live = kwargs.get(
        "is_live", random.choice([True, True, True, False])
    )  # 75% pass rate

    defaults = {
        "is_live": is_live,
        "confidence": fake.pyfloat(
            left_digits=1, right_digits=2, positive=True, min_value=0.70, max_value=1.0
        ),
        "face_detected": is_live,
        "face_quality_score": fake.pyfloat(
            left_digits=1, right_digits=2, positive=True, min_value=0.60, max_value=1.0
        ),
        "details": {
            "blur_score": fake.pyfloat(
                left_digits=1,
                right_digits=2,
                positive=False,
                min_value=0.0,
                max_value=1.0,
            ),
            "brightness_score": fake.pyfloat(
                left_digits=1,
                right_digits=2,
                positive=True,
                min_value=0.1,
                max_value=1.0,
            ),
            "eye_blink_detected": random.choice([True, False]),
        },
    }
    defaults.update(kwargs)
    return defaults


def face_match_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate face match result data between KTP and selfie.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with face match data matching FaceMatchResult schema:
            - is_match: Whether KTP and selfie faces match
            - similarity_score: Face similarity score (0.0 to 1.0)
            - threshold: Matching threshold used
            - ktp_face_found: Whether face was detected on KTP
            - selfie_face_found: Whether face was detected on selfie

    Example:
        >>> match = face_match_factory(is_match=True)
        >>> match['similarity_score']
        0.85
    """
    is_match = kwargs.get(
        "is_match", random.choice([True, True, False])
    )  # 66% match rate
    similarity_score = fake.pyfloat(
        left_digits=1,
        right_digits=2,
        positive=True,
        min_value=0.85 if is_match else 0.3,
        max_value=1.0 if is_match else 0.6,
    )

    defaults = {
        "is_match": is_match,
        "similarity_score": similarity_score,
        "threshold": 0.75,  # Standard face recognition threshold
        "ktp_face_found": random.choice(
            [True, True, True, False]
        ),  # 75% detection rate
        "selfie_face_found": random.choice([True, True, True, False]),
    }
    defaults.update(kwargs)
    return defaults


def dukcapil_result_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate Dukcapil (Indonesian civil registry) verification result.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with Dukcapil verification data matching DukcapilVerificationResult schema:
            - nik: 16-digit NIK
            - is_valid: Whether NIK is valid in Dukcapil database
            - name: Name from Dukcapil
            - birth_date: Birth date from Dukcapil
            - gender: Gender from Dukcapil
            - status: Verification status
            - match_score: Optional match score with uploaded KTP
            - notes: Optional verification notes

    Example:
        >>> result = dukcapil_result_factory(is_valid=True)
        >>> result['status']
        'VALID'
    """
    is_valid = kwargs.get(
        "is_valid", random.choice([True, True, True, False])
    )  # 75% valid rate

    birth_date_obj = fake.date_of_birth(minimum_age=17, maximum_age=80)

    defaults = {
        "nik": ktp_ocr_factory()["nik"],
        "is_valid": is_valid,
        "name": fake.name().upper(),
        "birth_date": birth_date_obj.strftime("%d-%m-%Y"),
        "gender": random.choice(["LAKI-LAKI", "PEREMPUAN"]),
        "status": (
            "VALID" if is_valid else random.choice(["INVALID", "NOT_FOUND", "DECEASED"])
        ),
        "match_score": (
            fake.pyfloat(
                left_digits=1,
                right_digits=2,
                positive=True,
                min_value=0.85 if is_valid else 0.0,
                max_value=1.0 if is_valid else 0.5,
            )
            if is_valid
            else None
        ),
        "notes": fake.sentence() if not is_valid else None,
    }
    defaults.update(kwargs)
    return defaults


def kyc_verification_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate complete KYC verification data with all components.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with complete KYC verification data matching KycVerification schema:
            - user_id: User identifier
            - verification_type: FULL_KYC or BASIC_KYC
            - status: Current verification status
            - ktp_image_url: Optional KTP image URL
            - selfie_image_url: Optional selfie image URL
            - ktp_ocr_result: Optional KTP OCR result
            - liveness_result: Optional liveness check result
            - face_match_result: Optional face match result
            - dukcapil_result: Optional Dukcapil verification result
            - rejection_reason: Optional rejection reason
            - created_at: Creation timestamp
            - updated_at: Last update timestamp
            - completed_at: Optional completion timestamp

    Example:
        >>> kyc = kyc_verification_factory(status="VERIFIED")
        >>> kyc['status']
        'VERIFIED'
    """
    status = kwargs.get(
        "status",
        random.choice(["PENDING", "PROCESSING", "VERIFIED", "REJECTED", "FAILED"]),
    )
    is_completed = status in ["VERIFIED", "REJECTED", "FAILED"]

    now = datetime.utcnow()
    created_at = kwargs.get(
        "created_at", fake.date_time_between(start_date="-30d", end_date="now")
    )

    defaults = {
        "user_id": user_factory()["user_id"],
        "verification_type": random.choice(["FULL_KYC", "BASIC_KYC"]),
        "status": status,
        "ktp_image_url": fake.url() if random.choice([True, False]) else None,
        "selfie_image_url": fake.url() if random.choice([True, False]) else None,
        "ktp_ocr_result": (
            ktp_ocr_factory() if random.choice([True, True, False]) else None
        ),
        "liveness_result": (
            liveness_result_factory() if random.choice([True, True, False]) else None
        ),
        "face_match_result": (
            face_match_factory() if random.choice([True, True, False]) else None
        ),
        "dukcapil_result": (
            dukcapil_result_factory() if random.choice([True, True, False]) else None
        ),
        "rejection_reason": fake.sentence() if status == "REJECTED" else None,
        "created_at": created_at,
        "updated_at": now if is_completed else created_at,
        "completed_at": now if is_completed else None,
    }
    defaults.update(kwargs)
    return defaults


def sample_ktp_image_base64() -> str:
    """
    Generate a sample base64-encoded KTP image.

    Returns:
        A minimal valid 1x1 pixel PNG image in base64 encoding.
        This is useful for testing image upload endpoints without needing actual images.

    Example:
        >>> img = sample_ktp_image_base64()
        >>> len(img)
        68
    """
    # Minimal 1x1 PNG image in base64
    return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="


def sample_selfie_image_base64() -> str:
    """
    Generate a sample base64-encoded selfie image.

    Returns:
        A minimal valid 1x1 pixel PNG image in base64 encoding.
        This is useful for testing selfie upload endpoints.

    Example:
        >>> img = sample_selfie_image_base64()
        >>> len(img)
        68
    """
    # Minimal 1x1 PNG image in base64 (same as KTP for testing)
    return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
