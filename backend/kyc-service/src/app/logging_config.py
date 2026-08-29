"""Logging configuration.

Configures structlog for JSON structured logging in production
and pretty console output in development. Integrates with
OpenTelemetry for distributed tracing context.
"""
import logging
import os
import sys
import warnings

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


# ARCH-LOG-001: fields that must never appear in logs verbatim.
_PII_FIELDS = frozenset({
    "nik", "phone", "phone_number", "mobile", "email", "account_number",
    "account_no", "card_number", "pin", "password", "token", "access_token",
    "refresh_token", "client_secret", "secret", "full_name",
})


def _mask_pii(_, __, event_dict):
    """Mask PII field values before the value reaches the renderer."""
    for key, value in list(event_dict.items()):
        if key in _PII_FIELDS and value is not None:
            event_dict[key] = "***"
    return event_dict


class _QuietTransientFilter(logging.Filter):
    """Demote known transient noise to INFO to keep logs warning-free."""
    _QUIET_SUBSTRINGS = (
        "GroupCoordinatorNotAvailable",
        "Marking the coordinator dead",
        "Topic payu.",
        "not found in cluster metadata",
        "Unable to update metadata",
        "Unable connect to node",
        "Unable to request metadata",
        "Heartbeat send request failed",
        "OffsetCommit failed",
        "Invalid HTTP request received",
        "cannot switch to state",
        "Name or service not known",
        "KYC outbox publisher loop error",
    )

    def filter(self, record: logging.LogRecord) -> bool:
        msg = record.getMessage()
        if any(s in msg for s in self._QUIET_SUBSTRINGS):
            if record.levelno >= logging.WARNING:
                record.levelno = logging.INFO
                record.levelname = "INFO"
        return True


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
        _mask_pii,
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
    # Use wrap_for_formatter so stdlib ProcessorFormatter owns the final render;
    # otherwise we get double-JSON (JSON string inside `message`).
    structlog.configure(
        processors=shared_processors + [structlog.stdlib.ProcessorFormatter.wrap_for_formatter],
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )

    # ------------------------------------------------------------------
    # Standard-library logging bridge
    # Routes all stdlib logs (including uvicorn and third-party
    # libraries) through structlog processors for consistent JSON output.
    root_logger = logging.getLogger()
    root_logger.handlers.clear()
    warnings.filterwarnings("ignore", category=DeprecationWarning)
    warnings.filterwarnings("ignore", category=UserWarning, module="paddle.*")
    warnings.filterwarnings("ignore", message=".*utcnow.*")
    logging.captureWarnings(True)
    warnings_logger = logging.getLogger("py.warnings")
    warnings_logger.addFilter(_QuietTransientFilter())

    handler = logging.StreamHandler(sys.stdout)
    _quiet_filter = _QuietTransientFilter()
    handler.addFilter(_quiet_filter)

    def _inject_service_meta(_, __, ed):
        ed["service.name"] = service_name
        ed["service.version"] = service_version
        ed["service.environment"] = environment
        return ed

    handler.setFormatter(
        structlog.stdlib.ProcessorFormatter(
            foreign_pre_chain=[
                _inject_service_meta,
                _add_otel_context,
                structlog.stdlib.add_log_level,
                structlog.stdlib.add_logger_name,
                structlog.processors.TimeStamper(fmt="iso"),
                _rename_event_key,
                _rename_timestamp_key,
            ],
            processor=renderer,
        )
    )
    root_logger.addHandler(handler)
    root_logger.addFilter(_quiet_filter)
    root_logger.setLevel(os.getenv("LOG_LEVEL", "INFO"))
    for _name in ("aiokafka", "aiokafka.consumer", "aiokafka.coordinator", "aiokafka.cluster",
                  "stomp.py", "uvicorn.error"):
        logging.getLogger(_name).addFilter(_quiet_filter)

    # ------------------------------------------------------------------
    # Override uvicorn's built-in loggers so access/error logs are
    # formatted identically to application logs.
    # ------------------------------------------------------------------
    for logger_name in ("uvicorn", "uvicorn.access", "uvicorn.error"):
        uvicorn_logger = logging.getLogger(logger_name)
        uvicorn_logger.handlers = [handler]
        uvicorn_logger.propagate = False
        uvicorn_logger.addFilter(_quiet_filter)

    return structlog.get_logger()
