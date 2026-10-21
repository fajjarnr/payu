# Ant Design System Guidelines — PayU Web App

## 1. Context and goals

**Design intent:** PayU web is a dense, trustworthy banking console where every screen reads as one system — Ant tokens, Ant components, keyboard-first.

Stack: Next.js 16.3.3 + React 19 + antd `^6.6.5` + Tailwind v4 CSS-first (`package.json:24-41`). Theme mounts as `AntdProvider` (ConfigProvider) in `src/components/AntdProvider.tsx:32-50`, bridged by `AntdShell` in `src/app/providers.tsx:85-98`, mounted in `src/app/[locale]/layout.tsx:72`. antd locale follows next-intl (`id` default, `en`) via `src/lib/antd-locale.ts:9-20`.

Non-goals: no new component library, no palette redesign in this file, no per-page exceptions.

## 2. Design tokens and foundations

### 2.1 Source-of-truth rule

**`src/theme/tokens.ts` must be the single source of truth for every antd-rendered color, radius, and height.** Two legacy sources disagree with it and must lose:

| Role | `tokens.ts` (antd truth) | `globals.css` legacy | Ant target (flag, §2.2) |
|---|---|---|---|
| Primary | `#00D09C` (`tokens.ts:27,36`) | `#0B7A55` (`globals.css:95`) / `hsl 160 84% 26%` (`globals.css:64`) | `#1677ff` |
| Success | `#00D09C` (`tokens.ts:37`) | `#00D09C` (`globals.css:100`) | `#16A34A` |
| Warning | `#B45309` (`tokens.ts:31`) | `#FFB800` (`globals.css:101`, `@theme:29`) | `#D97706` |
| Danger/error | `#C62828` (`tokens.ts:28`) | `#FF4757` (`globals.css:102`, `@theme:30`) | `#DC2626` |
| Surface | `#FFFFFF` (match) | `#FFFFFF` (`globals.css:103`) | `#FFFFFF` ✅ |
| Text | default ink (near) | `#1A1A2E` (`globals.css:105`) | `#111827` (near) |

Rules:

- **must** read color/radius/height from `token.*` (via `theme.useToken()` or `var(--ant-color-*)`); **must not** add new `bg-bank-green`, `bg-muted`, `text-muted-foreground`, `border-border`, `bg-card` classes — they bypass `ConfigProvider` and silently miss dark-mode switches (see §6 A-01).
- **must** treat `globals.css` `@theme` (`:9-56`) and `:root` (`:95-108`) as frozen legacy: read-only for old classes, no new aliases.
- `components.json` is dead shadcn config (`components.json:1-20`, `iconLibrary: lucide`, deleted `ui/` dir) — **must not** follow it; **should** delete it in a cleanup pass.

### 2.2 Brand-green flag (product decision, not code)

`tokens.ts:9-27` records PRODUCT DECISION 2026-10-14: vivid `#00D09C` kept at **2.00:1 on white** with an accepted AA failure. Adopting Ant primary `#1677ff` (or any AA-safe green) is a **product decision** — this file flags it, never sneaks it in. Until decided, §4 F-01 constrains *usage* (ink labels, no emerald small text), not the seed.

### 2.3 Typography

Inter everywhere: `FONT_FAMILY` (`tokens.ts:6`), `--font-sans/heading/mono` all resolve to Inter (`globals.css:53-55`) — no Plus Jakarta Sans, no JetBrains Mono. Declared scale is partial: base 14, headings 24/20/16/16/14 (`tokens.ts:61-67`); 12px eyebrows and 32px icons are ad-hoc per component.

- **must**: body ≥14px; eyebrow/labels ≥12px only with `fontWeight ≥ 700` and AA-safe color (never `colorPrimary` text on light).
- **should**: tabular numerals for money (`fontVariantNumeric: tabular-nums`, cf. `BalanceCard.tsx:95`).

### 2.4 Spacing

No declared scale. Observed: `Space` sizes 24/16/12/8, `Row` gutters 24/16, radius 12–16 (Button/Input/Card tokens: `tokens.ts:92-107`).

- **must**: new work uses 4-pt steps from {4, 8, 12, 16, 24, 32}; radius from {12, 16} (full pills only for tags/badges).
- **should**: section gap 24, intra-card stack 16, tight pairs 8.

## 3. Component-level rules

Conventions for all: antd component first; `ConfigProvider` tokens over Tailwind classes; 44px preferred touch target (24px WCAG 2.5.8 floor).

### 3.1 App shell — `DashboardLayout.tsx:134-217, 233-412`

- Anatomy: `Layout` + `Sider` 288px (`breakpoint=lg`) + sticky `Header` + `Content` max-1200 + nav `Drawer` + search `Drawer` + `MobileNav`.
- States: drawer open/closed; search inline ≥xl else trigger+drawer (`:302-320`). No shell-level loading/error — **must** stay that way (pages own states).
- Input: sidebar items navigate via `onClick → router.push` (`:173-176`) — **must** become real links (keyboard: Enter works, but no new-tab/context-menu semantics).
- Token usage: exemplary (`token.colorBgContainer/BorderSecondary/BgLayout/PrimaryBg`, `:244-245,270-271,348-349,362`).
- Edge: duplicate `aria-label="Pencarian cerdas"` on desktop + mobile inputs (`:303,402`) — **must** disambiguate (`…desktop` / `…mobile`).

### 3.2 KPI header — `dashboard/BalanceCard.tsx:42-134, 399-477`

- Anatomy: `Row/Col` grid; primary-balance + net-worth `Card`s; `Title level={2}` tabular with `overflowWrap: break-word` (`:91-103`); `Tag success` delta; `--` fallback (`:115-119`).
- States: `isLoading` prop declared (`:22,35`) but no skeleton branch in body — **must** wire loading UI before reuse.
- Token usage: `var(--ant-color-*)` inline — good; eyebrow in `colorPrimary` (`:56`) inherits F-01 — **must** switch eyebrow text to secondary ink.
- Decorative 8px dot (`:79-87`) **must** be `aria-hidden`.

### 3.3 Action grid — `dashboard/QuickActions.tsx:36-309`

- Anatomy: `Card role=region labelledby` (`:122-125`); Edit toggle with `aria-pressed` (`:140-151`); grid `xs12/sm6/lg6/xl4` (`:168-181`); tile `<a>` with 64px well + ellipsis description (`:233-284`); edit-mode hint `role=status` (`:156`).
- States: edit-mode dashed border + `fill-tertiary` (`:245-250`); first/last disable (`:289,298`). **Missing: hover, focus-visible, loading, error, empty — must add focus-visible ring ≥2px (§4 F-02).**
- Input: tiles are real links ✅; edit arrows handle ArrowUp/Left/Down/Right (`:220-229`); Naik/Turun 44px ✅ (`:292,302`).
- Token usage: render hardcodes `primary-bg` (`:270-271`); `color/bgColor` fields (`:42-43,52-53`, `bg-success-light`, `bg-chart-*`) are dead props — **must** delete them.
- Edge: description `nowrap+ellipsis` (`:280`) truncates; `Lihat Semua Fitur` (`:185-196`) has no `onClick` — **must** wire or remove.

### 3.4 Ledger — `dashboard/TransferActivity.tsx:97-237, 248-599`

- Anatomy: quick-transfer `Card` (category grid + 5 contact buttons 52px + 56px primary CTA `:353-367`); desktop `Table` (`:415-428`) + mobile `Card` list (`:431-523`) + bespoke empty (`:525-558`) + link footer (`:561-592`).
- States: skeleton list (`:401-411`) ✅; `Table locale.emptyText` (`:421`) is dead on mobile — **must** share one empty component; **no error branch — must add error + retry.**
- Input: row `Dropdown trigger=click` (`:226`) ✅; month button (`:378-398`) has no menu — **must** wire or remove; icon-only row buttons are ~32px — **should** pad to 44px on touch.
- Token usage: `var(--ant-color-*)` ✅; credit-amount `colorPrimary` (`:187-189`) inherits F-01 — **must** use success-dark text token.

### 3.5 Bottom nav — `MobileNav.tsx:16-116`

- Anatomy: `div[role=navigation]` + antd horizontal `Menu`, 5 items, `min-h-44px` links (`:60`), `aria-current=page` (`:59`) ✅.
- States: active = stroke 2.5px vs 2px (`:64-66`) + Menu selected — color/stroke-only distinction **should** gain a label-weight or indicator change.
- Fixes: wrapper **must** be `<nav>` not `div role` (`:82`); Menu-around-Links double semantics **should** simplify to a plain list.

### 3.6 List + form — `account/BeneficiaryManager.tsx:70-165`

- Anatomy: `Card > List role=list/listitem` (`:89-95`); field errors via `status=error` + `role=alert` (`:141-156`) ✅; `Skeleton aria-busy` (`:83`); `Empty` (`:84-87`, double description — **should** keep one).
- Input: delete is global `deleteMut.isPending` — **must** scope loading per row; delete hit area ~32px — **should** pad to 44px.
- Layout: `Col span 8/16` (`:137-152`) never stacks — **must** add `xs=24` stacking at 360px.
- Labels: `Typography.Text` as visual labels (`:140,147,154`) — **must** become `<label htmlFor>` (§4 F-04).

### 3.7 Promo hero — `cms/BannerCarousel.tsx:62-200`

- Anatomy: antd `Carousel scrollx dots=false`; slide `role=button tabIndex=0` (`:120-131`); prev/next circles (`:167-172`); dot `Button`s 44px (`:179-195`) ✅.
- States: skeleton (`:88-94`) ✅; error/empty returns `null` (`:96-98`) — **must** add error UI (§4 R-06).
- Input: slide has no accessible name (image `alt=title` duplicates `h3`, `:136,153`) — **must** give one `aria-label`; autoplay `setInterval` (`:63-70`) with no pause — **must** add pause-on-hover/focus + `prefers-reduced-motion` (§4 F-07); arrows `hidden sm:block` + hover-reveal (`:166-167`) — **must** keep arrows focus-visible on all viewports.
- Token usage: worst offender — `bg-bank-green/90` (`:149`), `bg-bank-green` dot (`:191`), `shadow-bank-green` (`:121`), `bg-muted/50` (`:91`) — **must** migrate per §6.

### 3.8 Auto modal — `cms/PromoPopup.tsx:51, 126-311`

- Anatomy: antd `Modal` 512 centered, mask `rgba(0,0,0,0.8)` (`:201`); sr-only title (`:206-209`) + visible `h3` (`:249`); claim/dismiss row (`:260-277`); dot divs (`:283-291`); validity line (`:297-306`).
- Behavior: auto-opens on 2s timer (`:127-140`) — **must not** steal focus; announce via polite region or require user trigger (§4 F-03). Pagination dots are `div`s — **must** be buttons.
- Token usage: `bg-bank-green/hover:bg-bank-emerald/text-surface` (`:265`), `border-border hover:bg-muted` (`:273`) — **must** migrate per §6.

### 3.9 Alert banner — `cms/EmergencyAlert.tsx:79-167`

- Anatomy: antd `Alert` list; icon+title (`:126-131`); `line-clamp-2` desc (`:134-136`); 44px dismiss (`:145-153`) ✅.
- Defects: wrapper focusable only when `actionUrl` (`:121`) but `onClick` always (`:160`) — **must** align (focusable ⟺ clickable); WARNING renders info-type `Alert` with warning wrapper (`:100-110,125`) — **must** map severity 1:1; clickable `Alert` wrapping dismiss `Button` nests interactives — **must** stopPropagation on dismiss.
- Missing: loading/empty return `null` (`:96-98`) — **must** stay silent for zero alerts but log errors.

### 3.10 Feedback — `feedback/FeedbackWidget.tsx:53-315`

- Anatomy: FAB 56px (`:170-178`, labeled ✅); `Modal` + category grid `h-24` (`:218-235`); labeled Input/TextArea (`:238-267`); `aria-live` count (`:268-270`) ✅; submit `h-16` gated on non-empty (`:303`) ✅.
- States: success panel (`:202-211`) ✅; fetch failure only `console.error` (`:153-154`) — **must** add user-facing error.
- Defects: custom `useFocusTrap` (`:64`) over antd Modal's own trap — **must** pick one; unselected categories signal by `opacity-60` only (`:228`) — **must** add border/icon non-color cue; FAB `bottom-24` (`:174`) vs MobileNav bar — **must** verify no 360px overlap.
- Token usage: heaviest legacy mix (`:174,204-205,227,248,264,273,288,295,304`) — migrate per §6.

### 3.11 Header atoms — `ThemeToggle.tsx:22-50`, `LanguageSwitcher.tsx:11-14`, `VIPBadge.tsx:33-103`

- ThemeToggle 48px labeled ✅ (`:30-49`); pre-mount skeleton div is inert ✅ (`:22-24`).
- LanguageSwitcher flag emoji (`LanguageSwitcher.tsx:12-13`) **must** be `aria-hidden` with text label announced.
- VIPBadge returns `null` when `!isVIP` (`VIPBadge.tsx:33-35`) — **must** document that absence is the empty state; tier colors use guarded `--tier-*` vars (`globals.css:110-118`) ✅.
- Raw `#ffffff` / `rgba(255,255,255,0.2)` progress colors (`PersonalizedGreeting.tsx:174-175`) **must** become tokens.

### 3.12 Provider — `AntdProvider.tsx:32-50`, `tokens.ts:70-128`

- `App component={false}` keeps DOM clean ✅; Button heights 48/56/44 bake in the touch floor ✅; danger-label black override (`tokens.ts:119-123`) is dark-only — **must** verify light-mode primary label contrast per F-01.

## 4. Accessibility requirements and testable acceptance criteria

Baseline: `.a11yrc.json:1-4` targets **WCAG 2.2 AA**; `.a11y-baseline.json:1-8` has zero tracked violations with WEB-CSP-001/002 as P1 gate. Playwright covers chromium + firefox + webkit + Mobile Chrome (`playwright.config.ts:38-65`); `test:a11y` runs the a11y dir, incl. production-component coverage (`package.json:17`).

| ID | Criterion (testable) | Maps |
|---|---|---|
| F-01 | Small text and button labels **must** measure ≥4.5:1. `#00D09C` at 2.00:1 (`tokens.ts:15,27`) **must not** render as text or under white labels: no `colorPrimary` eyebrow (`BalanceCard.tsx:56`), no `colorPrimary` credit amounts (`TransferActivity.tsx:187-189`), white-on-emerald pills (`BannerCarousel.tsx:149`, `PromoPopup.tsx:246`) **must** use ink-on-tint or darker text token. Test: axe `color-contrast` on every story. | WCAG 1.4.3 |
| F-02 | Every interactive surface **must** show a ≥2px visible focus indicator. QuickAction tiles (`QuickActions.tsx:252`, transition only) and carousel arrows (`BannerCarousel.tsx:167,170`, hover-reveal) fail today. Test: keyboard-walk each component, screenshot `:focus-visible`. | WCAG 2.4.7 / 2.4.13 |
| F-03 | No focus theft, no unfocusable click targets. Promo auto-open (`PromoPopup.tsx:127-140`) **must not** move focus; dots **must** be buttons (`:283-291`); EmergencyAlert focusable ⟺ clickable (`EmergencyAlert.tsx:121,160`). Test: open app idle 5s → `document.activeElement` unchanged; all click handlers reachable by Tab. | WCAG 2.1.1 / 4.1.2 / 3.2.1 |
| F-04 | Every input **must** have a programmatic label. `Typography.Text` pseudo-labels (`BeneficiaryManager.tsx:140,147,154`) **must** become `<label htmlFor>` matching input ids; submit gating (`FeedbackWidget.tsx:303`) **must** pair with exposed error text (today only `console.error :153-154`). Test: axe `label` rule; submit empty → error announced. | WCAG 3.2.2 / 3.3.1 |
| F-05 | Hit areas **must** be ≥24px (AA floor), **should** be ≥44px touch. Header/FAB/CTAs pass (44–56px). Row icon-only buttons (`TransferActivity.tsx:229-233,507-515`; `BeneficiaryManager.tsx:101-110`) pass floor but **should** pad to 44px. Test: measure bounding boxes at 360px + desktop. | WCAG 2.5.8 |
| F-06 | Muted/overlay text **must** pass contrast without opacity stacking. `text-muted-foreground + opacity-60` (`DashboardLayout.tsx:292`; `FeedbackWidget.tsx:196`) and white-on-`bank-green/90` (`BannerCarousel.tsx:149`) fail by construction — **must** use solid AA tokens. Test: axe `color-contrast` in both themes. | WCAG 1.4.3 |
| F-07 | Motion **must** respect users. Autoplay (`BannerCarousel.tsx:63-70`) **must** expose pause on hover/focus + honor `prefers-reduced-motion`; pulse/scale decoration (`FeedbackWidget.tsx:289`; `BannerCarousel.tsx:133`) **must** disable under reduced motion. Test: `prefers-reduced-motion: reduce` → zero autoplay/animation. | WCAG 2.2.2 / 2.3.3 |
| F-08 | Status changes **must** announce. Keep `aria-live` count (`FeedbackWidget.tsx:268-270`) and `role=status` hint (`QuickActions.tsx:156`); auto-dismiss (`FeedbackWidget.tsx:147-152`), cancel toasts (`TransferActivity.tsx:67-74`), silent CMS nulls **should** add polite announcements. Test: SR walk of submit/cancel/dismiss flows. | WCAG 4.1.3 |
| R-06 | Data fetch **must** resolve to content, skeleton, empty, or error — never silent `null` on failure (`BannerCarousel.tsx:96-98`; `EmergencyAlert.tsx:96-98`; `PromoPopup.tsx:187-192`). Test: error stub → visible error + retry. | — |

**WCAG 2.2 AA adopted:** `.a11yrc.json` covers 2.4.11/2.4.13 focus-appearance, 2.5.7 dragging, 2.5.8 target-size via F-02/F-05 above; Playwright runs firefox/webkit + Mobile Chrome (`playwright.config.ts:48-65`) and `test:a11y` covers production components (`package.json:17`, `src/__tests__/a11y/production-components.test.tsx`).

## 5. Content and tone standards with examples

Voice: concise, confident, action-oriented. Sentence case for UI; UPPERCASE only for eyebrow micro-labels. Indonesian default locale (`id`), English mirror — **must** route all strings through next-intl (see §6 A-05 for hardcoded violations).

Ambiguous-label fixes (before → after):

| Before (banned) | After (required) | Why |
|---|---|---|
| `Opsi transaksi` ×N rows (`TransferActivity.tsx:231`) | `Opsi untuk {description}` | Repeated accessible names are indistinguishable |
| `Batalkan transaksi` (`:509`) | `Batalkan {description} ({amount})` | Action needs its object |
| `Kontak 1…5` (`:337`) | `Transfer ke {name}` | Numbered ≠ named |
| `Pilih bulan` with no menu (`:381`) / `Lihat Detail` (`:207`) | Wire the menu, or remove the control | Dead controls lie |
| `Close` / `Close popup` in Indonesian UI (`FeedbackWidget.tsx:191`; `PromoPopup.tsx:218`) | `Tutup` / `Tutup promo` | Locale consistency |
| Logo `aria-label="PayU"` (`DashboardLayout.tsx:251,380`) | `PayU — beranda` | Brand ≠ destination |

**Should**: CTA verbs lead (`Kirim Sekarang`, `Buat dan unduh`); errors state what happened + one next step (`Gagal membatalkan — coba lagi`).

## 6. Anti-patterns and prohibited implementations

- **A-01 raw values / token bypass (High).** ~60 legacy hits (`bg-bank-green`, `bg-muted`, `text-muted-foreground`, `border-border`, `bg-card`) across `BannerCarousel.tsx:91,149,167,170,191`, `PromoPopup.tsx:246,265,273,288`, `FeedbackWidget.tsx:174,190,194,204-205,227-228,248,264,273,288,295,304`, `EmergencyAlert.tsx:107-108,138`, `statement-downloader.tsx:121,131,143,154,163,175,195,217,235-236,258-259,279,290,305-307,313,338,351`. **Must not** add new ones. Migration pairs: `bg-bank-green → Button type=primary` (or `token.colorPrimaryBg` for wells); `bg-bank-green/90 pill → Tag color=success` with ink text; `bg-muted/20 + border-border → Card` default; `text-muted-foreground → Typography.Text type=secondary`; `focus:border-bank-green/50 → antd status/border token`. Mixed `focus:border-primary` + `focus:border-bank-green/50` on one input (`FeedbackWidget.tsx:248,264`) **must** collapse to one token.
- **A-02 inconsistent spacing/radius (Medium).** Radius 16 vs `rounded-3xl` vs `rounded-sm` (`QuickActions.tsx:244,263`; `FeedbackWidget.tsx:190-191`); gutters 24/16 ad hoc vs token 12/16 (`tokens.ts:56-59`). **Must** use §2.4 steps; **do**: `radius 16 card / 12 input`; **don't**: `rounded-[3rem]` customs.
- **A-03 ambiguous labels (High).** §5 table is the fix list; **must** unique-per-context accessible names for repeated rows.
- **A-04 mixed metaphors / dead controls (Medium).** **Don't**: Menu-as-router (`DashboardLayout.tsx:138-176`) — do links; unclickable `Lihat Semua Fitur` (`QuickActions.tsx:185-196`) and month button (`TransferActivity.tsx:378-398`) — wire or delete; embla-shaped `CarouselApi` shim over antd Carousel (`BannerCarousel.tsx:13-19,47-60,184`); double focus-trap (`FeedbackWidget.tsx:64` + Modal `:180`); double `Empty` description (`BeneficiaryManager.tsx:85-86`).
- **A-05 hardcoded locale strings (Medium).** `'Selesai'/'Edit'` (`QuickActions.tsx:145-151`), `'Kirim Cepat'/'Kategori Favorit'` (`TransferActivity.tsx:251-266,331,366`), toast strings (`:70,72`) bypass next-intl while siblings use `t()`. **Must** externalize; flag-emoji locales (`LanguageSwitcher.tsx:12-13`) need `aria-hidden` verification.

## 7. QA checklist (executable in code review)

- [ ] Tokens: zero new `bg-bank-*` / `bg-muted` / `text-muted-foreground` / `border-border` classes; colors resolve to `tokens.ts` seeds.
- [ ] Contrast: body ≥4.5:1, no emerald small text or white-on-emerald labels (F-01); muted text solid, no `opacity-60` stacking (F-06).
- [ ] Focus: Tab-walk shows ≥2px `:focus-visible` on every control, tiles, slides, arrows (F-02); no hover-only affordances.
- [ ] Keyboard: all click targets Tab-reachable; no timer focus theft; dots/buttons real `<button>`s (F-03); arrows work in edit-mode grid.
- [ ] Labels: inputs have `<label htmlFor>` (F-04); repeated rows carry unique names (§5); locale strings via next-intl, no English strays.
- [ ] Targets: icon-only ≥24px, CTAs/nav ≥44px at 360px and desktop (F-05); FAB clears MobileNav; dismiss clears content.
- [ ] Motion: autoplay pausable + reduced-motion off (F-07); announcements polite for submit/cancel/dismiss (F-08).
- [ ] States: loading skeleton, shared empty, and error+retry all render — no silent `null` on failure (R-06).
- [ ] Responsive: beneficiary form stacks `xs=24`; tile/description truncation verified with longest `id` strings; desktop+mobile ledger parity.
- [ ] i18n/theme: `id` + `en` strings present; antd locale synced (`antd-locale.ts:9-20`); light + dark screenshots attached.

---
*Skipped: e2e spec bodies, `__tests__/a11y` assertions, `globals.css` dark block, `lib/a11y.tsx`, stores. Risk: contrast ratios cited from `tokens.ts` comments, not re-measured; dark-mode emerald and antd v6 focus-ring defaults unverified live.*
