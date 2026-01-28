# ADR-0000: Architecture Decision Record Guidelines

**Status**: Accepted  
**Date**: 2026-01-28  
**Deciders**: @antigravity-ai, @owner

## Context

As the PayU Digital Banking Platform grows in complexity across 20+ microservices, we need a systematic way to record and track architectural decisions. Relying on chat history or memory leads to "architectural drift" and tribal knowledge.

## Decision

We will adopt the **MADR (Markdown Architecture Decision Record)** format to document all significant architectural choices. ADRs will be stored in the `/docs/adr/` directory.

## ADR Lifecycle

- **Proposed**: Under discussion.
- **Accepted**: Decision made, ready for/in implementation.
- **Deprecated**: Decision is no longer relevant for new services.
- **Superseded**: Replaced by a more recent ADR.
- **Rejected**: Considered but decided against.

## Directory Structure

```
docs/adr/
├── README.md           # Index of all decisions
├── 0000-adr-guidelines.md
└── 0001-template.md    # Template for new records
```

## Consequences

**Good**:
- Permanent record of "Why" (not just "What").
- Easier onboarding for new engineers.
- Explicit impact analysis (Consequences section).

**Bad**:
- Overhead of writing and reviewing ADRs.
- Need to keep the index up-to-date.

---
*Last Updated: January 2026*
