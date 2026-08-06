---
name: web-artifacts-builder
description: Build standalone, self-contained HTML artifacts (interactive demos, PRDs, prototypes, offline tools) with Vite, Tailwind CSS, shadcn/ui, React, and single-file bundling. Covers scaffolding with the shadcn CLI, Vite configuration, Tailwind theming, component patterns, motion, charts, and producing one portable HTML file. Use when creating, building, or bundling a web artifact or demo in any frontend project.
---

# Web Artifacts Builder

Build standalone, high-fidelity web artifacts: interactive demos, PRDs,
prototypes, and offline tools delivered as a single self-contained HTML file.
The workflow is framework-standard — Vite for dev/build, shadcn/ui for
components, Tailwind for styling, and a single-file plugin to inline everything.
Verify every library with Context7 before relying on its API; do not assume a
version or config from memory.

## Context7 documentation gate

Before writing or changing code that uses a library, framework, SDK, API, CLI,
or cloud service:

1. Read the project `package.json` (or the framework docs) to determine the
   exact version in use.
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

Use Context7 for Vite (and `vite-plugin-singlefile`), Tailwind CSS, shadcn/ui
and the shadcn CLI, Radix UI, framer-motion, recharts, and lucide-react.

## Stack and setup

The standard stack for a modern single-file artifact:

- **Vite** (React + TypeScript template) for dev server and build.
- **Tailwind CSS** for utility-first styling — v4 is CSS-first (`@import
  "tailwindcss"` + `@theme`), while v3 uses `tailwind.config.js`. Match the
  version in the project; do not mix config styles.
- **shadcn/ui** for accessible, copy-paste components built on Radix UI and
  styled with Tailwind. Use the shadcn CLI (`npx shadcn@latest init -t vite`,
  `npx shadcn@latest add <component>`) rather than hand-copying components.
- **framer-motion** for animation, **recharts** for charts, **lucide-react**
  for icons — all optional, add only what the artifact needs.

## Scaffolding a new artifact

Use the shadcn CLI to scaffold a Vite project with components wired up:

```bash
# Create a new Vite project (interactive prompts for name/framework)
npm create vite@latest my-artifact -- --template react-ts

# Initialize shadcn/ui (Tailwind v4: config lives in CSS, not a JS file)
npx shadcn@latest init -t vite

# Add only the components you need
npx shadcn@latest add button card input dialog tabs select
```

- `npx shadcn@latest init` auto-detects the framework and generates
  `components.json`; in Tailwind v4 the `tailwind.config` field is left empty
  because theming is CSS-first.
- Prefer the CLI over copying component files manually — it resolves the
  correct Radix dependencies and aliases for the project.
- If the artifact must work offline as one file, keep dependencies minimal;
  every extra library grows the bundle.

## Single-file bundling

The goal is one portable HTML file with all JS/CSS inlined:

- **Recommended**: `vite-plugin-singlefile`. Add it to `vite.config.ts`:
  ```ts
  import { defineConfig } from "vite"
  import react from "@vitejs/plugin-react"
  import { viteSingleFile } from "vite-plugin-singlefile"

  export default defineConfig({
    plugins: [react(), viteSingleFile()],
    resolve: { alias: { "@": "/src" } },
  })
  ```
  The plugin sets `assetsInlineLimit` to always inline, disables CSS code
  splitting, sets `base: "./"`, and forces a single bundle (inline dynamic
  imports). `removeViteModuleLoader: true` strips Vite's loader for a truly
  standalone file.
- **Alternative**: Parcel (`npx parcel build index.html --no-source-maps`) +
  `html-inline` — an older approach that also produces a single file. Prefer
  the Vite plugin when on Vite, since it is the standard tooling.
- After building, verify: the file loads from `file://` (or any static host),
  has no console errors, and needs no network requests for fonts, CDNs, or
  APIs.

## Tailwind theming

- **Tailwind v4**: define tokens with `@theme` in the CSS entry:
  ```css
  @import "tailwindcss";
  @theme {
    --color-primary: oklch(0.55 0.18 160);   /* generates bg-primary, etc. */
    --font-display: "Satoshi", sans-serif;
  }
  ```
  Theme variables generate utility classes automatically and are available as
  plain CSS variables.
- **Tailwind v3**: extend `theme` in `tailwind.config.js` (colors, fontFamily,
  keyframes/animation), with `content` pointing at `index.html` and `src`.
- Dark mode: class-based (`darkMode: "class"` in v3, `@custom-variant dark`
  in v4) with a theme provider such as `next-themes`.
- Keep interactive colors accessible: choose a primary that meets 4.5:1
  contrast with the foreground (white or dark), not a bright brand color with
  white text. Define semantic tokens (background, foreground, card, muted,
  destructive, border, ring) so components stay consistent.

## Component and interaction patterns

- **Use shadcn components** (button, card, input, dialog, tabs, etc.) for
  consistent, accessible UI. They support `asChild` (via Radix Slot), CVA
  variants, and `cn()` (`clsx` + `tailwind-merge`) for merging classes.
- **Compose, don't rebuild**: `App.tsx` composes components; keep business
  logic in hooks/utilities so the artifact stays readable.
- **Icons**: lucide-react SVG components at fixed sizes; no emojis as icons.
- **Interactive tables/lists**: filter + sort with `useMemo`; animate
  enter/exit with framer-motion `AnimatePresence`.
- **Live-feel demos**: simulate streaming data with a hook (`useState` +
  `setInterval` producing mock events, capped) instead of calling real
  backends — keeps the artifact self-contained.
- **Charts**: recharts with `ResponsiveContainer`; style tooltips/axes to match
  the theme and remain readable in light and dark mode.

## Motion and accessibility

- Motion: framer-motion, durations ~0.3–0.5s, `easeOut`/spring; stagger for
  lists. Honor reduced motion via `useReducedMotion` (or
  `prefers-reduced-motion`) — never ship motion that ignores it.
- Accessibility is non-negotiable:
  - Contrast 4.5:1 normal text / 3:1 large UI.
  - Visible focus rings, keyboard operability, semantic HTML, ARIA labels.
  - Touch targets ≥ 44×44px; no layout shift (use skeletons for async).
- `cursor-pointer` on clickable elements (or the shadcn `--pointer` option).

## Quality checklist

- [ ] Context7 resolved the exact library and the pinned version was checked.
- [ ] Scaffolded with the shadcn CLI (`init -t vite`, `add <component>`); no
      hand-copied components with broken aliases.
- [ ] Tailwind version matched (v4 `@theme` CSS-first, or v3 config) and tokens
      define semantic colors.
- [ ] Primary action color meets 4.5:1 contrast with its foreground.
- [ ] Responsive at 375px, 768px, 1024px, 1920px; touch targets ≥ 44×44px.
- [ ] Icons are lucide SVGs; no emojis; `cursor-pointer` on clickables.
- [ ] Motion is 0.3–0.5s and honors reduced motion.
- [ ] All buttons, filters, and interactive states work; error states handled.
- [ ] Built with single-file bundling (`vite-plugin-singlefile` preferred);
      output loads offline with no console errors and no external requests.
- [ ] Bundle size reported with command output; no unnecessary dependencies.

## References

- [Vite documentation](https://vitejs.dev/)
- [vite-plugin-singlefile](https://github.com/richardtallent/vite-plugin-singlefile)
- [Tailwind CSS documentation](https://tailwindcss.com/docs)
- [shadcn/ui](https://ui.shadcn.com/)
- [shadcn CLI](https://ui.shadcn.com/docs/cli)
- [Radix UI primitives](https://www.radix-ui.com/primitives)
- [framer-motion documentation](https://motion.dev/)
- [Recharts](https://recharts.org/)
- [Lucide icons](https://lucide.dev/)
