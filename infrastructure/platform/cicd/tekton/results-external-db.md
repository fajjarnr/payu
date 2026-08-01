# Tekton Results on HA PostgreSQL (CNPG)

Migrated 2026-08-01 (DEVSECOPS-017 durable stores / DEPLOY-009):

- Database `tekton_results` + role `tektonresults` di CloudNativePG `payu-database` (dev, HA 3 instance).
- Data hasil pipeline (17 records) di-`pg_dump` dari internal PG → restore ke CNPG.
- TektonConfig `spec.result` (SINGULAR — `results` tak dikenal CRD):
  ```yaml
  spec:
    result:
      is_external_db: true
      db_host: payu-database-rw.payu-dev.svc.cluster.local
      db_port: 5432
      db_name: tekton_results
      db_secret_name: tekton-results-db
      db_sslmode: require
      db_enable_auto_migration: true
  ```
- Secret `tekton-results-db` (username/password) di `openshift-pipelines`.
- Jangan patch `TektonResult` CR langsung — operator me-revert (owner = TektonConfig); ubah via `TektonConfig.spec.result`.

Role creation (reproducible):
```sql
CREATE ROLE tektonresults WITH LOGIN PASSWORD '<from-vault>';
CREATE DATABASE tekton_results OWNER tektonresults;
```
