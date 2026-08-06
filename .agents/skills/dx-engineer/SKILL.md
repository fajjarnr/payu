---
name: dx-engineer
description: Developer experience engineering — Git workflows and Conventional Commits, git hooks (husky, commitlint, lint-staged), pull request and code review standards, TypeScript patterns and runtime validation, developer documentation and presentations, and developer portals (Backstage catalog). Use when designing, implementing, debugging, reviewing, or testing developer tooling, hooks, scripts, or documentation in any software project.
---

# DX Engineer

Make developer workflows predictable and documented: consistent Git hygiene,
fast local checks, verified tooling, and type-safe code. Read the target app's
`package.json`, `tsconfig.json`, hook files under `.husky/`, and existing
scripts before changing behavior. Reuse the project's tooling before adding a
new dependency or script.

## Context7 documentation gate

Before writing or changing code that uses a library, framework, SDK, API, CLI,
or cloud service:

1. Read the app's `package.json` (or POM) to determine the exact version in use.
2. Resolve the library in Context7. Prefer the official, high-reputation result
   and pin the query to the repository version when that version is available.
3. Query one concrete topic at a time: API, configuration, testing, migration,
   or integration behavior. Use the returned documentation as the source of
   truth; do not rely on remembered annotations, script names, or property
   namespaces.
4. If the exact version is not indexed, use the nearest official version only
   as a stated fallback, then verify the actual API in the project source
   before editing.
5. Re-resolve and re-query after changing a dependency version. Do not mix
   examples from different major versions.

Use Context7 for husky (v9 hook setup), commitlint (rules and config),
lint-staged (staged-file linting), Slidev (syntax and export), and Backstage
(catalog descriptor and TechDocs). Context7 does not replace project inspection
for team conventions.

## Git workflow and hooks

- Use a simple, effective branching model: a protected default branch, an
  integration branch, short-lived `feat/<ticket>-<description>` branches, and
  `hotfix/<description>` from the default branch when needed.
- Enforce Conventional Commits (`type(scope): subject`) with types `feat`, `fix`,
  `perf`, `refactor`, `test`, `docs`, `chore`, `ci`, `style`, `build`, `revert`;
  breaking changes use a footer or `!` and trigger a MAJOR bump (SemVer).
- Wire hooks with husky v9 (`npx husky init`, `prepare: "husky"` in
  `package.json`) and validate commit messages with commitlint
  (`extends: ['@commitlint/config-conventional']`). Use lint-staged for fast
  pre-commit checks on staged files only (ESLint `--fix`, Prettier `--write`),
  plus a type check and tests for changed files; leave the full suite to
  pre-push or CI.
- A solid hook layout:
  - `.husky/pre-commit` — lint-staged, `tsc --noEmit` (or the project's type
    check), tests for changed files.
  - `.husky/pre-push` — full test suite, type check, secret scan, and a
    `console.log` scan for production code.
  - `.husky/commit-msg` — commitlint `--edit $1` (or the existing validation).
- Never bypass hooks with `--no-verify` except as an emergency escape hatch,
  and say so explicitly when it is used. Keep hooks fast so they are not a
  reason to bypass them.

## Pull requests and review standards

- Follow the project's PR expectations: summary, type of change, testing
  evidence, and a checklist covering tests, documentation, secrets, and commit
  conventions.
- Review on the pillars: correctness (edge cases, nulls, empty input), clean
  code (SOLID, DRY, naming), performance (N+1, memory, blocking), security
  (authz, validation), and testability (dependency injection, isolation).
- Make review comments constructive and specific; propose an alternative when
  flagging a problem, and avoid bare verdicts such as "this is wrong".
- Use the project's domain rules as review anchors — for a financial platform,
  that includes precise decimal money handling, immutable ledgers with reversal
  entries, idempotency keys on mutations, atomic event publishing (outbox),
  ports-and-adapters boundaries, and PII masking.

## TypeScript patterns

- Use branded types for domain identifiers so different ID types cannot be
  mixed at compile time.
- Model state machines with discriminated unions and exhaustively switch on the
  discriminant; let TypeScript narrow the payload.
- Use mapped types and template literal types for type-safe route strings, and
  conditional types only where the shape genuinely branches.
- Write type guards (`state is Extract<State, { status: 'done' }>`) instead of
  scattering `as` casts; prefer `unknown` over `any` at trust boundaries.
- Validate runtime input with the project's actual validation library (for
  example zod) and infer the static type from the schema. Do not invent
  validation libraries the project does not use.
- Keep functional helpers small and single-purpose; a `pipe` or `Result` helper
  is only worth it when it removes real duplication.

## Documentation and presentations

- Write technical docs close to the code: README updates when APIs change, ADRs
  for architectural decisions, and TechDocs refs pointing at the changed
  directory in a developer portal catalog.
- Use Slidev for developer presentations. Verify the exact syntax and export
  flags in Context7 before relying on them (frontmatter layouts such as
  `two-cols`, `v-click`/`v-clicks`, `{monaco}`, mermaid diagrams, `slidev
  export --format pptx`).
- Keep slides source-controlled next to the content they explain and export for
  the audience; do not commit generated artifacts unless the project already
  does.
- Document developer workflows (hooks, setup, troubleshooting) in the project's
  docs folder so the onboarding path is self-service.

## Developer portal (Backstage)

- Keep the catalog file (for example `catalog-info.yaml`) the single source of
  truth for the developer portal: every service, website, app, library, and
  resource needs an entry with an owner, lifecycle, TechDocs ref, and accurate
  `dependsOn`/`providesApis` relationships. Verify the descriptor format in
  Context7 before adding a new kind or annotation.
- Use `backstage.io/techdocs-ref: dir:<path>` pointing at the actual
  documentation directory, and platform annotations (for example
  `backstage.io/kubernetes-id`) where the workload exists.
- Prefer the project's software templates over manual scaffolding when a new
  repo or service is requested; treat templates as the approved baseline, not a
  suggestion.
- Keep the catalog consistent with the repository: adding a service without a
  catalog entry, or a TechDocs ref without docs, is a DX debt to flag.

## DX quality gate

- Pre-commit checks: lint-staged on staged files, type check, and tests for
  changed files only.
- Pre-push or CI checks: full test suite with coverage, type check, secret
  scan, and no `console.log` in production code.
- CI should gate the same checks that run locally so `--no-verify` cannot hide
  failures.
- Test hooks and scripts as behavior: verify the exit code and message for a
  valid and an invalid commit message, a failing lint, and a failing type
  check. Do not claim a hook works without command output.

## Review checklist

- [ ] Context7 resolved the exact tool/library and the pinned version was checked.
- [ ] A failing test existed before production behavior changed.
- [ ] Commit messages follow Conventional Commits and hooks enforce them without `--no-verify` shortcuts.
- [ ] Hooks run only staged or related checks, and full checks run in CI or pre-push.
- [ ] TypeScript uses branded types, discriminated unions, and type guards; no new `any` at trust boundaries.
- [ ] Runtime validation uses the project's actual validation library and version.
- [ ] Docs, ADRs, and Slidev decks are updated with the change and referenced from the portal catalog.
- [ ] The catalog reflects new or changed components, owners, and dependencies.
- [ ] No hardcoded secrets, `console.log` in production code, or PII in logs and docs.
- [ ] Tests cover real behavior and the project quality gate passes with command output.

## References

- [husky setup and hooks](https://typicode.github.io/husky/)
- [commitlint rules and config](https://commitlint.js.org/reference/rules)
- [lint-staged](https://github.com/lint-staged/lint-staged)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Slidev syntax](https://sli.dev/guide/syntax)
- [Slidev layouts](https://sli.dev/guide/layout)
- [Slidev monaco editor](https://sli.dev/features/monaco-editor)
- [Slidev export](https://sli.dev/guide/exporting)
- [Backstage catalog descriptor format](https://backstage.io/docs/features/software-catalog/descriptor-format)
