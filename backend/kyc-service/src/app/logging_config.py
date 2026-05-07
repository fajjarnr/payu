"""Logging configuration.

Configures structlog for JSON structured logging in production
and pretty console output in development. Integrates with
OpenTelemetry for distributed tracing context.
"""

import logging
import os
import sys

import structlog


def _add_otel_context(logger, method_name, event_dict):
    """Extract trace_id and span_id from the active OpenTelemetry span."""
    try:
        from opentelemetry import trace

        span = trace.get_current_span()
        if span and span.get_span_context().is_valid:
            span_ctx = span.get_span_context()
            event_dict["trace_id"] = format(span_ctx.trace_id, "032x")
            event_dict["span_id"] = format(span_ctx.span_id, "016x")
    except ImportError:
        pass
    return event_dict


def _rename_event_key(_, __, event_dict):
    """Rename 'event' to 'message' for standard log format."""
    event_dict["message"] = event_dict.pop("event", "")
    return event_dict


def _rename_timestamp_key(_, __, event_dict):
    """Rename 'timestamp' to '@timestamp' (Elasticsearch convention)."""
    if "timestamp" in event_dict:
        event_dict["@timestamp"] = event_dict.pop("timestamp")
    return event_dict


def configure_logging() -> structlog.stdlib.BoundLogger:
    """Configure structured logging once at application startup.

    Must be called before any ``get_logger()`` calls so that
    structlog caches the correct configuration.

    Returns a pre-configured logger with service metadata bound.
    """
    service_name = os.getenv("SERVICE_NAME", "kyc-service")
    service_version = os.getenv("SERVICE_VERSION", "1.0.0")
    environment = os.getenv("ENVIRONMENT", "dev")

    is_production = environment in ("container", "prod", "staging", "production")

    # ------------------------------------------------------------------
    # Bind service metadata globally via contextvars so every structlog
    # logger automatically includes these fields.
    # ------------------------------------------------------------------
    structlog.contextvars.bind_contextvars(
        **{
            "service.name": service_name,
            "service.version": service_version,
            "service.environment": environment,
        }
    )

    # ------------------------------------------------------------------
    # Processors shared across dev and prod (field normalisation + OTel)
    # ------------------------------------------------------------------
    shared_processors = [
        structlog.contextvars.merge_contextvars,
        _add_otel_context,
        structlog.processors.add_log_level,
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.TimeStamper(fmt="iso"),
        _rename_event_key,
        _rename_timestamp_key,
    ]

    if is_production:
        renderer = structlog.processors.JSONRenderer()
    else:
        renderer = structlog.dev.ConsoleRenderer()

    # ------------------------------------------------------------------
    # Application loggers (structlog)
    # ------------------------------------------------------------------
    structlog.configure(
        processors=shared_processors + [renderer],
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )

    # ------------------------------------------------------------------
    # Standard-library logging bridge
    # Routes all stdlib logs (including uvicorn and third-party
    # libraries) through structlog processors for consistent JSON output.
    # ------------------------------------------------------------------
    root_logger = logging.getLogger()
    root_logger.handlers.clear()

    handler = logging.StreamHandler(sys.stdout)

    def _inject_service_meta(_, __, ed):
        ed["service.name"] = service_name
        ed["service.version"] = service_version
        ed["service.environment"] = environment
        return ed

    handler.setFormatter(
        structlog.stdlib.ProcessorFormatter(
            processor=renderer,
            foreign_pre_chain=[
                _inject_service_meta,
                _add_otel_context,
                structlog.stdlib.add_log_level,
                structlog.stdlib.add_logger_name,
                structlog.processors.TimeStamper(fmt="iso"),
                _rename_event_key,
                _rename_timestamp_key,
            ],
        )
    )
    root_logger.addHandler(handler)
    root_logger.setLevel(os.getenv("LOG_LEVEL", "INFO"))

    # ------------------------------------------------------------------
    # Override uvicorn's built-in loggers so access/error logs are
    # formatted identically to application logs.
    # ------------------------------------------------------------------
    for logger_name in ("uvicorn", "uvicorn.access", "uvicorn.error"):
        uvicorn_logger = logging.getLogger(logger_name)
        uvicorn_logger.handlers = [handler]
        uvicorn_logger.propagate = False

    return structlog.get_logger()
