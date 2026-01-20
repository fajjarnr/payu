from pydantic_settings import BaseSettings
from functools import lru_cache
import os


class Settings(BaseSettings):
    application_name: str = "PayU KYC Service"
    version: str = "1.0.0"

    # Server
    host: str = "0.0.0.0"
    port: int = 8007

    # Database
    database_url: str = os.getenv("DATABASE_URL", "postgresql+asyncpg://payu:payu@localhost:5432/payu_kyc")

    # Kafka
    kafka_bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    kafka_topic_kyc_verified: str = "payu.kyc.verified"
    kafka_topic_kyc_failed: str = "payu.kyc.failed"

    # Dukcapil Simulator
    dukcapil_url: str = os.getenv("DUKCAPIL_URL", "http://localhost:8091/api/v1")

    # ML Models
    ocr_model_path: str = "/app/models/ocr"
    face_model_path: str = "/app/models/face"
    liveness_model_path: str = "/app/models/liveness"

    # OCR Configuration
    ocr_confidence_threshold: float = 0.8
    ocr_language: str = "en"  # Indonesian OCR support

    # Face Recognition
    face_matching_threshold: float = 0.6  # Lower is stricter

    # Liveness Detection
    liveness_threshold: float = 0.7
    liveness_min_frames: int = 3

    # File Upload
    max_upload_size: int = 10 * 1024 * 1024  # 10MB
    allowed_extensions: set = {".jpg", ".jpeg", ".png"}

    # Security
    secret_key: str = os.getenv("SECRET_KEY", "change-me-in-production")
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30

    # Logging
    log_level: str = "INFO"
    log_format: str = "json"

    # Monitoring
    enable_metrics: bool = True
    enable_tracing: bool = True
    otlp_endpoint: str = os.getenv("OTLP_ENDPOINT", "http://localhost:4317")

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()
