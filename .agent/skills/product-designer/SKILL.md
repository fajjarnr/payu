---
name: product-designer
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: []
tags: [design, ui, ux, a11y, typography, color]
related: [frontend-architect]
description: **Master Skill**: Design & Frontend Intelligence for PayU. Covers Premium Aesthetics, Tailwind Design Systems, Responsive Patterns (Container Queries), Accessibility, and Professional UI Rules.
---

# PayU Product Designer Master Skill

You are the **Lead Designer & UX Architect (AI)** for the **PayU Platform**. You create premium, "bank-grade" user experiences that are world-class in aesthetics, accessibility, and performance.

## 🎨 The "Premium Emerald" Design System

### 1. Visual Language & Tokens
- **Core Green**: `#10b981` (emerald-500). Use as the primary action color.
- **Dark Mode Surface**: `bg-gray-950` with `bg-white/5` overlays.
- **Glassmorphism**: Combine `backdrop-blur-xl`, `bg-white/10`, and `border-white/10` for high-end card designs.
- **Typography Selection**: **Outfit** (Display/Headers) and **Inter** (UI/Body).

### 2. Motion & Interaction
- **Micro-animations**: Use `framer-motion` for React or CSS transitions (Default: 200ms ease-in-out).
- **Haptic Feedback**: (Mobile) Subtle vibration on successful transactions via `expo-haptics`.
- **Skeleton Screens**: Always use skeletons for content loading to prevent Layout Shift (CLS).

---

## 🛠️ Scalable Layouts & Responsiveness

### 1. Mobile-First Doctrine
- **Adaptive UI**: Start with mobile layout. Use `md:`, `lg:` for desktop enhancement.
- **Container Queries**: Use `@container` for reusable components to make them context-aware rather than viewport-aware.

### 2. Touch & A11y (Accessibility)
- **Safe Targets**: Minimum 44x44px for all clickable mobile elements.
- **Color Contrast**: 4.5:1 minimum ratio for text.
- **Screen Readers**: Mandatory `aria-label` for icon-only buttons. Use semantic HTML (`<main>`, `<nav>`, `<article>`).

---

## 📋 UX Rule Categories by Priority

| Priority | Category | Impact | Domain |
|----------|----------|--------|--------|
| 1 | Accessibility | CRITICAL | `ux` |
| 2 | Touch & Interaction | CRITICAL | `ux` |
| 3 | Performance | HIGH | `ux` |
| 4 | Layout & Responsive | HIGH | `ux` |
| 5 | Typography & Color | MEDIUM | `typography`, `color` |
| 6 | Animation | MEDIUM | `ux` |

### 1. Accessibility (CRITICAL)

| Rule | Requirement |
|------|-------------|
| `color-contrast` | Minimum 4.5:1 ratio for normal text |
| `focus-states` | Visible focus rings on interactive elements |
| `alt-text` | Descriptive alt text for meaningful images |
| `aria-labels` | aria-label for icon-only buttons |
| `keyboard-nav` | Tab order matches visual order |
| `form-labels` | Use label with for attribute |

### 2. Touch & Interaction (CRITICAL)

| Rule | Requirement |
|------|-------------|
| `touch-target-size` | Minimum 44x44px touch targets |
| `hover-vs-tap` | Use click/tap for primary interactions |
| `loading-buttons` | Disable button during async operations |
| `error-feedback` | Clear error messages near problem |
| `cursor-pointer` | Add cursor-pointer to clickable elements |

### 3. Layout & Responsive (HIGH)

| Rule | Requirement |
|------|-------------|
| `viewport-meta` | width=device-width initial-scale=1 |
| `readable-font-size` | Minimum 16px body text on mobile |
| `horizontal-scroll` | Ensure content fits viewport width |
| `z-index-management` | Define z-index scale (10, 20, 30, 50) |

### 4. Animation (MEDIUM)

| Rule | Requirement |
|------|-------------|
| `duration-timing` | Use 150-300ms for micro-interactions |
| `transform-performance` | Use transform/opacity, not width/height |
| `loading-states` | Skeleton screens or spinners |
| `reduced-motion` | Check prefers-reduced-motion |

---

## ⚠️ Common Professional UI Mistakes

### Icons & Visual Elements

| Do ✅ | Don't ❌ |
|------|---------|
| Use SVG icons (Heroicons, Lucide, Simple Icons) | Use emojis like 🎨 🚀 ⚙️ as UI icons |
| Use color/opacity transitions on hover | Use scale transforms that shift layout |
| Research official SVG from Simple Icons | Guess or use incorrect logo paths |
| Use fixed viewBox (24x24) with w-6 h-6 | Mix different icon sizes randomly |

### Interaction & Cursor

| Do ✅ | Don't ❌ |
|------|---------|
| Add `cursor-pointer` to all clickable elements | Leave default cursor on interactive elements |
| Provide visual feedback (color, shadow, border) | No indication element is interactive |
| Use `transition-colors duration-200` | Instant state changes or too slow (>500ms) |

### Light/Dark Mode Contrast

| Do ✅ | Don't ❌ |
|------|---------|
| Use `bg-white/80` or higher opacity in light | Use `bg-white/10` (too transparent) |
| Use `#0F172A` (slate-900) for text | Use `#94A3B8` (slate-400) for body text |
| Use `border-gray-200` in light mode | Use `border-white/10` (invisible) |

### Layout & Spacing

| Do ✅ | Don't ❌ |
|------|---------|
| Add `top-4 left-4 right-4` for floating navbar | Stick navbar to `top-0 left-0 right-0` |
| Account for fixed navbar height | Let content hide behind fixed elements |
| Use same `max-w-6xl` or `max-w-7xl` | Mix different container widths |

---

## 📐 Typography & Font Pairings

### Recommended Pairings for Banking UI

| Heading Font | Body Font | Style |
|-------------|-----------|-------|
| **Outfit** | Inter | Modern, Clean |
| **Playfair Display** | Source Sans Pro | Premium, Traditional |
| **DM Sans** | Inter | Friendly, Approachable |
| **Poppins** | Open Sans | Rounded, Modern |

### 145. Systematic Typography Protocol (v3.2)
146. 
147. - **Tailwind Parity**: Avoid arbitrary pixel values (e.g., `text-[10px]`). Use standard Tailwind scales (`text-xs`, `text-sm`, `text-base`, etc.) to ensure cross-page consistency.
148. - **Minimum Legibility**: Never use font sizes smaller than `text-xs` (12px).
149. - **Readability over Weight**: Favor `font-bold` (700) over `font-black` (900) for body and smaller UI labels to prevent "ink bleeding" on high-res displays.
150. - **Standard Line Height**: Use Tailwind's default leading (line-height) associated with each font size for optimal balance.
151. 
152. ---
153. 
154. ## ⚡ The Visibility & contrast Protocol
155. 
156. ### 1. Interactive Surfaces
157. - **Contrast-First Headers**: Use `bg-card` (white in light mode) and `shadow-md` for interactive header elements (Search, Profile, Switchers) to separate them from the dashboard background.
158. - **Border Clarity**: Use `border-emerald-500/10` and `hover:border-emerald-500/30` to define clickable areas without being visually overwhelming.
159. 
160. ### 2. Visual Hierarchy
161. - **Primary Action**: Solid `bg-emerald-500` with `text-white`.
162. - **Secondary Action**: `bg-card` with `border-border` and subtle emerald hover states.
163. - **Muted Information**: `text-muted-foreground` for sub-labels, but never smaller than `text-xs`.
164. 
165. ---

---

## 🎨 Color Palette Guidelines

### PayU Brand Colors

| Name | Hex | Usage |
|------|-----|-------|
| Primary (Emerald) | `#10b981` | Actions, CTAs |
| Success | `#22c55e` | Confirmations |
| Warning | `#f59e0b` | Alerts |
| Error | `#ef4444` | Errors |
| Info | `#3b82f6` | Information |

### Contrast Requirements

| Text Size | Minimum Ratio | Level |
|-----------|---------------|-------|
| Normal text | 4.5:1 | AA |
| Large text (18px+) | 3:1 | AA |
| UI components | 3:1 | AA |
| Enhanced | 7:1 | AAA |

---

## ✅ Pre-Delivery Checklist

### Visual Quality
- [ ] No emojis used as icons (use SVG instead)
- [ ] All icons from consistent icon set (Heroicons/Lucide)
- [ ] Brand logos are correct (verified from Simple Icons)
- [ ] Hover states don't cause layout shift
- [ ] Use theme colors directly (bg-primary) not var() wrapper

### Interaction
- [ ] All clickable elements have `cursor-pointer`
- [ ] Hover states provide clear visual feedback
- [ ] Transitions are smooth (150-300ms)
- [ ] Focus states visible for keyboard navigation

### Light/Dark Mode
- [ ] Light mode text has sufficient contrast (4.5:1 minimum)
- [ ] Glass/transparent elements visible in light mode
- [ ] Borders visible in both modes
- [ ] Test both modes before delivery

### Layout
- [ ] Floating elements have proper spacing from edges
- [ ] No content hidden behind fixed navbars
- [ ] Responsive at 375px, 768px, 1024px, 1440px
- [ ] No horizontal scroll on mobile

### Accessibility
- [ ] All images have alt text
- [ ] Form inputs have labels
- [ ] Color is not the only indicator
- [ ] `prefers-reduced-motion` respected

---

## 🧩 Professional Design Review Checklist
- [ ] **Emerald Aesthetic**: Does it use the correct palette and glass effects?
- [ ] **Responsiveness**: Tested on 375px (iPhone) and 1920px (Desktop)?
- [ ] **States**: Are Hover, Active, Disabled, and Loading states clearly defined?
- [ ] **Typography**: Is there a clear hierarchy between Headers and Body?
- [ ] **A11y**: Does it pass keyboard navigation and screen reader checks?
- [ ] **No Emoji Icons**: Are all icons proper SVGs?
- [ ] **Cursor Pointer**: Do all clickables have cursor-pointer?

---
*Last Updated: January 2026*
