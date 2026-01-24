# Accessibility (A11y) Guide for PayU Web App

This guide provides comprehensive accessibility practices and utilities for ensuring the PayU Web App is accessible to all users, including those using assistive technologies.

## Table of Contents

1. [Overview](#overview)
2. [WCAG Compliance](#wcag-compliance)
3. [A11y Utilities](#a11y-utilities)
4. [Component Guidelines](#component-guidelines)
5. [Testing](#testing)
6. [Best Practices](#best-practices)

## Overview

The PayU Web App is designed to meet **WCAG 2.1 Level AA** standards, ensuring accessibility for users with disabilities. This includes:

- Screen reader compatibility (NVDA, JAWS, VoiceOver, TalkBack)
- Keyboard navigation support
- Color contrast compliance (4.5:1 for normal text)
- Semantic HTML structure
- ARIA attributes where needed

## WCAG Compliance

### Perceivable

1. **Text Alternatives**: All images have descriptive `alt` text
2. **Adaptable**: Content can be presented in different ways
3. **Distinguishable**: Color contrast meets AA standards (4.5:1)
4. **Keyboard Accessible**: All functionality available via keyboard

### Operable

1. **Keyboard Accessible**: Full keyboard navigation with visible focus
2. **Enough Time**: No time limits without user control
3. **Seizures**: No flashing content (3 flashes/second)
4. **Navigable**: Easy navigation and skip links

### Understandable

1. **Readable**: Clear and simple language
2. **Predictable**: Consistent navigation and behavior
3. **Input Assistance**: Error identification and correction

### Robust

1. **Compatible**: Works with current and future assistive technologies

## A11y Utilities

The app provides several accessibility utilities in `@/lib/a11y`:

### SkipLink

Add a skip link for keyboard users to jump to main content:

```tsx
import { SkipLink } from '@/lib/a11y';

export default function Layout() {
  return (
    <>
      <SkipLink href="#main-content">Lanjut ke konten utama</SkipLink>
      <main id="main-content">
        {/* Content */}
      </main>
    </>
  );
}
```

### useFocusTrap

Trap focus within modals and dialogs:

```tsx
import { useFocusTrap } from '@/lib/a11y';

function Modal({ isOpen, onClose }) {
  const modalRef = useRef<HTMLDivElement>(null);
  useFocusTrap(isOpen, modalRef);

  return (
    <div ref={modalRef} role="dialog" aria-modal="true">
      {/* Modal content */}
    </div>
  );
}
```

### VisuallyHidden

Hide content visually but keep it accessible to screen readers:

```tsx
import { VisuallyHidden } from '@/lib/a11y';

<button>
  <VisuallyHidden>Tutup dialog</VisuallyHidden>
  <XIcon />
</button>
```

### checkContrast

Verify color contrast meets WCAG AA standards:

```tsx
import { checkContrast } from '@/lib/a11y';

if (checkContrast('#10b981', '#ffffff')) {
  // Contrast is good (>= 4.5:1)
}
```

## Component Guidelines

### Buttons

Always provide accessible labels:

```tsx
// Good
<button aria-label="Tutup dialog" onClick={onClose}>
  <XIcon />
</button>

// Better
<button onClick={onClose}>
  <XIcon />
  <span className="sr-only">Tutup</span>
</button>

// Best
<button onClick={onClose}>
  Tutup
  <XIcon aria-hidden="true" />
</button>
```

### Forms

Always associate labels with inputs:

```tsx
<div>
  <label htmlFor="email">Email</label>
  <input
    id="email"
    type="email"
    required
    aria-describedby="email-hint"
  />
  <span id="email-hint" className="text-sm">
    Masukkan email Anda
  </span>
</div>
```

### Links

Provide descriptive link text:

```tsx
// Good
<a href="/transfer" aria-label="Transfer uang ke akun lain">
  Transfer
</a>

// Avoid
<a href="/transfer">Klik di sini</a>
```

### Modals

Implement proper modal accessibility:

```tsx
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="modal-title"
  aria-describedby="modal-description"
>
  <h2 id="modal-title">Konfirmasi Transfer</h2>
  <p id="modal-description">
    Apakah Anda yakin ingin mengirim uang?
  </p>
</div>
```

### Progress Bars

Include proper ARIA attributes:

```tsx
<div
  role="progressbar"
  aria-valuenow={75}
  aria-valuemin={0}
  aria-valuemax={100}
  aria-label="Anggaran terpakai 75%"
>
  <div style={{ width: '75%' }} />
</div>
```

## Testing

### Automated Testing

Use jest-axe for automated accessibility testing:

```tsx
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

it('should have no accessibility violations', async () => {
  const { container } = render(<MyComponent />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

### Manual Testing Checklist

- [ ] Test with NVDA screen reader (Windows)
- [ ] Test with VoiceOver (macOS/iOS)
- [ ] Test with TalkBack (Android)
- [ ] Navigate using Tab key only
- [ ] Verify all interactive elements have visible focus
- [ ] Check color contrast using axe DevTools
- [ ] Verify all images have alt text
- [ ] Test with screen magnification (200% zoom)

### Keyboard Navigation

Ensure all functionality is accessible via keyboard:

| Key | Action |
|-----|--------|
| `Tab` | Move focus to next element |
| `Shift + Tab` | Move focus to previous element |
| `Enter` | Activate button/link |
| `Space` | Toggle checkbox/radio |
| `Escape` | Close modal/dropdown |
| `Arrow Keys` | Navigate within components |

## Best Practices

### 1. Semantic HTML

Use proper HTML elements:

```tsx
// Good
<nav>
  <ul>
    <li><a href="/">Home</a></li>
  </ul>
</nav>

// Avoid
<div className="nav">
  <div className="nav-item" onClick={() => navigate('/')}>Home</div>
</div>
```

### 2. ARIA Attributes

Use ARIA only when needed:

```tsx
// When HTML is sufficient
<button>Close</button>

// When ARIA is needed
<button aria-label="Close modal" onClick={onClose}>
  <XIcon />
</button>
```

### 3. Focus Management

Maintain logical focus order:

```tsx
// Restore focus after closing modal
const previousFocus = useRef<HTMLElement>(null);

const openModal = () => {
  previousFocus.current = document.activeElement as HTMLElement;
  // Open modal and focus first element
};

const closeModal = () => {
  // Close modal
  previousFocus.current?.focus(); // Restore focus
};
```

### 4. Color Contrast

Always meet WCAG AA standards:

- Normal text (< 18pt): 4.5:1 minimum
- Large text (≥ 18pt): 3:1 minimum
- UI components: 3:1 minimum

### 5. Error Messages

Provide accessible error feedback:

```tsx
<div role="alert" aria-live="assertive">
  <p>Transfer gagal: Saldo tidak mencukupi</p>
</div>
```

### 6. Loading States

Announce loading to screen readers:

```tsx
<div role="status" aria-live="polite">
  <p>Memproses transfer...</p>
</div>
```

## Color Palette (WCAG AA Compliant)

Our Premium Emerald color palette is designed for accessibility:

### Text Colors

| Usage | Color | Contrast Ratio |
|-------|-------|----------------|
| Primary text | `#0a0a0a` | 21:1 (AAA) |
| Secondary text | `#525252` | 7.5:1 (AAA) |
| Muted text | `#a3a3a3` | 4.5:1 (AA) |

### Background Colors

| Usage | Color | Notes |
|-------|-------|-------|
| Primary background | `#ffffff` | Pure white |
| Secondary background | `#fafafa` | Off-white |
| Accent | `#10b981` | Meets AA |

### Interactive Elements

All interactive elements meet contrast requirements:
- Buttons: 4.5:1 minimum
- Links: 4.5:1 minimum
- Focus indicators: 3:1 minimum against background

## Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [A11y Project Checklist](https://www.a11yproject.com/checklist/)
- [axe DevTools](https://www.deque.com/axe/devtools/)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- [NVDA Screen Reader](https://www.nvaccess.org/)
- [VoiceOver Shortcut Guide](https://www.apple.com/accessibility/voiceover/)

## Running Accessibility Tests

### Automated Tests

```bash
# Run all accessibility tests
npm run test -- a11y

# Run with coverage
npm run test:coverage

# Run specific test file
npm run test -- FinancialHealthScore.test.tsx
```

### Manual Audit

```bash
# Run accessibility audit script
npm run a11y:audit
```

### Playwright E2E Tests

```bash
# Run E2E accessibility tests
npm run test:e2e
```

## Continuous Improvement

Accessibility is an ongoing process. Regular audits and testing ensure we maintain high standards:

1. **Daily**: Automated tests in CI/CD pipeline
2. **Weekly**: Manual keyboard navigation testing
3. **Monthly**: Screen reader testing
4. **Quarterly**: Full accessibility audit with users

## Support

For accessibility questions or issues:
- Check the [WAI-ARIA Authoring Practices](https://www.w3.org/WAI/ARIA/apg/)
- Review component examples in `src/components/dashboard/`
- Consult with the accessibility team

---

*Remember: Accessible design is good design for everyone!*
