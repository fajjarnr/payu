---
name: ai-engineer
description: PayU AI and ML engineering for Python 3.12 FastAPI analytics and KYC services, fraud and risk rules, OCR/face/liveness inference, async SQLAlchemy/TimescaleDB/Kafka, Pydantic contracts, model lifecycle, observability, security, and Context7-first library verification. Use when designing, implementing, debugging, reviewing, or testing AI/ML features in this repository.
---

# PayU AI Engineer

Build small, explainable, reproducible AI services that respect PayU's money, privacy, security, and event-processing rules.

## Operating contract

- Read the repository `AGENTS.md`, the target service's `README.md`, `requirements.txt`, `pyproject.toml`, `Containerfile`, and tests before changing behavior.
- Read `api-architect`, `data-architect`, `quality-engineer`, or `cybersecurity-architect` when the task crosses those boundaries.
- Resolve every library or service in Context7 before relying on its API. Query the exact concept, compare the result with the pinned version, and do not silently upgrade dependencies.
- Start with a failing test for behavior changes. Make the smallest implementation pass, then run the narrowest useful test and the service quality gate.
- Treat existing code as evidence, not as proof that a security or money rule is already correct. Call out legacy gaps without copying them into new code.

## Repository baseline

The current Python AI surface is:

- `backend/analytics-service`: Python 3.12 on UBI9; FastAPI 0.115.0, Pydantic 2.9.0, async SQLAlchemy 2.0.35, asyncpg, aiokafka, TimescaleDB, Prometheus/OpenTelemetry, scikit-learn 1.5.1, pandas 2.2.2, NumPy 1.26.4, and CPU-only Torch 2.5.0.
- `backend/kyc-service`: FastAPI/Pydantic with PaddleOCR 2.9.0, Paddle2ONNX 1.3.0, OpenCV, Pillow, scikit-learn, and CPU-only Torch/TorchVision for OCR, face matching, and liveness.
- Containers install pinned requirements with `uv`, run as arbitrary-compatible non-root UID 1001, expose port 8080, and must remain CPU-safe unless a workload explicitly proves a GPU requirement.
- Analytics currently implements deterministic fraud rules, spending recommendations, robo-advisory templates, event-driven aggregation, and WebSocket updates. It does not currently provide an MLflow, Evidently, LangChain, OpenAI, or ONNX Runtime platform; do not present those as existing infrastructure.
- Context7 documentation may describe a newer release than the pinned packages. Verify compatibility before using newer APIs; update the dependency and its tests as a separate change.

## FastAPI service design

- Use `@asynccontextmanager` lifespan for database pools, Kafka consumers, tracing, and heavy model load/cleanup. Avoid import-time connections, model loading, or network calls.
- Use `async def` for non-blocking I/O. Keep CPU-bound inference out of the event loop: use a regular `def` path operation when appropriate or a bounded executor/`asyncio.to_thread` from an async path. Never create unbounded tasks or call `time.sleep` in async code.
- Treat Uvicorn worker count as an architecture decision. Each worker can load its own model and start its own Kafka consumer; verify memory, partition ownership, duplicate side effects, and shutdown behavior before changing `--workers`.
- Keep routers thin: authenticated request → validated DTO → application service → repository/adapter. Do not mix feature queries, model code, persistence, and response formatting in one route.
- Bound request body size, batch size, image dimensions, inference time, and external calls. Use explicit timeouts and bounded retries for remote dependencies.
- Keep CORS origins explicit, rate-limit expensive endpoints, propagate `X-Request-ID`/correlation IDs, and return the repository's versioned error envelope. Do not expose stack traces, prompts, PII, or model internals in responses.

## Pydantic contracts

- Define request and response DTOs with Pydantic v2. Prefer `Field` constraints, `Annotated`, `ConfigDict(extra="forbid")`, and strict fields at trust boundaries; do not accept silently coerced or unknown input where it changes meaning.
- Represent money as `Decimal`, quantized to `0.0001` with `ROUND_HALF_EVEN`. Use PostgreSQL `NUMERIC(19,4)`. A bounded risk score may be a float, but never use a float for amount, balance, fee, limit, or ledger data.
- Validate identifiers, timestamps, enums, currencies, image metadata, collection sizes, and nested metadata. Reject `NaN`, infinity, negative amounts, oversized payloads, and unexpected fields.
- Serialize deliberately with `model_dump(mode="json")`; keep inbound and outbound field aliases explicit for versioned contracts.
- Keep domain enums as top-level types and keep DTOs under the service's interfaces/models boundary before business logic.

## Analytics, features, and events

- Use async SQLAlchemy sessions and service-owned schemas. Keep feature queries bounded by time, indexed by the access path, and point-in-time correct; never use information that was unavailable when the transaction decision was made.
- Use `NUMERIC(19,4)` plus `Decimal` for transaction analytics and balances. Keep the distinction explicit between financial amounts and statistical measurements such as risk score, latency, or rate.
- Treat CloudEvents and event identity as part of the contract. Use the repository topic convention `payu.<domain>.<event-type>.v<n>` and publish through the approved outbox path when producing events.
- Make consumers at-least-once safe: claim `(source, event_id)` or an equivalent idempotency key, commit database work before acknowledging/advancing the offset, and test duplicate delivery, crash/retry, poison messages, and schema evolution.
- Do not perform a financial authorization, balance mutation, or ledger write directly from an AI score. Return a versioned decision with reason codes; let the owning domain enforce the final policy.
- Keep PII and biometric data minimized. Mask NIK, account identifiers, card data, IP/device identifiers, and image-derived data in logs; never log raw images, full payloads, secrets, or prompts.

## Model and rule lifecycle

For deterministic rules, such as the current fraud engine:

- Give the rule set, thresholds, weights, and decision mapping an explicit version.
- Keep score ranges, risk levels, triggered rules, recommended action, and manual-review behavior deterministic and explainable.
- Use `Decimal` for amount thresholds and test boundary values, timezone handling, missing history, duplicate events, and conflicting signals.
- Store enough non-sensitive provenance to reproduce a decision: model/rule version, feature names or reason codes, timestamp, and input contract version.

For learned models:

- Version dataset snapshots, feature definitions, code, dependencies, training seed, artifact digest, and evaluation report together.
- Split by time, account, or other leakage boundary appropriate to the problem. Fit transformations only on training data; use a pipeline so preprocessing cannot leak test information.
- Evaluate the operational metric, not only accuracy: calibration, precision/recall at the review or blocking threshold, false positives, false negatives, latency, and subgroup behavior.
- Validate feature ranges, missingness, schema, and distribution before inference. Fail closed or route to review when required features are unavailable.
- Treat model artifacts as executable input. Verify provenance and digest; never load untrusted pickle, joblib, or cloudpickle. Prefer a safer format such as ONNX or `skops` only after verifying support and dependency impact in Context7.
- Support shadow/canary rollout, explicit model fallback, rollback, and human review. Do not add A/B infrastructure until a concrete product decision requires it.

## OCR, face, and liveness

- Enforce authenticated ownership and purpose limitation before processing KYC media.
- Validate MIME type, file size, image dimensions, decode success, orientation, and decompression limits before handing data to PaddleOCR/OpenCV/Torch.
- Keep model paths and versions configuration-driven but sourced from the image or approved artifact store, never from user input. Verify artifact hashes and fail startup when a required model is absent or incompatible.
- Isolate CPU-heavy inference, cap concurrency, and emit latency/error/model-version metrics without recording the image or biometric template.
- Treat OCR output as untrusted data. Normalize and validate it against the Pydantic contract, mask sensitive fields in logs, and require a deterministic confidence/review policy for low-quality results.
- Test spoofed images, invalid encodings, oversized images, empty detections, low confidence, model load failure, timeout, and retry behavior.

## Observability and drift

- Instrument request count, error count, validation failures, inference latency, model/rule version, queue lag, consumer retries, feature missingness, and output distribution.
- Use Prometheus/OpenTelemetry already present in the Python services. Do not add MLflow, Evidently, a feature store, or another telemetry stack without a scoped requirement and dependency review.
- Alert on service SLOs, Kafka lag, database pool exhaustion, model load failures, latency budgets, and meaningful data-quality/drift thresholds. Sampling must not hide safety-critical errors.
- Never use high-cardinality labels containing user IDs, transaction IDs, raw prompts, or PII.

## Security and configuration

- Validate JWT signature, issuer, audience, expiry, and required claims through the approved Keycloak/Spring/gateway integration. Never authorize by merely base64-decoding a JWT. The analytics `require_auth` helper is legacy behavior to replace or isolate, not a template for new code.
- Enforce tenant/subject ownership server-side; do not trust `user_id` from a request body or forwarded header without authorization checks.
- Load secrets from Vault/approved secret operators. Do not use default `SECRET_KEY`, hard-coded credentials, HS256 examples, or secrets in `.env` files for production.
- Protect inference endpoints with authentication, authorization, rate limits, idempotency where retries can duplicate work, and safe error responses. Use mTLS and TLS verification for service and provider calls.
- For external LLMs, there is no approved provider dependency in the current baseline. If a task introduces one, threat-model prompt injection, data egress, tool access, retention, structured-output validation, timeout/retry, and human override first. Never send raw PII or use an LLM as the final authority for money movement, KYC approval, or access control.

## Testing and delivery

- Follow TDD and the service's existing pytest configuration; analytics currently gates coverage at 80%.
- Unit-test pure rules and feature transforms with real values and boundary cases. Do not test mock call choreography as the primary assertion.
- Add API contract tests for validation, authz, error envelopes, idempotency, rate limiting, and OpenAPI output.
- Add integration tests for PostgreSQL/TimescaleDB schema behavior, Kafka CloudEvents, duplicate delivery, transaction rollback, and lifecycle cleanup. Use Testcontainers or the repository's existing fixtures when available.
- Add inference tests for artifact provenance, deterministic outputs, malformed inputs, resource limits, timeouts, and model-version compatibility.
- Run the narrowest relevant tests first, then `pytest` with coverage, container/build checks, and the repository validation loop. Do not claim tests pass without command output.

## Review checklist

- [ ] Context7 resolved the exact library and the pinned version was checked.
- [ ] A failing test existed before production behavior changed.
- [ ] Money uses `Decimal`/`NUMERIC(19,4)` and `ROUND_HALF_EVEN`.
- [ ] Request, event, image, and model-artifact boundaries are validated and bounded.
- [ ] Feature computation is point-in-time correct and free of leakage.
- [ ] Consumer retries are idempotent and database acknowledgement ordering is safe.
- [ ] Model/rule version, provenance, rollback, and review behavior are explicit.
- [ ] Logs and metrics contain no PII, raw media, secrets, prompts, or high-cardinality identifiers.
- [ ] Authz, secrets, TLS, rate limits, and safe errors use approved PayU patterns.
- [ ] Tests cover real behavior, failure paths, and the service quality gate.

## Context7-first references

- [FastAPI lifespan and async](https://fastapi.tiangolo.com/advanced/events/)
- [FastAPI parameter validation](https://fastapi.tiangolo.com/reference/parameters/)
- [Pydantic fields and strict configuration](https://docs.pydantic.dev/latest/concepts/fields/)
- [scikit-learn common pitfalls](https://scikit-learn.org/stable/common_pitfalls.html)
- [scikit-learn model persistence and security](https://scikit-learn.org/stable/model_persistence.html)
