"""Middleware for correlation ID propagation in FastAPI/Starlette applications."""

import uuid
from typing import Callable, Optional

import structlog
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    """Middleware that manages correlation ID for incoming requests.

    Reads X-Correlation-Id header or generates a new one, then sets it in
    the structlog context for propagation to all downstream logs.

    Example:
        from fastapi import FastAPI
        from payu_logging import CorrelationIdMiddleware

        app = FastAPI()
        app.add_middleware(CorrelationIdMiddleware)
    """

    def __init__(
        self,
        app,
        header_name: str = "X-Correlation-Id",
        context_key: str = "correlation_id",
    ):
        super().__init__(app)
        self.header_name = header_name
        self.context_key = context_key

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # Extract or generate correlation ID
        correlation_id = self._get_correlation_id(request)

        # Add to response headers
        response = await call_next(request)
        response.headers[self.header_name] = correlation_id

        return response

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # Extract or generate correlation ID
        correlation_id = self._get_correlation_id(request)

        # Bind to structlog context for this request
        # This ensures all logs during request processing include correlation_id
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(**{self.context_key: correlation_id})

        try:
            response = await call_next(request)
            # Add correlation ID to response headers
            response.headers[self.header_name] = correlation_id
            return response
        finally:
            # Clear context after request
            structlog.contextvars.clear_contextvars()

    def _get_correlation_id(self, request: Request) -> str:
        """Extract correlation ID from header or generate new one."""
        correlation_id = request.headers.get(self.header_name)
        if not correlation_id:
            correlation_id = self._generate_correlation_id()
        return correlation_id

    def _generate_correlation_id(self) -> str:
        """Generate a new correlation ID."""
        return uuid.uuid4().hex


def get_correlation_id() -> Optional[str]:
    """Get the current correlation ID from context.

    Returns:
        The current correlation ID or None if not set.
    """
    return structlog.contextvars.get_contextvars().get("correlation_id")
