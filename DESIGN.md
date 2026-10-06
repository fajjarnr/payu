# DESIGN.md — PayU Web App

<!-- impeccable:design-schema 1 -->

## Platform

Adaptive (Next.js, responsive web — mobile-first breakpoints)

## Product Summary

PayU adalah platform digital banking untuk Indonesia. Target user: Millennials & Gen Z (18–40 tahun), mobile-native, mau simpel dan cepat. Web app adalah extension dari mobile app untuk account management dan transactions.

## Design Principles

1. **Mobile-first, always** — Design untuk screen terkecil dulu, web adalah extension
2. **Speed as a feature** — <2s launch, <3s transaction, minimal steps
3. **Trust through security** — Biometric auth, transparent security, no dark patterns
4. **Financial clarity** — User selalu tahu balance, spending, goals
5. **Indonesian identity** — Bahasa Indonesia, lokal payment methods (QRIS, BI-FAST)

## Visual Identity

### Color System

| Token | Value | Usage |
|-------|-------|-------|
| `primary` | `#00D09C` | Brand color — buttons, links, active states |
| `primary-dark` | `#00A87E` | Hover/active state for primary |
| `primary-light` | `#E6FFF7` | Subtle backgrounds, highlights |
| `secondary` | `#1A1A2E` | Dark navy — headings, dark surfaces |
| `accent` | `#FF6B35` | Accent — notifications, badges, CTAs |
| `success` | `#00D09C` | Success states (same as primary) |
| `warning` | `#FFB800` | Warning states |
| `error` | `#FF4757` | Error states, destructive actions |
| `surface` | `#FFFFFF` | Card backgrounds, modals |
| `surface-dim` | `#F8F9FA` | Page background, subtle sections |
| `text-primary` | `#1A1A2E` | Body text, headings |
| `text-secondary` | `#6B7280` | Captions, metadata, placeholders |
| `text-disabled` | `#B0B8C4` | Disabled states |
| `border` | `#E5E7EB` | Dividers, card borders |

### Typography

| Role | Size | Weight | Line Height | Usage |
|------|------|--------|-------------|-------|
| `display` | 32px | 700 | 1.2 | Hero numbers, balance display |
| `h1` | 24px | 700 | 1.3 | Page titles |
| `h2` | 20px | 600 | 1.3 | Section headings |
| `h3` | 16px | 600 | 1.4 | Card titles, subheadings |
| `body` | 14px | 400 | 1.5 | Body text, descriptions |
| `body-strong` | 14px | 600 | 1.5 | Emphasized body text |
| `caption` | 12px | 400 | 1.4 | Metadata, timestamps |
| `label` | 12px | 500 | 1.3 | Form labels, badges |

**Font Stack**: Inter (primary), system-ui fallback. Load via `next/font/google`.

### Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `space-1` | 4px | Tight gaps, icon-to-text |
| `space-2` | 8px | Inline gaps, list items |
| `space-3` | 12px | Card internal padding |
| `space-4` | 16px | Section gaps, form fields |
| `space-5` | 24px | Card padding, major sections |
| `space-6` | 32px | Page sections |
| `space-7` | 48px | Hero sections, major breaks |
| `space-8` | 64px | Page top/bottom padding |

### Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `radius-sm` | 6px | Small elements, badges, inputs |
| `radius-md` | 12px | Cards, buttons, modals |
| `radius-lg` | 16px | Large cards, bottom sheets |
| `radius-full` | 9999px | Pills, avatars, circular buttons |

### Elevation (Shadows)

| Token | Value | Usage |
|-------|-------|-------|
| `shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift, inputs |
| `shadow-md` | `0 4px 12px rgba(0,0,0,0.08)` | Cards, dropdowns |
| `shadow-lg` | `0 8px 24px rgba(0,0,0,0.12)` | Modals, bottom sheets |
| `shadow-xl` | `0 16px 48px rgba(0,0,0,0.16)` | Dialogs, popovers |

## Layout

### Breakpoints

| Name | Width | Usage |
|------|-------|-------|
| `mobile` | 0–639px | Base styles, single column |
| `tablet` | 640–1023px | Two-column layouts, sidebar |
| `desktop` | 1024px+ | Full layout, max-width containers |

### Grid

- **Max content width**: 1200px (desktop)
- **Gutter**: 16px (mobile), 24px (tablet+)
- **Columns**: 12-column grid on desktop, 4-column on mobile

### Navigation

- **Mobile**: Bottom tab bar (5 tabs max) — Home, Transfer, QRIS, Cards, Profile
- **Desktop**: Left sidebar with same sections + expanded sub-nav
- **Top bar**: Logo, search, notifications, avatar

## Component Patterns

### Buttons

| Variant | Style | Usage |
|---------|-------|-------|
| `primary` | Filled `primary` color, white text | Main CTAs, form submits |
| `secondary` | Outlined, `primary` border & text | Secondary actions |
| `ghost` | No border, `primary` text | Tertiary actions, links |
| `danger` | Filled `error` color | Destructive actions |

**Sizes**: `sm` (32px), `md` (40px), `lg` (48px)
**States**: default, hover, active, disabled, loading

### Cards

- Background: `surface`
- Border: 1px `border`
- Radius: `radius-md`
- Padding: `space-5`
- Shadow: `shadow-md`
- Hover: subtle lift (translateY(-2px) + `shadow-lg`)

### Forms

- **Inputs**: 44px height, `radius-sm`, 1px `border`, focus ring `primary`
- **Labels**: Above input, `label` size, `text-secondary`
- **Helper text**: Below input, `caption` size
- **Error text**: `error` color, `caption` size, icon + message
- **Validation**: Inline, on blur (not on every keystroke)

### Lists & Rows

- **Row height**: 56px minimum
- **Divider**: 1px `border` between rows
- **Hover**: `surface-dim` background
- **Active**: `primary-light` background + left border `primary`

### Modals & Sheets

- **Modal**: Centered, `radius-lg`, `shadow-xl`, backdrop blur
- **Bottom sheet**: Mobile only, slides up, `radius-lg` top corners
- **Close**: X button top-right, Esc to dismiss, backdrop click to close

### Empty States

- Icon or illustration (64px, `primary-light` color)
- Title (`h3`) + description (`body`, `text-secondary`)
- CTA button if applicable

### Loading States

- **Skeleton**: `surface-dim` background, pulse animation
- **Spinner**: `primary` color, 24px
- **Button loading**: Spinner replaces text, button disabled

## Interaction Patterns

### Transitions

- **Duration**: 150ms (micro), 250ms (standard), 350ms (complex)
- **Easing**: `cubic-bezier(0.4, 0, 0.2, 1)` (standard)
- **Properties**: opacity, transform, background-color, box-shadow

### Gestures (Mobile)

- **Swipe left on row**: Reveal actions (edit, delete)
- **Pull down**: Refresh
- **Long press**: Context menu (future)

### Feedback

- **Success**: Toast (bottom, auto-dismiss 3s) + haptic
- **Error**: Inline message + toast for critical errors
- **Loading**: Skeleton screens for pages, spinners for actions

## Accessibility

- **WCAG 2.1 AA** compliance required
- **Color contrast**: 4.5:1 minimum for text, 3:1 for large text
- **Touch targets**: 44×44px minimum
- **Focus indicators**: 2px `primary` outline, 2px offset
- **Screen reader**: Semantic HTML, ARIA labels, live regions for dynamic content
- **Keyboard**: All interactive elements focusable, logical tab order
- **Reduced motion**: Respect `prefers-reduced-motion`, disable animations

## Page Inventory

| Page | Route | Description |
|------|-------|-------------|
| Home/Dashboard | `/` | Balance overview, quick actions, recent transactions |
| Transfer | `/transfer` | Internal, BI-FAST, SKN/RTGS transfers |
| QRIS | `/qris` | Pay, receive, dynamic QR |
| Cards | `/cards` | Virtual debit cards, freeze/unfreeze, limits |
| Payments | `/payments` | Bill payment, pulsa, e-wallet top-up |
| Investments | `/investments` | Deposito, reksadana, emas |
| Profile | `/profile` | Account settings, security, preferences |
| Transaction History | `/transactions` | Full history with filters |
| Onboarding | `/onboarding` | eKYC flow (KTP, liveness, Dukcapil) |

## Design Tokens (CSS Custom Properties)

```css
:root {
  /* Colors */
  --color-primary: #00D09C;
  --color-primary-dark: #00A87E;
  --color-primary-light: #E6FFF7;
  --color-secondary: #1A1A2E;
  --color-accent: #FF6B35;
  --color-success: #00D09C;
  --color-warning: #FFB800;
  --color-error: #FF4757;
  --color-surface: #FFFFFF;
  --color-surface-dim: #F8F9FA;
  --color-text-primary: #1A1A2E;
  --color-text-secondary: #6B7280;
  --color-text-disabled: #B0B8C4;
  --color-border: #E5E7EB;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 24px;
  --space-6: 32px;
  --space-7: 48px;
  --space-8: 64px;

  /* Radius */
  --radius-sm: 6px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-full: 9999px;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.08);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.12);
  --shadow-xl: 0 16px 48px rgba(0,0,0,0.16);

  /* Transitions */
  --duration-fast: 150ms;
  --duration-normal: 250ms;
  --duration-slow: 350ms;
  --easing: cubic-bezier(0.4, 0, 0.2, 1);
}
```

## Dark Mode (Future)

Not in scope for v1. Design tokens use CSS custom properties so dark mode can be added later by overriding token values under `@media (prefers-color-scheme: dark)` or a `.dark` class.

---

**Version**: 1.0.0
**Last Updated**: 2026-10-06
**Maintainer**: PayU Design Team
