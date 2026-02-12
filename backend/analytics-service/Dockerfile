####
# Build stage - UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest AS builder

USER root
WORKDIR /app

# Install build dependencies
RUN dnf install -y gcc g++ glibc-devel && dnf clean all

# Copy requirements first for caching
COPY requirements.txt .

# Create virtual environment and install dependencies
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY src ./src

####
# Runtime stage - UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest

USER root

WORKDIR /app

# Create directories and set permissions first (as root)
RUN mkdir -p /app/tmp /app/logs /app/models /app/uploads && \
    chown -R 185:0 /app

# Copy virtual environment and source
COPY --from=builder --chown=185:0 /opt/venv /opt/venv
COPY --from=builder --chown=185:0 /app/src /app

ENV PATH="/opt/venv/bin:$PATH"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

USER 185

# Expose service port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080", "--workers", "4"]
