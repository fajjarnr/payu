"""
Idempotency utilities for ensuring safe retry of API requests.

BUG-ARCH-009 FIX: Migrated from in-memory dict to Redis-backed store
for multi-instance (pod) consistency on OpenShift.
"""

import hashlib
import json
from typing import Optional, Any, Dict
from structlog import get_logger

logger = get_logger(__name__)

# Try to import Redis; fallback to in-memory for local dev
try:
    import redis
    _redis_available = True
except ImportError:
    _redis_available = False


class IdempotencyStore:
    """Redis-backed store for idempotency keys with TTL."""

    def __init__(self, ttl_seconds: int = 86400, redis_url: str = None):
        self._ttl_seconds = ttl_seconds
        self._redis = None
        self._fallback_store: Dict[str, Dict[str, Any]] = {}

        if _redis_available and redis_url:
            try:
                self._redis = redis.from_url(redis_url, decode_responses=True)
                self._redis.ping()
                logger.info("Idempotency store connected to Redis", redis_url=redis_url)
            except Exception as e:
                logger.warning("Redis unavailable, falling back to in-memory", error=str(e))
                self._redis = None

    def _generate_key(
        self,
        idempotency_key: str,
        request_path: str,
        request_body: Optional[bytes] = None,
    ) -> str:
        key_data = f"{idempotency_key}:{request_path}"
        if request_body:
            key_data += f":{hashlib.sha256(request_body).hexdigest()}"
        return f"idempotency:{hashlib.sha256(key_data.encode()).hexdigest()}"

    async def get_cached_result(
        self,
        idempotency_key: str,
        request_path: str,
        request_body: Optional[bytes] = None,
    ) -> Optional[Dict[str, Any]]:
        key = self._generate_key(idempotency_key, request_path, request_body)

        if self._redis:
            try:
                cached = self._redis.get(key)
                if cached:
                    logger.info("Idempotency cache hit (Redis)", idempotency_key=idempotency_key)
                    return json.loads(cached)
            except Exception as e:
                logger.warning("Redis get failed", error=str(e))
        else:
            entry = self._fallback_store.get(key)
            if entry:
                logger.info("Idempotency cache hit (in-memory)", idempotency_key=idempotency_key)
                return entry.get("result")

        return None

    async def store_result(
        self,
        idempotency_key: str,
        request_path: str,
        result: Dict[str, Any],
        request_body: Optional[bytes] = None,
    ):
        key = self._generate_key(idempotency_key, request_path, request_body)

        if self._redis:
            try:
                self._redis.setex(key, self._ttl_seconds, json.dumps(result, default=str))
                logger.info("Idempotency result stored (Redis)", idempotency_key=idempotency_key)
                return
            except Exception as e:
                logger.warning("Redis set failed", error=str(e))

        self._fallback_store[key] = {"result": result}
        logger.info("Idempotency result stored (in-memory fallback)", idempotency_key=idempotency_key)


import os
_redis_url = os.getenv("REDIS_URL", os.getenv("PAYU_REDIS_URL", None))
idempotency_store = IdempotencyStore(ttl_seconds=86400, redis_url=_redis_url)


async def get_cached_result(
    idempotency_key: str, request_path: str, request_body: Optional[bytes] = None
) -> Optional[Dict[str, Any]]:
    return await idempotency_store.get_cached_result(idempotency_key, request_path, request_body)


async def cache_result(
    idempotency_key: str,
    request_path: str,
    result: Dict[str, Any],
    request_body: Optional[bytes] = None,
):
    await idempotency_store.store_result(idempotency_key, request_path, result, request_body)
