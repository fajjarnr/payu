# Playwright E2E Debugging Patterns

## Overview

This guide covers systematic debugging patterns for Playwright E2E tests in the PayU platform.

## Common E2E Test Failure Patterns

### Pattern 1: Strict Mode Violation

**Symptom:**
```
Error: strict mode violation: getByText('Pinjaman') resolved to 9 elements
```

**Root Cause:**
The text selector matches multiple elements on the page. Playwright requires explicit selection when multiple matches exist.

**Solution:**
```typescript
// ❌ WRONG - Ambiguous selector
await expect(page.getByText('Pinjaman')).toBeVisible();

// ✅ CORRECT - Use first() or more specific selector
await expect(page.getByText('Pinjaman').first()).toBeVisible();

// ✅ CORRECT - Use role for specificity
await expect(page.getByRole('button', { name: 'Pinjaman' })).toBeVisible();
```

### Pattern 2: Currency Format Mismatch

**Symptom:**
```
Error: getByText(/Rp 50\.000\.000/) - strict mode violation
```

**Root Cause:**
`Intl.NumberFormat` formats currency differently than expected. The format includes a non-breaking space (`\u00A0`).

**Diagnosis:**
```javascript
// Check actual format
console.log(new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  minimumFractionDigits: 0
}).format(50000000));
// Output: "Rp 50.000.000" (with non-breaking space)
```

**Solution:**
```typescript
// ❌ WRONG - No space handling
await expect(page.getByText(/Rp50\.000\.000/)).toBeVisible();

// ✅ CORRECT - Allow optional whitespace
await expect(page.getByText(/Rp\s*50\.000\.000/)).toBeVisible();
```

### Pattern 3: Tab/State Not Switching

**Symptom:**
```
Error: getByText('PayLater Limit') not visible after clicking PayLater tab
```

**Root Cause:**
React state update hasn't propagated when test checks for content. The click event fires, but state update is async.

**Solution:**
```typescript
// ❌ WRONG - No wait for state update
await page.click('[data-testid="paylater-tab"]');
await expect(page.getByText('PayLater Limit')).toBeVisible();

// ✅ CORRECT - Wait for content to appear
await page.click('[data-testid="paylater-tab"]');
await page.waitForSelector('text=PayLater Limit', { timeout: 5000 });
await expect(page.getByText('PayLater Limit')).toBeVisible();
```

### Pattern 4: Translation Content Mismatch

**Symptom:**
```
Error: getByText('Mulai Proses Verifikasi') not found
```

**Root Cause:**
Tests expect hardcoded text, but implementation uses `next-intl` translations with different content.

**Solution:**
```typescript
// ❌ WRONG - Hardcoded expectation
await expect(page.getByText('Mulai Proses Verifikasi')).toBeVisible();

// ✅ CORRECT - Match actual translation content
// Check messages/id.json first:
// "step1.button": "Lanjut ke Profil Data"
await expect(page.getByText('Lanjut ke Profil Data')).toBeVisible();
```

### Pattern 5: CSS Class Selector Issues

**Symptom:**
```
Error: locator('.text-5xl.font-black') - element not found
```

**Root Cause:**
1. Class order in HTML differs from selector
2. Class name changed in implementation
3. Multiple classes with similar names

**Solution:**
```typescript
// ❌ WRONG - Assumes exact class order
await expect(page.locator('.text-5xl.font-black')).toBeVisible();

// ✅ CORRECT - More flexible selector
await expect(page.locator('.text-5xl.font-bold')).toBeVisible();

// ✅ BETTER - Use text content or test IDs
await expect(page.getByText('785')).toBeVisible();
await expect(page.locator('[data-testid="credit-score-value"]')).toBeVisible();
```

## Debugging Workflow for E2E Tests

### Step 1: Run Test with Detailed Output

```bash
# Run single test with detailed output
npx playwright test --project=chromium lending-flow.spec.ts:25 --reporter=list

# Run with screenshot on failure
npx playwright test --project=chromium lending-flow.spec.ts -- screenshot-on-failure-only

# Run with trace (for deep debugging)
npx playwright test --project=chromium lending-flow.spec.ts --trace=on
```

### Step 2: Examine Error Context

Playwright generates helpful artifacts on failure:
- **Screenshot:** `test-results/<test-name>/test-failed-1.png`
- **Video:** `test-results/<test-name>/video.webm`
- **Error Context:** `test-results/<test-name>/error-context.md`
- **Trace:** `test-results/<test-name>/trace.zip`

### Step 3: Inspect Page State

```typescript
// Add temporary debugging
test('debugging example', async ({ page }) => {
  await page.goto('/lending');

  // Debug: List all matching elements
  const allElements = await page.getByText('Pinjaman').all();
  console.log('Found:', allElements.length, 'elements');

  // Debug: Get element HTML
  const element = await page.getByText('Pinjaman').first();
  console.log('HTML:', await element.innerHTML());

  // Debug: Screenshot
  await page.screenshot({ path: 'debug.png' });
});
```

### Step 4: Compare Against Implementation

1. **Read the page component:**
   ```bash
   # Find the page file
   find src/app -name "*lending*page.tsx"
   ```

2. **Check for test IDs:**
   ```tsx
   // Add to component for reliable testing
   <button data-testid="paylater-tab">PayLater</button>
   ```

3. **Check translation files:**
   ```bash
   # Check expected text content
   cat messages/id.json | jq '.auth.onboarding'
   ```

## Best Practices for E2E Tests

### 1. Use Test IDs for Critical Elements

```tsx
// Component
<button data-testid="submit-button">Submit</button>

// Test
await page.click('[data-testid="submit-button"]');
```

### 2. Prefer Role-Based Selectors

```typescript
// ✅ GOOD - Semantic and accessible
await expect(page.getByRole('button', { name: 'Submit' })).toBeVisible();

// ❌ AVOID - Brittle CSS selectors
await expect(page.locator('.bg-primary.text-white')).toBeVisible();
```

### 3. Use Explicit Waits for Dynamic Content

```typescript
// ✅ GOOD - Wait for specific condition
await page.waitForSelector('[data-testid="paylater-content"]', { timeout: 5000 });

// ❌ AVOID - Arbitrary timeout
await page.waitForTimeout(5000);
```

### 4. Match Text Flexibly

```typescript
// For dynamic content
await expect(page.getByText(/total/i)).toBeVisible();  // Case insensitive
await expect(page.getByText(/Rp\s*50/)).toBeVisible();  // Flexible spacing
await expect(page.getByText(/\d{1,3}/).toBeVisible());  // Pattern matching
```

### 5. Handle Strict Mode Violations

```typescript
// When multiple elements match
await expect(page.getByText('Submit').first()).toBeVisible();
await expect(page.getByText('Submit').nth(1)).toBeVisible();

// Or use more specific selector
await expect(page.getByRole('button', { name: 'Submit' })).toBeVisible();
```

## Platform-Specific Patterns

### Next.js i18n with next-intl

**Pattern:** Content loaded from translation files, not hardcoded

**Debugging:**
```bash
# Check translation content
cat messages/id.json | jq '.pageName'
```

**Test Pattern:**
```typescript
// Read translation first, then test
const expectedText = require('../messages/id.json').pageName.title;
await expect(page.getByText(expectedText)).toBeVisible();
```

### React State Updates

**Pattern:** State changes are async, need to wait for UI updates

**Test Pattern:**
```typescript
await page.click('[data-testid="tab-button"]');
await page.waitForSelector('[data-testid="tab-content"]', { timeout: 5000 });
await expect(page.getByText('Tab Content')).toBeVisible();
```

### Animation Transitions

**Pattern:** Framer Motion animations need time to complete

**Test Pattern:**
```typescript
// Disable animations in tests
page.addInitScript(() => {
  window.matchMedia = () => ({ matches: false });
});
```

## Quick Reference

| Issue | Solution |
|:---|:---|
| Strict mode violation | Use `.first()` or `getByRole()` |
| Text not found | Check translations, use regex |
| Element not visible | Use `waitForSelector()` |
| State not updated | Add explicit wait after click |
| Currency format | Use `\s*` for optional space |
| CSS class mismatch | Use test IDs or text content |
