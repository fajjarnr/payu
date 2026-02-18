"""Logger utilities for PayU logging."""

import structlog
from typing import Any


def get_logger(name: str | None = None, **context: Any) -> structlog.stdlib.BoundLogger:
    """Get a structured logger instance.

    Args:
        name: Logger name (typically __name__)
        **context: Additional context to bind to all logs from this logger

    Returns:
        A configured structlog logger instance

    Example:
        >>> from payu_logging import get_logger
        >>> logger = get_logger(__name__)
        >>> logger.info("User logged in", user_id="123")
    """
    logger = structlog.get_logger(name)

    if context:
        logger = logger.bind(**context)

    return logger
