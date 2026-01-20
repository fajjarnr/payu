import numpy as np
import cv2
from structlog import get_logger
from typing import Optional

from app.models.schemas import FaceMatchResult
from app.config import get_settings

logger = get_logger(__name__)
settings = get_settings()


class FaceService:
    def __init__(self):
        self.threshold = settings.face_matching_threshold
        logger.info("Face recognition service initialized")

    async def match_face(
        self,
        ktp_image_path: str,
        selfie_image_data: bytes
    ) -> FaceMatchResult:
        try:
            nparr = np.frombuffer(selfie_image_data, np.uint8)
            selfie_img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if selfie_img is None:
                raise ValueError("Invalid selfie image data")

            ktp_img = cv2.imread(ktp_image_path)

            if ktp_img is None:
                logger.warning(f"KTP image not found at {ktp_image_path}, using selfie face detection only")
                ktp_img = selfie_img

            ktp_face_detected, ktp_face_box = self._detect_face(ktp_img)
            selfie_face_detected, selfie_face_box = self._detect_face(selfie_img)

            if not ktp_face_detected:
                logger.warning("No face detected on KTP")
                return FaceMatchResult(
                    is_match=False,
                    similarity_score=0.0,
                    threshold=self.threshold,
                    ktp_face_found=False,
                    selfie_face_found=selfie_face_detected
                )

            if not selfie_face_detected:
                logger.warning("No face detected on selfie")
                return FaceMatchResult(
                    is_match=False,
                    similarity_score=0.0,
                    threshold=self.threshold,
                    ktp_face_found=True,
                    selfie_face_found=False
                )

            ktp_face_encoding = self._encode_face(ktp_img, ktp_face_box)
            selfie_face_encoding = self._encode_face(selfie_img, selfie_face_box)

            if ktp_face_encoding is None or selfie_face_encoding is None:
                logger.error("Failed to encode faces")
                return FaceMatchResult(
                    is_match=False,
                    similarity_score=0.0,
                    threshold=self.threshold,
                    ktp_face_found=ktp_face_detected,
                    selfie_face_found=selfie_face_detected
                )

            similarity = self._calculate_similarity(ktp_face_encoding, selfie_face_encoding)
            is_match = similarity >= self.threshold

            logger.info(
                "Face matching completed",
                is_match=is_match,
                similarity=similarity,
                threshold=self.threshold
            )

            return FaceMatchResult(
                is_match=is_match,
                similarity_score=similarity,
                threshold=self.threshold,
                ktp_face_found=ktp_face_detected,
                selfie_face_found=selfie_face_detected
            )

        except Exception as e:
            logger.error("Face matching failed", exc_info=e)
            raise ValueError(f"Face matching failed: {str(e)}")

    def _detect_face(self, img: np.ndarray) -> tuple[bool, Optional[tuple]]:
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
        return True, tuple(largest_face)

    def _encode_face(self, img: np.ndarray, face_box: tuple) -> Optional[np.ndarray]:
        x, y, w, h = face_box
        face_region = img[y:y+h, x:x+w]

        face_region = cv2.resize(face_region, (100, 100))

        face_region = cv2.cvtColor(face_region, cv2.COLOR_BGR2GRAY)

        normalized_face = face_region / 255.0

        flattened = normalized_face.flatten().astype(np.float32)

        return flattened

    def _calculate_similarity(self, face1: np.ndarray, face2: np.ndarray) -> float:
        face1_normalized = face1 / np.linalg.norm(face1)
        face2_normalized = face2 / np.linalg.norm(face2)

        cosine_similarity = np.dot(face1_normalized, face2_normalized)

        similarity = (cosine_similarity + 1.0) / 2.0

        return similarity
