####
# Single Stage Build - Using UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest

USER root
WORKDIR /app

# Install system dependencies
RUN dnf install -y gcc g++ glibc-devel zlib-devel libjpeg-devel libpng-devel cmake && \
    dnf clean all

# Copy requirements and install dependencies
COPY requirements.txt ./
# Install to the active virtualenv (no --user)
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY src/ /app/src/

# Create directories for ML models and temp files
RUN mkdir -p /app/models /app/temp /app/uploads && \
    chown -R 185:0 /app

USER 185

# Expose service port
EXPOSE 8007

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8007/q/health || exit 1

# Application configuration
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV LOG_LEVEL=INFO

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8007", "--workers", "4"]
