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

# Create virtual environment and install dependencies
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir -r kyc-service/requirements.txt

####
# Runtime stage - UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest

WORKDIR /app

# Copy virtual environment and source
COPY --from=builder /opt/venv /opt/venv
COPY --from=builder /app/kyc-service/src /app/src

ENV PATH="/opt/venv/bin:$PATH"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Create directories and set permissions
RUN mkdir -p /app/tmp /app/logs /app/models /app/uploads && \
    chown -R 185:0 /app

USER 185

# Expose service port
EXPOSE 8080

# Health check (Standardize to /health or /actuator/health for uniformity)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080", "--workers", "4"]
