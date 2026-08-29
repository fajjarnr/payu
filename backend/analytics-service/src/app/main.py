from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from structlog import get_logger
from prometheus_client import make_asgi_app
from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
import uuid

from app.logging_config import configure_logging
from app.config import get_settings
from app.api.v1 import analytics_router
from app.api.v1.websocket import websocket_router
from app.api.responses import ApiResponse
from app.database import init_db, close_db
from app.messaging.kafka_consumer import KafkaConsumerService
from app.rate_limit import limiter

configure_logging()
logger = get_logger(__name__)
settings = get_settings()

kafka_consumer: KafkaConsumerService | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global kafka_consumer
    startup_logger = logger.bind(
        service=settings.application_name, version=settings.version
    )
    startup_logger.info("Starting Analytics Service")

    if settings.enable_tracing:
        from opentelemetry import trace
        from opentelemetry.sdk.trace import TracerProvider
        from opentelemetry.sdk.trace.export import BatchSpanProcessor
        from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

        provider = TracerProvider()
        processor = BatchSpanProcessor(
            OTLPSpanExporter(endpoint=settings.otlp_endpoint)
        )
        provider.add_span_processor(processor)
        trace.set_tracer_provider(provider)
        startup_logger.info("OpenTelemetry tracing enabled")

    await init_db()
    startup_logger.info("Database initialized")

    kafka_consumer = KafkaConsumerService()
    await kafka_consumer.start()
    startup_logger.info("Kafka consumer started")

    yield

    if kafka_consumer:
        await kafka_consumer.stop()
        startup_logger.info("Kafka consumer stopped")

    await close_db()
    startup_logger.info("Shutting down Analytics Service")


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.application_name,
        version=settings.version,
        description="Analytics Service with TimescaleDB and ML Recommendations",
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )

    # Initialize rate limiter (shared instance — analytics.py uses the same)
    app.state.limiter = limiter

    # BUG-BE-048: CORS origins based on ENVIRONMENT env var
    import os
    cors_origins = [
        "https://payu.fajjjar.my.id",
        "https://app.payu.fajjjar.my.id",
        "https://api.payu.fajjjar.my.id",
        "https://backoffice.payu.fajjjar.my.id",
    ]
    if os.getenv("ENVIRONMENT", "production").lower() == "development":
        cors_origins.extend([
            "http://localhost:3000",
            "http://localhost:8080",
        ])

    app.add_middleware(
        CORSMiddleware,
        allow_origins=cors_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        allow_headers=[
            "X-Idempotency-Key",
            "Idempotency-Key",
            "X-Request-ID",
            "X-Correlation-Id",
            "Authorization",
            "Content-Type",
            "X-Client-Id",
            "X-Device-Id",
            "X-Signature",
            "X-Timestamp",
        ],
        expose_headers=["X-Request-ID", "X-Correlation-Id", "X-Idempotency-Key"],
    )

    # Include routers with rate limiting
    app.include_router(analytics_router, prefix="/api/v1")
    app.include_router(websocket_router)

    if settings.enable_metrics:
        metrics_app = make_asgi_app()
        app.mount("/metrics", metrics_app)

    if settings.enable_tracing:
        from opentelemetry import trace
        from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
        from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor

        FastAPIInstrumentor.instrument_app(app, tracer_provider=trace.get_tracer_provider())
        HTTPXClientInstrumentor().instrument()

    @app.middleware("http")
    async def add_request_id(request: Request, call_next):
        """Add request ID for tracing and correlation."""
        request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
        request.state.request_id = request_id
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id
        return response

    @app.get("/health")
    @limiter.limit("60/minute")
    async def health_check(request: Request):
        return ApiResponse.create_success(
            data={
                "status": "healthy",
                "service": settings.application_name,
                "version": settings.version,
            },
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

    @app.get("/ready")
    @limiter.limit("60/minute")
    async def readiness_check(request: Request):
        return ApiResponse.create_success(
            data={"status": "ready", "service": settings.application_name},
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

    @app.exception_handler(RateLimitExceeded)
    async def rate_limit_handler(request: Request, exc: RateLimitExceeded):
        """Handle rate limit exceeded errors."""
        request_id = getattr(request.state, "request_id", None)
        logger.warning(
            "Rate limit exceeded",
            path=request.url.path,
            client=get_remote_address(request),
            request_id=request_id,
        )
        return JSONResponse(
            status_code=429,
            content=ApiResponse.create_error(
                code="ANA_RAT_001",
                message="Rate limit exceeded. Please try again later.",
                request_id=request_id,
            ).model_dump(),
        )

    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        """Handle unhandled exceptions with standardized response."""
        request_id = getattr(request.state, "request_id", None)
        logger.error(
            "Unhandled exception",
            exc_info=exc,
            path=request.url.path,
            request_id=request_id,
        )
        return JSONResponse(
            status_code=500,
            content=ApiResponse.create_error(
                code="ANA_SYS_001",
                message="An unexpected error occurred. Please try again later.",
                request_id=request_id,
            ).model_dump(),
        )

    return app


app = create_app()
