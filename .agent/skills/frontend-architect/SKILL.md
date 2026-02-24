---
name: frontend-architect
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: [product-designer]
tags: [frontend, react, nextjs, typescript, performance, accessibility]
related: [product-designer, bff-architect, mobile-architect]
description: **Master Skill**: Complete Next.js 15+ Architecture for PayU Digital Banking. Covers TypeScript Strict, Server Components, State Management, Security, Premium Design System, Accessibility, Testing, and Performance patterns.
---

## 📚 Reference Implementation Patterns
For detailed patterns and historical context on PayU frontend, see:
- [Frontend Architecture Patterns](./references/FRONTEND_PATTERNS.md)

# PayU Frontend Architect Master Skill

You are the **Principal Frontend Architect** for the **PayU Digital Banking Platform**. You build premium, ultra-fast, secure financial applications using **Next.js 15+**, **TypeScript Strict**, and the **App Router** architecture. You collaborate directly with the **Product Designer** to deliver a "Premium Emerald" experience.

## ⚡ Design-Logic Synchronization Protocol (v4.0)
1. **Fluidity First**: Dashboards must use a fluid, full-width layout. Fixed-width containers (`max-w-7xl`) are for content-heavy articles, not functional dashboards.
2. **Atomic Implementation**: Components must strictly use established tokens (`rounded-2xl`, `bank-green`). Deviations require specific architectural justification.
3. **No Small Text**: Strictly enforce `text-xs` (12px) as the absolute minimum font size. Refuse design mocks that use smaller text.

---

## 🛡️ TypeScript Strict Best Practices

### 1. Const Types Pattern (vs Enums)
ALWAYS create `const` objects first, then derive types:

```typescript
// ✅ Correct: Single source of truth, runtime values
const TRANSACTION_STATUS = {
  PENDING: "pending",
  COMPLETED: "completed",
  FAILED: "failed",
} as const;

type TransactionStatus = (typeof TRANSACTION_STATUS)[keyof typeof TRANSACTION_STATUS];

// ❌ Wrong: Hardcoded union types
type TransactionStatus = "pending" | "completed" | "failed";
```

### 2. Flat Interfaces
Extract nested objects into separate interfaces:

```typescript
// ✅ Correct
interface AccountBalance {
  available: number;
  pending: number;
}
interface Account {
  id: string;
  balance: AccountBalance;
}

// ❌ Wrong: Inline nesting
interface Account {
  balance: { available: number; pending: number };
}
```

### 3. No `any` Policy
Use `unknown` + Type Guards:

```typescript
function isTransaction(input: unknown): input is Transaction {
  return typeof input === "object" && input !== null && "id" in input && "amount" in input;
}
```

---

## 🚀 Next.js 15+ Architecture

### 1. Rendering Strategy

| Pattern | Use Case |
|:--------|:---------|
| **Server Components (RSC)** | Data fetching, secrets, database access |
| **Client Components** | Interactivity, hooks, event listeners |
| **Streaming + Suspense** | Slow domains (Reports, Transaction History) |
| **Partial Prerendering (PPR)** | Static shells + dynamic islands |

### 2. Advanced Caching (`'use cache'`)

```tsx
async function ExchangeRates() {
  'use cache'
  cacheTag('rates')
  cacheLife('minutes')
  return await api.getRates()
}
```

| API | Purpose |
|:----|:--------|
| `cacheLife()` | Duration profiles (`'hours'`, `'minutes'`) |
| `cacheTag()` | Label for invalidation |
| `updateTag()` | Immediate invalidation (read-your-own-writes) |
| `revalidateTag()` | Background revalidation (SWR pattern) |

### 3. Server Actions & Forms (React 19 + Next.js 15)

In Next.js 15, `useActionState` (formerly `useFormState`) is the preferred way to handle form state with Server Actions.

```tsx
// app/actions/transfer.ts
'use server'

import { z } from 'zod';

export async function createTransfer(prevState: any, formData: FormData) {
  // Logic here
  return { success: true };
}

// app/components/TransferForm.tsx
'use client'

import { useActionState } from 'react';
import { createTransfer } from '@/app/actions/transfer';

export function TransferForm() {
  const [state, action, isPending] = useActionState(createTransfer, null);

  return (
    <form action={action}>
      <input name="amount" type="number" />
      <button type="submit" disabled={isPending}>
        {isPending ? 'Processing...' : 'Transfer'}
      </button>
      {state?.success && <p>Success!</p>}
    </form>
  );
}
```

### 4. Async APIs (Breaking Change in Next.js 15)
The following APIs are now asynchronous. Always `await` them:
- `cookies()`
- `headers()`
- `params` and `searchParams` in Page/Layout and `generateMetadata`.

```tsx
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const theme = cookieStore.get('theme');
  // ...
}
```

---

## 🧠 State Management (2-Pillar Model)

### Pillar 1: Server State (React Query)

```typescript
// hooks/useAccounts.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export const accountKeys = {
  all: ['accounts'] as const,
  detail: (id: string) => [...accountKeys.all, 'detail', id] as const,
};

export function useAccounts() {
  return useQuery({
    queryKey: accountKeys.all,
    queryFn: () => accountApi.getAccounts(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: accountApi.createTransfer,
    onMutate: async (newTransfer) => {
      await queryClient.cancelQueries({ queryKey: accountKeys.all });
      const previous = queryClient.getQueryData(accountKeys.all);
      // Optimistic update
      queryClient.setQueryData(accountKeys.all, (old: Account[]) =>
        old.map(acc => acc.id === newTransfer.sourceId
          ? { ...acc, balance: acc.balance - newTransfer.amount }
          : acc
        )
      );
      return { previous };
    },
    onError: (err, variables, context) => {
      queryClient.setQueryData(accountKeys.all, context?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
```

### Pillar 2: Client State (Zustand + Slices)

```typescript
// store/slices/uiSlice.ts
import { StateCreator } from 'zustand';

export interface UISlice {
  sidebarOpen: boolean;
  modal: string | null;
  toggleSidebar: () => void;
  openModal: (id: string) => void;
  closeModal: () => void;
}

export const createUISlice: StateCreator<UISlice> = (set) => ({
  sidebarOpen: true,
  modal: null,
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  openModal: (id) => set({ modal: id }),
  closeModal: () => set({ modal: null }),
});

// store/index.ts
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { createUISlice, UISlice } from './slices/uiSlice';
import { createAuthSlice, AuthSlice } from './slices/authSlice';

type StoreState = UISlice & AuthSlice;

export const useStore = create<StoreState>()(
  devtools((...args) => ({
    ...createUISlice(...args),
    ...createAuthSlice(...args),
  }))
);

// ✅ Atomic selectors (prevents over-renders)
export const useSidebarOpen = () => useStore((s) => s.sidebarOpen);
export const useModal = () => useStore((s) => s.modal);
```

---

## 🔐 Security for Banking Apps

### Token Storage Strategy

| Platform | Storage | Notes |
|:---------|:--------|:------|
| Web | HttpOnly Cookies / Memory | NEVER localStorage for tokens |
| Mobile | SecureStore (iOS Keychain / Android Keystore) | Encrypted storage |

### API Client with Token Refresh

```typescript
// lib/api/client.ts
import axios from 'axios';
import { useAuthStore } from '@/store/authStore';

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  timeout: 15000,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['X-Request-ID'] = crypto.randomUUID();
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = useAuthStore.getState().refreshToken;
        const { data } = await axios.post('/auth/refresh', { refreshToken });
        useAuthStore.getState().setTokens(data);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return apiClient(originalRequest);
      } catch {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

### Security Checklist

| Practice | Implementation |
|:---------|:---------------|
| **XSS Prevention** | Sanitize inputs, use React's built-in escaping |
| **CSRF Protection** | SameSite cookies, CSRF tokens |
| **CSP Headers** | Strict Content-Security-Policy |
| **HTTPS Only** | Enforce in production, HSTS headers |
| **PII Masking** | Never log tokens, mask sensitive data |

---

## 🎨 Premium Emerald Design System

### Anti-AI Slop Policy
Avoid generic AI aesthetics. PayU must feel **Bespoke**, **Luxury**, and **Memorable**.

### Typography Pairing (Modern Enterprise Standard)
- **Headers & Display**: `Outfit` (Modern, Geometric, Premium Aesthetic)
- **Body & UI Elements**: `Inter` (Optimized for financial data & legibility)
- **Tracking**: Use `tracking-tight` for headers and `tracking-widest` for small uppercase labels (`text-xs`).
- **Contrast**: Maintain a clear typographic scale with extreme size contrast for high-end editorial effect.

### Design Tokens (Tailwind)

```typescript
// tailwind.config.ts
theme: {
  extend: {
    colors: {
      primary: "hsl(var(--primary))",
      "bank-green": "#10b981",
      background: "hsl(var(--background))",
      foreground: "hsl(var(--foreground))",
      muted: "hsl(var(--muted))",
      destructive: "hsl(var(--destructive))",
    },
  },
}
```

### CVA (Class Variance Authority) Pattern

```typescript
// components/ui/button.tsx
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md font-medium transition-colors focus-visible:ring-2 disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-primary/90',
        destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90',
        outline: 'border border-input bg-background hover:bg-accent',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 px-3',
        lg: 'h-11 px-8',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
  )
);
```

### Glassmorphism & Premium Surfaces

```tsx
// Premium modal backdrop
<div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50">
  <div className="bg-card rounded-3xl shadow-2xl overflow-hidden">
    {/* Modal content */}
  </div>
</div>

// Dashboard layout (Fluid Full-Width)
<div className="flex h-screen w-full overflow-hidden bg-background">
  <aside className="w-64 border-r border-border/40 overflow-y-auto hidden lg:flex flex-col">
    <SidebarContent />
  </aside>
  <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
    {/* Full-width container: NO max-w centering */}
    <div className="flex-1 overflow-y-auto p-6 lg:p-10">
      <MainContent />
    </div>
  </main>
</div>
```

---

## 🏗️ Component Architecture

### Compound Components Pattern

```typescript
// components/ui/card.tsx
const CardContext = createContext<{ variant?: string }>({});

export const Card = ({ className, variant, children, ...props }) => (
  <CardContext.Provider value={{ variant }}>
    {/* Use rounded-2xl as the platform standard */}
    <div className={cn('rounded-2xl border bg-card shadow-sm', className)} {...props}>
      {children}
    </div>
  </CardContext.Provider>
);

export const CardHeader = ({ className, ...props }) => (
  <div className={cn('flex flex-col space-y-1.5 p-6 md:p-8', className)} {...props} />
);

export const CardTitle = ({ className, ...props }) => (
  <h3 className={cn('text-2xl font-semibold tracking-tight', className)} {...props} />
);

export const CardContent = ({ className, ...props }) => (
  <div className={cn('p-6 pt-0', className)} {...props} />
);
```

### Render Props for Flexibility

```typescript
interface DataLoaderProps<T> {
  queryKey: string[];
  queryFn: () => Promise<T>;
  children: (data: T, refetch: () => void) => React.ReactNode;
}

export function DataLoader<T>({ queryKey, queryFn, children }: DataLoaderProps<T>) {
  const { data, isLoading, error, refetch } = useQuery({ queryKey, queryFn });

  if (isLoading) return <Skeleton />;
  if (error) return <ErrorState onRetry={refetch} />;
  return <>{children(data!, refetch)}</>;
}

// Usage
<DataLoader queryKey={['accounts']} queryFn={fetchAccounts}>
  {(accounts, refetch) => <AccountList accounts={accounts} onRefresh={refetch} />}
</DataLoader>
```

---

## 📝 Forms (React Hook Form + Zod)

```typescript
// components/forms/TransferForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const transferSchema = z.object({
  recipientAccount: z.string().min(10, 'Invalid account number'),
  amount: z.number().positive().max(100000000, 'Amount exceeds limit'),
  description: z.string().max(100).optional(),
});

type TransferFormData = z.infer<typeof transferSchema>;

export function TransferForm() {
  const transfer = useTransfer();

  const form = useForm<TransferFormData>({
    resolver: zodResolver(transferSchema),
    defaultValues: { amount: 0, description: '' },
  });

  const onSubmit = async (data: TransferFormData) => {
    try {
      await transfer.mutateAsync(data);
      form.reset();
    } catch (error) {
      form.setError('root', { message: 'Transfer failed' });
    }
  };

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
      <Input
        {...form.register('recipientAccount')}
        placeholder="Recipient Account"
        error={form.formState.errors.recipientAccount?.message}
      />
      <Input
        {...form.register('amount', { valueAsNumber: true })}
        type="number"
        placeholder="Amount"
        error={form.formState.errors.amount?.message}
      />
      <Button type="submit" loading={transfer.isPending}>
        Transfer
      </Button>
    </form>
  );
}
```

---

## ⚡ Performance Optimization

### 1. Import Strategy

```typescript
// ❌ Barrel imports (loads entire library)
import { Check, X, Plus } from 'lucide-react';

// ✅ Direct imports (tree-shakeable)
import Check from 'lucide-react/dist/esm/icons/check';
import X from 'lucide-react/dist/esm/icons/x';
```

### 2. Dynamic Imports

```typescript
import dynamic from 'next/dynamic';

const ChartComponent = dynamic(() => import('@/components/charts/LineChart'), {
  loading: () => <ChartSkeleton />,
  ssr: false,
});

const PDFViewer = dynamic(() => import('@/components/PDFViewer'), {
  ssr: false,
});
```

### 3. Virtualization for Long Lists

```typescript
import { useVirtualizer } from '@tanstack/react-virtual';

export function TransactionList({ transactions }: { transactions: Transaction[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: transactions.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 72,
    overscan: 5,
  });

  return (
    <div ref={parentRef} className="h-[600px] overflow-auto">
      <div style={{ height: `${virtualizer.getTotalSize()}px`, position: 'relative' }}>
        {virtualizer.getVirtualItems().map((virtualRow) => (
          <div
            key={virtualRow.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              transform: `translateY(${virtualRow.start}px)`,
            }}
          >
            <TransactionRow transaction={transactions[virtualRow.index]} />
          </div>
        ))}
      </div>
    </div>
  );
}
```

### 4. Memoization

```typescript
// Expensive computation
const sortedTransactions = useMemo(
  () => transactions.sort((a, b) => b.timestamp - a.timestamp),
  [transactions]
);

// Stable callback reference
const handleSearch = useCallback((query: string) => {
  setSearchQuery(query);
}, []);

// Pure component
export const TransactionCard = React.memo<TransactionCardProps>(({ transaction }) => (
  <div className="p-4 border rounded-lg">{/* ... */}</div>
));
```

---

## ♿ Accessibility (WCAG 2.1 AA)

### Requirements Checklist

| Requirement | Implementation |
|:------------|:---------------|
| **Keyboard Navigation** | All interactive elements focusable, logical tab order |
| **Screen Readers** | ARIA labels, semantic HTML, alt text |
| **Color Contrast** | 4.5:1 normal text, 3:1 large text |
| **Form Errors** | Descriptive messages linked via `aria-describedby` |
| **Focus Indicators** | Visible rings, skip links |
| **Dynamic Content** | `aria-live` regions for status updates |

### Accessible Components

```tsx
// Accessible button with icon
<button aria-label="Close dialog" onClick={onClose}>
  <XIcon aria-hidden="true" />
</button>

// Form with error announcement
<div role="alert" aria-live="polite">
  {error && <p className="text-destructive">{error}</p>}
</div>

// Focus trap for modals
<FocusTrap>
  <dialog aria-modal="true" aria-labelledby="dialog-title">
    <h2 id="dialog-title">Confirm Transfer</h2>
    {/* content */}
  </dialog>
</FocusTrap>
```

---

## 🧪 Testing Strategy

### Unit Tests (Vitest + Testing Library)

```typescript
// tests/unit/TransferForm.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TransferForm } from '@/components/forms/TransferForm';

describe('TransferForm', () => {
  it('validates required fields', async () => {
    const user = userEvent.setup();
    render(<TransferForm />);

    await user.click(screen.getByRole('button', { name: /transfer/i }));

    expect(screen.getByText('Invalid account number')).toBeInTheDocument();
  });

  it('submits valid data', async () => {
    const user = userEvent.setup();
    const mockTransfer = vi.fn().mockResolvedValue({});

    render(<TransferForm onSubmit={mockTransfer} />);

    await user.type(screen.getByPlaceholderText('Recipient Account'), '1234567890');
    await user.type(screen.getByPlaceholderText('Amount'), '100000');
    await user.click(screen.getByRole('button', { name: /transfer/i }));

    expect(mockTransfer).toHaveBeenCalledWith({
      recipientAccount: '1234567890',
      amount: 100000,
    });
  });
});
```

### E2E Tests (Playwright)

```typescript
// tests/e2e/transfer.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Transfer Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="phone"]', '+6281234567890');
    await page.fill('input[name="pin"]', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('completes transfer successfully', async ({ page }) => {
    await page.click('text=Transfer');
    await page.fill('input[name="recipient"]', '1234567890');
    await page.fill('input[name="amount"]', '100000');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Transfer successful')).toBeVisible();
  });

  test('shows error for insufficient balance', async ({ page }) => {
    await page.click('text=Transfer');
    await page.fill('input[name="amount"]', '999999999999');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Insufficient balance')).toBeVisible();
  });
});
```

---

## 🎭 Animation Strategy

### Framer Motion (UI/UX Default)

```tsx
import { motion, AnimatePresence } from 'framer-motion';

export function Modal({ isOpen, onClose, children }) {
  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm"
        >
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.95, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            className="bg-card rounded-3xl shadow-2xl"
          >
            {children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
```

### GSAP (Complex Timelines)

```tsx
import { useRef } from 'react';
import { gsap } from 'gsap';
import { useGSAP } from '@gsap/react';

export function LandingHero() {
  const container = useRef<HTMLDivElement>(null);

  useGSAP(() => {
    const tl = gsap.timeline();
    tl.from('.hero-title', { y: 50, opacity: 0, duration: 0.8 })
      .from('.hero-subtitle', { y: 30, opacity: 0, duration: 0.6 }, '-=0.4')
      .from('.hero-cta', { scale: 0.9, opacity: 0, duration: 0.5 }, '-=0.3');
  }, { scope: container });

  return (
    <div ref={container}>
      <h1 className="hero-title">PayU Banking</h1>
      <p className="hero-subtitle">Digital banking reimagined</p>
      <button className="hero-cta">Get Started</button>
    </div>
  );
}
```

---

## ⚡ Performance Critical Rules

### 1. Eliminate Waterfalls (CRITICAL)

```typescript
// ❌ WRONG: Sequential fetching (waterfall)
const user = await fetchUser()
const posts = await fetchPosts(user.id)
const comments = await fetchComments(posts[0].id)

// ✅ RIGHT: Parallel fetching
const [user, posts, comments] = await Promise.all([
  fetchUser(),
  fetchPosts(),
  fetchComments()
])
```

### 2. Bundle Size Optimization

```tsx
// ❌ WRONG: Barrel import loads entire library
import { Check } from 'lucide-react'

// ✅ RIGHT: Direct import loads only what you need
import Check from 'lucide-react/dist/esm/icons/check'

// ✅ RIGHT: Dynamic import for heavy components
import dynamic from 'next/dynamic'
const MonacoEditor = dynamic(() => import('./monaco-editor'), { ssr: false })
```

### 3. Re-render Prevention

```tsx
// ✅ Memoize expensive components
const ProductItem = memo(function ProductItem({ item, onPress }) {
  const handlePress = useCallback(() => onPress(item.id), [item.id, onPress])
  return <div onClick={handlePress}>{item.name}</div>
})

// ✅ Use lazy state initialization
const [items, setItems] = useState(() => calculateInitialItems())
```

### Key Metrics to Track
- **TTI** (Time to Interactive): When page becomes fully interactive
- **LCP** (Largest Contentful Paint): When main content is visible
- **FID** (First Input Delay): Responsiveness to user interactions
- **CLS** (Cumulative Layout Shift): Visual stability

---

## 📱 Responsive Design Patterns

### Container Queries

```css
/* Component-level responsiveness */
.card-container { container-type: inline-size; }

@container (min-width: 400px) {
  .card {
    display: grid;
    grid-template-columns: 200px 1fr;
  }
}
```

```tsx
// Tailwind container queries
<div className="@container">
  <article className="flex flex-col @md:flex-row @md:gap-4">
    <img className="w-full @md:w-48 @lg:w-64" />
  </article>
</div>
```

### Fluid Typography

```css
:root {
  --text-base: clamp(1rem, 0.9rem + 0.5vw, 1.125rem);
  --text-xl: clamp(1.25rem, 1rem + 1.25vw, 1.5rem);
  --text-4xl: clamp(2.25rem, 1.75rem + 2.5vw, 3.5rem);
}
```

### Auto-Fit Grid

```css
.grid-auto {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(300px, 100%), 1fr));
  gap: 1.5rem;
}
```

### Mobile Viewport Units

```css
/* Use dynamic viewport for mobile browser UI */
.full-height { height: 100dvh; }  /* Not 100vh! */
```

---

## 🔍 Frontend Architect Checklist

### Infrastructure
- [ ] Next.js 15 App Router (no Pages Router)
- [ ] TypeScript Strict mode enabled
- [ ] ESLint + Prettier configured
- [ ] Tailwind CSS with design tokens

### Security
- [ ] Tokens in HttpOnly cookies / memory only
- [ ] CSP headers configured
- [ ] XSS/CSRF protection
- [ ] PII never in localStorage

### Performance
- [ ] Direct imports (no barrel files)
- [ ] Dynamic imports for heavy components
- [ ] Virtualization for lists > 100 items
- [ ] Image optimization with next/image
- [ ] No request waterfalls (use Promise.all)
- [ ] Suspense boundaries for streaming

### UX
- [ ] Skeletons for all async boundaries
- [ ] Error Boundaries per route segment
- [ ] Optimistic UI for mutations
- [ ] Loading states for all actions

### Accessibility
- [ ] WCAG 2.1 AA compliance
- [ ] Keyboard navigation tested
- [ ] Screen reader tested
- [ ] Color contrast verified

### Responsive
- [ ] Mobile-first breakpoints
- [ ] Container queries for components
- [ ] Fluid typography (clamp)
- [ ] Touch targets 44x44px minimum

### Testing
- [ ] Unit tests (Vitest) > 80% coverage
- [ ] E2E tests (Playwright) for critical flows
- [ ] Visual regression tests
- [ ] Performance budgets enforced

---

## 📚 References

### Local Reference Files

| Category | Topic | File |
|----------|-------|------|
| **Performance** | 45+ React/Next.js optimization rules from Vercel | [react-performance-guidelines.md](./references/react-performance-guidelines.md) |
| **Composition** | React composition patterns (compound, slot, HOC) | [react-composition-patterns.md](./references/react-composition-patterns.md) |
| **Senior** | Senior frontend engineering practices | [senior-practices.md](./references/senior-practices.md) |

### Key Performance Rules (Quick Reference)

1. **Eliminating Waterfalls (CRITICAL)**: Use `Promise.all()` for independent fetches
2. **Bundle Size (CRITICAL)**: Direct imports, avoid barrel files, dynamic imports
3. **Server Components (HIGH)**: Minimize data at RSC boundaries
4. **Re-render Optimization (MEDIUM)**: Memoize expensive components, narrow effect deps

### External Documentation

- [Next.js Documentation](https://nextjs.org/docs)
- [React Documentation](https://react.dev)
- [Vercel Bundle Optimization](https://vercel.com/blog/how-we-optimized-package-imports-in-next-js)
- [SWR Documentation](https://swr.vercel.app)

---

## 🚨 P19 Audit Status — Frontend Gaps (Feb 2026)

> **CRITICAL**: Read `.agent/context/P19-AUDIT-STATUS.md` for full details.
> **Web-App Score: 72/100** | **Mobile Score: 58/100**

### P0 Security Blocker: JWT in localStorage

**THIS IS THE #1 PRIORITY FIX FOR FRONTEND.**

- **File**: `frontend/web-app/src/lib/api.ts` (line 15, 61) uses `localStorage.setItem('token', ...)`
- **File**: `frontend/web-app/src/stores/authStore.ts` says "tokens ONLY in httpOnly cookies" — **LIE**
- **Impact**: Any XSS attack steals all tokens. PCI-DSS violation.
- **Fix**: BFF (Backend-for-Frontend) pattern using Next.js API routes as a proxy

#### BFF Implementation (from `docs/guides/LESSONS.md`):

```typescript
// app/api/auth/login/route.ts — Token stays server-side
export async function POST(req: NextRequest) {
  const { username, password } = await req.json();
  const res = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const data = await res.json();
  
  const response = NextResponse.json({ success: true });
  response.cookies.set('access_token', data.accessToken, {
    httpOnly: true, secure: true, sameSite: 'strict',
    path: '/', maxAge: 900, // 15 min
  });
  return response;
}
```

- After fixing, **remove ALL `localStorage.getItem('token')` / `setItem('token')` calls**
- Update `api.ts` to use `credentials: 'include'` instead of `Authorization` header
- **Remediation code**: R-001 (8 SP)

### P1 Issues

| Issue | File | Fix |
|:------|:-----|:----|
| All remote images allowed | `next.config.ts` | Whitelist specific CDN domains (R-010) |
| No CSP headers | `next.config.ts` | Add `Content-Security-Policy` headers |
| OAuth token in URL | Auth callback | Use auth code flow with PKCE |

### E2E Testing Status

- 12 Playwright spec files, ~424 tests, **<15% passing**
- Auth fixture broken (middleware redirects not handled)
- Investment module: tests written but features NOT implemented
- Lending, KYC, Bill Pay: major implementation gaps
- **Fix approach**: Create proper auth fixture, skip unimplemented tests, fix selectors (R-009)

### Mobile App Gaps

- Score: **58/100** — Incomplete feature implementations
- No biometric authentication
- Missing offline-first patterns
- Several screens are placeholder/skeleton only

---
*Last Updated: February 2026 (P19 Audit)*
