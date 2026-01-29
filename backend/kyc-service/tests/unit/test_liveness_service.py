"""
Unit tests for Liveness Service - Anti-spoofing functionality
Tests cover face detection, quality assessment, and liveness scoring
"""

import pytest
import sys
import numpy as np
from unittest.mock import MagicMock, patch

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.ml.liveness_service import LivenessService


@pytest.mark.unit
class TestLivenessService:
    """Unit tests for Liveness detection service"""

    @pytest.fixture
    def liveness_service(self):
        """Create liveness service instance"""
        return LivenessService()

    @pytest.fixture
    def valid_face_image(self):
        """Create a test image with a face-like region"""
        img = np.zeros((480, 640, 3), dtype=np.uint8)
        # Add some noise to simulate texture
        img[100:300, 200:400] = np.random.randint(
            100, 200, (200, 200, 3), dtype=np.uint8
        )
        return img

    @pytest.fixture
    def blank_image(self):
        """Create a blank test image"""
        return np.zeros((480, 640, 3), dtype=np.uint8)

    @pytest.mark.asyncio
    async def test_check_liveness_no_face_detected(self, liveness_service, blank_image):
        """Test liveness check when no face is detected"""
        with patch("cv2.imdecode", return_value=blank_image):
            result = await liveness_service.check_liveness(b"fake_image")

            assert result.is_live is False
            assert result.face_detected is False
            assert result.confidence == 0.0
            assert result.face_quality_score == 0.0
            assert "reason" in result.details

    @pytest.mark.asyncio
    async def test_check_liveness_invalid_image(self, liveness_service):
        """Test liveness check with invalid image"""
        with patch("cv2.imdecode", return_value=None):
            with pytest.raises(ValueError, match="Invalid image data"):
                await liveness_service.check_liveness(b"invalid")

    @pytest.mark.asyncio
    async def test_check_liveness_exception_handling(self, liveness_service):
        """Test exception handling in liveness check"""
        with patch("cv2.imdecode", side_effect=Exception("Decode error")):
            with pytest.raises(ValueError, match="Liveness check failed"):
                await liveness_service.check_liveness(b"corrupted_image")

    def test_detect_face_with_face(self, liveness_service, valid_face_image):
        """Test face detection when face is present"""
        # Mock Haar cascade to return face coordinates
        mock_faces = np.array([[200, 100, 200, 200]])  # x, y, w, h

        with patch("cv2.CascadeClassifier") as mock_cascade:
            mock_instance = MagicMock()
            mock_instance.detectMultiScale.return_value = mock_faces
            mock_cascade.return_value = mock_instance

            detected, face_box = liveness_service._detect_face(valid_face_image)

            assert detected is True
            assert face_box is not None
            assert len(face_box) == 4

    def test_detect_face_without_face(self, liveness_service, blank_image):
        """Test face detection when no face is present"""
        # Mock Haar cascade to return empty array
        with patch("cv2.CascadeClassifier") as mock_cascade:
            mock_instance = MagicMock()
            mock_instance.detectMultiScale.return_value = np.array([])
            mock_cascade.return_value = mock_instance

            detected, face_box = liveness_service._detect_face(blank_image)

            assert detected is False
            assert face_box is None

    def test_detect_face_selects_largest_face(self, liveness_service):
        """Test that largest face is selected when multiple faces detected"""
        # Mock multiple faces
        mock_faces = np.array(
            [
                [100, 100, 100, 100],  # Area: 10000
                [200, 200, 200, 200],  # Area: 40000 (largest)
                [400, 400, 50, 50],  # Area: 2500
            ]
        )

        with patch("cv2.CascadeClassifier") as mock_cascade:
            mock_instance = MagicMock()
            mock_instance.detectMultiScale.return_value = mock_faces
            mock_cascade.return_value = mock_instance

            detected, face_box = liveness_service._detect_face(
                np.zeros((480, 640, 3), dtype=np.uint8)
            )

            assert detected is True
            assert face_box[2] == 200 and face_box[3] == 200  # Largest face

    def test_assess_face_quality(self, liveness_service):
        """Test face quality assessment"""
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)
        face_box = (0, 0, 200, 200)

        quality_score = liveness_service._assess_face_quality(img, face_box)

        assert 0.0 <= quality_score <= 1.0
        assert quality_score > 0  # Should have some quality

    def test_calculate_sharpness(self, liveness_service):
        """Test sharpness calculation"""
        # Create a sharp image with high variance
        sharp_img = np.random.randint(0, 255, (100, 100, 3), dtype=np.uint8)
        sharpness = liveness_service._calculate_sharpness(sharp_img)
        assert sharpness > 0

        # Create a blurry image with low variance
        blurry_img = np.ones((100, 100, 3), dtype=np.uint8) * 128
        blur_sharpness = liveness_service._calculate_sharpness(blurry_img)
        assert blur_sharpness < sharpness

    def test_calculate_brightness(self, liveness_service):
        """Test brightness calculation"""
        # Dark image
        dark_img = np.zeros((100, 100, 3), dtype=np.uint8)
        dark_score = liveness_service._calculate_brightness(dark_img)
        assert dark_score < 0.5

        # Bright image
        bright_img = np.ones((100, 100, 3), dtype=np.uint8) * 255
        bright_score = liveness_service._calculate_brightness(bright_img)
        assert bright_score < 0.5

        # Optimal brightness (around 128)
        optimal_img = np.ones((100, 100, 3), dtype=np.uint8) * 128
        optimal_score = liveness_service._calculate_brightness(optimal_img)
        assert optimal_score > 0.9

    def test_calculate_blur_score(self, liveness_service):
        """Test blur score calculation"""
        # Create test image with some variance
        img = np.random.randint(100, 150, (100, 100, 3), dtype=np.uint8)
        blur_score = liveness_service._calculate_blur_score(img)

        assert 0.0 <= blur_score <= 1.0

    def test_calculate_liveness_score(self, liveness_service):
        """Test liveness score calculation"""
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)
        face_box = (0, 0, 200, 200)

        liveness_score = liveness_service._calculate_liveness_score(img, face_box)

        assert 0.0 <= liveness_score <= 1.0

    def test_extract_liveness_features(self, liveness_service):
        """Test liveness feature extraction"""
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)
        face_box = (0, 0, 200, 200)

        features = liveness_service._extract_liveness_features(img, face_box)

        assert "eye_openness" in features
        assert "mouth_movement" in features
        assert "skin_texture" in features
        assert "head_pose" in features

        # All features should be between 0 and 1
        for value in features.values():
            assert 0.0 <= value <= 1.0

    def test_extract_liveness_features_face_region_bounds(self, liveness_service):
        """Test liveness feature extraction handles edge cases"""
        # Small face box
        img = np.random.randint(0, 255, (100, 100, 3), dtype=np.uint8)
        face_box = (0, 0, 50, 50)

        features = liveness_service._extract_liveness_features(img, face_box)

        assert isinstance(features, dict)
        assert len(features) == 4

    @pytest.mark.asyncio
    async def test_check_liveness_below_threshold(self, liveness_service):
        """Test liveness check when score is below threshold"""
        # Create image that won't pass liveness
        img = np.zeros((100, 100, 3), dtype=np.uint8)

        with patch("cv2.imdecode", return_value=img):
            with patch.object(
                liveness_service, "_detect_face", return_value=(True, (0, 0, 100, 100))
            ):
                with patch.object(
                    liveness_service, "_calculate_liveness_score", return_value=0.5
                ):
                    result = await liveness_service.check_liveness(
                        b"low_liveness_image"
                    )

                    assert result.is_live is False
                    assert result.face_detected is True
                    assert result.confidence == 0.5

    @pytest.mark.asyncio
    async def test_check_liveness_above_threshold(self, liveness_service):
        """Test liveness check when score is above threshold"""
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)

        with patch("cv2.imdecode", return_value=img):
            with patch.object(
                liveness_service, "_detect_face", return_value=(True, (0, 0, 200, 200))
            ):
                with patch.object(
                    liveness_service, "_calculate_liveness_score", return_value=0.8
                ):
                    result = await liveness_service.check_liveness(
                        b"high_liveness_image"
                    )

                    assert result.is_live is True
                    assert result.face_detected is True
                    assert result.confidence == 0.8

    def test_liveness_feature_weights(self, liveness_service):
        """Test that liveness features use correct weights"""
        # Mock features with known values
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)
        face_box = (0, 0, 200, 200)

        with patch.object(
            liveness_service,
            "_extract_liveness_features",
            return_value={
                "eye_openness": 1.0,
                "mouth_movement": 1.0,
                "head_pose": 1.0,
                "skin_texture": 1.0,
            },
        ):
            score = liveness_service._calculate_liveness_score(img, face_box)
            # All features at 1.0 should give score of 1.0
            assert score == 1.0

        with patch.object(
            liveness_service,
            "_extract_liveness_features",
            return_value={
                "eye_openness": 0.5,
                "mouth_movement": 0.5,
                "head_pose": 0.5,
                "skin_texture": 0.5,
            },
        ):
            score = liveness_service._calculate_liveness_score(img, face_box)
            # All features at 0.5 should give score around 0.5
            assert abs(score - 0.5) < 0.01

    @pytest.mark.asyncio
    async def test_check_liveness_returns_details(self, liveness_service):
        """Test that liveness check returns detailed information"""
        img = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)

        with patch("cv2.imdecode", return_value=img):
            with patch.object(
                liveness_service, "_detect_face", return_value=(True, (0, 0, 200, 200))
            ):
                with patch.object(
                    liveness_service, "_assess_face_quality", return_value=0.75
                ):
                    with patch.object(
                        liveness_service, "_calculate_liveness_score", return_value=0.85
                    ):
                        result = await liveness_service.check_liveness(b"test_image")

                        assert "face_quality" in result.details
                        assert "liveness_features" in result.details
                        assert result.details["face_quality"] == 0.75
