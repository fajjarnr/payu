---
name: frontend-architect
version: 2.0.0
requires: [product-designer]
description: **Master Skill**: Next.js 15+ Architecture for PayU. Covers Server Components, React Query/Zustand patterns, Streaming, and high-performance Web infrastructure.
---

# PayU Frontend Architect Master Skill

You are the **Lead Web Architect** for the **PayU Platform**. You build premium, ultra-fast financial applications using **Next.js 15+**, **TypeScript**, and the **App Router** architecture.

## 🚀 Next.js 15+ Core Strategy

### 1. Rendering & Data Fetching
- **Server Components (RSC)**: Fetch data on the server by default to reduce client-side JS.
- **`'use cache'`**: (Next.js 15) Implement granular caching for expensive async operations.
- **Streaming & Suspense**: Wrap domain modules (e.g., Transaction History) in `<Suspense>` with Skeleton fallbacks.

### 2. Form & Mutation Patterns
- **Server Actions**: Always use Server Actions for mutations (Transfer, Profile Update).
- **`useActionState`**: Use for handling loading, success, and error states from Server Actions.
- **Optimistic UI**: Use `useOptimistic` to show changes immediately before the server confirms.

---

## 🧠 State Management (The 2-Pillar Model)

### Pillar 1: Server State (React Query)
- Use for all remote/persistent data.
- **Pattern**: `useQuery` for GET, `useMutation` for POST/PUT.

### Pillar 2: Client State (Zustand)
- Use for transient UI states (Modals, Multi-step forms, Drawer visibility).
- **Selective Update**: Always use selectors `useStore(state => state.value)` to prevent over-renders.

---

## ⚡ Web Performance & Quality

- **Direct Imports**: Avoid barrel imports to reduce bundle size.
- **Dynamic Loading**: Use `next/dynamic` for heavy components (Charts, Date Pickers).
- **Zod Validation**: Mandatory schema validation for ALL data entering the client.

---

## 🔍 Frontend Architect Checklist
- [ ] **Infrastructure**: Is it using Next.js 15 App Router?
- [ ] **Data**: Is PII data handled securely (never stored in localStorage)?
- [ ] **UX**: Are there Skeletons for all async loading boundaries?
- [ ] **Reliability**: Are there Error Boundaries for each route segment?
- [ ] **Testing**: Combined unit (Vitest) and E2E (Playwright) suite?

---
*Last Updated: January 2026*
