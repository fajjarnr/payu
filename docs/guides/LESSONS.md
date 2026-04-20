# 🧠 PayU Lessons Learned (Session Log)

This document serves as a chronological log of "Lessons Learned" and critical architectural discoveries made during development sessions. Detailed implementation patterns have been migrated to the **AI Agent Skill Ecosystem** in `.agent/skills/`.

---

| ID | Lesson Title | Date | Domain | Migrated To |
|:---|:---|:---|:---|:---|
| L-001 | Python ML/AI Services — Stay on Debian Slim | Feb 26, 2026 | Platform | `ai-engineer`, `platform-engineer` |
| L-002 | Domain Routing Strategy — Gateway API + Istio | Feb 26, 2026 | Infrastructure | `platform-engineer` |
| L-003 | Domain Migration — Scope & Safe Replacement | Feb 26, 2026 | DevOps | `platform-engineer` |
| L-004 | Container Image Pinning | Feb 26, 2026 | Platform | `platform-engineer` |
| L-005 | Backstage catalog-info.yaml — Single Root File | Feb 26, 2026 | Developer Hub | `dx-engineer` |
| L-006 | OSS Version Compatibility Matrix | Feb 26, 2026 | Architecture | `principal-architect` |
| L-007 | Istio Ingress Gateway — Router Node Placement | Feb 26, 2026 | Infrastructure | `platform-engineer` |
| L-008 | Code Health Anti-Patterns in Multi-Pod | Feb 26, 2026 | Backend | `core-banking-engineer` |
| L-009 | Payment Gateway — Webhook Delivery Patterns | Feb 28, 2026 | Gateway | `core-banking-engineer` |
| L-010 | Settlement & Revenue Share — Financial Engine | Feb 28, 2026 | FinOps | `finops-engineer` |
| L-011 | .gitignore `out/` vs Hexagonal `port/out/` | Mar 14, 2026 | Architecture | `platform-engineer`, `core-banking-engineer` |
| L-012 | Kafka Deserialization Class Mismatch | Mar 15, 2026 | Integration | `integration-architect` |
| L-013 | Saga Starter — Missing `saga_instances` Table | Mar 15, 2026 | Backend | `integration-architect` |
| L-014 | Podman Local Infrastructure — Storage | Mar 15, 2026 | DevOps | `platform-engineer` |
| L-015 | IDOR Vulnerability — User Ownership Verification | Mar 16, 2026 | Security | `cybersecurity-architect` |
| L-016 | Next.js Layout vs Page — Context Scope | Feb 27, 2026 | Development | `frontend-architect` |
| L-017 | Framer Motion Layout Animations — `layoutId` | Feb 27, 2026 | UX | `frontend-architect` |
| L-018 | Webhook Validation — Replay Attack Protection | Mar 16, 2026 | Security | `api-architect` |
| L-019 | E2E Test Resilience — Infra vs Logic Failures | Mar 16, 2026 | Quality | `quality-engineer` |
| L-020 | Accessibility — `aria-live` for Errors | Mar 17, 2026 | A11y | `frontend-architect` |
| L-021 | Backlog Hygiene — Bug Count Integrity | Mar 16, 2026 | Process | `dx-engineer` |
| L-022 | Mobile Responsive — `100dvh` vs `100vh` | Mar 17, 2026 | UX | `frontend-architect` |
| L-023 | Bulk Audit Approach — Verify Before Fixing | Mar 17, 2026 | Audit | `principal-architect`, `quality-engineer` |
| L-024 | Auth Parameter Changes Break Unit Tests | Mar 17, 2026 | Testing | `core-banking-engineer`, `quality-engineer` |
| L-025 | Constructor Signature Changes Cascade | Mar 17, 2026 | Shared Libs | `core-banking-engineer`, `quality-engineer` |
| L-026 | DevSecOps Bootstrap — CRD Source Must Match Runtime Manifests | Apr 20, 2026 | Platform | `platform-engineer`, `cybersecurity-architect` |

---
*Last Updated: April 20, 2026*
