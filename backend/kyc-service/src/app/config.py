from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
import os


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        protected_namespaces=("settings_",),
    )

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

    # Artemis / STOMP
    artemis_host: str = os.getenv("ARTEMIS_HOST", "localhost")
    artemis_stomp_port: int = int(os.getenv("ARTEMIS_STOMP_PORT", "61613"))
    artemis_username: str = os.getenv("ARTEMIS_USERNAME", "admin")
    artemis_password: str = os.getenv("ARTEMIS_PASSWORD", "admin")
    artemis_kyc_queue: str = "payu.kyc.commands"
    artemis_heartbeat_send_ms: int = int(os.getenv("ARTEMIS_HEARTBEAT_SEND_MS", "30000"))
    artemis_heartbeat_receive_ms: int = int(os.getenv("ARTEMIS_HEARTBEAT_RECEIVE_MS", "30000"))

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
    # BUG-AUTH-034: secret_key and algorithm configurable via environment.
    # Prefer RS256 with public key in production; HS256 acceptable for
    # gateway-validated tokens where KYC service only verifies the signature.
    secret_key: str = os.getenv("SECRET_KEY", "")
    algorithm: str = os.getenv("JWT_ALGORITHM", "HS256")
    access_token_expire_minutes: int = 30

    # Logging
    log_level: str = "INFO"
    log_format: str = "json"

    # Monitoring
    enable_metrics: bool = True
    enable_tracing: bool = True
    otlp_endpoint: str = os.getenv("OTLP_ENDPOINT", "http://localhost:4317")


@lru_cache()
def get_settings() -> Settings:
    settings = Settings()
    if not settings.secret_key:
        raise ValueError("SECRET_KEY environment variable must be set. Never run without it.")
    return settings
