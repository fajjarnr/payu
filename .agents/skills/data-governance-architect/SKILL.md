---
name: data-governance-architect
description: PayU data governance for catalog ownership, data classification, PII and financial-data handling, lineage, consent and purpose, data quality, retention, access audit, data-subject workflows, and UU PDP/POJK/BI evidence. Use when defining what data may be collected, used, shared, retained, or audited; do not use this skill for schema/index/migration implementation, which belongs to data-architect.
---

# PayU Data Governance Architect

Define the rules, ownership, evidence, and accountability for data. Hand database schemas, migrations, indexes, RLS, locking, backups, and storage implementation to `data-architect`; coordinate with `cybersecurity-architect` for controls and `compliance-auditor`/legal for regulatory interpretation.

## Operating contract

- Read `AGENTS.md`, the owning service's catalog descriptor, API/event contract, migration history, security controls, and operations policy before making a governance decision.
- Start from the data asset and its purpose, not from a catalog or compliance product.
- Verify current repository and deployed configuration. A YAML example, audit note, catalog entry, or retention ConfigMap is not proof that a control is enforced.
- Use Context7 before relying on Backstage, OpenLineage, Great Expectations, a catalog client, or another third-party tool. Resolve the installed or proposed version, query the exact concept, and record version mismatches.
- Do not make legal/compliance claims from memory. Map each obligation to an approved policy, legal/compliance owner, effective date, evidence source, and review date.
- Do not expose raw PII in catalog metadata, lineage payloads, quality results, examples, logs, or tickets.

## Repository baseline

Treat these as observed implementation points to verify when they drift:

- `catalog-info.yaml` is the current Backstage/Red Hat Developer Hub inventory surface. It models systems, components, APIs, resources, ownership, lifecycle, dependencies, links, and operational annotations; it is not a complete column-level data catalog.
- The platform has service-owned domains including account, transaction, wallet, KYC, compliance, analytics, lending, investment, and partner. Confirm the actual data owner and steward with the domain team; Backstage service ownership alone does not prove data ownership.
- `compliance-service` records `DataAccessAudit` fields such as subject, accessor, service, resource, operation, purpose, source address, success, and time. `security-starter` publishes audit records through the approved outbox path to `payu.security.audit-log.v1`; preserve this boundary and test delivery failure/replay.
- `security-starter` provides `@Sensitive`-based masking and log masking. Verify annotation coverage and output tests for every new sensitive DTO; do not assume all PII is automatically protected.
- `infrastructure/platform/security/compliance/data-retention-policy.yaml` contains proposed retention and cleanup behavior. It is not a legal approval or evidence of an active, safe policy. In particular, never use a generic cleanup job to delete immutable ledger facts or compliance evidence.
- OpenLineage, DataHub, and Great Expectations are not observed as installed platform dependencies. Treat them as optional integrations requiring an owner, architecture decision, privacy review, and operational budget.

## Governance workflow

1. **Scope the asset** — identify the system, table/topic/object/API, fields, environment, data subject, producer, consumers, and business purpose.
2. **Assign accountability** — record business owner, data steward, technical custodian, processors, consumers, approver, and escalation path. Keep data ownership separate from service deployment ownership.
3. **Classify** — classify at field level where sensitivity differs; record the rationale, handling rules, and review date.
4. **Define permitted use** — record purpose, legal/policy basis, consent requirement, allowed consumers, geographic boundary, sharing restrictions, and prohibited uses.
5. **Map lineage** — trace source → command/API or event → transformation → storage → downstream consumer/report/model. Include schema/version and time semantics.
6. **Define quality** — write measurable rules, severity, threshold, owner, validation cadence, failure action, and evidence location.
7. **Define lifecycle** — set retention trigger, archive tier, legal hold behavior, anonymization/deletion method, approver, and restore implications.
8. **Prove and review** — attach catalog validation, access logs, quality results, lineage evidence, retention dry-runs, reconciliation, and review sign-off. Reassess after schema, purpose, consumer, or regulation changes.

## Classification and handling

Use labels that are understandable to the business and map them to the approved PayU policy. Do not invent a universal numeric level without policy approval. At minimum distinguish:

- **Public** — safe for public release after owner approval.
- **Internal** — operational or business data for PayU workforce and approved systems.
- **Confidential** — customer profile, contact data, transaction details, balances, fraud signals, and partner data requiring controlled access.
- **Restricted** — authentication material, PINs, secrets, PAN/CVV, NIK, biometrics, identity documents, and data whose exposure creates severe harm.

For each field, record `classification`, `contains_pii`, `contains_financial_data`, `contains_authentication_or_biometric_data`, `masking`, `encryption/tokenization`, allowed roles, purpose, retention, and data owner. Classification is not a substitute for threat modeling or access control.

Never put NIK, PIN, PAN, CVV, passwords, tokens, biometric templates, raw identity documents, or full account numbers in catalog descriptions, test fixtures, lineage facets, metrics labels, or logs. Use stable opaque asset IDs and masked examples.

## Catalog and ownership

Use Backstage descriptors for discoverability:

- `System` groups a business or platform boundary; `Component` represents a deployed service; `API` represents a contract; `Resource` represents infrastructure or a data store.
- Set stable `metadata.name`, `spec.owner`, `spec.system`, lifecycle, dependencies, API relations, and source/TechDocs links. Keep annotations operational and non-sensitive.
- Add a pointer to the governed data contract or classification record instead of embedding secrets, PII samples, or an unmaintainable column dump in `catalog-info.yaml`.
- Record an owner and steward who can approve access, purpose changes, quality exceptions, retention, and incident response. An unowned asset is a governance failure, not merely missing metadata.
- Detect stale descriptors: missing owners, broken links, orphaned resources, duplicate identifiers, undocumented dependencies, and catalog entries that claim a lifecycle not supported by deployment evidence.

## Lineage and data contracts

Represent lineage as metadata, not as a copy of production data. For each edge record:

- source and target asset IDs, field mappings when material, producer/consumer, transformation or decision logic, event/API/schema version, timestamp semantics, run/version ID, owner, classification propagation, and quality result;
- Kafka topic, CloudEvents type, outbox boundary, database table, report, feature, or model only when it is an actual repository/deployment boundary;
- whether the edge is authoritative, derived, cached, replicated, exported, or manually corrected.

Use OpenLineage only when a real producer and backend are funded and operated. Its run/job/dataset identity and facets are suitable for lineage metadata; do not emit raw payloads, PII, secrets, or unrestricted SQL containing sensitive values. Column-level lineage is required only for high-impact fields or transformations where the risk justifies the maintenance cost.

For every governed API/event/data asset, maintain a contract containing schema/version, owner, purpose, classifications, permitted consumers, retention, quality rules, failure behavior, and change-approval path. Use the existing OpenAPI/CloudEvents conventions; do not create a second incompatible schema registry by hand.

## Privacy, purpose, and consent

- Apply collection minimization, purpose limitation, access limitation, retention limitation, and secure disposal. A field without a documented purpose should not be collected or replicated.
- Record consent version, notice/purpose, subject, timestamp, channel, scope, expiry, withdrawal, and downstream enforcement where consent is the legal/policy basis. Never default optional analytics or partner sharing to enabled without product and compliance approval.
- Separate essential service processing from marketing, analytics/personalization, and third-party sharing. A withdrawal must stop the applicable use and be traceable to affected consumers.
- Support approved data-subject access, correction, portability, objection/withdrawal, and erasure workflows. Verify identity, scope, export format, redaction, legal hold, and completion evidence.
- Do not call every request “GDPR.” Use the applicable Indonesian policy/law and approved legal interpretation. The governance skill records controls and evidence; it does not certify compliance.
- For erasure or correction, preserve immutable financial facts and audit evidence where retention is required. Pseudonymize or remove only non-essential personal links through an approved, reversible-tested workflow; never update or delete posted ledger entries as a convenience.

## Data quality and reconciliation

Define a quality contract per critical asset:

- dimensions: completeness, validity, uniqueness, consistency, timeliness/freshness, accuracy, reconciliation, and schema compatibility;
- rule, population/batch, severity, threshold, owner, cadence, evidence location, alert, quarantine/replay behavior, and exception expiry;
- financial invariants such as `NUMERIC(19,4)` representation, explicit currency, balanced debit/credit totals, idempotent event identity, and source-to-projection reconciliation.

Quality targets are policy decisions, not generic numbers. Do not copy `99.9%`, `100%`, or a fixed retention period without a domain owner and measured baseline. Great Expectations may implement validations if approved, but a suite/checkpoint result is evidence only when it ran against the named batch and its result is retained.

When a rule fails, preserve the failed batch identity, rule version, observed values in masked/aggregated form, impact, owner, remediation, and revalidation result. Do not silently coerce, drop, or rewrite bad financial data.

## Retention, archive, deletion, and legal hold

A retention record must state asset, classification, retention trigger, duration, legal/policy basis, owner, archive location, access controls, deletion/anonymization method, verification, and legal-hold override. The trigger may be account closure, event completion, consent withdrawal, record supersession, or another approved event—not simply “created_at plus N days.”

- Treat current retention manifests as configuration proposals until compliance/legal and the data owner approve them. Verify the live controller, namespace, credentials, target tables, schedule, and dry-run output before activation.
- Separate operational log/trace retention, Kafka retention, audit retention, KYC evidence, analytics projections, and immutable financial records. They have different owners and recovery requirements.
- Never run broad `DELETE` or partition drops over ledger, journal, posted transaction, balance history, or audit evidence. Use archive, legal hold, anonymization of approved fields, or a forward domain workflow.
- Make cleanup jobs least-privileged, scoped, observable, idempotent, dry-run capable, and blocked by legal hold. Keep deletion receipts and reconciliation evidence without logging the deleted PII.
- Test retention against restore, replication, outbox replay, subject requests, and incident investigation before production rollout.

## Access governance and audit evidence

- Grant least privilege by role, purpose, tenant, environment, and field; separate customer, support, operations, compliance, auditor, and platform access.
- Require approval, expiry, periodic review, and immediate revocation for elevated or break-glass access. Do not treat a Backstage owner or database role as automatic permission to view PII.
- Audit reads, exports, searches, changes, failed attempts, consent changes, subject requests, retention actions, and policy exceptions. Capture actor, subject, purpose, resource, operation, service, tenant, time, result, correlation ID, and approval reference.
- Keep audit records tamper-evident and delivered through the approved outbox path. Monitor relay lag, failure, duplicate events, and gaps; protect audit-query endpoints themselves.
- Mask identifiers and error details in audit evidence. Store enough metadata to investigate without placing raw PII or secrets in the event.

## Verification checklist

- [ ] Asset, field scope, owner, steward, custodian, consumers, purpose, and classification are recorded.
- [ ] Backstage metadata is valid and points to, but does not replace, the governed data contract.
- [ ] Source-to-consumer lineage includes event/API/schema versions and no raw PII.
- [ ] Consent/legal basis, permitted uses, subject workflow, and cross-border/third-party restrictions are approved.
- [ ] Quality rules have owners, thresholds, failure actions, and retained batch results.
- [ ] Retention trigger, legal basis, archive/disposal, legal hold, dry-run, and restore implications are documented.
- [ ] Financial and audit records are protected from accidental update/delete.
- [ ] Access reviews, masking, encryption, audit events, outbox delivery, and query access are tested.
- [ ] Regulatory statements cite an approved source and review date; no unverified compliance score is claimed.

## Context7-first references

- [Backstage catalog descriptor format](https://backstage.io/docs/features/software-catalog/descriptor-format)
- [OpenLineage core specification](https://openlineage.io/docs/spec/facets/)
- [Great Expectations validation workflow](https://docs.greatexpectations.io/docs/core/introduction/gx_overview)
