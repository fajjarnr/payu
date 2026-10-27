# Changelog

## [1.6.0] - 2026-10-27

### Changed (Wise re-skin — total UI/UX overhaul per `wise/DESIGN.md`)

- **Token foundation**: `src/theme/tokens.ts` rewritten to the Wise palette — lime primary `#9fe870` with ink labels `#0e0f0c` (13.05:1), sage layout `#e8ebe6`, explicit hover `#cdffad` / active `#c5edab` (Wise lightens on interaction, inverse of the antd algorithm), semantic family `positive #2ead4b` / `warning #ffd11a` / `negative #d03238` / `info #38c8ff` with deep text roles, ink input borders, radius 12/24, headings 32/24. Dark mode is an ink derivation (layout `#0e0f0c`, containers `#1a1c1a`, text `#cdffad`).
- **`INK_ON_EMERALD` → `ON_PRIMARY`** (clean cutover, 15 files, no alias). New fixed-surface constants: `INK_SURFACE`, `INK_SURFACE_SOFT`, `INK_SURFACE_BORDER`, `LIME_ACCENT`, `SAGE_ON_INK`. `CARD_ART_GRADIENT` → ink-green, `BRAND_PANEL_*` → ink/sage, `DANGER_PANEL_BG` → `#320707`.
- **Charts**: `chart-palette.ts` derived from the Wise semantic family, theme-aware (`chartPalette(isDark)`), every mark ≥3:1 on its surface.
- **Fonts**: Manrope 800 display (`--font-display`, `.payu-display`) + Inter body via `next/font`; product headings Inter 600, landing display Manrope 800.
- **Shell**: header rhythm unified 72px mobile+desktop; logo mark radius 24; MobileNav de-shadowed with pill active indicator; backoffice header/content responsive (search hidden <640px); EmptyState/RouteError carded on sage.
- **Landing**: split server-first — `page.tsx` is a Server Component (semantic HTML: antd `Card`/`Typography`/`@ant-design/icons` are client-only and 500 in RSC); client leaves `LandingNav`, `ConverterCard`, `LandingIcon`. Sage hero-band + Manrope display + currency-converter card + dark security card + ink footer. Verified 200 + screenshots at 360px and 1440px, zero horizontal overflow.
- **Pages**: all ~30 routes reskinned — lime/success/info never as text on light surfaces (deep text tokens), ink labels on lime fills, radius 24 cards/tiles/pills, shadows removed (elevation = surface contrast), product text 600. Money/auth/BFF/i18n logic untouched.
- **Guards**: `tokens.test.ts`, `antd-theme.test.tsx` (both-mode fill/label pairs, text-role-on-tint), `chart-palette.test.ts` (≥3:1 per surface), `design-system-guard` radius scale +24, `pages-token-migration` (ON_PRIMARY), landing test retargeted to `HeroTitle`.

### Verified (2026-10-27)

- `tsc --noEmit` clean; `vitest` **2113/2113**; `test:a11y` 56/56; `check:i18n` parity 1244/1244; `next build` 83 routes; landing e2e 2/2 on `/en` (hero + 360px overflow); `a11y:audit` no new violations. `lint` 3 errors / 37 warnings = pre-existing baseline (verified via stash), plus zero new warnings. E2E `dashboard-landing-flow` on `/` fails with `ERR_TOO_MANY_REDIRECTS` — pre-existing proxy loop, reproduced on unmodified baseline.
## 2026-10-07 — Restore files lost to backdrop-filter removal

Commit: `a76b949b1` (`fix(web-app): resolve P0/P1/P2 audit issues`)

During emergency fixes, a `backdrop-filter` removal pass deleted JSX blocks
alongside the CSS declarations, blanking out whole components/pages. The
following 8 files were restored from git HEAD:

- `src/components/MobileHeader.tsx`
- `src/app/[locale]/page.tsx`
- `src/app/[locale]/backoffice/layout.tsx`
- `src/components/cms/BannerCarousel.tsx`
- `src/app/[locale]/cards/page.tsx`
- `src/app/[locale]/backoffice/fraud/[id]/page.tsx`
- `src/app/[locale]/merchant/register/page.tsx`
- `src/app/[locale]/qris/page.tsx`

Reason: the backdrop-filter removal had deleted JSX blocks, not just the
`backdrop-filter` CSS rules.
