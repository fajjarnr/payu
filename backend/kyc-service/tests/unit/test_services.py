import pytest
from unittest.mock import MagicMock
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
        sample_image = b'fake_image_data'

        with pytest.mock.patch.object(ocr_service, '_extract_text') as mock_extract:
            mock_extract.return_value = """
            NIK: 3201234567890001
            Nama: JOHN DOE
            Tempat/Tgl Lahir: Jakarta, 01-01-1990
            Jenis Kelamin: LAKI-LAKI
            Alamat: Jl. Test No. 1
            """
            result = await ocr_service.extract_ktp_data(sample_image)

            assert result.nik == "3201234567890001"
            assert result.name == "JOHN DOE"
            assert result.gender == "LAKI-LAKI"
            assert result.confidence > 0

    @pytest.mark.asyncio
    async def test_extract_ktp_data_invalid_image(self, ocr_service):
        """Test OCR with invalid image data"""
        invalid_image = b'invalid'

        with pytest.raises(ValueError, match="Invalid image data"):
            await ocr_service.extract_ktp_data(invalid_image)


@pytest.mark.unit
class TestLivenessService:
    """Unit tests for Liveness service"""

    @pytest.fixture
    def liveness_service():
        from app.ml.liveness_service import LivenessService
        return LivenessService()

    @pytest.mark.asyncio
    async def test_check_liveness_success(self, liveness_service):
        """Test successful liveness check"""
        with pytest.mock.patch.object(liveness_service, '_detect_face') as mock_detect:
            mock_detect.return_value = (True, (10, 10, 100, 100))

            with pytest.mock.patch.object(liveness_service, '_calculate_liveness_score') as mock_score:
                mock_score.return_value = 0.85

                result = await liveness_service.check_liveness(b'fake_image')

                assert result.is_live == True
                assert result.confidence == 0.85
                assert result.face_detected == True


@pytest.mark.unit
class TestFaceService:
    """Unit tests for Face service"""

    @pytest.fixture
    def face_service():
        from app.ml.face_service import FaceService
        return FaceService()

    @pytest.mark.asyncio
    async def test_match_face_success(self, face_service):
        """Test successful face matching"""
        ktp_path = "/tmp/ktp.jpg"
        selfie_data = b'fake_selfie'

        with pytest.mock.patch.object(face_service, '_detect_face') as mock_detect:
            mock_detect.return_value = (True, (10, 10, 100, 100))

            with pytest.mock.patch.object(face_service, '_encode_face') as mock_encode:
                mock_encode.return_value = [0.1] * 10000

                result = await face_service.match_face(ktp_path, selfie_data)

                assert result.is_live == True
                assert result.similarity_score > 0


@pytest.mark.unit
class TestDukcapilClient:
    """Unit tests for Dukcapil client"""

    @pytest.fixture
    def dukcapil_client():
        from app.adapters.dukcapil_client import DukcapilClient
        return DukcapilClient()

    @pytest.mark.asyncio
    async def test_verify_nik_success(self, dukcapil_client):
        """Test successful NIK verification"""
        with pytest.mock.patch.object(dukcapil_client.client, 'post') as mock_post:
            mock_response = MagicMock()
            mock_response.json.return_value = {
                'nik': '3201234567890001',
                'is_valid': True,
                'name': 'JOHN DOE',
                'birth_date': '1990-01-01',
                'gender': 'LAKI-LAKI',
                'status': 'VALID'
            }
            mock_response.raise_for_status = MagicMock()
            mock_post.return_value = mock_response

            result = await dukcapil_client.verify_nik('3201234567890001')

            assert result.nik == '3201234567890001'
            assert result.is_valid == True
            assert result.name == 'JOHN DOE'
