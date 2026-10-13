# Ant Design System: Implementation Guidelines (web-app)

> Single source of truth for the web-app's antd v6 token layer and component rules.
> All contrast ratios below are measured (WCAG 2.x relative luminance), not estimated.
Four guards enforce them, and any edit to `src/theme/tokens.ts` that breaks an
invariant fails CI:
- `src/__tests__/theme/tokens.test.ts` checks the configured values (light mode and unmapped dark aliases render verbatim).
- `src/__tests__/theme/antd-theme.test.tsx` extracts the CSS variables cssinjs actually generates for both algorithms and re-checks every pair, including generated aliases (hover/active steps, tints, borders) and button label tokens.
- `src/__tests__/theme/tier-colors.test.ts` parses `globals.css` and checks the `--tier-*` variables and the accent pair in both themes.
- `src/__tests__/lib/chart-palette.test.ts` checks every chart color on both surfaces.

## 1. Context and goals

**Design intent:** one calm, data-dense surface where hierarchy and status read instantly, every control works from the keyboard, and density comes from tokens instead of per-page tuning.

The web-app runs antd v6 (`antd` `^6.6.5`) with a single `ConfigProvider` in `src/components/AntdProvider.tsx`. All theming flows from `src/theme/tokens.ts`:

- `lightTokens` / `darkTokens`: seed and alias tokens, selected by `isDark`.
- `lightComponentTokens` / `darkComponentTokens`: component overrides (Button, Card, Input, Table).
- Pages read runtime values via `theme.useToken()` (`token.colorPrimary`, `token.colorPrimaryBg`, ...). **Never** re-hardcode a hex that a token already provides.

Goals, in priority order:

1. **Accessibility.** WCAG 2.2 AA on both themes, all interaction states. Accessibility wins over brand fidelity when they conflict (section 2.1).
2. **Consistency.** Same tokens, states, and densities on every screen; no page-level overrides.
3. **Reviewability.** Every rule resolves to a token, a measured ratio, or a threshold a reviewer can check in the diff.

**Scope:** the antd token layer plus the component rules in section 3. **Declared non-goals for this revision:** canvas chart palettes in `analytics/page.tsx`, the `--color-accent` marketing CSS variables in `globals.css`, and VIP tier colors in `src/hooks/useVIPStatus.ts`. These are tracked as findings in section 6.

## 2. Design tokens and foundations

### 2.1 Why the token set is explicit and split per theme

antd renders seed colors as **text** (links, form errors, tag text, alert text), as **icons** (3:1 non-text), and as **fills under labels** (primary and danger buttons). Pages also use `token.colorPrimary` directly as text and icon color (`[locale]/lending/page.tsx`, `support/page.tsx`, `qris/page.tsx`, `pockets/page.tsx`, `analytics/page.tsx`, and the landing page).

Two mechanisms make this hard, and both are handled explicitly:

1. **One hue cannot serve both jobs on both surfaces.** The vivid brand emerald `#00D09C` measures **2.00:1** on white: illegal as text and as a fill under a white label. Light mode therefore uses the repo's own AA-safe primary `#0B7A55` (= CSS `--color-primary`, 5.34:1). Dark surfaces (`#141414`) keep vivid brand colors, which pass there.
2. **antd's generated palette steps do not respect AA.** Generated hover steps lighten (white on the generated light primary hover `#228762` is 4.46:1, just under), generated dark steps darken (generated dark error text `#dc3f4d` is 4.26:1), generated tints from the darker seed fail (`success-bg` rendered as the mid gray `#a8bab1`, 2.8:1 with emerald text), and generated borders land near 2:1. Every step, tint, and border is therefore set explicitly with measured values.

The dark algorithm also **remaps seeds**: `#00D09C` renders as `#03b488`, `#FF6B6B` as `#dc5e5e`. The rendered values (not the configured ones) are what the tests measure.

### 2.2 Measured allowlist

Light mode (surface `#FFFFFF`):

| Role | Value | Ratio | Used as |
|:--|:--|--:|:--|
| `colorPrimary` / `colorLink` / `colorSuccess` / `colorInfo` | `#0B7A55` | 5.34:1 | links, active nav, status text/icons, primary fill under white label |
| `colorPrimary` hover / active | `#0A6B48` / `#08543A` | 6.54:1 / 8.98:1 | hover and pressed states |
| `colorError` | `#C62828` | 5.62:1 | error text, danger fill under white label, error borders |
| `colorError` hover / active | `#A61B1B` / `#8B1A1A` | 7.52:1 / above | danger button states |
| `colorWarning` | `#B45309` | 5.02:1 | warning text, icons, borders |
| `colorWarning` hover / active | `#9A4A08` / `#80400A` | 6.26:1 / 7.91:1 | warning states |
| `colorTextPlaceholder` | `#6B7280` | 4.83:1 | placeholders (antd default is ~2.3:1) |
| `colorPrimaryBg` / `colorInfoBg` | `#E6FFF7` | 5.09:1 with primary text | tints behind tags, badges |
| `colorSuccessBg` | `#F0FDF4` | 5.11:1 | success tag tint |
| `colorWarningBg` | `#FFFBEB` | 4.84:1 | warning tag tint |
| `colorErrorBg` | `#FEF2F2` | 5.14:1 | error tag tint |
| borders (primary/success/info, warning, error) | seed values | 5.34:1 / 5.02:1 / 5.62:1 | component boundaries (SC 1.4.11, 3:1) |
| `Table.headerColor` | `#6B7280` | 4.83:1 | table headers |
| CSS `--color-text-primary` | `#1A1A2E` | 17.06:1 (16.18:1 on dim) | body copy |
| CSS `--color-text-secondary` | `#6B7280` | 4.83:1 (4.59:1 on dim) | secondary copy |
| antd secondary text | `rgba(0,0,0,0.65)` | 7.00:1 composited | `Typography.Text type="secondary"` |

Dark mode (container `#141414`, values as rendered by the dark algorithm):

| Role | Configured seed | Rendered | Ratio | Used as |
|:--|:--|:--|--:|:--|
| `colorPrimary` / `colorLink` / `colorSuccess` / `colorInfo` | `#00D09C` | `#03b488` | 6.92:1 | links, nav, status text/icons |
| hover / active / text steps | `#22ca9a` | `#22ca9a` | 8.0:1+ | all interaction states |
| `colorError` | `#FF6B6B` | `#dc5e5e` | 5.09:1 text, 4.26:1 border/icon | error borders and icons |
| `colorErrorText` / hover / active | `#ff9a9a` / `#ff8a8a` | verbatim | 9.07:1+ | error text |
| `colorWarning` | `#FFB800` | `#dc9f03` | 7.90:1 | warning text, icons, borders |
| `colorWarningText` / hover / active | `#FFB800` / `#e8b727` | verbatim | 9.86:1+ | warning text |
| `colorTextLightSolid` | `#0A2E22` | verbatim | 5.53:1 on primary fill | primary button label |
| `Button.primaryColor` | `#0A2E22` | verbatim | 5.53:1 on `#03b488` | primary solid button label |
| `Button.dangerColor` | `#000000` | verbatim | 5.80:1 on `#dc5e5e` | danger solid button label (white 3.62:1 and ink 4.07:1 both fail) |
| `colorTextPlaceholder` | `#9CA3AF` | verbatim | 7.26:1 | placeholders |
| tints (primary/info, success, warning, error) | `#082F24` / `#112521` / `#2c2211` / `#2c1a1b` | verbatim | 4.57:1+ with their text | tag and alert backgrounds |
| `Table.headerColor` | `#ACACAC` | verbatim | 8.12:1 | table headers |
| antd secondary text | `rgba(255,255,255,0.65)` | `#ADADAD` composited | 8.21:1 | `type="secondary"` |

**Rejected pairs, do not reintroduce:** `#00D09C` as light-mode text (2.00:1), `#FF4757` as light-mode text (3.34:1), `#FFB800` as light-mode text (1.73:1), `#FF6B35` accent on white (2.84:1), `#E5E7EB`-class dividers as component boundaries (1.24:1), `#E03131` text on `#FEF2F2` (below 4.5:1, which is why light error is `#C62828`). `#B0B8C4` (`--color-text-disabled`) is exempt only on genuinely disabled controls, which WCAG excludes.

### 2.3 Typography, radius, density, motion

- **Type:** Inter stack (`Inter, system-ui, sans-serif`), base `fontSize: 14`. Heading levels map to product roles, not antd defaults: `fontSizeHeading1: 24`, `fontSizeHeading2: 20`, `fontSizeHeading3/4: 16`, `fontSizeHeading5: 14`. Do not reassign these per page.
- **Radius:** `borderRadius: 12`, `borderRadiusLG: 16`. antd v6 dropped the `borderRadiusXL` alias; set 24px per-component (for example Modal or Sheet) when those migrations land.
- **Density:** `Button` heights 44 / 48 / 56 (`SM` / default / `LG`), `Input` height 48, radius 12. 48px controls satisfy the 24px WCAG minimum with touch headroom.
- **Motion:** state feedback only (hover, focus, open/close), 120 to 240ms, `ease-out`. The `pulse 2s infinite` scan frame in `qris/page.tsx` is the only ambient animation, scoped to the scanning state. Respect `prefers-reduced-motion: reduce` for anything new.

### 2.4 The CSS-variable layer (globals.css)

Two roles live outside antd tokens because their consumers cannot read antd tokens:

| Layer | Values | Guard |
|:--|:--|:--|
| VIP tier colors (`--tier-*`) | `VIPBadge` renders `tierColor` as text and icon color on the card surface. `useVIPStatus` returns `var(--tier-*)`, defined per theme in `globals.css`: light `#8C5A2B` / `#707070` / `#92700F` / `#5F6B76` / `#2E7D9A` / `#0B7A55` / `#6B7280` (4.62:1 to 5.81:1 on white), dark `#D9A066` / `#C9CDD3` / `#FFD666` / `#D8DEE6` / `#9BE3F5` / `#00D09C` / `#9CA3AF` (7.26:1 to 13.61:1 on `#141414`) | `src/__tests__/theme/tier-colors.test.ts` parses `globals.css` and checks both themes |
| Chart palette (`src/lib/chart-palette.ts`) | Canvas plots cannot resolve `var()` at draw time, so the palette is explicit and theme-aware. Series: light `#0B7A55` / `#D97706` / `#7C3AED` / `#E11D48` / `#64748B` / `#0E7490`, dark `#00D09C` / `#34D399` / `#F59E0B` / `#8B5CF6` / `#F43F5E` / `#94A3B8`; marks need 3:1 (SC 1.4.11), axis labels 4.5:1 (`#6B7280` / `#ACACAC`) | `src/__tests__/lib/chart-palette.test.ts` |
| `--color-accent` | `#C2410C` with `--color-accent-foreground: #FFFFFF` (5.18:1). No live consumer; the pair is sound if used | covered by the tier-colors test |

## 3. Component-level rules

### 3.1 Shared state matrix

Applies to every interactive component. Deviations must be written down in the component.

| State | Visual | Notes |
|:--|:--|:--|
| default | Surface fill, default border, `--color-text-primary` text | Buttons: variant fill per 3.2 |
| hover | Explicit hover token from `tokens.ts` | 120ms transition; do not regenerate per page |
| focus-visible | antd focus outline on keyboard focus | Must stay visible; never `outline: none` without a replacement |
| active | Explicit active token | |
| disabled | antd disabled tokens (exempt from AA) | Keep `disabled` semantics; add a tooltip when the reason is not obvious |
| loading | Spin plus persistent label, `aria-busy` | Never replace the label with a spinner alone |
| error | `colorError` border and message | Message stays programmatically associated (see 4.5) |

### 3.2 Button

- **Variants:** `type="primary"` (fill `colorPrimary`, label `Button.primaryColor`: white 5.34:1 in light, ink 5.53:1 in dark), `type="default"`, `type="text"`, and `danger` (fill `colorError`, label `Button.dangerColor`: white 5.62:1 in light, black 5.80:1 in dark).
- **Sizes:** `controlHeightSM: 44` for dense tables, default 48, `controlHeightLG: 56`. `fontWeight: 700` is the system button weight.
- **Rules:** one primary button per screen; icon-only buttons require `aria-label`; labels name the outcome ("Buat transfer"), never "Submit".
- **Don't:** a second filled primary button next to the first, or a raw hex fill bypassing `token.colorPrimary`.

### 3.3 Form controls (Input, Select, Form.Item)

- Persistent visible label above the control; the placeholder is a hint (`colorTextPlaceholder`, 4.83:1 / 7.26:1), never the label.
- Errors keep the default association (antd wires `aria-describedby`); error text renders in `colorError` family tokens, all measured above.
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
- **Navigation:** Menu and Tabs active states derive from `colorPrimary` (5.34:1 light / 6.92:1 dark). The skip link in `src/lib/a11y.tsx` stays first in tab order.
- **Feedback:** `Alert`, `Tag`, `message`, and `notification` colors derive from the seeds and tints above; do not pass raw hex to `Alert type` or `Tag color` overrides.

## 4. Accessibility requirements and testable acceptance criteria

- **4.1 (SC 1.4.3, 1.4.11):** The allowlist in 2.2 is the only legal set of text, fill, tint, and boundary pairs. Verify: `npx vitest --run src/__tests__/theme` must pass; any new token pair gets covered before merge.
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

**Done (this revision, 2026-10-13):** tokens extracted to `src/theme/tokens.ts` with explicit, measured values for every text, fill, tint, and border role in both themes and all interaction states; antd-generated steps that failed AA are overridden; dark mode keeps the vivid brand palette with ink or black button labels; VIP tier colors moved to per-theme CSS variables (4.62:1 to 13.61:1, both themes); the analytics chart palette moved to a measured, theme-aware module (marks 3:1, axis labels 4.5:1); `--color-accent` darkened to `#C2410C` (5.18:1). Guards: `tokens.test.ts` (config intent), `antd-theme.test.tsx` (rendered CSS variables, both algorithms), `tier-colors.test.ts`, `chart-palette.test.ts`.

**Prohibited:**

| Anti-pattern | Why | Fix |
|:--|:--|:--|
| Raw hex in components where a token exists | Palette edits miss call sites; bypasses the AA guard | `theme.useToken()` values |
| Reintroducing vivid brand colors as light-mode text seeds | 2.00:1 / 3.34:1 / 1.73:1 | keep the `tokens.ts` allowlist |
| `rgba(0,0,0,...)` fixed component colors | fails light, invisible in dark | per-theme tokens |
| Deleting an explicit step/tint/border to "use antd defaults" | generated steps fail AA (4.46:1, 4.26:1, mid-gray tints) | keep explicit overrides |
| Hover-only row actions | unreachable by keyboard | visible on hover and focus |
| Placeholder as label | label vanishes; association lost | persistent visible label |
| One-button label token across fills in dark mode | no single label color passes on both emerald and red fills | `Button.primaryColor` / `Button.dangerColor` |
| Chart or tier colors as raw hex in components | canvas cannot read tokens and themes diverge | `src/lib/chart-palette.ts`, `--tier-*` CSS variables |

**Findings, not yet fixed (each is a separate ticket):**

1. Pre-existing unit-test failures unrelated to theming: 7 files, 23 tests (`pages/StatementsPage`, `BillsPage`, `ExchangePage`, `CardsPage`, `QRISPage`, `MerchantPage`, `SettingsPage`) fail in `next-intl` provider setup; verified failing on a pristine tree before this change.
2. `useVIPStatus.test.tsx` keeps its label and tier-mapping assertions; exact tier-color pins were removed because the invariant lives in `tier-colors.test.ts`.

## 7. QA checklist (code review)

- [ ] Color edits touch only `src/theme/tokens.ts`, `globals.css` tier/accent vars, or `src/lib/chart-palette.ts`; no raw hex in components.
- [ ] `npx vitest --run src/__tests__/theme src/__tests__/lib` passes (config intent, rendered invariants both themes, tier colors, chart palette).
- [ ] New token usages (badges, tints, chart constants) appear in the allowlist with a measured ratio.
- [ ] Buttons: one primary per screen, icon-only has `aria-label`, loading keeps the label, label tokens untouched.
- [ ] Inputs: persistent label, placeholder is a hint, errors use the default association.
- [ ] Table: header tokens untouched or measured, empty and loading states present, actions keyboard-reachable.
- [ ] Both themes checked (toggle in the UI) for the changed surface.
- [ ] Copy passes section 5; `npm run check:i18n` green.
