import numpy as np
import cv2
from structlog import get_logger
from typing import Dict, Any

from app.models.schemas import LivenessCheckResult
from app.config import get_settings

logger = get_logger(__name__)
settings = get_settings()


class LivenessService:
    def __init__(self):
        self.min_frames = settings.liveness_min_frames
        self.threshold = settings.liveness_threshold
        logger.info("Liveness detection service initialized")

    async def check_liveness(self, image_data: bytes) -> LivenessCheckResult:
        try:
            nparr = np.frombuffer(image_data, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if img is None:
                raise ValueError("Invalid image data")

            face_detected, face_box = self._detect_face(img)

            if not face_detected:
                logger.warning("No face detected in selfie")
                return LivenessCheckResult(
                    is_live=False,
                    confidence=0.0,
                    face_detected=False,
                    face_quality_score=0.0,
                    details={"reason": "No face detected"}
                )

            face_quality_score = self._assess_face_quality(img, face_box)
            liveness_score = self._calculate_liveness_score(img, face_box)
            is_live = liveness_score >= self.threshold

            details = {
                "face_quality": face_quality_score,
                "liveness_features": self._extract_liveness_features(img, face_box)
            }

            logger.info(
                "Liveness check completed",
                is_live=is_live,
                confidence=liveness_score,
                face_quality=face_quality_score
            )

            return LivenessCheckResult(
                is_live=is_live,
                confidence=liveness_score,
                face_detected=True,
                face_quality_score=face_quality_score,
                details=details
            )

        except Exception as e:
            logger.error("Liveness check failed", exc_info=e)
            raise ValueError(f"Liveness check failed: {str(e)}")

    def _detect_face(self, img: np.ndarray) -> tuple[bool, Any]:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        face_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        )

        faces = face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(30, 30)
        )

        if len(faces) == 0:
            return False, None

        largest_face = max(faces, key=lambda x: x[2] * x[3])
        return True, largest_face

    def _assess_face_quality(self, img: np.ndarray, face_box: Any) -> float:
        x, y, w, h = face_box
        face_region = img[y:y+h, x:x+w]

        sharpness = self._calculate_sharpness(face_region)
        brightness = self._calculate_brightness(face_region)
        blur_score = self._calculate_blur_score(face_region)

        quality_score = (sharpness * 0.4) + (brightness * 0.3) + (blur_score * 0.3)

        return min(quality_score, 1.0)

    def _calculate_sharpness(self, img: np.ndarray) -> float:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        laplacian = cv2.Laplacian(gray, cv2.CV_64F)
        variance = laplacian.var()
        return min(variance / 500.0, 1.0)

    def _calculate_brightness(self, img: np.ndarray) -> float:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        mean_brightness = np.mean(gray) / 255.0
        return 1.0 - abs(0.5 - mean_brightness) * 2.0

    def _calculate_blur_score(self, img: np.ndarray) -> float:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        fm = cv2.Laplacian(gray, cv2.CV_64F).var()
        return min(fm / 100.0, 1.0)

    def _calculate_liveness_score(self, img: np.ndarray, face_box: Any) -> float:
        x, y, w, h = face_box
        face_region = img[y:y+h, x:x+w]

        features = self._extract_liveness_features(img, face_box)

        eye_openness = features.get('eye_openness', 0.5)
        mouth_movement = features.get('mouth_movement', 0.0)
        head_pose = features.get('head_pose', 0.5)
        skin_texture = features.get('skin_texture', 0.5)

        liveness_score = (
            eye_openness * 0.3 +
            mouth_movement * 0.2 +
            head_pose * 0.25 +
            skin_texture * 0.25
        )

        return min(liveness_score, 1.0)

    def _extract_liveness_features(self, img: np.ndarray, face_box: Any) -> Dict[str, float]:
        x, y, w, h = face_box

        eye_region_top = y + int(h * 0.35)
        eye_region_height = int(h * 0.15)
        eye_region = img[eye_region_top:eye_region_top+eye_region_height, x:x+w]

        mouth_region_top = y + int(h * 0.70)
        mouth_region_height = int(h * 0.15)
        mouth_region = img[mouth_region_top:mouth_region_top+mouth_region_height, x:x+w]

        eye_variance = np.var(eye_region) if eye_region.size > 0 else 0
        mouth_variance = np.var(mouth_region) if mouth_region.size > 0 else 0

        eye_openness = min(eye_variance / 50.0, 1.0)
        mouth_movement = min(mouth_variance / 30.0, 1.0)

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        sobelx = cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=3)
        sobely = cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=3)
        gradient_magnitude = np.sqrt(sobelx**2 + sobely**2)
        skin_texture = np.mean(gradient_magnitude) / 100.0

        return {
            'eye_openness': eye_openness,
            'mouth_movement': mouth_movement,
            'skin_texture': min(skin_texture, 1.0),
            'head_pose': 0.5
        }
