"""Idempotency utilities backed by Red Hat Data Grid REST."""

import hashlib
import json
import os
import ssl
from typing import Optional, Any, Dict
from urllib.parse import quote

import httpx
from structlog import get_logger

logger = get_logger(__name__)

class IdempotencyStore:
    """Data Grid REST-backed idempotency store with an opt-in local fallback."""

    def __init__(
        self,
        ttl_seconds: int = 86400,
        rest_url: Optional[str] = None,
        username: Optional[str] = None,
        password: Optional[str] = None,
        auth_type: str = "digest",
        ca_file: Optional[str] = None,
        client_cert_file: Optional[str] = None,
        client_key_file: Optional[str] = None,
        client: Optional[httpx.AsyncClient] = None,
    ):
        self._ttl_seconds = ttl_seconds
        self._rest_url = rest_url.rstrip("/") if rest_url else None
        self._cache_name = os.getenv("PAYU_CACHE_NAME", "payu")
        self._client = client
        self._fallback_store: Dict[str, Dict[str, Any]] = {}

        if self._rest_url and self._client is None:
            if bool(username) != bool(password):
                raise ValueError("Data Grid REST username and password must be configured together")
            if auth_type not in {"basic", "digest"}:
                raise ValueError("Data Grid REST auth type must be basic or digest")

            auth = None
            if username:
                auth = (username, password) if auth_type == "basic" else httpx.DigestAuth(username, password)

            verify: ssl.SSLContext | bool = True
            if ca_file or client_cert_file:
                verify = ssl.create_default_context(cafile=ca_file)
                if client_cert_file:
                    verify.load_cert_chain(client_cert_file, client_key_file)

            self._client = httpx.AsyncClient(
                base_url=self._rest_url,
                auth=auth,
                verify=verify,
                timeout=httpx.Timeout(5.0, connect=2.0),
                trust_env=False,
            )
            logger.info("Idempotency store configured for Data Grid REST", cache_name=self._cache_name)

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

        if self._client:
            try:
                response = await self._client.get(self._entry_path(key), headers={"Accept": "text/plain"})
                if response.status_code == 404:
                    return None
                response.raise_for_status()
                logger.info("Idempotency cache hit (Data Grid REST)")
                return json.loads(response.text)
            except httpx.HTTPError as error:
                logger.error("Data Grid REST idempotency read failed", error=str(error))
                raise RuntimeError("Data Grid REST idempotency read failed") from error

        entry = self._fallback_store.get(key)
        if entry:
            logger.info("Idempotency cache hit (in-memory)")
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

        if self._client:
            try:
                response = await self._client.put(
                    self._entry_path(key),
                    content=json.dumps(result, default=str),
                    headers={"Content-Type": "text/plain"},
                    params={"timeToLiveSeconds": self._ttl_seconds},
                )
                response.raise_for_status()
                logger.info("Idempotency result stored (Data Grid REST)")
                return
            except httpx.HTTPError as error:
                logger.error("Data Grid REST idempotency write failed", error=str(error))
                raise RuntimeError("Data Grid REST idempotency write failed") from error

        self._fallback_store[key] = {"result": result}
        logger.info("Idempotency result stored (in-memory fallback)", idempotency_key=idempotency_key)

    def _entry_path(self, key: str) -> str:
        return f"/rest/v2/caches/{quote(self._cache_name, safe='')}/{quote(key, safe='')}"


idempotency_store = IdempotencyStore(
    ttl_seconds=86400,
    rest_url=os.getenv("PAYU_CACHE_REST_URL"),
    username=os.getenv("PAYU_CACHE_REST_USERNAME"),
    password=os.getenv("PAYU_CACHE_REST_PASSWORD"),
    auth_type=os.getenv("PAYU_CACHE_REST_AUTH_TYPE", "digest"),
    ca_file=os.getenv("PAYU_CACHE_REST_CA_FILE"),
    client_cert_file=os.getenv("PAYU_CACHE_REST_CLIENT_CERT_FILE"),
    client_key_file=os.getenv("PAYU_CACHE_REST_CLIENT_KEY_FILE"),
)


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
