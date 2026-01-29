"""
Unit tests for Dukcapil Client - NIK verification functionality
Tests cover API calls, error handling, and response parsing
"""

import pytest
import sys
from unittest.mock import MagicMock, patch
import httpx

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.adapters.dukcapil_client import DukcapilClient
from app.models.schemas import DukcapilVerificationResult


@pytest.mark.unit
class TestDukcapilClient:
    """Unit tests for Dukcapil integration client"""

    @pytest.fixture
    def dukcapil_client(self):
        """Create Dukcapil client instance"""
        return DukcapilClient()

    @pytest.fixture
    def mock_success_response(self):
        """Mock successful API response"""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.json.return_value = {
            "nik": "3201012345678901",
            "is_valid": True,
            "name": "BUDI SANTOSO",
            "birth_date": "1990-01-01",
            "gender": "LAKI-LAKI",
            "status": "VALID",
            "match_score": 0.95,
            "notes": "Data matched successfully",
        }
        mock_resp.raise_for_status = MagicMock()
        return mock_resp

    @pytest.fixture
    def mock_failure_response(self):
        """Mock failed verification response"""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.json.return_value = {
            "nik": "3201012345678901",
            "is_valid": False,
            "name": "",
            "birth_date": "",
            "gender": "",
            "status": "INVALID",
            "match_score": 0.1,
            "notes": "NIK not found in database",
        }
        mock_resp.raise_for_status = MagicMock()
        return mock_resp

    @pytest.mark.asyncio
    async def test_verify_nik_success(self, dukcapil_client, mock_success_response):
        """Test successful NIK verification"""
        with patch.object(
            dukcapil_client.client, "post", return_value=mock_success_response
        ) as mock_post:
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert isinstance(result, DukcapilVerificationResult)
            assert result.nik == "3201012345678901"
            assert result.is_valid is True
            assert result.name == "BUDI SANTOSO"
            assert result.birth_date == "1990-01-01"
            assert result.gender == "LAKI-LAKI"
            assert result.status == "VALID"
            assert result.match_score == 0.95
            assert result.notes == "Data matched successfully"

            # Verify correct API call
            mock_post.assert_called_once()
            call_args = mock_post.call_args
            assert "verify" in call_args[0][0]

    @pytest.mark.asyncio
    async def test_verify_nik_invalid_nik(self, dukcapil_client, mock_failure_response):
        """Test NIK verification with invalid NIK"""
        with patch.object(
            dukcapil_client.client, "post", return_value=mock_failure_response
        ) as mock_post:
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert result.is_valid is False
            assert result.status == "INVALID"
            assert result.name == ""
            assert result.birth_date == ""
            assert result.gender == ""

    @pytest.mark.asyncio
    async def test_verify_nik_http_error_404(self, dukcapil_client):
        """Test NIK verification with 404 HTTP error"""
        mock_resp = MagicMock()
        mock_resp.status_code = 404
        error = httpx.HTTPStatusError(
            "Not Found", request=MagicMock(), response=mock_resp
        )

        with patch.object(dukcapil_client.client, "post", side_effect=error):
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert result.is_valid is False
            assert result.status == "ERROR"
            assert "HTTP Error: 404" in result.notes

    @pytest.mark.asyncio
    async def test_verify_nik_http_error_500(self, dukcapil_client):
        """Test NIK verification with 500 HTTP error"""
        mock_resp = MagicMock()
        mock_resp.status_code = 500
        error = httpx.HTTPStatusError(
            "Internal Server Error", request=MagicMock(), response=mock_resp
        )

        with patch.object(dukcapil_client.client, "post", side_effect=error):
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert result.is_valid is False
            assert result.status == "ERROR"
            assert "HTTP Error: 500" in result.notes

    @pytest.mark.asyncio
    async def test_verify_nik_network_error(self, dukcapil_client):
        """Test NIK verification with network error"""
        error = httpx.ConnectError("Connection failed")

        with patch.object(dukcapil_client.client, "post", side_effect=error):
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert result.is_valid is False
            assert result.status == "ERROR"
            assert "Error" in result.notes

    @pytest.mark.asyncio
    async def test_verify_nik_timeout_error(self, dukcapil_client):
        """Test NIK verification with timeout"""
        error = httpx.TimeoutException("Request timed out")

        with patch.object(dukcapil_client.client, "post", side_effect=error):
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert result.is_valid is False
            assert result.status == "ERROR"
            assert "Error" in result.notes

    @pytest.mark.asyncio
    async def test_verify_nik_partial_response(self, dukcapil_client):
        """Test NIK verification with partial response data - missing name is empty string"""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        # DukcapilVerificationResult has all fields required but we test with minimal data
        # The get() with defaults handles missing keys
        mock_resp.json.return_value = {
            "nik": "3201012345678901",
            "is_valid": True,
            "name": "",  # Empty string for name
            "birth_date": "",
            "gender": "",
            "status": "UNKNOWN",  # Status should have a value
        }
        mock_resp.raise_for_status = MagicMock()

        with patch.object(dukcapil_client.client, "post", return_value=mock_resp):
            result = await dukcapil_client.verify_nik("3201012345678901")

            # Should use default values for missing fields
            assert result.nik == "3201012345678901"
            assert result.is_valid is True
            assert result.name == ""
            assert result.status == "UNKNOWN"

    @pytest.mark.asyncio
    async def test_verify_nik_empty_nik(self, dukcapil_client, mock_success_response):
        """Test NIK verification with empty NIK"""
        with patch.object(
            dukcapil_client.client, "post", return_value=mock_success_response
        ):
            result = await dukcapil_client.verify_nik("")

            # Should still attempt the call (API will validate)
            assert isinstance(result, DukcapilVerificationResult)

    @pytest.mark.asyncio
    async def test_verify_nik_malformed_nik(
        self, dukcapil_client, mock_failure_response
    ):
        """Test NIK verification with malformed NIK"""
        with patch.object(
            dukcapil_client.client, "post", return_value=mock_failure_response
        ):
            result = await dukcapil_client.verify_nik("ABC123")

            assert isinstance(result, DukcapilVerificationResult)

    @pytest.mark.asyncio
    async def test_verify_nik_json_decode_error(self, dukcapil_client):
        """Test NIK verification when response JSON is invalid - caught and returns error result"""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.json.side_effect = ValueError("Invalid JSON")
        mock_resp.raise_for_status = MagicMock()

        with patch.object(dukcapil_client.client, "post", return_value=mock_resp):
            result = await dukcapil_client.verify_nik("3201012345678901")

            # Exception is caught and returns error result
            assert result.is_valid is False
            assert result.status == "ERROR"
            assert "Error" in result.notes

    @pytest.mark.asyncio
    async def test_verify_nik_generic_exception(self, dukcapil_client):
        """Test NIK verification with unexpected exception"""
        with patch.object(
            dukcapil_client.client, "post", side_effect=Exception("Unexpected error")
        ):
            result = await dukcapil_client.verify_nik("3201012345678901")

            assert result.is_valid is False
            assert result.status == "ERROR"
            assert "Error: Unexpected error" in result.notes

    @pytest.mark.asyncio
    async def test_verify_nik_request_payload(
        self, dukcapil_client, mock_success_response
    ):
        """Test that correct request payload is sent"""
        with patch.object(
            dukcapil_client.client, "post", return_value=mock_success_response
        ) as mock_post:
            await dukcapil_client.verify_nik("3201012345678901")

            # Check that NIK was sent in request body
            call_kwargs = mock_post.call_args[1]
            assert "json" in call_kwargs
            assert call_kwargs["json"]["nik"] == "3201012345678901"

    @pytest.mark.asyncio
    async def test_verify_nik_different_valid_statuses(self, dukcapil_client):
        """Test various valid status responses"""
        for status in ["VALID", "ACTIVE", "EXISTING"]:
            mock_resp = MagicMock()
            mock_resp.status_code = 200
            mock_resp.json.return_value = {
                "nik": "3201012345678901",
                "is_valid": True,
                "name": "TEST USER",
                "birth_date": "1990-01-01",
                "gender": "LAKI-LAKI",
                "status": status,
            }
            mock_resp.raise_for_status = MagicMock()

            with patch.object(dukcapil_client.client, "post", return_value=mock_resp):
                result = await dukcapil_client.verify_nik("3201012345678901")
                assert result.status == status

    @pytest.mark.asyncio
    async def test_verify_nik_with_match_score(self, dukcapil_client):
        """Test NIK verification with match score"""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.json.return_value = {
            "nik": "3201012345678901",
            "is_valid": True,
            "name": "BUDI SANTOSO",
            "birth_date": "1990-01-01",
            "gender": "LAKI-LAKI",
            "status": "VALID",
            "match_score": 0.87,
        }
        mock_resp.raise_for_status = MagicMock()

        with patch.object(dukcapil_client.client, "post", return_value=mock_resp):
            result = await dukcapil_client.verify_nik("3201012345678901")
            assert result.match_score == 0.87

    @pytest.mark.asyncio
    async def test_close_client(self, dukcapil_client):
        """Test closing the HTTP client"""
        await dukcapil_client.close()
        # Should not raise any exceptions

    def test_client_initialization(self):
        """Test that client initializes with correct settings"""
        client = DukcapilClient()
        assert client.client is not None
        assert hasattr(client, "base_url")

    @pytest.mark.asyncio
    async def test_verify_nik_concurrent_requests(
        self, dukcapil_client, mock_success_response
    ):
        """Test handling concurrent NIK verification requests"""
        import asyncio

        async def verify(nik):
            with patch.object(
                dukcapil_client.client, "post", return_value=mock_success_response
            ):
                return await dukcapil_client.verify_nik(nik)

        # Run concurrent requests
        results = await asyncio.gather(
            verify("3201012345678901"),
            verify("3201012345678902"),
            verify("3201012345678903"),
        )

        assert len(results) == 3
        for result in results:
            assert result.is_valid is True

    @pytest.mark.asyncio
    async def test_verify_nik_preserves_original_nik_in_error(self, dukcapil_client):
        """Test that original NIK is preserved even on error"""
        test_nik = "3201012345678901"
        mock_resp = MagicMock()
        mock_resp.status_code = 500
        error = httpx.HTTPStatusError(
            "Server Error", request=MagicMock(), response=mock_resp
        )

        with patch.object(dukcapil_client.client, "post", side_effect=error):
            result = await dukcapil_client.verify_nik(test_nik)

            assert result.nik == test_nik
            assert result.is_valid is False

    @pytest.mark.asyncio
    async def test_verify_nik_female_gender(self, dukcapil_client):
        """Test NIK verification with female gender"""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.json.return_value = {
            "nik": "3201012345678904",
            "is_valid": True,
            "name": "SITI AMINAH",
            "birth_date": "1992-05-15",
            "gender": "PEREMPUAN",
            "status": "VALID",
        }
        mock_resp.raise_for_status = MagicMock()

        with patch.object(dukcapil_client.client, "post", return_value=mock_resp):
            result = await dukcapil_client.verify_nik("3201012345678904")
            assert result.gender == "PEREMPUAN"

    @pytest.mark.asyncio
    async def test_verify_nik_with_notes_field(self, dukcapil_client):
        """Test various notes/messages from Dukcapil"""
        test_notes = [
            "Data matched successfully",
            "Minor discrepancies in name spelling",
            "Address needs verification",
            "Record expired, please visit Dukcapil",
        ]

        for notes in test_notes:
            mock_resp = MagicMock()
            mock_resp.status_code = 200
            mock_resp.json.return_value = {
                "nik": "3201012345678901",
                "is_valid": True,
                "name": "TEST USER",
                "birth_date": "1990-01-01",
                "gender": "LAKI-LAKI",
                "status": "VALID",
                "notes": notes,
            }
            mock_resp.raise_for_status = MagicMock()

            with patch.object(dukcapil_client.client, "post", return_value=mock_resp):
                result = await dukcapil_client.verify_nik("3201012345678901")
                assert result.notes == notes
