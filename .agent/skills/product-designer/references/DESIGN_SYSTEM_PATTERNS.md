# Product Design & Design System Patterns

## 🎨 Aesthetic Principles (Premium Emerald)
*   **Trust & Professionalism**: Favor standard international banking aesthetics (Clean, White/Dark, Sans-serif, Glassmorphism). 
    *   **Primary Color**: Emerald Green (#10b981).
    *   **Background**: Slate/Gray-950 for dark mode.
*   **Cultural Subtlety**: Avoid over-utilizing cultural themes (Wayang, Philosophy) in a Fintech UI as it can clash with "Premium" quality expectations. Use them as subtle background accents or stylized SVG icons.
*   **Rich Micro-animations**: Use Framer Motion or CSS transitions for:
    *   Smooth button hover states.
    *   Drawer/Modal easing.
    *   Number counting animations for balances.

## 📱 Responsive & Component Design
*   **The Golden Ratio (Cards)**: Use `aspect-[1.586]` for Credit/Debit card components to match ISO/IEC 7810 standards.
*   **Fluid Scaling**: Use `vw` (viewport width) combined with `max-w` (max width) for core UI elements to ensure they look premium on both mobile and desktop.
    *   Example: `w-[85vw] max-w-[340px]`.
*   **Mobile Stacking**: Force `flex-col` for complex layouts on mobile. Add generous vertical padding (`py-16+`) to avoid occlusion by fixed navigation bars.

## ♿ Accessibility (A11y)
*   **Contrast Ratios**: Ensure minimum 4.5:1 contrast for normal text. Use `hsl(var(--muted-foreground))` carefully.
*   **Interactive Elements**: 
    *   Large touch targets (min 44x44px for mobile).
    *   Visible focus rings for keyboard navigation.
*   **Semantic HTML**: Use `<main>`, `<nav>`, `<header>`, and proper heading hierarchy (`h1` -> `h6`).

## 🧩 Atomic Design System
*   **Atoms**: Standardized buttons, inputs, badges.
*   **Molecules**: Search bars, Card headers, Form fields with labels.
*   **Organisms**: Navbar, Transaction lists, Wallet cards.
*   **Templates/Pages**: Dashboard layout, Transfer flow.
