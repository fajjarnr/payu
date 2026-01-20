import numpy as np
import cv2
from paddleocr import PaddleOCR
from typing import Tuple
from structlog import get_logger

from app.models.schemas import KtpOcrResult
from app.config import get_settings

logger = get_logger(__name__)
settings = get_settings()


class OcrService:
    def __init__(self):
        self.ocr = PaddleOCR(
            use_angle_cls=True,
            lang='en',
            use_gpu=False,
            show_log=False
        )
        logger.info("PaddleOCR initialized")

    async def extract_ktp_data(self, image_data: bytes) -> KtpOcrResult:
        try:
            nparr = np.frombuffer(image_data, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if img is None:
                raise ValueError("Invalid image data")

            result = self.ocr.ocr(img, cls=True)

            ocr_text = self._extract_text(result)

            parsed_data = self._parse_ktp_data(ocr_text)

            confidence = self._calculate_confidence(result)

            logger.info(
                "KTP OCR extraction completed",
                nik=parsed_data.get('nik', 'N/A'),
                confidence=confidence
            )

            return KtpOcrResult(
                **parsed_data,
                confidence=confidence
            )

        except Exception as e:
            logger.error("KTP OCR extraction failed", exc_info=e)
            raise ValueError(f"OCR failed: {str(e)}")

    def _extract_text(self, ocr_result: list) -> str:
        if not ocr_result or not ocr_result[0]:
            return ""

        texts = []
        for line in ocr_result[0]:
            if line:
                text = line[1][0]
                texts.append(text)

        return "\n".join(texts)

    def _parse_ktp_data(self, text: str) -> dict:
        lines = text.strip().split('\n')
        data = {}

        for line in lines:
            line = line.strip()

            if ':' in line:
                key, value = line.split(':', 1)
                key = key.strip().lower()
                value = value.strip()

                if 'nik' in key or 'no' in key and '16' in value:
                    data['nik'] = value.replace(' ', '').replace('-', '')
                elif 'nama' in key:
                    data['name'] = value
                elif 'tempat' in key or 'lahir' in key:
                    parts = value.split(',')
                    if len(parts) == 2:
                        data['birth_date'] = parts[1].strip()
                elif 'alamat' in key:
                    data['address'] = value
                elif 'kelurahan' in key or 'desa' in key:
                    data['district'] = value
                elif 'kecamatan' in key:
                    data['district'] = value
                elif 'kabupaten' in key or 'kota' in key:
                    data['city'] = value
                elif 'provinsi' in key:
                    data['province'] = value
                elif 'pekerjaan' in key:
                    data['occupation'] = value
                elif 'kewarganegaraan' in key:
                    data['nationality'] = value
            elif len(line) == 16 and line.isdigit():
                data['nik'] = line
            elif self._is_date_format(line):
                data['birth_date'] = line

        defaults = {
            'gender': 'LAKI-LAKI',
            'province': 'Unknown',
            'city': 'Unknown',
            'district': 'Unknown',
            'nationality': 'WNI',
            'marital_status': 'Belum Kawin',
            'occupation': 'Tidak Bekerja'
        }

        return {**defaults, **data}

    def _is_date_format(self, text: str) -> bool:
        parts = text.replace('-', ' ').split()
        return (
            len(parts) == 3 and
            parts[0].isdigit() and len(parts[0]) == 2 and
            parts[1].isdigit() and len(parts[1]) == 2 and
            parts[2].isdigit() and len(parts[2]) == 4
        )

    def _calculate_confidence(self, ocr_result: list) -> float:
        if not ocr_result or not ocr_result[0]:
            return 0.0

        confidences = []
        for line in ocr_result[0]:
            if line:
                confidence = line[1][1]
                confidences.append(confidence)

        return sum(confidences) / len(confidences) if confidences else 0.0
