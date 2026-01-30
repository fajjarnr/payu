# PayU Mobile App Accessibility Guide

> Comprehensive accessibility guidelines and testing procedures for the PayU Digital Banking mobile application.

---

## Table of Contents

1. [Overview](#overview)
2. [WCAG 2.1 Compliance](#wcag-21-compliance)
3. [Component Guidelines](#component-guidelines)
4. [Testing Procedures](#testing-procedures)
5. [Screen Reader Support](#screen-reader-support)
6. [Checklists](#checklists)
7. [Resources](#resources)

---

## Overview

PayU Mobile App is committed to providing an accessible banking experience for all users, including those using assistive technologies such as screen readers (VoiceOver on iOS, TalkBack on Android).

### Target Compliance Levels

| Level | Status | Description |
|-------|--------|-------------|
| **WCAG 2.1 Level A** | Required | Minimum accessibility requirements |
| **WCAG 2.1 Level AA** | Required | Industry standard for web/mobile apps |
| **WCAG 2.1 Level AAA** | Recommended | Enhanced accessibility where possible |

### Supported Assistive Technologies

- **iOS**: VoiceOver, Switch Control, Full Keyboard Access
- **Android**: TalkBack, Switch Access, Voice Access
- **Both**: Dynamic Type/Text Scaling, Reduce Motion, High Contrast

---

## WCAG 2.1 Compliance

### Level A Requirements

#### 1.1.1 Non-text Content (A)
- All images must have descriptive `accessibilityLabel`
- Icons must have meaningful labels (not just "icon")
- Decorative elements must be marked `accessible={false}`

```tsx
// Good
<Image
  source={require('./assets/transfer-icon.png')}
  accessibilityLabel="Transfer money"
/>

// Bad - missing label
<Image source={require('./assets/transfer-icon.png')} />
```

#### 1.3.1 Info and Relationships (A)
- Use semantic roles (`button`, `header`, `link`)
- Associate labels with inputs
- Group related elements

```tsx
// Good
<AccessibleInput
  label="Account Number"
  accessibilityLabel="Account Number, required"
/>
```

#### 2.1.1 Keyboard (A)
- All interactive elements must be reachable via keyboard
- Touch targets minimum 44x44 points
- Logical tab order

#### 2.4.4 Link Purpose (A)
- Link/button labels must describe the action
- Avoid "Click Here" or "Read More"

```tsx
// Good
<AccessibleButton
  label="Transfer to Savings Account"
  onPress={handleTransfer}
/>

// Bad
<AccessibleButton label="Click Here" onPress={handleTransfer} />
```

### Level AA Requirements

#### 1.4.3 Contrast (Minimum) (AA)
- Normal text: 4.5:1 contrast ratio
- Large text (18pt+ or 14pt+ bold): 3:1 contrast ratio

| Element | Foreground | Background | Ratio |
|---------|------------|------------|-------|
| Primary Button | #FFFFFF | #10B981 | 3.5:1 |
| Body Text | #FFFFFF | #111827 | 16.1:1 |
| Error Text | #EF4444 | #111827 | 5.3:1 |

#### 1.4.4 Resize Text (AA)
- Support text scaling up to 200%
- Use relative units (sp for text)
- Layout must adapt to larger text

#### 1.4.11 Non-text Contrast (AA)
- UI components: 3:1 contrast ratio
- Focus indicators: 3:1 contrast ratio

#### 2.4.7 Focus Visible (AA)
- Focus indicators must be visible
- Minimum 2px outline or background change

### Level AAA Requirements (Recommended)

#### 1.4.6 Contrast (Enhanced) (AAA)
- Normal text: 7:1 contrast ratio
- Large text: 4.5:1 contrast ratio

#### 2.1.3 Keyboard (No Exception) (AAA)
- All functionality available via keyboard

---

## Component Guidelines

### AccessibleButton

Standard accessible button component with built-in touch target validation.

```tsx
import { AccessibleButton } from '@/src/components/ui/AccessibleButton';

<AccessibleButton
  label="Transfer"
  hint="Opens transfer screen"
  variant="primary"
  size="md"
  onPress={handleTransfer}
  accessibilityRole="button"
/>
```

**Props:**

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `label` | string | Yes | Button text and screen reader label |
| `hint` | string | No | Describes action result |
| `variant` | string | No | `primary`, `secondary`, `tertiary`, `danger`, `ghost` |
| `size` | string | No | `sm`, `md`, `lg` |
| `loading` | boolean | No | Shows loading state |
| `loadingLabel` | string | No | Label announced during loading |

**Accessibility Features:**
- Minimum 44x44 touch target (enforced)
- Proper `accessibilityRole="button"`
- Loading state announcements
- Disabled state handling

### AccessibleInput

Accessible text input with label association and error announcements.

```tsx
import { AccessibleInput } from '@/src/components/ui/AccessibleInput';

<AccessibleInput
  label="Account Number"
  placeholder="Enter 10-digit account number"
  required
  error={errors.accountNumber}
  helperText="Found on your debit card"
  onChangeText={setAccountNumber}
/>
```

**Props:**

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `label` | string | Yes | Input label |
| `required` | boolean | No | Marks field as required |
| `error` | string | No | Error message (announced to screen reader) |
| `helperText` | string | No | Helper text below input |
| `secure` | boolean | No | Password input with visibility toggle |

**Accessibility Features:**
- Label associated with input
- Error announcements (`accessibilityLiveRegion="assertive"`)
- Required field indication
- Helper text for context

### Custom Hooks

#### useAccessibility

Main hook combining all accessibility features.

```tsx
import { useAccessibility } from '@/src/hooks/useAccessibility';

function MyComponent() {
  const {
    screenReader,
    announce,
    focus,
    preferences,
    form,
    isScreenReaderEnabled,
    reduceMotionEnabled,
  } = useAccessibility();

  // Check if screen reader is active
  if (screenReader.isEnabled) {
    // Provide enhanced accessibility
  }

  // Announce important update
  announce.announceAssertive('Payment successful');

  // Set focus to element
  focus.setFocus(myRef);

  // Respect user preferences
  if (!reduceMotionEnabled) {
    // Play animation
  }
}
```

#### useScreenReader

Detect and monitor screen reader status.

```tsx
import { useScreenReader } from '@/src/hooks/useAccessibility';

function MyComponent() {
  const { isEnabled, isActive, refresh } = useScreenReader();

  // Refresh status when needed
  useEffect(() => {
    const interval = setInterval(refresh, 5000);
    return () => clearInterval(interval);
  }, [refresh]);
}
```

#### useAccessibilityAnnounce

Announce messages to screen readers.

```tsx
import { useAccessibilityAnnounce } from '@/src/hooks/useAccessibility';

function TransferScreen() {
  const { announcePolite, announceAssertive, announceCurrency } = useAccessibilityAnnounce();

  const handleTransfer = async () => {
    const result = await transfer(amount);

    if (result.success) {
      announceAssertive('Transfer completed successfully');
      announceCurrency(result.newBalance);
    } else {
      announceAssertive('Transfer failed. Please try again.');
    }
  };
}
```

---

## Testing Procedures

### Automated Testing

Run the accessibility test suite:

```bash
# Run all accessibility tests
npm test -- src/testing/accessibility.test.tsx

# Run with coverage
npm test -- src/testing/accessibility.test.tsx --coverage
```

### Manual Testing Checklist

#### Screen Reader Testing (iOS - VoiceOver)

1. Enable VoiceOver: Settings > Accessibility > VoiceOver
2. Navigate with swipe gestures (right/left)
3. Activate with double-tap
4. Use rotor to navigate by elements

**Test Scenarios:**
- [ ] All buttons have descriptive labels
- [ ] Input fields announce labels and hints
- [ ] Error messages are announced immediately
- [ ] Loading states are announced
- [ ] Balance updates are announced
- [ ] Transaction confirmations are announced

#### Screen Reader Testing (Android - TalkBack)

1. Enable TalkBack: Settings > Accessibility > TalkBack
2. Navigate with swipe gestures
3. Activate with double-tap
4. Use local context menu

**Test Scenarios:**
- Same as iOS, plus:
- [ ] Touch exploration works correctly
- [ ] Focus order is logical
- [ ] Custom actions are accessible

#### Touch Target Testing

Use Xcode Accessibility Inspector or Android Layout Inspector:

- [ ] All buttons minimum 44x44 pt
- [ ] All inputs minimum 44x44 pt
- [ ] All list items minimum 44x44 pt
- [ ] Sufficient spacing between targets (minimum 8pt)

#### Contrast Testing

Use contrast checker tools:

- [ ] Normal text (4.5:1 minimum)
- [ ] Large text (3:1 minimum)
- [ ] UI components (3:1 minimum)
- [ ] Focus indicators (3:1 minimum)

### Testing Utilities

```tsx
import {
  validateTouchTarget,
  validateContrast,
  validateA11yLabels,
} from '@/src/utils/accessibility';

// Validate touch target
test('button has minimum touch target', () => {
  const result = validateTouchTarget(48, 48);
  expect(result.isValid).toBe(true);
});

// Validate contrast
test('text meets contrast requirements', () => {
  const result = validateContrast('#ffffff', '#111827', false);
  expect(result.isValid).toBe(true);
  expect(result.level).toBe('AAA');
});

// Validate labels
test('all interactive elements have labels', () => {
  const elements = [
    { label: 'Transfer', role: 'button' },
    { label: 'Account', role: 'text' },
  ];
  const result = validateA11yLabels(elements);
  expect(result.isValid).toBe(true);
});
```

---

## Screen Reader Support

### iOS VoiceOver

**Gestures:**
- Swipe right/left: Navigate to next/previous element
- Double-tap: Activate element
- Three-finger swipe: Scroll
- Two-finger scrub: Go back
- Rotor: Two-finger rotate to change navigation mode

**Best Practices:**
- Use `accessibilityTraits` for element types
- Implement `accessibilityElements` for custom views
- Test with different speech rates

### Android TalkBack

**Gestures:**
- Swipe right/left: Navigate to next/previous element
- Double-tap: Activate element
- Two-finger swipe: Scroll
- L-shaped gesture: Local context menu

**Best Practices:**
- Use `importantForAccessibility` to hide decorative elements
- Implement `AccessibilityDelegate` for custom behavior
- Test with different feedback settings

### Currency Announcements

Special formatting for financial amounts:

```tsx
import { formatCurrencyForA11y } from '@/src/utils/accessibility';

// Indonesian
formatCurrencyForA11y(1500000); // "1 juta 500 ribu rupiah"
formatCurrencyForA11y(50000);   // "50 ribu rupiah"

// English
formatCurrencyForA11y(1500000, 'IDR', 'en'); // "1.50 million rupiah"
```

---

## Checklists

### Pre-Release Accessibility Checklist

#### Per-Screen Checklist

- [ ] All images have `accessibilityLabel` or `accessible={false}`
- [ ] All buttons have descriptive labels (not "OK" or "Click Here")
- [ ] All inputs have associated labels
- [ ] Error messages are announced to screen readers
- [ ] Loading states are announced
- [ ] Success confirmations are announced
- [ ] All touch targets are minimum 44x44
- [ ] Color contrast meets WCAG AA
- [ ] Focus indicators are visible
- [ ] Text scales correctly to 200%

#### Critical User Flows

- [ ] **Login**: All fields labeled, errors announced, biometrics accessible
- [ ] **Transfer**: Amount announced, confirmation accessible, success announced
- [ ] **QRIS**: Camera accessible, scan result announced
- [ ] **Balance Check**: Balance announced with proper formatting
- [ ] **Transaction History**: List navigable, details accessible
- [ ] **Profile**: All settings accessible, logout confirmation

### Component Development Checklist

When creating new components:

- [ ] Use `AccessibleButton` or `AccessibleInput` as base
- [ ] Add `accessibilityLabel` prop
- [ ] Add `accessibilityHint` for complex actions
- [ ] Set appropriate `accessibilityRole`
- [ ] Handle `accessibilityState` (disabled, selected, etc.)
- [ ] Validate touch target size
- [ ] Test with screen reader
- [ ] Add accessibility tests

---

## Resources

### Documentation

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [React Native Accessibility](https://reactnative.dev/docs/accessibility)
- [iOS Accessibility](https://developer.apple.com/accessibility/)
- [Android Accessibility](https://developer.android.com/guide/topics/ui/accessibility)

### Tools

- **iOS**: Accessibility Inspector (Xcode)
- **Android**: Accessibility Scanner (Play Store)
- **Web**: axe DevTools, WAVE
- **Color**: WebAIM Contrast Checker

### Internal Resources

- `/src/utils/accessibility.ts` - Utility functions
- `/src/components/ui/AccessibleButton.tsx` - Accessible button
- `/src/components/ui/AccessibleInput.tsx` - Accessible input
- `/src/hooks/useAccessibility.ts` - Accessibility hooks
- `/src/testing/accessibility.test.tsx` - Test suite

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-30 | Initial accessibility guide |

---

## Contact

For accessibility questions or issues:
- Create an issue in the project repository
- Contact the accessibility team
- Tag PRs with `accessibility` label

---

*Last Updated: January 2026*
