####
# Build stage - Using Red Hat UBI9 Python 3.12
####
FROM registry.access.redhat.com/ubi9/python-312:1-18 AS build

USER root
WORKDIR /build

# Install system dependencies
RUN microdnf install -y gcc g++ glibc-devel && \
    microdnf clean all

# Copy requirements and install dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir --user -r requirements.txt

####
# Runtime stage - Using minimal UBI9 Python 3.12
####
FROM registry.access.redhat.com/ubi9/python-312-minimal:1-18

LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Analytics Service - User insights, ML recommendations, Time-series analytics"
LABEL version="1.0.0"

USER 185

WORKDIR /app

# Copy installed packages from build stage
COPY --from=build /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Copy application code
COPY src/ /app/src/

# Expose service port
EXPOSE 8008

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8008/q/health || exit 1

# Application configuration
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV LOG_LEVEL=INFO

# Run application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8008", "--workers", "4"]
