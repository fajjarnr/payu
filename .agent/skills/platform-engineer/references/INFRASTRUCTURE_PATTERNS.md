# Infrastructure & Containerization Patterns

## 🐳 Podman & Container Orchestration
*   **Volume Syntax**: Use standard bind-mount or named volumes. Advanced Docker types may fail.
*   **Fully Qualified Names**: Prepend `docker.io/` to avoid interactive prompts.
*   **Local Tagging**: Always provide an `image:` tag (e.g., `localhost/payu-service`) when using `build`.
*   **Port Standardization**: All microservices MUST listen on internal port **8080**.
    *   **Dockerfile**: `EXPOSE 8080`.
    *   **App**: `server.port=8080`.
    *   **Compose**: Map host ports (8001:8080) but healthcheck `localhost:8080`.

## 🏗️ Monorepo Build Optimization
*   **Shared Library Access**: Set build `context` to `backend/` root, not service subfolder.
*   **Maven Flags**: Use `-pl :service-name -am` to build only what's needed.
*   **Decoupled Build**: For resource-constrained environments, build JAR on host and only `COPY` it in the Dockerfile.

## 🐍 Python ML Containerization
*   **Base Image**: Use `python:3.12-slim` for services needing `libGL.so.1` or `libgomp.so.1` (OpenCV, OCR). UBI9 Minimal is too restrictive for some C-extensions.
*   **Build Tool**: Use `uv` for 5x faster package installation compared to `pip`.
*   **Memory**: ML services (KYC, Analytics) need ~2GB RAM limits to avoid "Killed" (137) errors during model loading.

## ☁️ OpenShift Specifics
*   **NetworkPolicy**: Ensure pods have `app.kubernetes.io/part-of: payu-banking` label to allow intra-namespace traffic. Default router policy might block pod-to-pod calls.
*   **DataGrid (Infinispan)**: RESP connector does NOT support custom `port: 6379`. It uses the default `11222`.
*   **Vault Dev Mode**: Use simple `Deployment` (not StatefulSet). Set `VAULT_ADDR=http://127.0.0.1:8200` for healthchecks.
*   **ExternalName DNS**: Always use FQDN (e.g., `...svc.cluster.local`) for ExternalName services to avoid NXDOMAIN errors.
