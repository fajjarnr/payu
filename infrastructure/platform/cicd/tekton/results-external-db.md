# Tekton Results durable backend decision

Status verified 2026-08-06: the Tekton Results operator, API, watcher, and
365-day retention policy are Ready, but the live `TektonResult` still has
`is_external_db: false` and uses the operator-managed PostgreSQL deployment.

The CNPG `payu-tekton-results` Database object exists in `payu-dev`, but it is
not connected to Tekton Results. The expected `tekton-results-db` Secret is also
not present; the operator-managed `tekton-results-postgres` Secret is not a
production durable-secret contract.

Do not patch `TektonResult` directly: it is owned by `TektonConfig` and the
operator reverts direct edits. The production migration remains open until a
dedicated Vault-backed database role/Secret exists, the database endpoint is
reachable from `openshift-pipelines`, and the migration is verified with live
API records plus a restore test.
