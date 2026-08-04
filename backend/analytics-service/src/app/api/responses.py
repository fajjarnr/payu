"""
API Response utilities for standard response envelope.

Provides consistent response format across all API endpoints:
{
    "success": bool,
    "data": Any,
    "error": Optional[ErrorDetail],
    "meta": Optional[MetaData]
}
"""

from typing import Any, Optional, Dict, List
from datetime import datetime
from pydantic import BaseModel, Field


class ErrorDetail(BaseModel):
    """Standard error detail structure."""

    code: str = Field(..., description="Error code for programmatic handling")
    message: str = Field(..., description="Human-readable error message")
    details: Optional[Dict[str, Any]] = Field(
        default=None, description="Additional error context"
    )
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class MetaData(BaseModel):
    """Metadata for paginated or enriched responses."""

    page: Optional[int] = Field(default=None, description="Current page number")
    page_size: Optional[int] = Field(default=None, description="Items per page")
    total: Optional[int] = Field(default=None, description="Total items available")
    total_pages: Optional[int] = Field(
        default=None, description="Total pages available"
    )
    request_id: Optional[str] = Field(
        default=None, description="Request correlation ID"
    )
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class ApiResponse(BaseModel):
    """Standard API response envelope."""

    success: bool = Field(..., description="Indicates if the request was successful")
    data: Optional[Any] = Field(default=None, description="Response payload")
    error: Optional[ErrorDetail] = Field(
        default=None, description="Error details if success=False"
    )
    meta: Optional[MetaData] = Field(default=None, description="Response metadata")

    @classmethod
    def create_success(
        cls,
        data: Any = None,
        meta: Optional[MetaData] = None,
        request_id: Optional[str] = None,
    ) -> "ApiResponse":
        """Create a successful response."""
        if meta is None and request_id:
            meta = MetaData(request_id=request_id)
        elif meta and request_id:
            meta.request_id = request_id

        return cls(success=True, data=data, meta=meta)

    def model_dump(self, *args, **kwargs):
        kwargs.setdefault("mode", "json")
        return super().model_dump(*args, **kwargs)

    @classmethod
    def create_error(
        cls,
        code: str,
        message: str,
        details: Optional[Dict[str, Any]] = None,
        request_id: Optional[str] = None,
    ) -> "ApiResponse":
        """Create an error response."""
        return cls(
            success=False,
            error=ErrorDetail(
                code=code,
                message=message,
                details=details,
            ),
            meta=MetaData(request_id=request_id) if request_id else None,
        )

    @classmethod
    def paginated(
        cls,
        data: List[Any],
        page: int,
        page_size: int,
        total: int,
        request_id: Optional[str] = None,
    ) -> "ApiResponse":
        """Create a paginated response."""
        total_pages = (total + page_size - 1) // page_size if page_size > 0 else 0
        meta = MetaData(
            page=page,
            page_size=page_size,
            total=total,
            total_pages=total_pages,
            request_id=request_id,
        )
        return cls.create_success(data=data, meta=meta, request_id=request_id)
