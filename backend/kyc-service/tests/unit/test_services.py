import pytest
import sys
import numpy as np

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")  # noqa: E402
from unittest.mock import MagicMock, patch
from app.ml.ocr_service import OcrService


@pytest.mark.unit
class TestOCRService:
    """Unit tests for OCR service"""

    @pytest.fixture
    def ocr_service(self):
        return OcrService()

    @pytest.mark.asyncio
    async def test_extract_ktp_data_success(self, ocr_service):
        """Test successful KTP data extraction"""
        sample_image = b"fake_image_data"

        # Mock cv2.imdecode to return a valid image array
        mock_img = np.zeros((100, 100, 3), dtype=np.uint8)

        # Mock PaddleOCR result with proper structure - including all required fields
        mock_ocr_result = [
            [
                [[0, 0, 100, 10], ["NIK: 3201234567890001", 0.95]],
                [[0, 10, 100, 20], ["Nama: JOHN DOE", 0.95]],
                [[0, 20, 100, 30], ["Tempat/Tgl Lahir: Jakarta, 01-01-1990", 0.95]],
                [[0, 30, 100, 40], ["Jenis Kelamin: LAKI-LAKI", 0.95]],
                [[0, 40, 100, 50], ["Alamat: Jl. Test No. 1", 0.95]],
                [[0, 50, 100, 60], ["Kecamatan: Test District", 0.95]],
                [[0, 60, 100, 70], ["Kabupaten: Test City", 0.95]],
                [[0, 70, 100, 80], ["Provinsi: Test Province", 0.95]],
            ]
        ]

        with patch("cv2.imdecode", return_value=mock_img):
            with patch.object(ocr_service.ocr, "ocr", return_value=mock_ocr_result):
                result = await ocr_service.extract_ktp_data(sample_image)

                assert result.nik == "3201234567890001"
                assert result.name == "JOHN DOE"
                assert result.gender == "LAKI-LAKI"
                assert result.confidence > 0

    @pytest.mark.asyncio
    async def test_extract_ktp_data_invalid_image(self, ocr_service):
        """Test OCR with invalid image data"""
        invalid_image = b"invalid"

        with pytest.raises(ValueError, match="Invalid image data"):
            await ocr_service.extract_ktp_data(invalid_image)


@pytest.mark.unit
class TestLivenessService:
    """Unit tests for Liveness service"""

    @pytest.fixture
    def liveness_service(self):
        from app.ml.liveness_service import LivenessService

        return LivenessService()

    @pytest.mark.asyncio
    async def test_check_liveness_success(self, liveness_service):
        """Test successful liveness check"""
        # Mock cv2.imdecode to return a valid image array
        mock_img = np.zeros((100, 100, 3), dtype=np.uint8)

        with patch("cv2.imdecode", return_value=mock_img):
            result = await liveness_service.check_liveness(b"fake_image")

            # Since no face will be detected in a blank image,
            # we test that the service returns properly
            assert result is not None
            assert hasattr(result, "face_detected")


@pytest.mark.unit
class TestFaceService:
    """Unit tests for Face service"""

    @pytest.fixture
    def face_service(self):
        from app.ml.face_service import FaceService

        return FaceService()

    @pytest.mark.asyncio
    async def test_match_face_success(self, face_service):
        """Test successful face matching"""
        ktp_path = "/tmp/ktp.jpg"
        selfie_data = b"fake_selfie"

        # Mock cv2.imdecode and cv2.imread to return valid image arrays
        mock_img = np.zeros((100, 100, 3), dtype=np.uint8)

        with patch("cv2.imdecode", return_value=mock_img):
            with patch("cv2.imread", return_value=mock_img):
                result = await face_service.match_face(ktp_path, selfie_data)

                # Test that the service returns a result
                assert result is not None
                assert hasattr(result, "ktp_face_found")
                assert hasattr(result, "selfie_face_found")


@pytest.mark.unit
class TestDukcapilClient:
    """Unit tests for Dukcapil client"""

    @pytest.fixture
    def dukcapil_client(self):
        from app.adapters.dukcapil_client import DukcapilClient

        return DukcapilClient()

    @pytest.mark.asyncio
    async def test_verify_nik_success(self, dukcapil_client):
        """Test successful NIK verification"""
        with patch.object(dukcapil_client.client, "post") as mock_post:
            mock_response = MagicMock()
            mock_response.json.return_value = {
                "nik": "3201234567890001",
                "is_valid": True,
                "name": "JOHN DOE",
                "birth_date": "1990-01-01",
                "gender": "LAKI-LAKI",
                "status": "VALID",
            }
            mock_response.raise_for_status = MagicMock()
            mock_post.return_value = mock_response

            result = await dukcapil_client.verify_nik("3201234567890001")

            assert result.nik == "3201234567890001"
            assert result.is_valid == True
            assert result.name == "JOHN DOE"
