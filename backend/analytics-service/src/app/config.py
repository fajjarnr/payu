from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
import os


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        protected_namespaces=("settings_",),
    )

    application_name: str = "PayU Analytics Service"
    version: str = "1.0.0"

    # Server
    host: str = "0.0.0.0"
    port: int = 8008

    # Database (TimescaleDB)
    database_url: str = os.getenv("DATABASE_URL", "postgresql+asyncpg://payu:${DB_PASSWORD}@localhost:5432/payu_analytics")

    # Kafka
    kafka_bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    kafka_consumer_group: str = "analytics-service-group"
    kafka_topics: list[str] = [
        # ANA-TOPIC-001: standard payu.<domain>.<event-type>.v<n> names.
        "payu.wallet.balance-changed.v1",
        "payu.transaction.initiated.v1",
        "payu.transaction.completed.v1",
        "payu.transaction.failed.v1",
        "payu.kyc.verified.v1"
    ]

    # TimescaleDB Configuration
    timescale_hypertable_retention_days: int = 365
    timescale_chunk_interval_days: int = 7

    # Analytics Configuration
    analytics_aggregation_window_hours: int = 24
    analytics_cache_ttl_seconds: int = 300

    # ML Configuration
    model_retrain_interval_hours: int = 24
    recommendation_batch_size: int = 100

    # Security
    secret_key: str = os.getenv("SECRET_KEY", "")
    algorithm: str = "HS256"
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
