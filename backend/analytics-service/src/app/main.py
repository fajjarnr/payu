from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from structlog import get_logger
from prometheus_client import make_asgi_app
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor

from app.config import get_settings
from app.api.v1 import analytics_router
from app.database import init_db, close_db
from app.messaging.kafka_consumer import KafkaConsumerService

logger = get_logger(__name__)
settings = get_settings()

kafka_consumer: KafkaConsumerService | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global kafka_consumer
    startup_logger = logger.bind(service=settings.application_name, version=settings.version)
    startup_logger.info("Starting Analytics Service")

    if settings.enable_tracing:
        provider = TracerProvider()
        processor = BatchSpanProcessor(OTLPSpanExporter(endpoint=settings.otlp_endpoint))
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
        lifespan=lifespan
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(analytics_router, prefix="/api/v1")

    if settings.enable_metrics:
        metrics_app = make_asgi_app()
        app.mount("/metrics", metrics_app)

    FastAPIInstrumentor.instrument_app(app, tracer_provider=trace.get_tracer_provider())
    HTTPXClientInstrumentor().instrument()

    @app.get("/health")
    async def health_check():
        return {"status": "healthy", "service": settings.application_name, "version": settings.version}

    @app.get("/ready")
    async def readiness_check():
        return {"status": "ready", "service": settings.application_name}

    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        logger.error("Unhandled exception", exc_info=exc, path=request.url.path)
        return JSONResponse(
            status_code=500,
            content={"detail": "Internal server error", "error_code": "ANA_SYS_001"}
        )

    return app


app = create_app()
