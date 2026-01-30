"""
Idempotency utilities for ensuring safe retry of API requests.

Stores idempotency keys with their results to return the same response
for duplicate requests within a TTL window.
"""

import hashlib
from typing import Optional, Any, Dict
from datetime import datetime, timedelta
from structlog import get_logger

logger = get_logger(__name__)


class IdempotencyStore:
    """In-memory store for idempotency keys with TTL."""

    def __init__(self, ttl_seconds: int = 86400):
        """
        Initialize the idempotency store.

        Args:
            ttl_seconds: Time-to-live for cached results (default: 24 hours)
        """
        self._store: Dict[str, Dict[str, Any]] = {}
        self._ttl_seconds = ttl_seconds

    def _generate_key(
        self,
        idempotency_key: str,
        request_path: str,
        request_body: Optional[bytes] = None,
    ) -> str:
        """Generate a unique key combining idempotency key and request context."""
        key_data = f"{idempotency_key}:{request_path}"
        if request_body:
            key_data += f":{hashlib.sha256(request_body).hexdigest()}"
        return hashlib.sha256(key_data.encode()).hexdigest()

    def _is_expired(self, stored_at: datetime) -> bool:
        """Check if a stored entry has expired."""
        return datetime.utcnow() > stored_at + timedelta(seconds=self._ttl_seconds)

    def _cleanup_expired(self):
        """Remove expired entries from the store."""
        expired_keys = [
            key
            for key, value in self._store.items()
            if self._is_expired(value["stored_at"])
        ]
        for key in expired_keys:
            del self._store[key]

    async def get_cached_result(
        self,
        idempotency_key: str,
        request_path: str,
        request_body: Optional[bytes] = None,
    ) -> Optional[Dict[str, Any]]:
        """
        Retrieve cached result for an idempotency key.

        Args:
            idempotency_key: The idempotency key from the request header
            request_path: The API endpoint path
            request_body: Optional request body for additional uniqueness

        Returns:
            Cached result if found and not expired, None otherwise
        """
        self._cleanup_expired()

        key = self._generate_key(idempotency_key, request_path, request_body)
        entry = self._store.get(key)

        if not entry:
            return None

        if self._is_expired(entry["stored_at"]):
            del self._store[key]
            return None

        logger.info(
            "Idempotency cache hit",
            idempotency_key=idempotency_key,
            request_path=request_path,
        )

        return entry["result"]

    async def store_result(
        self,
        idempotency_key: str,
        request_path: str,
        result: Dict[str, Any],
        request_body: Optional[bytes] = None,
    ):
        """
        Store result for an idempotency key.

        Args:
            idempotency_key: The idempotency key from the request header
            request_path: The API endpoint path
            result: The result to cache
            request_body: Optional request body for additional uniqueness
        """
        key = self._generate_key(idempotency_key, request_path, request_body)

        self._store[key] = {"result": result, "stored_at": datetime.utcnow()}

        logger.info(
            "Idempotency result stored",
            idempotency_key=idempotency_key,
            request_path=request_path,
        )


# Global idempotency store instance
idempotency_store = IdempotencyStore(ttl_seconds=86400)  # 24 hours


async def get_cached_result(
    idempotency_key: str, request_path: str, request_body: Optional[bytes] = None
) -> Optional[Dict[str, Any]]:
    """Convenience function to get cached result from global store."""
    return await idempotency_store.get_cached_result(
        idempotency_key, request_path, request_body
    )


async def cache_result(
    idempotency_key: str,
    request_path: str,
    result: Dict[str, Any],
    request_body: Optional[bytes] = None,
):
    """Convenience function to store result in global store."""
    await idempotency_store.store_result(
        idempotency_key, request_path, result, request_body
    )
