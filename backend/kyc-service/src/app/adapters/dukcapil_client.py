import httpx
from structlog import get_logger
from typing import Optional

from app.models.schemas import DukcapilVerificationResult
from app.config import get_settings

logger = get_logger(__name__)
settings = get_settings()


class DukcapilClient:
    def __init__(self):
        self.base_url = settings.dukcapil_url
        self.client = httpx.AsyncClient(timeout=10.0)
        logger.info("Dukcapil client initialized", base_url=self.base_url)

    async def verify_nik(self, nik: str) -> DukcapilVerificationResult:
        try:
            logger.info("Verifying NIK with Dukcapil", nik=nik)

            response = await self.client.post(
                f"{self.base_url}/verify",
                json={"nik": nik}
            )

            response.raise_for_status()
            data = response.json()

            logger.info("Dukcapil verification completed", nik=nik, is_valid=data.get('is_valid'))

            return DukcapilVerificationResult(
                nik=data.get('nik', nik),
                is_valid=data.get('is_valid', False),
                name=data.get('name', ''),
                birth_date=data.get('birth_date', ''),
                gender=data.get('gender', ''),
                status=data.get('status', 'UNKNOWN'),
                match_score=data.get('match_score'),
                notes=data.get('notes')
            )

        except httpx.HTTPStatusError as e:
            logger.error("Dukcapil HTTP error", status_code=e.response.status_code, exc_info=e)
            return DukcapilVerificationResult(
                nik=nik,
                is_valid=False,
                name='',
                birth_date='',
                gender='',
                status='ERROR',
                notes=f"HTTP Error: {e.response.status_code}"
            )
        except Exception as e:
            logger.error("Dukcapil verification failed", exc_info=e)
            return DukcapilVerificationResult(
                nik=nik,
                is_valid=False,
                name='',
                birth_date='',
                gender='',
                status='ERROR',
                notes=f"Error: {str(e)}"
            )

    async def close(self):
        await self.client.aclose()
