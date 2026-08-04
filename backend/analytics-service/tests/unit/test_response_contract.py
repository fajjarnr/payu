from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.api.v1.analytics import get_user_metrics


@pytest.mark.asyncio
async def test_analytics_error_path_returns_standard_error_envelope():
    request = MagicMock()
    request.state.request_id = "test-request-id"
    db = AsyncMock()

    with patch("app.api.v1.analytics.AnalyticsService") as service_class:
        service_class.return_value.get_user_metrics.side_effect = RuntimeError(
            "database unavailable"
        )

        result = await get_user_metrics(
            request,
            "user_123",
            db,
            {"sub": "user_123"},
        )

    assert result["success"] is False
    assert result["error"]["code"] == "ANA_SYS_001"
