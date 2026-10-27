# Ant Design System: Implementation Guidelines (web-app)

> Single source of truth for the web-app's antd v6 token layer and component rules.
> Visual language follows `wise/DESIGN.md` (Wise re-skin 2026-10-27, web-app `1.6.0`).
> All contrast ratios below are measured (WCAG 2.x relative luminance), not estimated.
> Guards, all failing CI on a broken invariant:
> - `src/__tests__/theme/tokens.test.ts` checks the configured values.
> - `src/__tests__/theme/antd-theme.test.tsx` extracts the CSS variables cssinjs actually generates for both algorithms and re-checks every pair, including generated aliases (hover/active steps, tints, borders) and button label tokens. The lime family NEVER renders as text — text states are pinned deep shades, asserted structurally.
> - `src/__tests__/theme/tier-colors.test.ts` parses `globals.css` and checks the `--tier-*` variables and the accent pair in both themes.
> - `src/__tests__/lib/chart-palette.test.ts` checks every series mark ≥3:1 on its theme surface plus AA axis labels.
> - `src/__tests__/theme/design-system-guard.test.ts` bans raw hex outside the theme module and pins the radius scale (2/4/6/8/12/16/24/9999).

## 1. Context and goals

**Design intent:** one calm Scandinavian-fintech surface — sage canvas, white 24px cards, a single lime CTA — where hierarchy and status read instantly, every control works from the keyboard, and mobile 360px and desktop 1440px are equal targets (responsive parity, never mobile-first extension).

The web-app runs antd v6 (`antd` `^6.6.5`) with a single `ConfigProvider` in `src/components/AntdProvider.tsx`. All theming flows from `src/theme/tokens.ts`:

- `lightTokens` / `darkTokens`: seed and alias tokens, selected by `isDark`.
- `lightComponentTokens` / `darkComponentTokens`: component overrides (Button, Card, Modal, Input, Select, Tag, Menu, Tabs, Table).
- Pages read runtime values via `theme.useToken()` (`token.colorPrimary`, `token.colorPrimaryBg`, ...). **Never** re-hardcode a hex that a token already provides.
- Fixed brand surfaces live in the same module: `ON_PRIMARY` (ink labels on lime fills), `CARD_ART_GRADIENT`, `BRAND_PANEL_*`, `DANGER_PANEL_BG`, and the always-ink surfaces `INK_SURFACE*` / `LIME_ACCENT` / `SAGE_ON_INK` (dark card, footer).

**PRODUCT DECISION 2026-10-27 (Wise re-skin).** The palette follows `wise/DESIGN.md`: lime primary `#9fe870` with ink labels (13.05:1), ink text `#0e0f0c`, sage layout `#e8ebe6`. The 2026-10-14 emerald decision (`#00D09C`) is fully superseded — zero references remain in `src/`. Lime on white is 1.47:1, so the primary/success/info fills NEVER render as text: their text roles pin deep shades (`positive-deep #054d28`, 10.01:1). The contrast guard enforces this structurally, not as exceptions.

**Scope:** the antd token layer plus the component rules in section 3. The landing page is a Server Component with three client leaves (`LandingNav`, `ConverterCard`, `LandingIcon`) — antd `Card`/`Typography` and `@ant-design/icons` (top-level `createContext`) 500 in RSC, so the server renders semantic HTML + server-safe antd (`Button`, `Tag`, `Row`/`Col`, `Divider`, `Flex`).

## 2. Design tokens and foundations

### 2.1 Color architecture

| Mode | Primary | Success | Info | Error | Warning |
|:--|:--|:--|:--|:--|:--|
| light | `#9fe870` fill, ink `#0e0f0c` labels (13.05:1); hover `#cdffad`, active `#c5edab` (explicit — Wise lightens) | seed `#2ead4b` (fills), text `#054d28` (10.01:1) | seed `#38c8ff` (fills), text `#054d28` | `#d03238` (5.01:1 both ways); hover `#a72027`, active `#a7000d` | seed `#ffd11a` (fills), text `#4a3b1c` (7.44:1 on the seed) |
| dark (ink `#0e0f0c`) | lime fills, ink labels (13.05:1+), text roles `#cdffad` (16.94:1) | text `#cdffad` on tint `#0d2b16` (13.46:1) | text `#cdffad` | text `#ff9a9a`, danger labels `#000000` (5.8:1 on `#dc5e5e`) | text `#FFB800`/`#e8b727` |

Text neutrals: `colorText #0e0f0c` (19.23:1), `colorTextSecondary`/`colorTextDescription #454745` (9.37:1), placeholder `#6B7280` (4.83:1). Wise `mute #868685` (3.64:1) is decorative-only — never small text. Input/tertiary borders are ink 1px; layout dividers are sage.

### 2.2 Measured state of the enforced roles

Light mode (surface `#FFFFFF`, layout `#e8ebe6`):

| Role | Value | Ratio | Notes |
|:--|:--|--:|:--|
| `colorPrimary` + `Button.primaryColor` | `#9fe870` / `#0e0f0c` | 13.05:1 | primary CTA, all states incl. hover/active |
| `colorPrimaryHover` / `colorPrimaryActive` | `#cdffad` / `#c5edab` | 16.94 / 14.72 under ink | explicit: antd's algorithm would darken |
| `colorLink` / `colorPrimaryText` / `colorSuccessText` | `#054d28` (+hover `#163300`, active `#0e0f0c`) | 10.01:1+ | every lime-family text role |
| `colorError` / hover / active | `#d03238` / `#a72027` / `#a7000d` | 5.01 / 7.29 / 7.93 | error text, danger fill under white label |
| `colorWarningText` (content) | `#4a3b1c` | 7.44:1 on `#ffd11a`, 10.86:1 on white | warning text; the seed never renders as text |
| `colorWarningBg` / `colorErrorBg` / `colorSuccessBg` | `#FFFBEB` / `#FEF2F2` / `#e2f6d5` | text 4.5:1+ | tag and alert tints |
| `Tag.colorText` / `colorErrorBg`+`colorErrorText` | `#054d28` / `#320707`+white | 8.76 / 17.96:1 | badge-positive / badge-negative |
| `colorTextPlaceholder` | `#6B7280` | 4.83:1 | antd default is ~2.3:1 |
| `Table.headerBg` / `headerColor` | `#e8ebe6` / `#454745` | 7.79:1 | sage header, body-ink labels |
| `Menu.itemSelectedColor` / `itemSelectedBg` | `#054d28` / `#e2f6d5` | 8.76:1 | sidebar active row |
| `--color-accent` / foreground | `#C2410C` / `#FFFFFF` | 5.18:1 | no live consumer; sound if used |

Dark mode (layout `#0e0f0c`, container `#1a1c1a`):

| Role | Value | Ratio | Notes |
|:--|:--|--:|:--|
| primary text roles | `#cdffad` / `#e2f6d5` / `#9fe870` | 16.94 / 16.82 / 13.05:1 | link, primary-text, success-text + states |
| `colorErrorText` / border | `#ff9a9a` / `#ff8a8a` | 9.07 / 8.12:1 | error text |
| `colorWarningText` / steps / border | `#FFB800` / `#e8b727` | 10.62 / 9.86:1 | warning text |
| `colorSuccessBg` / `colorSuccessBorder` | `#0d2b16` / `#cdffad` | 13.46:1 | dark success tint |
| `colorTextPlaceholder` / `colorTextDescription` | `#9CA3AF` / `#A6A6A6` | 7.26 / 7.9:1 | placeholders, secondary ink |
| `Table.headerColor` | `#e8ebe6` | 15.98:1 | sage on ink |
| `Button.dangerColor` | `#000000` | 5.80:1 on the red fill | white on `#dc5e5e` is 3.62:1 |
| tints (warning / error) | `#2c2211` / `#2c1a1b` | text 4.57:1+ | tag and alert backgrounds |

**No product-decision exceptions remain.** Every pair above is enforced in both themes. The lime fills are structurally excluded from text loops because they never render as text by design.

### 2.3 The CSS-variable layer (globals.css)

| Layer | Values | Guard |
|:--|:--|:--|
| VIP tier colors (`--tier-*`) | `VIPBadge` renders `tierColor` as text and icon color on the card surface. `useVIPStatus` returns `var(--tier-*)`, defined per theme: light `#8C5A2B` / `#707070` / `#92700F` / `#5F6B76` / `#2E7D9A` / `#054d28` (tier-vip) / `#6B7280` (4.62:1 to 10.01:1 on white), dark `#D9A066` / `#C9CDD3` / `#FFD666` / `#D8DEE6` / `#9BE3F5` / `#9fe870` / `#9CA3AF` (7.57:1 to 15.98:1 on ink) | `tier-colors.test.ts` parses `globals.css` for both themes |
| Chart palette (`src/lib/chart-palette.ts`) | Canvas cannot resolve `var()` at draw time. Theme-aware: light series `#163300` / `#054d28` / `#d03238` / `#b86700` / `#454745` / `#0e0f0c` (4.22:1 to 19.23:1 on white), dark `#9fe870` / `#cdffad` / `#ffd11a` / `#38c8ff` / `#ff9a9a` / `#e8ebe6` (9.47:1 to 16.94:1 on ink); axis labels ` #454745` / `#e8ebe6` | `chart-palette.test.ts` checks every mark ≥3:1 + AA labels |
| Legacy aliases (`--background`, `--primary`, …) | Retinted to Wise HSL (bg `120 6% 90%` sage, fg ink) but still a second source of truth — components must read `token.*` / `var(--ant-color-*)` | `legacy-css-vars.test.ts` bans them in components |

### 2.4 Typography, radius, density, motion

- **Type:** Inter body (`Inter, system-ui, sans-serif`), base `fontSize: 14`; Manrope 800 display (`--font-display`, `.payu-display`) for the landing hero only. Product headings: `fontSizeHeading1: 32` (display-sm), `fontSizeHeading2: 24` (display-xs), H3/H4 16, H5 14 — all weight 600. Button labels 16/600. Do not reassign per page.
- **Radius:** `borderRadius: 12` (inputs, small tiles), `borderRadiusLG: 24` (cards, buttons, tiles, modals — the Wise signature), pill 9999 (badges). antd v6 dropped the `borderRadiusXL` alias; 24px is set per-component (Button, Card, Modal, Tag).
- **Density:** `Button` heights 44 / 48 / 56 (`SM` / default / `LG`), `Input`/`Select` height 48 with ink borders. 48px controls satisfy the 24px WCAG minimum with touch headroom.
- **Elevation:** surface contrast (white cards on sage), not shadow. `Card.boxShadowTertiary` is `none`; Modal/Drawer/Toast keep antd defaults as the only elevated surfaces.
- **Motion:** state feedback only (hover, focus, open/close), existing duration/easing tokens. Respect `prefers-reduced-motion: reduce` for anything new.

## 3. Component-level rules

### 3.1 Shared state matrix

| State | Visual | Notes |
|:--|:--|:--|
| default | Surface fill, default border, ink text | Buttons: variant fill per 3.2 |
| hover | Explicit lighter tint (`#cdffad` primary) | 120–250ms transition; do not regenerate per page |
| focus-visible | 3px ink outline, offset 2 (`.payu-focus-ring`) | Must stay visible; lime is 1.47:1 and never a focus color |
| active | Explicit neutral tint (`#c5edab` primary) | |
| disabled | Sage fill + body text (not opacity) | Keep `disabled` semantics; add a tooltip when the reason is not obvious |
| loading | Spin plus persistent label, `aria-busy` | Never replace the label with a spinner alone |
| error | `colorError` border and message | Message stays programmatically associated (see 4.5) |

### 3.2 Button

- **Variants:** `button-primary` (fill `#9fe870`, ink label, 13.05:1), `button-secondary` (sage fill, ink label), `button-tertiary` (white + 1px ink border, ink label), `button-icon-circular` (white, full-round, 48px, `aria-label` required), and `danger` (fill `#d03238` light with white label 5.01:1; `#dc5e5e` dark with black label 5.80:1 via `Button.dangerColor`).
- **Sizes:** `controlHeightSM: 44` for dense tables, default 48, `controlHeightLG: 56`. `fontWeight: 600`, radius 24, padding 12/24.
- **Rules:** one primary button per screen; labels name the outcome ("Kirim uang"), never "Submit".

### 3.3 Form controls (Input, Select, Form.Item)

- Persistent visible label above the control; the placeholder is a hint (`colorTextPlaceholder`, 4.83:1 / 7.26:1), never the label.
- Errors keep the default association (antd wires `aria-describedby`); error text renders in the measured `colorError` family.
- `Input`/`Select` keep `controlHeight: 48`, `borderRadius: 12`, ink 1px border (also the 3:1 component boundary).
- Money inputs: `inputMode="decimal"`, values stay canonical decimal strings — never `valueAsNumber`, numeric coercion, or client-side balance arithmetic.
- Long values scroll inside the control; the control never expands the layout.

### 3.4 Table

- `headerBg: #e8ebe6` (sage), `headerColor` per theme (`#454745` light 7.79:1 / `#e8ebe6` dark 15.98:1). Do not revert to a fixed `rgba(0,0,0,...)`: it fails in light and disappears in dark.
- Empty state: a named empty message plus the primary action, never a blank grid. Loading: skeleton rows so the layout does not jump.
- Row actions stay in the DOM and keyboard-reachable; hover-only reveals are prohibited.
- Overflow: wrap in a scroll container; never clip amount or status cells.

### 3.5 Card, Dialog, Navigation, Feedback

- **Card:** `borderRadiusLG: 24`, no border, no shadow — white on sage IS the elevation. Feature variants: `card-feature-sage` / `card-feature-green` (`#e2f6d5`) / `card-feature-dark` (ink + lime text, 13.05:1).
- **Dialog / Modal:** radius 24 + elevated shadow (the sanctioned exception); antd handles focus trap, Escape, and focus return; keep those defaults. Destructive confirmations use the danger button.
- **Navigation:** sidebar rows white with the lime indicator for active (`itemSelectedBg #e2f6d5`, label `#054d28` 8.76:1); mobile bottom nav with pill indicator + `aria-current`. The skip link in `src/lib/a11y.tsx` stays first in tab order.
- **Feedback:** `Tag` text roles pinned (`colorText #054d28`, error `#ffffff` on `#320707` dark); `Alert`/`message`/`notification` derive from the seeds and tints above; do not pass raw hex to `Alert type` or `Tag color` overrides.

## 4. Accessibility requirements and testable acceptance criteria

- **4.1 (SC 1.4.3, 1.4.11):** The allowlist in 2.2 is the only legal set of text, fill, tint, and boundary pairs — no exceptions. Verify: `npx vitest --run src/__tests__/theme src/__tests__/lib` must pass; any new token pair gets covered before merge.
- **4.2 (SC 2.4.7, 2.4.11):** Keyboard focus shows the 3px ink outline on every control. Verify: Tab through each changed screen; no invisible or clipped focus, no `outline: none` without a replacement.
- **4.3 (SC 2.1.1):** Complete one full task per changed screen keyboard-only (Enter/Space activate, Escape closes, arrows move in menus and tabs, focus returns after dialogs).
- **4.4 (SC 2.5.8):** Pointer targets are at least 24x24px; the 44px minimum control height covers touch.
- **4.5 (SC 3.3.1, 4.1.3):** Inputs keep persistent labels; error messages stay associated and announced. Verify: submit an invalid form with a screen reader.
- **4.6 (SC 1.4.1):** Status is never color-only; keep the icon plus text pairing that `Alert` and `Tag` provide. Lime is the CTA, never a success indicator — success comes from the positive family.
- **4.7 (SC 2.3.1):** `prefers-reduced-motion: reduce` neutralizes transitions and ambient animation.
- **4.8 (dark mode):** Both themes pass the same checks; a change that passes in light but fails in dark is a defect. Verify with the store theme toggle plus 4.1's test.
- **4.9 (responsive parity):** Every changed screen renders at 360px and 1440px with no horizontal overflow (tables get a labelled scroll region) and no clipped labels.

## 5. Content and tone standards

- Voice: concise, confident, helpful, clear, friendly, professional, action-oriented, low-jargon — the friendly fintech register, short sentences, action verbs.
- Sentence case for titles and labels; tracked-uppercase micro-labels ("TOTAL MASUK") only for fixed category labels.
- Buttons: verb + object ("Kirim uang", "Buka Rekening"). Never "Submit", "Click here".
- Confirmations: amount + recipient + ETA. "Rp1.500.000 terkirim ke Budi · tiba ~5 menit".
- Errors: what happened, why, next step, and the record. "Saldo tidak cukup. Kurangi nominal atau pilih sumber dana lain." + "Coba lagi" (the RFC 9457 code stays in technical detail, never the headline).
- Empty states: situation + CTA. "Belum ada transaksi. Kirim uang pertama Anda dalam 2 menit."
- Amounts and dates: keep the existing Indonesian formatting (`Rp` prefix, `.` thousands, ISO dates in tables) with consistent decimals.
- Copy ships in Indonesian only: `messages/id.json` validity (non-empty leaves) is enforced by `npm run check:i18n`.

## 6. Anti-patterns and migration notes

**Done (Wise re-skin, 2026-10-27, web-app 1.6.0):** full palette cutover to `wise/DESIGN.md` (lime/ink/sage + semantic family, `ON_PRIMARY` rename, fixed-surface constants, theme-aware chart palette, Manrope display); shell unified; landing split server-first with three client leaves; all ~30 routes reskinned (no lime-as-text, radius 24, shadows removed, text 600); guards rewritten with zero product-decision exceptions; vitest 2113/2113, build 83 routes, landing verified at 360px + 1440px.

**Prohibited:**

| Anti-pattern | Why | Fix |
|:--|:--|:--|
| Raw hex in components where a token exists | Palette edits miss call sites; bypasses the guards | `theme.useToken()` values or `@/theme/tokens` constants |
| A second brand accent next to the lime | Wise has exactly one CTA color | lime for CTAs; positive-family for success, cyan+ink for info |
| Lime/positive/info fills as text or icons on light surfaces | 1.47–2.92:1, unreadable | deep text roles (`*-text`), ink labels on fills |
| `mute #868685` for small text | 3.64:1 on white | `body #454745` / ink |
| Sharp-rectangle CTAs / radius below 24 on cards | breaks the Wise geometry | 24px pill-rectangle, pill 9999 for badges |
| Card shadows as primary elevation | Wise elevates via surface contrast | white on sage; shadow only for Modal/Drawer/Toast |
| White labels on lime fills | 1.47:1 | `ON_PRIMARY` ink (13.05:1) |
| `rgba(0,0,0,...)` fixed component colors | fails light, invisible in dark | per-theme tokens |
| Deleting an explicit step/tint to "use antd defaults" | generated steps fail AA and invert Wise hover (lighten vs darken) | keep explicit overrides |
| Hover-only row actions | unreachable by keyboard | visible on hover and focus |
| Placeholder as label | label vanishes; association lost | persistent visible label |
| Silently skipping a guard pair without a dated reason | hides regressions | document the exception in `tokens.ts` and the test |
| Chart or tier colors as raw hex in components | canvas cannot read tokens and themes diverge | `src/lib/chart-palette.ts`, `--tier-*` CSS variables |
| Importing `@ant-design/icons`, antd `Card`/`Typography` in a Server Component | top-level `createContext` → build/runtime 500 | resolve via a `"use client"` leaf (see `LandingIcon`) |

**Findings, not yet fixed (each is a separate ticket):**

1. `warning-deep #b86700` is 4.22:1 on white — large/bold-only; normal text uses `warning-content #4a3b1c`. Enforced by convention, not yet by a guard.
2. Playwright `dashboard-landing-flow` on `/` fails with `ERR_TOO_MANY_REDIRECTS` — pre-existing proxy loop, reproduced on the unmodified baseline (2026-10-27). Tracked here; landing verified directly on `/en` instead.
3. `npm run lint` carries 3 errors / 37 warnings on the unmodified baseline (react-hooks purity in `BannerCarousel`/`useAnalytics`/`useCompliance` + warnings) — untouched by this re-skin.

## 7. QA checklist (code review)

- [ ] Color edits touch only `src/theme/tokens.ts`, `globals.css` tier/accent vars, or `src/lib/chart-palette.ts`; no raw hex in components.
- [ ] `npx vitest --run src/__tests__/theme src/__tests__/lib` passes; zero product-decision exceptions.
- [ ] The primary button renders lime `#9fe870` with ink `#0e0f0c` labels in both themes (browser or guard).
- [ ] New token usages (badges, tints, chart constants) appear in the allowlist with a measured ratio.
- [ ] Buttons: one primary per screen, icon-only has `aria-label`, loading keeps the label, label tokens untouched.
- [ ] Inputs: persistent label, placeholder is a hint, errors use the default association, money stays decimal strings.
- [ ] Table: header tokens untouched or measured, empty and loading states present, actions keyboard-reachable.
- [ ] Both themes checked (toggle in the UI) for the changed surface.
- [ ] 360px and 1440px checked: no overflow, no clipped labels.
- [ ] Copy passes section 5; `npm run check:i18n` green.
