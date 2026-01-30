####
# Multi-Stage Build - KYC Service (FastAPI)
# Builder: UBI9 Python 3.11
# Runtime: UBI9 Minimal
####

# ============================================
# Stage 1: Builder
# ============================================
FROM registry.access.redhat.com/ubi9/python-311:latest AS builder

USER root
WORKDIR /app

# Install build dependencies for native extensions
RUN dnf install -y gcc g++ glibc-devel zlib-devel libjpeg-devel libpng-devel cmake && \
    dnf clean all

# Copy requirements and install Python dependencies
COPY requirements.txt ./
RUN pip install --user --no-cache-dir -r requirements.txt

# Copy application source
COPY src/ /app/src/

# ============================================
# Stage 2: Runtime
# ============================================
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3

# OCI Metadata Labels
LABEL org.opencontainers.image.title="PayU KYC Service"
LABEL org.opencontainers.image.description="KYC verification service with OCR and liveness detection"
LABEL org.opencontainers.image.vendor="PayU"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="PayU Platform Team"

# Install runtime dependencies
RUN microdnf install -y python3.11 curl && \
    microdnf clean all && \
    rm -rf /var/cache/yum

WORKDIR /app

# Copy Python packages from builder
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV LOG_LEVEL=INFO

# Copy application source
COPY --from=builder /app/src /app/src

# Create directories for ML models, temp files, and logs
RUN mkdir -p /app/models /app/temp /app/uploads /app/logs && \
    chown -R 185:0 /app

USER 185

# Volume mounts for persistent data
VOLUME ["/app/models", "/app/temp", "/app/uploads", "/app/logs"]

# Expose service port
EXPOSE 8007

# Health check - Fixed endpoint from /q/health to /health
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8007/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8007", "--workers", "4"]
