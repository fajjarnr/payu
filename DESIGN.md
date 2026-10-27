# DESIGN.md — PayU Web App

<!-- impeccable:design-schema 1 -->

> Visual language follows `wise/DESIGN.md` (Wise re-skin, web-app `1.6.0`) and the
> single-locale cutover (web-app `1.7.0`). Source of truth for values is
> `frontend/web-app/src/theme/tokens.ts` (antd seeds + component tokens);
> `frontend/web-app/src/app/globals.css` carries the Tailwind aliases, tier
> colors, fonts, and motion tokens. All contrast ratios below are measured
> (WCAG 2.x relative luminance), not estimated.

## Platform

Adaptive (Next.js 16 + React 19 + antd 6 + Tailwind 4, responsive web —
360px mobile and 1440px desktop are equal targets, never mobile-first extension).

## Product Summary

PayU adalah platform digital banking untuk Indonesia. Target user: Millennials & Gen Z (18–40 tahun), mobile-native, mau simpel dan cepat. Web app adalah client penuh — Bahasa Indonesia saja (single-locale sejak `1.7.0`, semua route `/id/...`), dengan metode pembayaran lokal (QRIS, BI-FAST, SNAP-BI).

## Design Principles

1. **Responsive parity** — Design untuk 360px dan 1440px sebagai target setara; tidak ada "web adalah extension"
2. **One lime CTA** — Satu-satunya aksen adalah lime `#9fe870`; elevasi dari kontras permukaan (putih di atas sage), bukan shadow
3. **Speed as a feature** — <2s launch, <3s transaction, minimal steps
4. **Trust through security** — Biometric auth, transparent security, no dark patterns
5. **Financial clarity** — User selalu tahu balance, spending, goals
6. **Indonesian identity** — Bahasa Indonesia saja, `id-ID` formatting (`1.234,56`), QRIS/BI-FAST/SNAP-BI

## Visual Identity

### Color System

| Token | Value | Ratio | Usage |
|-------|-------|------:|-------|
| `primary` | `#9fe870` | 1.47:1 on white (fills only) | Lime CTA — buttons, fills, active indicators. **Never text** |
| `primary-hover` | `#cdffad` | 16.94 under ink | Hover state (Wise lightens — explicit, never derived) |
| `primary-active` | `#c5edab` | 14.72 under ink | Active/pressed state |
| `primary-pale` | `#e2f6d5` | — | Soft tints, badge backgrounds, selected menu fill |
| `on-primary` | `#0e0f0c` | 13.05:1 on lime | Ink labels on every lime fill (`ON_PRIMARY`) |
| `ink` | `#0e0f0c` | 19.23:1 on white | Default text, headings, dark surfaces, input borders |
| `ink-deep` | `#163300` | 13.93:1 on white | Primary/success text hover state |
| `body` | `#454745` | 9.37:1 on white | Secondary body text, table headers on sage (7.79:1) |
| `canvas` | `#ffffff` | — | Card interiors |
| `canvas-soft` | `#e8ebe6` | — | Sage page background, layout dividers |
| `positive` | `#2ead4b` | 2.92:1 (fills only) | Success seed — fills, never text |
| `positive-deep` | `#054d28` | 10.01:1 on white | Every lime-family text role: links, accents, success labels |
| `info` | `#38c8ff` | 1.93:1 (fills only) | Info seed — fills, never text |
| `warning` | `#ffd11a` | 1.46:1 (fills only) | Warning seed — fills, never text |
| `warning-content` | `#4a3b1c` | 7.44:1 on seed | Text on warning surfaces |
| `warning-deep` | `#b86700` | 4.22:1 on white | Large/bold-only; normal text uses `warning-content` |
| `negative` | `#d03238` | 5.01:1 both ways | Error text, destructive fills under white labels |
| `negative-hover` / `negative-active` | `#a72027` / `#a7000d` | 7.29 / 7.93 under white | Error interaction states |
| `negative-bg` | `#320707` | 17.96:1 under white | Dark maroon callouts, badge-negative, danger panel |
| `placeholder` | `#6B7280` | 4.83:1 | Placeholders (antd default ~2.3:1 is unusable) |
| `accent` | `#C2410C` | 5.18:1 under white | Tertiary accent — no live consumer; sound if used |
| `accent-orange` / `accent-cyan` | `#ffc091` / `#38c8ff` | — | Illustrative content only, never UI chrome |

Notes:

- Wise `mute #868685` (3.64:1) is decorative-only — small text uses `body`/`ink`.
- Lime is the CTA, never a success indicator — success comes from the positive family.
- Fixed brand surfaces (`CARD_ART_GRADIENT`, `BRAND_PANEL_*`, `DANGER_PANEL_BG`,
  `INK_SURFACE*`, `LIME_ACCENT`, `SAGE_ON_INK`) live in `tokens.ts` so no page
  holds a raw hex (guard: `design-system-guard.test.ts`).

### Typography

| Role | Size | Weight | Line Height | Tracking | Usage |
|------|------|--------|-------------|----------|-------|
| `display` | `clamp(40px, 6vw, 64px)` | 800 | 0.95 | −0.02em | Landing hero only (`.payu-display`) |
| `display-section` | `clamp(32px, 4vw, 48px)` | 800 | 0.95 | −0.02em | Landing CTA headline |
| `h1` | 32px | 600 | 1.2 | −0.96px | Page titles (`display-sm`) |
| `h2` | 24px | 600 | 1.3 | −0.48px | Section headings (`display-xs`) |
| `h3` / `h4` | 16px | 600 | 1.4 | −0.025em | Card titles, subheadings |
| `h5` | 14px | 600 | 1.4 | −0.025em | Small headings |
| `body` | 14px | 400 | 1.5 | −0.011em | Body text, descriptions (app base) |
| `body-strong` | 14px | 600 | 1.5 | −0.011em | Emphasized body text |
| `body-lg` | 20px | 400 | 1.5 | 0 | Lead paragraphs (landing hero) |
| `button` | 14–16px | 600 | 1.5 | 0 | Button labels (16 at `lg` size) |
| `caption` | 12px | 400 | 1.4 | 0 | Metadata, timestamps |
| `label` | 12px | 600 | 1.3 | +0.1em, uppercase | Form labels, eyebrows, badges |

**Font Stack**: Inter (body + all product headings, `--font-inter`) and Manrope 800
(display, `--font-display`, landing hero only — the sanctioned Wise Sans
substitute per `wise/DESIGN.md`). Loaded via `next/font/google` in
`src/app/[locale]/layout.tsx`. Product text is never 700/800.

### Spacing Scale

Base unit 4px (Wise). Section bands pad 48px top/bottom; card interiors pad 24px.

| Token | Value | Usage |
|-------|-------|-------|
| `xxs` | 2px | Hairline tweaks |
| `xs` (`space-1`) | 4px | Tight gaps, icon-to-text, badge padding-y |
| `sm` (`space-2`) | 8px | Inline gaps, list items |
| `md` (`space-3`) | 12px | Button padding-y, badge padding-x, card internal |
| `lg` (`space-4`) | 16px | Section gaps, form fields, mobile page padding |
| `xl` (`space-5`) | 24px | Card padding/interior, major sections, desktop page padding |
| `2xl` (`space-6`) | 32px | Page sections |
| `3xl` (`space-7`) | 48px | Hero/section bands, major breaks |

### Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `none` | 0px | Full-bleed bands |
| `sm` | 8px | Inline pills, small badges |
| `md` | 12px | Form inputs, small tiles |
| `lg` | 16px | Mid-size tiles, inner wells |
| `xl` | 24px | **Canonical card + button + tile radius** (`borderRadiusLG`) |
| `pill` / `full` | 9999px | Status pills, avatars, circular buttons |

Guard: `design-system-guard.test.ts` pins inline radii to
2/4/6/8/12/16/24/9999. antd v6 dropped the `borderRadiusXL` alias — 24px is set
per-component (Button, Card, Modal, Tag).

### Elevation & Depth

| Level | Treatment | Use |
|-------|-----------|-----|
| Level 0 — Flat | No shadow, no border | Default — everything |
| Level 1 — Hairline | 1px solid `ink` | Text inputs, selects, tertiary buttons |
| Level 2 — Surface contrast | White card on sage canvas | Cards — the contrast **is** the elevation |

There is no shadow scale: `Card.boxShadowTertiary` is `none` and card-level
`boxShadow` is banned. The only elevated surfaces are Modal, Drawer, and Toast,
which keep antd defaults.

## Layout

### Breakpoints

| Name | Width | Key changes |
|------|-------|-------------|
| `mobile` | < 768px | Hero stacks, converter full-width, grids 1-up; header 72px / 16px padding |
| `tablet` | 768–1023px | Grids 2-up; landing nav links appear at 768px |
| `desktop` | ≥ 1024px | Sidebar 288px, hero split, full grids; bottom nav hidden |

Implementation notes (Tailwind): `sm:640px` for page padding + header greeting,
`md:768px` for landing nav, `lg:1024px` for shell/sidebar/bottom-nav,
`xl:1280px` for the inline header search.

### Grid

- **App shell**: 1440px — `DashboardLayout` owns the content box:
  `max-w-[1440px]`, `px-4`/`py-4` mobile, `sm:px-6`/`sm:py-6`. **Pages must not
  re-implement it** (no own `max-width`, no own horizontal padding). Content
  reserves `96px + safe-area` bottom clearance for the mobile nav.
- **Marketing**: 1200px (`CONTENT_WIDTH` on the landing page).
- **Grid**: antd 24-column (`Row`/`Col`); gutter 24 (16 on tight mobile rows);
  landing hero gutter 40 at `lg`.
- **Narrow measures** are for single-column forms only (e.g. `max-w-4xl` = 896px
  on merchant registration), expressed with the same class vocabulary.

### Navigation

- **Mobile**: Bottom tab bar, 5 tabs — Dashboard, Transfer, QRIS, Cards,
  Settings (`MobileNav`, `lg:hidden`). Pill indicator + `aria-current`; hidden
  on login/onboarding and when unauthenticated.
- **Desktop**: Left sidebar 288px, grouped menus (overview / transactions /
  finance / others), active row = pale fill `#e2f6d5` + deep label `#054d28`
  (8.76:1). Mobile drawer mirrors it at 288px.
- **Top bar**: 72px everywhere (shell, backoffice, landing) — logo mark
  (lime 24px tile + wordmark), greeting, theme toggle, search (inline at `xl`,
  44px trigger + drawer below), notifications, avatar + profile menu.
- **Backoffice**: Separate shell — collapsible sider, 72px header, sage content
  (16px mobile / 32px desktop padding); universal search hidden below 640px.

## Component Patterns

### Buttons

| Variant | Style | Usage |
|---------|-------|-------|
| `primary` | Lime fill `#9fe870`, ink `#0e0f0c` label (13.05:1) | Main CTAs, form submits — one per screen |
| `secondary` | Sage `#e8ebe6` fill, ink label | Secondary actions |
| `tertiary` (`default`) | White, 1px ink border, ink label | Tertiary actions |
| `text` / `link` | No fill, deep-green `#054d28` text | Inline actions, links (10.01:1) |
| `danger` | `#d03238` fill, white label (5.01:1) | Destructive actions |
| `icon-circular` | White, full-round, 48px, ink icon, `aria-label` | Icon-only actions |

**Sizes**: `sm` 44px (dense tables), `md` 48px (default), `lg` 56px.
Labels 600, radius 24, padding 12/24.
**States**: default → hover (`#cdffad`) → active (`#c5edab`) — Wise lightens;
focus-visible 3px ring + 2px offset; disabled (sage fill + body text, never
opacity); loading (spinner + persistent label, `aria-busy`).

### Cards

- Default (`card-content`): white, **no border, no shadow**, radius 24, padding 24.
- `card-feature-sage`: sage fill; `card-feature-green`: `#e2f6d5` fill
  (how-it-works); `card-feature-dark`: ink fill + lime text 13.05:1 (promos,
  QR showcase, help panels).
- `hoverable` only when the card is actionable; no lift/shadow on hover.
- `currency-converter-card`: white, 1px ink border, radius 24 (landing hero).

### Forms

- **Inputs**: 48px height, radius 12, 1px **ink** border (also the 3:1 component
  boundary), body 16/14; focus ring 3px + 2px offset. Same for Select.
- **Labels**: Above input, 12px/600 uppercase +0.1em, `text-secondary`.
- **Helper text**: Below input, caption size.
- **Error text**: `negative` + icon, programmatically associated
  (`aria-describedby`, antd default wiring).
- **Validation**: Inline, on blur (not on every keystroke).
- **Money inputs**: `inputMode="decimal"`, values stay canonical decimal
  strings — never `valueAsNumber`, numeric coercion, or client-side balance
  arithmetic (`DECIMAL(19,4)` is enforced server-side).

### Lists & Rows

- **Row targets**: 44px minimum for interactive rows/links.
- **Divider**: 1px `canvas-soft`/sage between rows.
- **Hover**: `fill-tertiary` background.
- **Selected**: `primary-pale` fill + deep-green label; selectable tiles add a
  1–2px `primary-text` border (never the lime fill as a border — 1.47:1).

### Tables

- Header: sage background `#e8ebe6` + `body` labels (7.79:1 light;
  transparent + sage 15.98:1 dark). Never a fixed `rgba(0,0,0,…)` — it fails
  in light and disappears in dark.
- Empty: named empty message + primary action, never a blank grid.
- Loading: skeleton rows so layout does not jump.
- Row actions stay in the DOM and keyboard-reachable; hover-only reveals banned.
- Overflow wraps in a labelled scroll region; never clip amount/status cells.

### Modals & Sheets

- **Modal**: Centered, radius 24, **elevated shadow** (sanctioned exception);
  antd focus trap + Esc + focus return stay on. Destructive confirmations use
  the danger button.
- **Drawer**: Mobile nav (left, 288px) and search (top); labelled distinctly
  from their desktop twins.
- **Close**: X button top-right, Esc to dismiss, backdrop click to close.

### Badges & Tags

- `badge-positive`: `#e2f6d5` + `#054d28` (8.76:1); `badge-negative`: `#320707`
  + white (17.96:1 light; `#ff9a9a` on `#320707` dark). Pill 9999, 14/600,
  padding 4/12. Tag labels always use `*-text` roles, never raw seeds.

### Empty States

`EmptyState` atom (max 448px, centered): white card radius 24 → 64px icon
(`text-secondary`) → `h2` title → secondary description → optional primary CTA.
A page h1 owns the heading order — the empty title is `h2`, not `h3`.

### Loading States

`LoadingState`: `skeleton` variant mirrors the shell box (1440px, `aria-busy`,
no layout jump); `spinner` variant is `role="status"` + large Spin + the
`common.loading` label (the label carries meaning — never a bare spinner).
Button loading keeps the label next to the spinner.

## Interaction Patterns

### Transitions

Motion tokens (`globals.css`) + arrival easing `cubic-bezier(0.16, 1, 0.3, 1)`:

| Token | Value | Usage |
|-------|-------|-------|
| `--duration-instant` | 100ms | Micro feedback |
| `--duration-fast` | 150ms | Hover, focus |
| `--duration-normal` | 250ms | Standard transitions, amount settle |
| `--duration-slow` | 350ms | Receipt unfold, complex |
| `--duration-focal` | 600ms | Seal-lock focal moments |

Keyframes: `fade-in` (landing), `seal-lock`, `receipt-unfold`,
`amount-settle`. Properties: opacity, transform, background-color.

### Gestures

No custom gestures: the web relies on explicit controls with
`touch-action: manipulation` (no tap delay, no double-tap zoom). No swipe
actions, no pull-to-refresh — every action has a visible, keyboard-reachable
control.

### Feedback

- **Success/info/warning/error**: `notify.*` → antd `message` (`role="alert"`);
  never the dead `useUIStore` toast slice.
- **Form errors**: inline message + icon, announced; critical failures add a toast.
- **Loading**: skeleton screens for pages (`aria-busy`), spinners + labels for actions.
- **Pending/unknown financial outcomes**: explicit pending state + reconcile —
  a timeout never proves a write failed; writes carry one `X-Idempotency-Key`.

## Accessibility

- **WCAG 2.2 AA** compliance required.
- **Color contrast**: 4.5:1 minimum for text, 3:1 for large text and component
  boundaries; focus indicator 3px at 3:1+ with 2px offset (2.4.11/2.4.13);
  targets ≥24px, controls 44–48px (2.5.8).
- **Lime is a fill, never a text color**: `#9fe870` is 1.47:1 on white, so
  brand-carrying text (links, accents, success amounts) uses `positive-deep`
  `#054d28` (10.01:1). Labels on a lime fill use ink `#0e0f0c` (13.05:1) —
  the same ink the primary button label uses (`ON_PRIMARY`).
- **Never stack opacity on text**: `opacity` compounds with colour (guard:
  `design-system-guard.test.ts`); use a solid AA token instead.
- **Never color-only status**: icon + text pairing on every `Alert`/`Tag`.
- **Focus indicators**: 3px outline (`.payu-focus-ring`), 2px offset — one rule
  app-wide; lime is never a focus color.
- **Screen reader**: Semantic HTML first (`h1`/`h2`/`p`/`strong` on the server
  landing — antd `Card`/`Typography`/icons are client-only and 500 in RSC);
  ARIA labels, live regions (`role="status"`, `aria-live`) for dynamic content.
- **Keyboard**: All interactive elements focusable, logical tab order; skip
  link first; dialogs trap + return focus; menus/tabs arrow-navigable.
- **Reduced motion**: `prefers-reduced-motion` neutralizes all keyframes.
- **Responsive**: 360px and 1440px verified — no horizontal overflow (labelled
  scroll regions for tables), no clipped labels.
- **Guards**: `tokens.test.ts` (config), `antd-theme.test.tsx` (rendered CSS
  vars, both themes, zero exceptions), `tier-colors.test.ts`, `chart-palette`
  (≥3:1 marks), `design-system-guard`, `legacy-css-vars`, axe suites.

## Page Inventory

Single-locale: every route lives under `/id/...` (`/` → `/id`). 47 routes build.

| Page | Route | Description |
|------|-------|-------------|
| Landing | `/id` | Server component: sage hero + Manrope display + converter card, features, how-it-works, dark security card, CTA, ink footer |
| Login | `/id/login` | Brand panel + form; session bootstrap |
| Forgot password | `/id/forgot-password` | Reset flow |
| Onboarding | `/id/onboarding` | eKYC flow (KTP, liveness, Dukcapil) |
| Dashboard | `/id/dashboard` | Balance overview, quick actions, recent activity |
| Transfer | `/id/transfer` | Internal, BI-FAST, SKN/RTGS; review → receipt flow |
| Transactions | `/id/transactions` | Full history with filters |
| Scheduled transfers | `/id/scheduled-transfers` | Recurring/scheduled list |
| QRIS | `/id/qris` | Scan frame, personal QR (dark card), limits, history |
| Bills | `/id/bills` | Biller grid, pay flow, success receipt |
| Split bill | `/id/split-bill` | Shared bills, participants, settle |
| Pockets | `/id/pockets` | Saving pockets, goals, shared |
| Cards | `/id/cards` | Virtual debit cards, freeze/unfreeze, limits |
| Investments | `/id/investments` | Deposito, reksadana, emas |
| Exchange | `/id/exchange` | FX converter + rate display + history |
| Lending | `/id/lending` | Loans + PayLater (lime gradient limit card) |
| Rewards | `/id/rewards` | Points, cashback, referrals, promos |
| Analytics | `/id/analytics` | Canvas charts (theme-aware `chartPalette`) |
| Statements | `/id/statements` | Statement generator + downloader |
| Security | `/id/security` | Sessions, devices, panic protocol (maroon panel) |
| Settings | `/id/settings` | Profile, preferences, theme |
| Notifications | `/id/notifications` | Notification center |
| Support | `/id/support` | Channels, FAQ, status |
| Merchant | `/id/merchant` | Merchant dashboard |
| Merchant register | `/id/merchant/register` | Narrow-form registration (896px) |
| Legal terms / privacy | `/id/legal/terms`, `/id/legal/privacy` | Legal text (Indonesian) |
| Backoffice | `/id/backoffice/*` | Command center: kyc, fraud, customers, cms, campaigns, compliance, fx-rates, partners |

## Design Tokens (CSS Custom Properties)

Truth lives in `tokens.ts` (antd) — `globals.css` holds the Tailwind aliases,
tier colors, fonts, and motion. Representative subset:

```css
@theme inline {
  --color-primary-dark: #054d28;
  --color-primary-light: #e2f6d5;
  --color-secondary: #0e0f0c;
  --color-success: #2ead4b;
  --color-warning: #4a3b1c;
  --color-error: #d03238;
  --color-surface: #FFFFFF;
  --color-surface-dim: #e8ebe6;
  --color-text-primary: #0e0f0c;
  --color-text-secondary: #454745;
  --font-sans: var(--font-inter), system-ui, sans-serif;
  --font-display: var(--font-display), var(--font-inter), system-ui, sans-serif;
}

:root {
  /* sage layout, ink text, lime primary (HSL mirrors of tokens.ts) */
  --background: 120 6% 90%;
  --foreground: 120 7% 5%;
  --primary: 103 89% 71%;
  --color-primary: #9fe870;
  --color-border: #e8ebe6;

  /* VIP tiers, 4.5:1+ per theme (guard: tier-colors.test.ts) */
  --tier-vip: #054d28;      /* dark: #9fe870 */
  --tier-standard: #6B7280; /* dark: #9CA3AF */
  /* … bronze/silver/gold/platinum/diamond … */

  /* Motion */
  --duration-instant: 100ms;
  --duration-fast: 150ms;
  --duration-normal: 250ms;
  --duration-slow: 350ms;
  --duration-focal: 600ms;
  --ease-arrive: cubic-bezier(0.16, 1, 0.3, 1);
}

.payu-display {
  font-family: var(--font-display);
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 0.95;
}

.payu-focus-ring:focus-visible {
  outline: 3px solid var(--ant-color-primary-border);
  outline-offset: 2px;
}
```

Components read `token.*` / `var(--ant-color-*)` — the `globals.css` aliases are
not theme-aware (guard: `legacy-css-vars.test.ts`).

## Dark Mode (Shipped Scope)

Ink derivation (`wise/DESIGN.md` defines no dark tokens): layout `#0e0f0c`,
containers `#1a1c1a`, text sage `#e8ebe6`, text roles resolve to the light lime
tint `#cdffad` (16.94:1); success tint `#0d2b16`; error/warning keep measured
dark seeds; danger labels black (5.8:1 on `#dc5e5e`). Dark-only surfaces
(`card-feature-dark`, footer, brand panel) are fixed ink in **both** themes via
`INK_SURFACE*` constants. New work must verify both themes (axe
`color-contrast` in each + the rendered-token guard).

**Version**: 1.7.0
**Last Updated**: 2026-10-27
**Maintainer**: PayU Design Team
