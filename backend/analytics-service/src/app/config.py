from pydantic_settings import BaseSettings
from functools import lru_cache
import os


class Settings(BaseSettings):
    application_name: str = "PayU Analytics Service"
    version: str = "1.0.0"

    # Server
    host: str = "0.0.0.0"
    port: int = 8008

    # Database (TimescaleDB)
    database_url: str = os.getenv("DATABASE_URL", "postgresql+asyncpg://payu:payu@localhost:5432/payu_analytics")

    # Kafka
    kafka_bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    kafka_consumer_group: str = "analytics-service-group"
    kafka_topics: list[str] = [
        "payu.wallet.balance.changed",
        "payu.transactions.initiated",
        "payu.transactions.completed",
        "payu.transactions.failed",
        "payu.kyc.verified"
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
