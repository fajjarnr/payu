####
# Build stage - UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest AS builder

USER root
WORKDIR /app

# Install build dependencies
RUN dnf install -y gcc g++ glibc-devel && dnf clean all

# Copy entire project
COPY . .

# Install dependencies from the specific service directory
RUN pip install --user --no-cache-dir -r analytics-service/requirements.txt

####
# Runtime stage - UBI9 Minimal
####
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3

# Install runtime dependencies
RUN microdnf install -y curl-minimal && microdnf clean all

WORKDIR /app

# Copy Python packages and source
COPY --from=builder /root/.local /root/.local
COPY --from=builder /app/analytics-service/src /app/src

ENV PATH=/root/.local/bin:$PATH
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Create directories and set permissions
RUN mkdir -p /app/tmp /app/logs /app/models /app/uploads && \
    chown -R 185:0 /app

USER 185

# Expose service port
EXPOSE 8008

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8008/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8008", "--workers", "4"]
