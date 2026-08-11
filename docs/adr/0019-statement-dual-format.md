# ADR-0019: Statement Format — Dual Output (PDF + JSON/CSV)

**Status**: Accepted  
**Date**: 2026-05-07  
**Deciders**: PayU Engineering Team

## Context

`statement-service` generates account statements. Need to decide output format and audience.

## Decision

**Dual Output — PDF for end-users, JSON/CSV for project clients via API.**

### End-User (PDF)
- Existing endpoints: `GET /api/v1/statements/{id}`, `GET /api/v1/statements/{id}/download`
- PDF generated with RustFS storage, sent via email or downloaded
- Branded with PayU/project client logo

### Project Client (JSON/CSV)
- New endpoint: `GET /v1/partner/statements?from=YYYY-MM-DD&to=YYYY-MM-DD`
- Returns structured data for client's own reporting/analytics
- Supports pagination and date range filtering

### API Design
```
# End-user
GET    /api/v1/statements                      # List statements
GET    /api/v1/statements/{id}                 # Get statement metadata  
GET    /api/v1/statements/{id}/download        # Download PDF

# Project client (SNAP-BI)
GET    /v1/partner/statements                  # Query statements (JSON/CSV)
POST   /v1/partner/statements/generate         # Request statement generation
```

## Consequences

- ✅ End-users get familiar PDF format
- ✅ Project clients get machine-readable data for integration
- ✅ Clear separation between consumer API and partner API
- ⚠️ Statement service must support both output formats
- ⚠️ Partner API needs auth (SNAP-BI token)
