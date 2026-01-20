####
# Single Stage Build - Using UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest

USER root
WORKDIR /app

# Install system dependencies
RUN dnf install -y gcc g++ glibc-devel && \
    dnf clean all

# Copy requirements and install dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY src/ /app/src/

USER 185

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
