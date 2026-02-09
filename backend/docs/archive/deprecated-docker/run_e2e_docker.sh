#!/bin/bash
set -e

echo "Starting PayU E2E Docker Environment..."

# Update host for simulators if needed, or rely on docker networking.
# Start Docker Compose
docker-compose up -d --build

echo "Waiting for services to be healthy..."
# A simple wait loop or use docker-compose wait (if available in newer versions)
# or wait for a specific service.
# Let's wait for gateway.

MAX_RETRIES=30
for i in $(seq 1 $MAX_RETRIES); do
    if curl -s http://localhost:8080/q/health > /dev/null; then
        echo "Gateway is up!"
        break
    fi
    echo "Waiting for Gateway... ($i/$MAX_RETRIES)"
    sleep 5
done

if ! curl -s http://localhost:8080/q/health > /dev/null; then
    echo "Gateway failed to start."
    docker-compose logs gateway-service
    exit 1
fi

echo "Setting up Python Environment..."
cd tests/e2e_blackbox
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

echo "Running E2E Tests..."
pytest test_full_flow.py -v

echo "Tests Completed."
# Optional: Tear down
# docker-compose down
