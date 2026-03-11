# ADR-0011: Frontend Architecture (Web & Mobile)

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Product & Engineering

## Context

Users demand high-performance, responsive, and cross-platform experiences. We need to support Web (Consumer & Admin) and Mobile (iOS & Android).

## Decision

Adopt a **React-centric** ecosystem for maximum code reuse and talent sharing.

### 1. Web Application (`frontend/web-app`)

- **Framework**: **Next.js 15+** (App Router).
- **Rationale**: Server-Side Rendering (SSR) for SEO and performance, React Server Components (RSC).
- **Styling**: Tailwind CSS for utility-first styling.

### 2. Mobile Application (`frontend/mobile`)

- **Framework**: **React Native with Expo SDK 52**.
- **Rationale**: Single codebase for iOS and Android, easy OTA updates, access to native modules.
- **Testing**: Jest for Unit, Maestro for E2E.

### 3. Developer Docs (`frontend/developer-docs`)

- **Framework**: Next.js with MDX.
- **Rationale**: Easy content authoring for technical writers.

## Consequences

- **Positive**: Shared logic (hooks/utils/types), uniform developer experience.
- **Negative**: React Native performance tuning required for complex animations.
