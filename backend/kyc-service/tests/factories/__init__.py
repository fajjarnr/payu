"""
Test Data Factories for KYC Service

This module provides factory functions for generating test data using Faker.
Factories help create realistic test data variations and reduce hardcoded values.

Usage:
    from tests.factories import (
        user_factory,
        ktp_ocr_factory,
        liveness_result_factory,
        face_match_factory,
        kyc_verification_factory
    )

    # Generate random test user
    user = user_factory()

    # Generate with overrides
    custom_user = user_factory(kyc_status="VERIFIED")

    # Generate KTP OCR result
    ktp_data = ktp_ocr_factory()
"""

from .kyc_factory import (
    user_factory,
    ktp_ocr_factory,
    liveness_result_factory,
    face_match_factory,
    dukcapil_result_factory,
    kyc_verification_factory,
    sample_ktp_image_base64,
    sample_selfie_image_base64,
)

__all__ = [
    "user_factory",
    "ktp_ocr_factory",
    "liveness_result_factory",
    "face_match_factory",
    "dukcapil_result_factory",
    "kyc_verification_factory",
    "sample_ktp_image_base64",
    "sample_selfie_image_base64",
]
