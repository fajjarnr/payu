# Context7 Workflow for Third-Party Library Upgrades

> PayU Digital Banking Platform — Developer guide for verifying third-party library APIs and versions before upgrading, in line with `AGENTS.md` and `.agents/skills/dx-engineer/SKILL.md`.

---

## Why

PayU relies on fast-moving third-party libraries (Spring Boot, Quarkus, Jackson, TanStack Query, Next.js, Zod, React Native/Expo, etc.). Training data and in-repo examples can go stale. Context7 returns the **current, versioned** documentation for a library so you never code against an outdated API.

## When to use

- Upgrading a library version (e.g. Spring Boot 3.4 → 4.1, Next.js 15 → 16, Quarkus 3.x).
- Introducing a new third-party dependency.
- Changing the way you call an existing library API.
- Debugging a library-version-specific error.

## Workflow

1. **Resolve the library ID** — call the Context7 resolver with the library name (e.g. `Next.js`, `Spring Boot`, `TanStack Query`). Pick the best match by source reputation and snippet coverage, and pin a version when the project pins one (e.g. `/vercel/next.js/v16.0.0`).
2. **Query the relevant concept** — fetch docs scoped to a single concept (e.g. "Spring Boot 4 Actuator endpoints", "Next.js Server Components", "Zod schema validation"). Verify the API shape you plan to use.
3. **Cross-check against the pinned version** — confirm the docs match the version actually in `backend/pom.xml` / `backend/<svc>/pom.xml` or `frontend/web-app/package.json`.
4. **Update the code + changelog** — implement using the verified API, then record the version bump in `CHANGELOG.md` and any affected `SERVICE_CATALOG.md` / `ARCHITECTURE.md` entries (see L-281).
5. **Verify** — run the relevant build/tests (`mvn -f backend/pom.xml clean package -DskipTests`, `npm run test` in `frontend/web-app`) before committing.

## Quick reference (commonly used libraries)

| Library | Context7 path | Typical query |
|---------|---------------|---------------|
| Next.js | `/vercel/next.js` | "Server Components vs Client Components" |
| React | `/react/react` | "useEffect cleanup" |
| Spring Boot | `/spring-projects/spring-boot` | "Actuator endpoints" |
| Quarkus | `/quarkusio/quarkus` | "Config properties" |
| TanStack Query | `/TanStack/query` | "useQuery vs useMutation" |
| Zod | `/colinhacks/zod` | "schema validation" |
| Expo | `/expo/expo` | "app config plugins" |

> **Rule**: never assume a third-party API from memory. Always resolve + query Context7 before generating code against a library.
