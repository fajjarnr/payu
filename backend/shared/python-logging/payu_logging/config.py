"""Configuration for PayU logging."""

import os
import sys
from dataclasses import dataclass
from typing import Optional

import structlog


@dataclass
class LoggingConfig:
    """Configuration for PayU logging.

    Attributes:
        service_name: Name of the service (e.g., "kyc-service")
        service_version: Version of the service (e.g., "1.0.0")
        environment: Environment name (dev, staging, prod)
        log_level: Logging level (DEBUG, INFO, WARNING, ERROR)
        json_format: Whether to output JSON (True for prod, False for dev)
        correlation_id_header: HTTP header name for correlation ID
    """

    service_name: str = "unknown-service"
    service_version: str = "1.0.0"
    environment: str = "dev"
    log_level: str = "INFO"
    json_format: bool = False
    correlation_id_header: str = "X-Correlation-Id"

    @classmethod
    def from_env(cls) -> "LoggingConfig":
        """Create configuration from environment variables."""
        return cls(
            service_name=os.getenv("PAYU_SERVICE_NAME", "unknown-service"),
            service_version=os.getenv("PAYU_SERVICE_VERSION", "1.0.0"),
            environment=os.getenv("PAYU_ENVIRONMENT", "dev"),
            log_level=os.getenv("PAYU_LOG_LEVEL", "INFO"),
            json_format=os.getenv("PAYU_LOG_FORMAT", "text").lower() == "json"
            or os.getenv("PAYU_ENVIRONMENT", "dev") in ["staging", "prod"],
            correlation_id_header=os.getenv(
                "PAYU_CORRELATION_HEADER", "X-Correlation-Id"
            ),
        )


def configure_logging(config: Optional[LoggingConfig] = None) -> None:
    """Configure structlog with PayU standard settings.

    Args:
        config: Logging configuration. If None, uses environment variables.
    """
    if config is None:
        config = LoggingConfig.from_env()

    # Set standard library logging level
    import logging

    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=getattr(logging, config.log_level.upper()),
    )

    # Configure structlog
    shared_processors = [
        # Add timestamp
        structlog.processors.TimeStamper(fmt="iso"),
        # Add log level
        structlog.stdlib.add_log_level,
        # Add logger name
        structlog.stdlib.add_logger_name,
        # Format exception info
        structlog.processors.format_exc_info,
        # Add call site info (file, line)
        structlog.processors.CallsiteParameterAdder(
            {
                structlog.processors.CallsiteParameter.FILENAME,
                structlog.processors.CallsiteParameter.FUNC_NAME,
                structlog.processors.CallsiteParameter.LINENO,
            }
        ),
    ]

    if config.json_format:
        # JSON format for production
        structlog.configure(
            processors=shared_processors
            + [
                # Add service metadata
                lambda _, __, event_dict: {
                    **event_dict,
                    "service": config.service_name,
                    "service_version": config.service_version,
                    "environment": config.environment,
                },
                # Add OpenTelemetry trace info
                _add_otel_trace_info,
                # Render as JSON
                structlog.processors.JSONRenderer(),
            ],
            wrapper_class=structlog.stdlib.BoundLogger,
            context_class=dict,
            logger_factory=structlog.stdlib.LoggerFactory(),
            cache_logger_on_first_use=True,
        )
    else:
        # Plain text format for development
        structlog.configure(
            processors=shared_processors
            + [
                # Render as console output
                structlog.dev.ConsoleRenderer(
                    colors=True,
                    exception_formatter=structlog.dev.rich_traceback,
                )
            ],
            wrapper_class=structlog.stdlib.BoundLogger,
            context_class=dict,
            logger_factory=structlog.stdlib.LoggerFactory(),
            cache_logger_on_first_use=True,
        )


def _add_otel_trace_info(
    logger: object, method_name: str, event_dict: dict
) -> dict:
    """Add OpenTelemetry trace and span IDs to log event."""
    try:
        from opentelemetry import trace

        current_span = trace.get_current_span()
        span_context = current_span.get_span_context()

        if span_context.is_valid:
            event_dict["trace_id"] = format_trace_id(span_context.trace_id)
            event_dict["span_id"] = format_span_id(span_context.span_id)
            event_dict["trace_flags"] = str(span_context.trace_flags)
    except ImportError:
        # OpenTelemetry not installed, skip
        pass

    return event_dict


def format_trace_id(trace_id: int) -> str:
    """Format trace ID as 32-character hex string."""
    return format(trace_id, "032x")


def format_span_id(span_id: int) -> str:
    """Format span ID as 16-character hex string."""
    return format(span_id, "016x")
