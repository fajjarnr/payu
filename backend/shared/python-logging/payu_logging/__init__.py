"""
PayU Logging - Standardized logging for PayU Python microservices.

This package provides structured JSON logging compatible with PayU's
Java logging-starter, including OpenTelemetry trace correlation.

Usage:
    from payu_logging import configure_logging, get_logger

    # Configure once at application startup
    configure_logging()

    # Get logger instance
    logger = get_logger(__name__)

    # Log with structured data
    logger.info("Processing request", user_id="123", amount=1000)
"""

from .config import configure_logging, LoggingConfig
from .logger import get_logger
from .middleware import CorrelationIdMiddleware

__version__ = "1.0.0"
__all__ = [
    "configure_logging",
    "LoggingConfig",
    "get_logger",
    "CorrelationIdMiddleware",
]
