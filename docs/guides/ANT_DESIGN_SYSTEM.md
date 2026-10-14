# Ant Design System: Implementation Guidelines (web-app)

> Single source of truth for the web-app's antd v6 token layer and component rules.
> All contrast ratios below are measured (WCAG 2.x relative luminance), not estimated.
> Guards, all failing CI on a broken invariant:
> - `src/__tests__/theme/tokens.test.ts` checks the configured values.
> - `src/__tests__/theme/antd-theme.test.tsx` extracts the CSS variables cssinjs actually generates for both algorithms and re-checks every pair, including generated aliases (hover/active steps, tints, borders) and button label tokens. The brand emerald roles are explicit, dated exceptions there, not silent skips.
> - `src/__tests__/theme/tier-colors.test.ts` parses `globals.css` and checks the `--tier-*` variables and the accent pair in both themes.
> - `src/__tests__/lib/chart-palette.test.ts` pins the restored chart palette.

## 1. Context and goals

**Design intent:** one calm, data-dense surface where hierarchy and status read instantly, every control works from the keyboard, and density comes from tokens instead of per-page tuning.

The web-app runs antd v6 (`antd` `^6.6.5`) with a single `ConfigProvider` in `src/components/AntdProvider.tsx`. All theming flows from `src/theme/tokens.ts`:

- `lightTokens` / `darkTokens`: seed and alias tokens, selected by `isDark`.
- `lightComponentTokens` / `darkComponentTokens`: component overrides (Button, Card, Input, Table).
- Pages read runtime values via `theme.useToken()` (`token.colorPrimary`, `token.colorPrimaryBg`, ...). **Never** re-hardcode a hex that a token already provides.

**PRODUCT DECISION 2026-10-14 (brand emerald).** The brand emerald is the vivid `#00D09C`, unchanged since the antd migration (commit `7cd9f2abd`). One revision darkened it to `#0B7A55` for WCAG AA; the product owner rejected that look and the vivid green is restored verbatim (commits `7774ca8d1` and `2347ded9e` reverted for the emerald family). Accepted consequence, documented in `src/theme/tokens.ts`: `#00D09C` measures 2.00:1 on white, so it fails AA as small text (links, tag text, the `token.colorPrimary` text usages in `[locale]/page.tsx`, `lending`, `support`, `qris`, `pockets`, `analytics`) and as a fill under a white button label. Restoring AA for the green later is a per-usage change (a darker text token where text is needed, or ink labels on primary fills), not another palette swap. Verified live in Chromium 2026-10-14: primary button fill `rgb(0, 208, 156)`, white label, `--ant-color-link: #00d09c`.

**Scope:** the antd token layer plus the component rules in section 3. **Declared non-goals for this revision:** the marketing `--color-*` CSS variables beyond `--color-accent`, and premium marketing surfaces.

## 2. Design tokens and foundations

### 2.1 Color architecture

| Mode | Primary/link/success/info | Error | Warning |
|:--|:--|:--|:--|
| light | `#00D09C` (brand, see product decision) | `#C62828` (5.62:1 on white) | `#B45309` (5.02:1) |
| dark (`#141414`) | `#00D09C` (renders `#03b488`, 6.92:1) | seed `#FF6B6B` (renders `#dc5e5e`, 5.09:1) | seed `#FFB800` (renders `#dc9f03`, 7.90:1) |

Every non-emerald role carries measured, explicit values because antd's generated palette does not respect AA on its own: generated hover steps lighten (white on the generated light primary hover is 4.46:1, error text in dark 4.26:1) and generated tints from darker seeds fail. The emerald family intentionally has **no** step/tint/border overrides: it renders exactly as the pre-migration-fix app did.

### 2.2 Measured state of the enforced roles

Light mode (surface `#FFFFFF`):

| Role | Value | Ratio | Notes |
|:--|:--|--:|:--|
| `colorError` / hover / active | `#C62828` / `#A61B1B` / `#8B1A1A` | 5.62 / 7.52 / above | error text, danger fill under white label |
| `colorWarning` / hover / active | `#B45309` / `#9A4A08` / `#80400A` | 5.02 / 6.26 / 7.91 | warning text, icons, borders |
| `colorTextPlaceholder` | `#6B7280` | 4.83:1 | antd default is ~2.3:1 |
| `colorWarningBg` / `colorErrorBg` | `#FFFBEB` / `#FEF2F2` | 4.84 / 5.14 with their text | tag and alert tints |
| `Table.headerColor` | `#6B7280` | 4.83:1 | was `rgba(0,0,0,0.45)` (3.36:1) |
| CSS `--color-text-primary` | `#1A1A2E` | 17.06:1 | body copy |
| CSS `--color-text-secondary` | `#6B7280` | 4.83:1 | secondary copy |
| antd secondary text | `rgba(0,0,0,0.65)` | 7.00:1 composited | `Typography.Text type="secondary"` |
| `--color-accent` / foreground | `#C2410C` / `#FFFFFF` | 5.18:1 | no live consumer; sound if used |

Dark mode (container `#141414`):

| Role | Value | Ratio | Notes |
|:--|:--|--:|:--|
| `colorError` (rendered) | `#dc5e5e` | 5.09:1 | error borders and icons |
| `colorErrorText` / hover / active | `#ff9a9a` / `#ff8a8a` | 9.07:1+ | error text |
| `colorWarning` (rendered) | `#dc9f03` | 7.90:1 | warning text, icons |
| `colorWarningText` / steps | `#FFB800` / `#e8b727` | 10.62 / 9.86:1 | warning text |
| `colorTextPlaceholder` | `#9CA3AF` | 7.26:1 | placeholders |
| `Table.headerColor` | `#ACACAC` | 8.12:1 | old fixed `rgba(0,0,0,0.45)` was invisible in dark |
| `Button.dangerColor` | `#000000` | 5.80:1 on the red fill | white on `#dc5e5e` is 3.62:1 |
| tints (warning / error) | `#2c2211` / `#2c1a1b` | text stays 4.57:1+ | tag and alert backgrounds |

**Exception (product decision above):** emerald text, emerald fill labels, emerald tints, and emerald borders are not ratio-enforced.

### 2.3 The CSS-variable layer (globals.css)

| Layer | Values | Guard |
|:--|:--|:--|
| VIP tier colors (`--tier-*`) | `VIPBadge` renders `tierColor` as text and icon color on the card surface. `useVIPStatus` returns `var(--tier-*)`, defined per theme: light `#8C5A2B` / `#707070` / `#92700F` / `#5F6B76` / `#2E7D9A` / `#0B7A55` (tier-vip) / `#6B7280` (4.62:1 to 5.81:1 on white), dark `#D9A066` / `#C9CDD3` / `#FFD666` / `#D8DEE6` / `#9BE3F5` / `#00D09C` / `#9CA3AF` (7.26:1 to 13.61:1 on `#141414`) | `tier-colors.test.ts` parses `globals.css` for both themes |
| Chart palette (`src/lib/chart-palette.ts`) | Canvas cannot resolve `var()` at draw time. Restored brand palette: series `#00D09C` / `#34D399` / `#F59E0B` / `#8B5CF6` / `#F43F5E` / `#64748B`, secondary `#9CA3AF`, axis label `#6B7280`, stroke `#FFFFFF` | pinned by `chart-palette.test.ts` |

### 2.4 Typography, radius, density, motion

- **Type:** Inter stack (`Inter, system-ui, sans-serif`), base `fontSize: 14`. Heading levels map to product roles: `fontSizeHeading1: 24`, `fontSizeHeading2: 20`, `fontSizeHeading3/4: 16`, `fontSizeHeading5: 14`. Do not reassign these per page.
- **Radius:** `borderRadius: 12`, `borderRadiusLG: 16`. antd v6 dropped the `borderRadiusXL` alias; set 24px per-component (for example Modal or Sheet) when those migrations land.
- **Density:** `Button` heights 44 / 48 / 56 (`SM` / default / `LG`), `Input` height 48, radius 12. 48px controls satisfy the 24px WCAG minimum with touch headroom.
- **Motion:** state feedback only (hover, focus, open/close), 120 to 240ms, `ease-out`. The `pulse 2s infinite` scan frame in `qris/page.tsx` is the only ambient animation, scoped to the scanning state. Respect `prefers-reduced-motion: reduce` for anything new.

## 3. Component-level rules

### 3.1 Shared state matrix

| State | Visual | Notes |
|:--|:--|:--|
| default | Surface fill, default border, `--color-text-primary` text | Buttons: variant fill per 3.2 |
| hover | Derived hover token from the seed | 120ms transition; do not regenerate per page |
| focus-visible | antd focus outline on keyboard focus | Must stay visible; never `outline: none` without a replacement |
| active | Derived active token | |
| disabled | antd disabled tokens (exempt from AA) | Keep `disabled` semantics; add a tooltip when the reason is not obvious |
| loading | Spin plus persistent label, `aria-busy` | Never replace the label with a spinner alone |
| error | `colorError` border and message | Message stays programmatically associated (see 4.5) |

### 3.2 Button

- **Variants:** `type="primary"` (fill `#00D09C`, white label, the brand look; 2.00:1, accepted per the product decision), `type="default"`, `type="text"`, and `danger` (fill `#C62828` in light with white label 5.62:1; `#dc5e5e` in dark with black label 5.80:1 via `Button.dangerColor`).
- **Sizes:** `controlHeightSM: 44` for dense tables, default 48, `controlHeightLG: 56`. `fontWeight: 700` is the system button weight.
- **Rules:** one primary button per screen; icon-only buttons require `aria-label`; labels name the outcome ("Buat transfer"), never "Submit".

### 3.3 Form controls (Input, Select, Form.Item)

- Persistent visible label above the control; the placeholder is a hint (`colorTextPlaceholder`, 4.83:1 / 7.26:1), never the label.
- Errors keep the default association (antd wires `aria-describedby`); error text renders in the measured `colorError` family.
- `Input` keeps `controlHeight: 48`, `borderRadius: 12`.
- Long values scroll inside the control; the control never expands the layout.

### 3.4 Table

- `headerBg: 'transparent'`, `headerColor` per theme (`#6B7280` light / `#ACACAC` dark; both measured). Do not revert to a fixed `rgba(0,0,0,...)`: it fails in light and disappears in dark.
- Empty state: a named empty message plus the primary action, never a blank grid. Loading: skeleton rows so the layout does not jump.
- Row actions stay in the DOM and keyboard-reachable; hover-only reveals are prohibited.
- Overflow: wrap in a scroll container; never clip amount or status cells.

### 3.5 Card, Dialog, Navigation, Feedback

- **Card:** `borderRadiusLG: 16` with the tertiary shadow token. Borders first, shadow second; cards do not stack drop shadows.
- **Dialog / Modal:** antd handles focus trap, Escape, and focus return; keep those defaults. Destructive confirmations use the danger button.
- **Navigation:** Menu and Tabs active states derive from `colorPrimary` (brand emerald in light, `#03b488` in dark). The skip link in `src/lib/a11y.tsx` stays first in tab order.
- **Feedback:** `Alert`, `Tag`, `message`, and `notification` colors derive from the seeds and tints above; do not pass raw hex to `Alert type` or `Tag color` overrides.

## 4. Accessibility requirements and testable acceptance criteria

- **4.1 (SC 1.4.3, 1.4.11):** The allowlist in 2.2 is the only legal set of text, fill, tint, and boundary pairs, with the documented emerald exception. Verify: `npx vitest --run src/__tests__/theme src/__tests__/lib` must pass; any new token pair gets covered before merge.
- **4.2 (SC 2.4.7, 2.4.11):** Keyboard focus shows the antd focus outline on every control. Verify: Tab through each changed screen; no invisible or clipped focus, no `outline: none` without a replacement.
- **4.3 (SC 2.1.1):** Complete one full task per changed screen keyboard-only (Enter/Space activate, Escape closes, arrows move in menus and tabs, focus returns after dialogs).
- **4.4 (SC 2.5.8):** Pointer targets are at least 24x24px; the 44px minimum control height covers touch.
- **4.5 (SC 3.3.1, 4.1.3):** Inputs keep persistent labels; error messages stay associated and announced. Verify: submit an invalid form with a screen reader.
- **4.6 (SC 1.4.1):** Status is never color-only; keep the icon plus text pairing that `Alert` and `Tag` provide.
- **4.7 (SC 2.3.1):** `prefers-reduced-motion: reduce` neutralizes transitions; the QRIS scan pulse is excluded as state feedback.
- **4.8 (dark mode):** Both themes pass the same checks; a change that passes in light but fails in dark is a defect. Verify with the store theme toggle plus 4.1's test.

## 5. Content and tone standards

- Voice: concise, confident, helpful, clear, professional, action-oriented, low-jargon.
- Sentence case for titles and labels; use the existing tracked-uppercase micro-labels ("TOTAL MASUK") sparingly and only for fixed category labels.
- Buttons: verb + object ("Buat transfer", "Tambah beneficiary"). Never "Submit", "Click here", "Get started".
- Errors: what happened, why, next step, and the record. "Transfer gagal: bank tujuan tidak aktif. Coba lagi atau pilih bank lain."
- Amounts and dates: keep the existing Indonesian formatting (`Rp` prefix, `.` thousands, ISO dates in tables) with consistent decimals.
- Copy ships in both locales: `id` and `en` parity is enforced by `npm run check:i18n`.

## 6. Anti-patterns and migration notes

**Done (this revision, 2026-10-13/14):** tokens extracted to `src/theme/tokens.ts`; error, warning, placeholder, table-header, tier, accent, and chart roles carry measured values; the brand emerald is restored verbatim with its AA gap documented as a product decision; guards cover config, rendered CSS variables, CSS variables in `globals.css`, and the chart palette; the 7 pre-existing red page-test files were repaired (missing intl provider, partial react-query mock, stale copy pins).

**Prohibited:**

| Anti-pattern | Why | Fix |
|:--|:--|:--|
| Raw hex in components where a token exists | Palette edits miss call sites; bypasses the guards | `theme.useToken()` values |
| Reintroducing the darker emerald "for contrast" | rejected by the product owner 2026-10-14 | keep the brand emerald; fix AA per usage if ever required |
| `rgba(0,0,0,...)` fixed component colors | fails light, invisible in dark | per-theme tokens |
| Deleting an explicit step/tint to "use antd defaults" | generated steps fail AA for error/warning/placeholder roles | keep explicit overrides |
| Hover-only row actions | unreachable by keyboard | visible on hover and focus |
| Placeholder as label | label vanishes; association lost | persistent visible label |
| Silently skipping a guard pair without a dated reason | hides regressions | document the exception in `tokens.ts` and the test |
| Chart or tier colors as raw hex in components | canvas cannot read tokens and themes diverge | `src/lib/chart-palette.ts`, `--tier-*` CSS variables |

**Findings, not yet fixed (each is a separate ticket):**

1. The emerald AA gap from the 2026-10-14 product decision: vivid green text (links, tags, `token.colorPrimary` text usages), white-on-emerald primary labels, emerald tints, and emerald borders all sit at or below their thresholds on white. Tracked here; the fix, when requested, is per-usage tokens, not a palette swap.
2. Chart marks follow the same decision (2.00:1 primary, 1.9:1 mint and amber on white).

## 7. QA checklist (code review)

- [ ] Color edits touch only `src/theme/tokens.ts`, `globals.css` tier/accent vars, or `src/lib/chart-palette.ts`; no raw hex in components.
- [ ] `npx vitest --run src/__tests__/theme src/__tests__/lib` passes; any new exception is dated and reasoned in code.
- [ ] The primary button still renders `#00d09C` with a white label in light mode (browser or guard).
- [ ] New token usages (badges, tints, chart constants) appear in the allowlist with a measured ratio, or join the documented exception with a reason.
- [ ] Buttons: one primary per screen, icon-only has `aria-label`, loading keeps the label, label tokens untouched.
- [ ] Inputs: persistent label, placeholder is a hint, errors use the default association.
- [ ] Table: header tokens untouched or measured, empty and loading states present, actions keyboard-reachable.
- [ ] Both themes checked (toggle in the UI) for the changed surface.
- [ ] Copy passes section 5; `npm run check:i18n` green.
