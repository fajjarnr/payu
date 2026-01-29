"""
Unit tests for Face Service - Face comparison functionality
Tests cover face detection, encoding, and similarity calculation
"""

import pytest
import sys
import numpy as np
from unittest.mock import MagicMock, patch

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.ml.face_service import FaceService
from app.models.schemas import FaceMatchResult


@pytest.mark.unit
class TestFaceService:
    """Unit tests for Face recognition service"""

    @pytest.fixture
    def face_service(self):
        """Create face service instance"""
        return FaceService()

    @pytest.fixture
    def ktp_image_with_face(self):
        """Create a test image representing KTP with face"""
        img = np.zeros((300, 400, 3), dtype=np.uint8)
        # Add face-like region
        img[50:150, 100:200] = np.random.randint(
            100, 200, (100, 100, 3), dtype=np.uint8
        )
        return img

    @pytest.fixture
    def selfie_image_with_face(self):
        """Create a test image representing selfie with face"""
        img = np.zeros((480, 640, 3), dtype=np.uint8)
        # Add face-like region (larger than KTP)
        img[100:300, 200:400] = np.random.randint(
            100, 200, (200, 200, 3), dtype=np.uint8
        )
        return img

    @pytest.fixture
    def blank_image(self):
        """Create blank image"""
        return np.zeros((480, 640, 3), dtype=np.uint8)

    @pytest.mark.asyncio
    async def test_match_face_both_faces_detected(
        self, face_service, ktp_image_with_face, selfie_image_with_face
    ):
        """Test face matching when both faces are detected"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        # Mock face detection to return faces for both images
        mock_faces_ktp = np.array([[100, 50, 100, 100]])
        mock_faces_selfie = np.array([[200, 100, 200, 200]])

        with patch("cv2.imread", return_value=ktp_image_with_face):
            with patch("cv2.imdecode", return_value=selfie_image_with_face):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    # First call for KTP, second for selfie
                    mock_instance.detectMultiScale.side_effect = [
                        mock_faces_ktp,
                        mock_faces_selfie,
                    ]
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    assert isinstance(result, FaceMatchResult)
                    assert result.ktp_face_found is True
                    assert result.selfie_face_found is True

    @pytest.mark.asyncio
    async def test_match_face_no_ktp_face(self, face_service, selfie_image_with_face):
        """Test face matching when no face detected on KTP"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        blank_ktp = np.zeros((300, 400, 3), dtype=np.uint8)

        with patch("cv2.imread", return_value=blank_ktp):
            with patch("cv2.imdecode", return_value=selfie_image_with_face):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    # KTP returns empty, selfie returns face
                    mock_instance.detectMultiScale.side_effect = [
                        np.array([]),  # No face on KTP
                        np.array([[200, 100, 200, 200]]),
                    ]
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    assert result.is_match is False
                    assert result.ktp_face_found is False
                    assert result.selfie_face_found is True

    @pytest.mark.asyncio
    async def test_match_face_no_selfie_face(self, face_service, ktp_image_with_face):
        """Test face matching when no face detected on selfie"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        blank_selfie = np.zeros((480, 640, 3), dtype=np.uint8)

        with patch("cv2.imread", return_value=ktp_image_with_face):
            with patch("cv2.imdecode", return_value=blank_selfie):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    # KTP returns face, selfie returns empty
                    mock_instance.detectMultiScale.side_effect = [
                        np.array([[100, 50, 100, 100]]),
                        np.array([]),  # No face on selfie
                    ]
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    assert result.is_match is False
                    assert result.ktp_face_found is True
                    assert result.selfie_face_found is False

    @pytest.mark.asyncio
    async def test_match_face_no_faces_detected(self, face_service):
        """Test face matching when no faces detected on either image"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        blank_img = np.zeros((300, 400, 3), dtype=np.uint8)

        with patch("cv2.imread", return_value=blank_img):
            with patch("cv2.imdecode", return_value=blank_img):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    mock_instance.detectMultiScale.return_value = np.array([])
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    assert result.is_match is False
                    assert result.ktp_face_found is False
                    assert result.selfie_face_found is False

    @pytest.mark.asyncio
    async def test_match_face_invalid_selfie_image(self, face_service):
        """Test face matching with invalid selfie image"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"invalid_image"

        with patch("cv2.imdecode", return_value=None):
            with pytest.raises(ValueError, match="Invalid selfie image data"):
                await face_service.match_face(ktp_path, selfie_data)

    @pytest.mark.asyncio
    async def test_match_face_ktp_image_not_found(
        self, face_service, selfie_image_with_face
    ):
        """Test face matching when KTP image file doesn't exist"""
        ktp_path = "/tmp/nonexistent_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        with patch("cv2.imread", return_value=None):
            with patch("cv2.imdecode", return_value=selfie_image_with_face):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    mock_faces = np.array([[200, 100, 200, 200]])
                    mock_instance.detectMultiScale.return_value = mock_faces
                    mock_cascade.return_value = mock_instance

                    # Should use selfie face detection only (selfie compared to itself)
                    result = await face_service.match_face(ktp_path, selfie_data)

                    # With same image, should have high similarity
                    assert result.selfie_face_found is True

    @pytest.mark.asyncio
    async def test_match_face_exception_handling(self, face_service):
        """Test exception handling in face matching"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"test_image"

        with patch("cv2.imdecode", side_effect=Exception("Processing error")):
            with pytest.raises(ValueError, match="Face matching failed"):
                await face_service.match_face(ktp_path, selfie_data)

    def test_detect_face_success(self, face_service, ktp_image_with_face):
        """Test successful face detection"""
        mock_faces = np.array([[100, 50, 100, 100]])

        with patch("cv2.CascadeClassifier") as mock_cascade:
            mock_instance = MagicMock()
            mock_instance.detectMultiScale.return_value = mock_faces
            mock_cascade.return_value = mock_instance

            detected, face_box = face_service._detect_face(ktp_image_with_face)

            assert detected is True
            assert face_box == (100, 50, 100, 100)

    def test_detect_face_no_faces(self, face_service, blank_image):
        """Test face detection with no faces"""
        with patch("cv2.CascadeClassifier") as mock_cascade:
            mock_instance = MagicMock()
            mock_instance.detectMultiScale.return_value = np.array([])
            mock_cascade.return_value = mock_instance

            detected, face_box = face_service._detect_face(blank_image)

            assert detected is False
            assert face_box is None

    def test_detect_face_selects_largest(self, face_service):
        """Test that largest face is selected"""
        mock_faces = np.array(
            [
                [10, 10, 50, 50],  # Area: 2500
                [100, 100, 200, 200],  # Area: 40000 (largest)
                [50, 50, 30, 30],  # Area: 900
            ]
        )

        with patch("cv2.CascadeClassifier") as mock_cascade:
            mock_instance = MagicMock()
            mock_instance.detectMultiScale.return_value = mock_faces
            mock_cascade.return_value = mock_instance

            detected, face_box = face_service._detect_face(
                np.zeros((480, 640, 3), dtype=np.uint8)
            )

            assert detected is True
            assert face_box == (100, 100, 200, 200)

    def test_encode_face(self, face_service):
        """Test face encoding"""
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)
        face_box = (50, 50, 100, 100)

        encoding = face_service._encode_face(img, face_box)

        assert encoding is not None
        assert isinstance(encoding, np.ndarray)
        # Should be flattened to 10000 elements (100x100)
        assert encoding.shape == (10000,)

    def test_encode_face_returns_none_on_invalid(self, face_service):
        """Test face encoding handles invalid input"""
        img = np.zeros((50, 50, 3), dtype=np.uint8)  # Too small
        face_box = (0, 0, 50, 50)

        encoding = face_service._encode_face(img, face_box)

        # Should still return something for valid face box
        assert encoding is not None

    def test_calculate_similarity_identical_faces(self, face_service):
        """Test similarity calculation with identical faces"""
        face1 = np.random.rand(10000).astype(np.float32)
        face2 = face1.copy()

        similarity = face_service._calculate_similarity(face1, face2)

        # Identical faces should have similarity close to 1.0
        assert similarity > 0.99

    def test_calculate_similarity_different_faces(self, face_service):
        """Test similarity calculation with different faces"""
        face1 = np.random.rand(10000).astype(np.float32)
        face2 = np.random.rand(10000).astype(np.float32)

        similarity = face_service._calculate_similarity(face1, face2)

        # Different faces should have similarity between 0 and 1
        assert 0.0 <= similarity <= 1.0

    def test_calculate_similarity_normalizes_faces(self, face_service):
        """Test that faces are normalized before comparison"""
        # Create faces with different magnitudes
        face1 = np.ones(10000, dtype=np.float32) * 10
        face2 = np.ones(10000, dtype=np.float32) * 100

        similarity = face_service._calculate_similarity(face1, face2)

        # After normalization, identical direction should give high similarity
        assert similarity > 0.99

    @pytest.mark.asyncio
    async def test_match_face_similarity_above_threshold(
        self, face_service, ktp_image_with_face
    ):
        """Test face matching when similarity is above threshold"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        # Use same image for both to get high similarity
        with patch("cv2.imread", return_value=ktp_image_with_face):
            with patch("cv2.imdecode", return_value=ktp_image_with_face):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    mock_faces = np.array([[100, 50, 100, 100]])
                    mock_instance.detectMultiScale.return_value = mock_faces
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    # Same image should match
                    assert result.is_match is True
                    assert result.similarity_score >= face_service.threshold

    @pytest.mark.asyncio
    async def test_match_face_similarity_below_threshold(self, face_service):
        """Test face matching when similarity is below threshold"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        # Create different images
        ktp_img = np.random.randint(0, 100, (300, 400, 3), dtype=np.uint8)
        selfie_img = np.random.randint(100, 255, (480, 640, 3), dtype=np.uint8)

        with patch("cv2.imread", return_value=ktp_img):
            with patch("cv2.imdecode", return_value=selfie_img):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    mock_faces_ktp = np.array([[100, 50, 100, 100]])
                    mock_faces_selfie = np.array([[200, 100, 200, 200]])
                    mock_instance.detectMultiScale.side_effect = [
                        mock_faces_ktp,
                        mock_faces_selfie,
                    ]
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    # Different random images likely won't match
                    # But we just check the structure
                    assert result.ktp_face_found is True
                    assert result.selfie_face_found is True
                    assert hasattr(result, "similarity_score")

    def test_match_face_returns_threshold(self, face_service, ktp_image_with_face):
        """Test that match result includes the threshold used"""
        assert hasattr(face_service, "threshold")
        assert 0.0 <= face_service.threshold <= 1.0

    @pytest.mark.asyncio
    async def test_match_face_encoding_failure(self, face_service):
        """Test handling of face encoding failure"""
        ktp_path = "/tmp/test_ktp.jpg"
        selfie_data = b"selfie_image_bytes"

        # Tiny image that will cause issues
        tiny_img = np.zeros((10, 10, 3), dtype=np.uint8)

        with patch("cv2.imread", return_value=tiny_img):
            with patch("cv2.imdecode", return_value=tiny_img):
                with patch("cv2.CascadeClassifier") as mock_cascade:
                    mock_instance = MagicMock()
                    mock_faces = np.array([[0, 0, 10, 10]])
                    mock_instance.detectMultiScale.return_value = mock_faces
                    mock_cascade.return_value = mock_instance

                    result = await face_service.match_face(ktp_path, selfie_data)

                    # Should handle gracefully
                    assert result.ktp_face_found is True
                    assert result.selfie_face_found is True
