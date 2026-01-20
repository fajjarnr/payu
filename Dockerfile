FROM continuumio/miniconda3

WORKDIR /app

# Install testing dependencies
COPY requirements-test.txt .
RUN pip install --no-cache-dir /tmp/pip -r requirements-test.txt

# Copy application code
COPY src/ /app/src

# Create non-root user
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app && \
    chmod -R 755 /app
USER appuser

EXPOSE 8005

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8005"]
