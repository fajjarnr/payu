import httpx
import os
import uuid

import pytest

from app.api.idempotency import IdempotencyStore


async def test_store_uses_datagrid_rest_json_contract():
    requests = []

    async def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        if request.method == "GET":
            return httpx.Response(404)
        return httpx.Response(204)

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://datagrid.test"
    ) as client:
        store = IdempotencyStore(
            ttl_seconds=60,
            rest_url="https://datagrid.test",
            client=client,
        )
        assert await store.get_cached_result("key", "/v1/analytics") is None
        await store.store_result("key", "/v1/analytics", {"status": "ok"})

    assert [request.method for request in requests] == ["GET", "PUT"]
    assert requests[1].url.params["timeToLiveSeconds"] == "60"
    assert requests[1].headers["content-type"] == "text/plain"
    assert requests[1].content == b'{"status": "ok"}'


@pytest.mark.integration
async def test_store_round_trips_through_local_datagrid_rest():
    rest_url = os.getenv("PAYU_CACHE_REST_URL")
    if not rest_url:
        pytest.skip("PAYU_CACHE_REST_URL is not configured")

    store = IdempotencyStore(
        ttl_seconds=60,
        rest_url=rest_url,
        username=os.getenv("PAYU_CACHE_REST_USERNAME"),
        password=os.getenv("PAYU_CACHE_REST_PASSWORD"),
        auth_type=os.getenv("PAYU_CACHE_REST_AUTH_TYPE", "digest"),
    )
    try:
        key = str(uuid.uuid4())
        result = {"status": "ok", "source": "analytics"}
        await store.store_result(key, "/v1/analytics", result)
        assert await store.get_cached_result(key, "/v1/analytics") == result
    finally:
        await store._client.aclose()
