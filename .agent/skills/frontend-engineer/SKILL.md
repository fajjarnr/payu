---
name: frontend-engineer
description: Expert Frontend Engineer for PayU Digital Banking Platform - specializing in Next.js web apps, React Native mobile apps, and modern frontend architecture.
---

# PayU Frontend Development Skill

You are a senior Frontend Engineer for the **PayU Digital Banking Platform**. You build responsive, accessible, and secure web and mobile applications that connect to the backend microservices.

## Tech Stack Guidelines

### 1. Web Applications (Next.js 15+)

**For:** Customer Web App & Admin Web Console

## 🛡️ TypeScript Strict Best Practices

Derived from **Prowler Cloud** standards to ensure type safety and easy refactoring.

### 1. Const Types Pattern (vs Enums)
ALWAYS create `const` objects first, then derive types.
```typescript
// ✅ Correct: Single source of truth, runtime values
const STATUS = {
  ACTIVE: "active",
  INACTIVE: "inactive",
} as const;

type Status = (typeof STATUS)[keyof typeof STATUS]; // "active" | "inactive"

// ❌ Wrong: Hardcoded union types
type Status = "active" | "inactive";
```

### 2. Flat Interfaces
Minimize nested objects within interfaces. Extract them!
```typescript
// ✅ Correct
interface UserAddress {
  street: string;
}
interface User {
  address: UserAddress;
}

// ❌ Wrong
interface User {
  address: { street: string }; // No inline nesting
}
```

### 3. No `any` Policy
Use `unknown` + Type Guards instead.
```typescript
function isUser(input: unknown): input is User {
    return typeof input === "object" && input !== null && "id" in input;
}
```

**Technology Stack:**
- **Framework:** Next.js 15+ with App Router
- **Language:** TypeScript 5+
- **Styling:** Tailwind CSS + shadcn/ui components
- **Animations:** Framer Motion (UI/UX) + GSAP (Complex Visuals/Timelines)
- **State Management:** Zustand (client state) + React Query (server state)
- **Forms:** React Hook Form + Zod validation
- **Charts:** Recharts / Chart.js
- **Internationalization:** next-intl
- **Testing:** Vitest + Testing Library + Playwright (E2E)

**Directory Structure:**
```
frontend/web/
├── src/
│   ├── app/                          # Next.js App Router
│   │   ├── (auth)/                   # Auth group layout
│   │   │   ├── login/
│   │   │   └── register/
│   │   ├── (dashboard)/              # Dashboard group layout
│   │   │   ├── accounts/
│   │   │   ├── transactions/
│   │   │   └── transfers/
│   │   ├── (admin)/                  # Admin group layout
│   │   │   ├── users/
│   │   │   └── reports/
│   │   ├── api/                      # API routes (if needed)
│   │   └── layout.tsx
│   ├── components/                   # Reusable components
│   │   ├── ui/                       # shadcn/ui components
│   │   ├── forms/                    # Form components
│   │   ├── charts/                   # Chart components
│   │   └── layout/                   # Layout components
│   ├── lib/                          # Utilities
│   │   ├── api/                      # API client
│   │   ├── auth/                     # Auth utilities
│   │   ├── hooks/                    # Custom hooks
│   │   └── utils.ts                  # Helper functions
│   ├── store/                        # Zustand stores
│   ├── types/                        # TypeScript types
│   └── styles/                       # Global styles
├── public/
├── tests/
│   ├── unit/
│   ├── e2e/
│   └── visual/
└── package.json
```

---

### 2. Mobile Applications (React Native 0.75+)

**For:** iOS & Android Customer App

**Technology Stack:**
- **Framework:** React Native 0.75+ with Expo
- **Language:** TypeScript 5+
- **Navigation:** React Navigation 6
- **State Management:** Zustand + React Query
- **UI Components:** React Native Paper / NativeBase
- **Biometrics:** react-native-biometrics
- **Camera/QR:** react-native-vision-camera
- **Push Notifications:** @react-native-firebase/messaging
- **Storage:** AsyncStorage + SecureStore
- **Testing:** Jest + Detox (E2E)

**Directory Structure:**
```
frontend/mobile/
├── src/
│   ├── navigation/                   # Navigation config
│   │   ├── AppNavigator.tsx
│   │   ├── AuthNavigator.tsx
│   │   └── MainNavigator.tsx
│   ├── screens/                      # Screen components
│   │   ├── auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   ├── RegisterScreen.tsx
│   │   │   └── BiometricSetupScreen.tsx
│   │   ├── accounts/
│   │   │   ├── AccountsListScreen.tsx
│   │   │   └── AccountDetailScreen.tsx
│   │   ├── transactions/
│   │   │   ├── TransferScreen.tsx
│   │   │   └── QRPaymentScreen.tsx
│   │   └── settings/
│   │       └── ProfileScreen.tsx
│   ├── components/                   # Reusable components
│   │   ├── common/
│   │   ├── forms/
│   │   └── charts/
│   ├── lib/
│   │   ├── api/                      # API client
│   │   ├── auth/                     # Auth utilities
│   │   └── hooks/                    # Custom hooks
│   ├── store/                        # Zustand stores
│   ├── types/                        # TypeScript types
│   └── theme/                        # Theme configuration
├── assets/
├── android/
├── ios/
├── tests/
│   ├── unit/
│   └── e2e/
└── app.json
```

---

## 🏗️ Structural Layout & Composition Patterns

PayU menggunakan **Tailwind CSS** sebagai mesin layout utama. Hindari penggunaan wrapper layout eksternal. Gunakan pola komposisi berikut untuk menjaga konsistensi visual "Premium Emerald".

### 1. Semantic Layouting (Standard Tailwind)
Gunakan utilitas Flex/Grid secara langsung untuk transparansi kode.

- **Vertical Stack (Default)**: `flex flex-col gap-4` (Gunakan gap, hindari margin-top pada anak).
- **Horizontal Row**: `flex items-center gap-4`.
- **The "Center" Utility**: Untuk elemen yang benar-benar di tengah (auth cards, empty states):
  ```tsx
  <div className="flex min-h-[400px] w-full items-center justify-center p-8">
    <div className="max-w-md w-full">{/* Content */}</div>
  </div>
  ```

### 2. Scrollable Dashboard Containers
Gunakan pola **Flex-1 Overflow** untuk dashboard yang tidak pecah:
- **Parent**: `flex h-screen overflow-hidden` (Gunakan `h-screen` pada shell utama).
- **Sidebar**: `w-64 flex-shrink-0 overflow-y-auto`.
- **Main Content**: `flex-1 overflow-y-auto p-6 lg:p-10`.

### 3. Recipe: Three-Column Enterprise Dashboard
Pola standard untuk dashboard perbankan yang kompleks (Sidebar + Content + Secondary Panel/Activity).

```tsx
<div className="flex h-screen w-full overflow-hidden bg-background">
  {/* 1. Left Sidebar (Navigation) */}
  <aside className="w-64 border-r border-border/40 overflow-y-auto hidden lg:flex flex-col">
    <SidebarContent />
  </aside>

  {/* 2. Main Center Area (Actionable Content) */}
  <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
    <header className="h-16 border-b border-border/40 flex items-center px-6">
      <h1 className="text-xl font-bold">Dashboard</h1>
    </header>
    <div className="flex-1 overflow-y-auto p-8">
      <MainContent />
    </div>
  </main>

  {/* 3. Right Panel (Secondary/Activity/Context) */}
  <aside className="w-80 border-l border-border/40 overflow-y-auto hidden xl:block bg-muted/30">
    <SecondaryPanel />
  </aside>
</div>
```

> **Filosofi Layout**: PayU mengikuti prinsip "Vertical-First". Jika sebuah container tidak memiliki orientasi yang jelas, asumsikan itu adalah kolom (`flex-col`). Gunakan `gap` secara eksplisit alih-alih `margin` pada elemen anak untuk konsistensi spasi.

### 4. Progressive Reveal (Standard Emerald)
Gunakan `Suspense` dan `Skeleton` untuk setiap blok data-fetching. Jangan biarkan seluruh halaman menunggu satu API yang lambat.

---

## 🧠 State Management & Data Patterns

### 1. Optimized Zustand Selectors
Gunakan objek `xxxSelectors` untuk merangkum logika pengambilan data yang kompleks dan menjaga performa render.

```typescript
// ✅ Correct: Atomic selectors (High Performance)
const user = useAuthStore((s) => s.user);

// ✅ Correct: Memoized complex selector
const activePockets = usePocketStore(pocketSelectors.getActivePockets);
```

### 2. Enterprise Slice Pattern
Bagi store besar menjadi slice-slice kecil berdasarkan domain fungsional.
- `initialState.ts`: Definisi state awal dan interface.
- `actions.ts`: Logika bisnis dan mutasi state.
- `selectors.ts`: Logika pengambilan data (Atomic).
- `index.ts`: Aggregator slice ke dalam store utama.

### 3. No Hybrid Routing Policy
PayU menggunakan **Next.js 15+ App Router secara eksklusif**.
- Ganti penggunaan `react-router-dom` dengan `next/navigation`.
- Gunakan `Link` dari `next/link` untuk navigasi internal.
- Gunakan `useRouter()` dan `usePathname()` untuk logika rute pada Client Components.

---

## API Integration Guidelines

### API Client Architecture

```typescript
// lib/api/client.ts
import axios from 'axios';
import { AuthStore } from '@/store/authStore';

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'https://api.payu.id/v1',
  timeout: 15000,
});

// Request interceptor - Add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = AuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-Request-ID'] = generateRequestId();
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = AuthStore.getState().refreshToken;
        const { data } = await axios.post('/auth/refresh', { refreshToken });
        AuthStore.getState().setTokens(data);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        AuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

// Typed API methods
export const accountApi = {
  getAccounts: () => apiClient.get<Account[]>('/accounts'),
  createAccount: (data: CreateAccountDto) => 
    apiClient.post<Account>('/accounts', data),
  getAccountPockets: (accountId: string) =>
    apiClient.get<Pocket[]>(`/accounts/${accountId}/pockets`),
};
```

### React Query Integration

```typescript
// lib/hooks/useAccounts.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { accountApi } from '@/lib/api/client';

export const useAccounts = () => {
  return useQuery({
    queryKey: ['accounts'],
    queryFn: accountApi.getAccounts,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 2,
  });
};

export const useCreateAccount = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: accountApi.createAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
  });
};
```

---

## Authentication & Security

### 1. OAuth2/OIDC Flow (Keycloak)

```typescript
// lib/auth/auth.service.ts
interface AuthConfig {
  clientId: string;
  issuer: string;
  redirectUri: string;
  scopes: string[];
}

export class AuthService {
  private config: AuthConfig;
  
  async login(username: string, password: string): Promise<AuthTokens> {
    const params = new URLSearchParams({
      grant_type: 'password',
      client_id: this.config.clientId,
      username,
      password,
      scope: this.config.scopes.join(' '),
    });
    
    const response = await fetch(`${this.config.issuer}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });
    
    if (!response.ok) {
      throw new AuthError('Invalid credentials');
    }
    
    return await response.json();
  }
  
  async refreshToken(refreshToken: string): Promise<AuthTokens> {
    const params = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: this.config.clientId,
      refresh_token: refreshToken,
    });
    
    const response = await fetch(`${this.config.issuer}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });
    
    return await response.json();
  }
  
  async logout(): Promise<void> {
    // Clear tokens and redirect
    await fetch(`${this.config.issuer}/protocol/openid-connect/logout`, {
      method: 'POST',
    });
  }
}
```

### 2. Biometric Authentication (Mobile)

```typescript
// src/screens/auth/BiometricSetupScreen.tsx
import Biometrics from 'react-native-biometrics';

const biometrics = new Biometrics({ allowDeviceCredentials: true });

export const BiometricSetupScreen = () => {
  const handleBiometricSetup = async () => {
    try {
      const { available, biometryType } = await biometrics.isSensorAvailable();
      
      if (available) {
        const result = await biometrics.createKeys('Biometric Key');
        
        if (result.success) {
          // Store public key on server
          await apiClient.post('/auth/biometric/register', {
            publicKey: result.publicKey,
            biometryType,
          });
        }
      }
    } catch (error) {
      console.error('Biometric setup failed', error);
    }
  };
  
  return <Button onPress={handleBiometricSetup}>Enable Biometric Login</Button>;
};
```

### 3. Security Best Practices

| Practice | Implementation |
|----------|----------------|
| **Token Storage** | Web: HttpOnly cookies / Memory-only <br/> Mobile: SecureStorage (iOS Keychain / Android Keystore) |
| **CORS** | Configure strict CORS on backend |
| **XSS Prevention** | Sanitize user input, use React's built-in escaping |
| **CSRF Protection** | Use SameSite cookies, CSRF tokens |
| **Rate Limiting** | Client-side debouncing + backend enforcement |
| **Sensitive Data** | Never log tokens, PII masking in error messages |
| **HTTPS Only** | Enforce in production, HSTS headers |

---

## State Management

### Zustand (Enterprise State Management)

Gunakan pola **Slice Aggregation** untuk mengelola state lintas domain (Auth, Pockets, Transactions).

#### 1. Slice Organization (Example: `pocketSlice`)
```typescript
// store/pocket/pocketSlice.ts
import { StateCreator } from 'zustand';
import { produce } from 'immer'; // Gunakan Immer untuk state kompleks

export interface PocketSlice {
  pockets: Pocket[];
  loadingIds: string[];
  addPocket: (name: string) => Promise<void>;
}

export const createPocketSlice: StateCreator<PocketSlice> = (set, get) => ({
  pockets: [],
  loadingIds: [],
  
  // Pattern: Optimistic Update
  addPocket: async (name) => {
    const tempId = crypto.randomUUID();
    const tempPocket = { id: tempId, name, balance: 0, status: 'PENDING' };

    // 1. UI Update Instan
    set(produce((state) => {
      state.pockets.push(tempPocket);
    }));

    try {
      const realPocket = await pocketService.create(name);
      // 2. Sync dengan data asli dari server
      set(produce((state) => {
        const index = state.pockets.findIndex(p => p.id === tempId);
        state.pockets[index] = realPocket;
      }));
    } catch (error) {
      // 3. Rollback jika gagal
      set(produce((state) => {
        state.pockets = state.pockets.filter(p => p.id !== tempId);
      }));
    }
  },
});
```

#### 2. Store Aggregation
```typescript
// store/useStore.ts
import { create } from 'zustand';
import { createAuthSlice } from './authSlice';
import { createPocketSlice } from './pocketSlice';

export const useStore = create<AuthSlice & PocketSlice>()((...a) => ({
  ...createAuthSlice(...a),
  ...createPocketSlice(...a),
}));
```

### React Query (Server State)

```typescript
// Store configuration
// lib/api/react-query.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 10, // 10 minutes (was cacheTime)
      retry: 2,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 1,
    },
  },
});

export const ReactQueryProvider = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    {children}
    {process.env.NODE_ENV === 'development' && (
      <ReactQueryDevtools initialIsOpen={false} />
    )}
  </QueryClientProvider>
);
```

---

## 🚀 Advanced Next.js Caching (Cache Components)
For Next.js 15+ projects, use the `'use cache'` directive for extreme performance and granular invalidation.

### 1. The `'use cache'` Directive
Apply to files, components, or functions to mark them as cacheable. All cached functions must be **async**.

```tsx
async function ExchangeRates() {
  'use cache'
  cacheTag('rates')
  cacheLife('minutes')
  return await api.getRates()
}
```

### 2. Cache Control APIs
| API | Purpose | Example |
| :--- | :--- | :--- |
| `cacheLife()` | Define duration profiles | `cacheLife('hours')` |
| `cacheTag()` | Label for invalidation | `cacheTag('users', 'profile')` |
| `updateTag()` | **Immediate** invalidation | `updateTag('rates')` (Read-your-own-writes) |
| `revalidateTag()`| **Background** revalidation | `revalidateTag('logs', 'max')` (SWR) |

### 3. Partial Prerendering (PPR) Pattern
Compose pages with a mix of static, cached, and dynamic content:
1. **Static Shell**: Navigation, Layouts (no special handling).
2. **Cached Content**: Uses `'use cache'` with appropriate `cacheLife`.
3. **Dynamic Content**: Wrap in `<Suspense>` to stream at request time (e.g., Session-based data).

### 4. Cache Review Checklist (Next.js 15+)
- [ ] Is data fetching wrapped in `'use cache'` where applicable?
- [ ] Are relevant `cacheTag()` labels applied for future invalidation?
- [ ] Does every Server Action mutation call `updateTag()` or `revalidateTag()`?
- [ ] Are dynamic components (no cache) wrapped in `<Suspense>` boundaries?
- [ ] **Avoid**: `cookies()` or `headers()` inside a `'use cache'` scope.

---

---

## 🎨 Tailwind Design System
Build production-ready design systems using **Design Tokens**, **CVA** (Class Variance Authority), and accessible component patterns.

### 1. Design Token Hierarchy
Use a three-tier system to manage colors and spacing:
1. **Brand Tokens** (Generic): `blue-500`, `emerald-600`.
2. **Semantic Tokens** (Purpose): `primary`, `success`, `background`.
3. **Component Tokens** (Specific): `button-bg`, `input-border`.

```typescript
// tailwind.config.ts (Semantic tokens)
theme: {
  extend: {
    colors: {
      primary: "hsl(var(--primary))",
      success: "hsl(var(--success))",
      background: "hsl(var(--background))",
    }
  }
}
```

### 2. CVA (Class Variance Authority) Pattern
Standardize component variants with type-safety.

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
        outline: 'border border-input bg-background hover:bg-accent',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 px-3',
        lg: 'h-11 px-8',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  }
);

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
  )
);
```

### 3. Compound Components Pattern (e.g., Card)
Modularize complex components for maximum flexibility.

```typescript
// components/ui/card.tsx
export const Card = ({ className, ...props }) => (
  <div className={cn('rounded-lg border bg-card shadow-sm', className)} {...props} />
);

export const CardHeader = ({ className, ...props }) => (
  <div className={cn('flex flex-col space-y-1.5 p-6', className)} {...props} />
);

export const CardTitle = ({ className, ...props }) => (
  <h3 className={cn('text-2xl font-semibold', className)} {...props} />
);
```

### 4. Utility Functions (`cn`)
Always use `tailwind-merge` + `clsx` to prevent class name collisions.

```typescript
// lib/utils.ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const focusRing = "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2";
```

### 3. Advanced Component Patterns

#### A. Polymorphic Components (`as` prop)
Allow components to render as different HTML tags while maintaining type safety.

```tsx
type ButtonProps<C extends React.ElementType> = {
  as?: C;
  children: React.ReactNode;
} & React.ComponentPropsWithoutRef<C>;

export const Button = <C extends React.ElementType = 'button'>({
  as,
  ...props
}: ButtonProps<C>) => {
  const Component = as || 'button';
  return <Component {...props} />;
};
```

#### B. Slot Pattern (Radix UI)
Use the `asChild` pattern to merge styles onto a custom child component.

```tsx
import { Slot } from '@radix-ui/react-slot';

export const Button = ({ asChild, ...props }) => {
  const Comp = asChild ? Slot : 'button';
  return <Comp {...props} />;
};
```

#### C. Headless UI Hooks
Separate logic from presentation for complex components like Toggles or Listboxes.

```tsx
function useToggle() {
  const [pressed, setPressed] = useState(false);
  return {
    pressed,
    toggleProps: {
      role: 'button',
      'aria-pressed': pressed,
      onClick: () => setPressed(!pressed),
    },
  };
}
```

### 4. Robust Theming Architecture
Implement a global `ThemeProvider` for full control over dark mode and system preferences.

```tsx
// 1. Detect system preference
const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

// 2. Apply theme to document root
document.documentElement.classList.add(isDark ? "dark" : "light");

// 3. Persist in LocalStorage
localStorage.setItem("theme", "dark");
```

---

## 🎨 The Premium Emerald Design Engine (Anti-AI Slop Policy)

Adopsi dari **Claude Frontend Design Standards** untuk menghindari estetika AI generik yang membosankan. PayU harus terasa **Bespoke**, **Luxury**, dan **Memorable**.

### 1. Unified Aesthetic Strategy
Jangan menjadi "rata-rata". Pilih arah yang berani:
- **Tone PayU**: *Luxury Refined* (Elite, Clean, Powerful).
- **Primary Color**: `bank-green` (#10b981) sebagai aksen tajam, bukan warna yang membanjiri layar.
- **Surface**: Gunakan **Glassmorphism** (`backdrop-blur`) dan **Z-axis depth** alih-alih flat design.

### 2. Typography Pairing (Bespoke Standard)
DILARANG menggunakan satu font generik (seperti Inter saja atau Arial) untuk seluruh aplikasi.
- **Headers (Display)**: WAJIB gunakan **Outfit** (Modern, Geometric, Premium).
- **Body UI**: Gunakan **Inter** (Clean, Readable) dengan *letter-spacing* yang diatur ketat (`tracking-tight` untuk headers).
- **Mixing**: Gunakan kontras ukuran yang ekstrem antara header dan sub-header untuk efek editorial.

### 3. Backgrounds & Textures
DILARANG menggunakan warna solid `#000` atau `#fff` murni tanpa tekstur.
- **Noise & Grain**: Gunakan overlay noise halus (opacity 2-3%) untuk memberikan kesan material organik.
- **Mesh Gradients**: Gunakan gradasi mesh yang subtil di sudut layar untuk kedalaman atmosfer.
- **Shadows**: Gunakan *Soft Layered Shadows* (2-3 lapis shadow dengan blur berbeda) alih-alih satu shadow pekat.

### 4. Intentional Motion (High-Impact Display)
DILARANG memberikan animasi kecil yang tersebar tidak teratur. Fokus pada "High-Impact Moments".
- **Staggered Orchestration**: Gunakan `animation-delay` untuk memunculkan elemen satu per satu saat halaman dimuat (reveal effect) alih-alih muncul sekaligus.
- **Scroll-Triggered Surprises**: Gunakan `Motion` library untuk memberikan animasi masuk yang subtil saat user scroll ke bagian tertentu.
- **Hover Micro-interactions**: Berikan feedback visual yang mengejutkan (misal: pergeseran shadow halus atau perubahan tekstur noise) saat elemen interaktif di-hover.

### 5. Editorial Spatial Composition
DILARANG menggunakan grid simetris yang membosankan tanpa variasi.
- **Grid-Breaking Elements**: Sesekali biarkan gambar atau card keluar dari garis container utama (overlap).
- **Asymmetric Balance**: Gunakan ruang negatif (whitespace) secara berani untuk menekankan elemen penting (CTA).
- **Diagonal Flow**: Gunakan kemiringan subtil atau elemen dekoratif diagonal untuk memandu mata pengguna ke arah navigasi utama.

### 6. Anti-AI Slop Checklist
JANGAN lakukan hal berikut:
- [ ] **Purple-White Gradients**: Terlalu sering digunakan oleh AI generik.
- [ ] **Cookie-Cutter Cards**: Kartu dengan border-radius kecil dan shadow standar. (Gunakan `rounded-3xl` dan shadow khusus).
- [ ] **Generic Icons**: Gunakan library yang konsisten (Lucide) dengan perlakuan visual khusus (dua warna atau gradient).
- [ ] **Symmetric Perfect Grids**: Gunakan sesekali asimetri atau elemen yang memotong garis grid (*grid-breaking*) untuk kesan desain manual.

---

## 🏗️ Component Architecture (Composition Patterns)

Adopt **Vercel's Composition Patterns** to avoid prop drilling and "boolean prop explosion".

### 1. Avoid Boolean Prop Proliferation
Never create components with excessive configuration flags (`<Card isEditing isProMode isMobile ...>`).
- **Antipattern**: One giant component with 20 `if/else`.
- **Solution**: Create **Explicit Component Variants** composed of shared primitives.
    ```tsx
    // ❌ Wrong
    <Composer isEditing isThread channelId="123" />
    
    // ✅ Right
    <ThreadComposer channelId="123" />
    <EditComposer messageId="456" />
    ```

### 2. Compound Components
Use this pattern for complex UI widgets (Dropdowns, Dialogs, Cards) to keep state flexible.
- **Rule**: Parent provides Context; Children consume it. Visual hierarchy is decoupled from logic.
    ```tsx
    // Usage
    <Composer.Provider>
        <Composer.Frame>
            <Composer.Input />
            <Composer.Footer>
              <Composer.Submit />
            </Composer.Footer>
        </Composer.Frame>
    </Composer.Provider> // Submit button works even if moved outside Frame!
    ```

### 3. Decouple State from UI
UI components should receive state via **Generic Context Interfaces**, not be hardcoded to a specific store.
- **Goal**: The same `<Composer.Input>` should work for both a "New Message" (Local State/Zustand) and "Edit Message" (Server State/ReactQuery).
- **Implementation**: Define `interface ComposerActions { submit: () => void }` and implement it in different Providers.

---

## 💎 Premium Overlays & Imperative Modal Pattern

Untuk modal dialog dan overlay yang premium, gunakan pola **Imperative API** untuk mengurangi boilerplate state. Pola ini memungkinkan pemanggilan modal langsung dari fungsi/callback tanpa mengelola state `open` secara manual di level page.

### 1. Struktur Folder (Feature-Driven)
Gunakan pendekatan mandiri untuk setiap modal besar:
```
features/
└── MyTransactionModal/
    ├── index.tsx           # Export createMyTransactionModal()
    └── TransactionContent.tsx # UI Komponen Modal
```

### 2. Implementasi Komponen Konten (`TransactionContent.tsx`)
Gunakan `framer-motion` untuk standar transisi PayU.

```tsx
'use client';

import { motion } from 'framer-motion';
import { useTranslations } from 'next-intl';

export const TransactionContent = ({ onClose }: { onClose: () => void }) => {
  const t = useTranslations('transfers');

  return (
    <div className="p-8">
      <h2 className="text-2xl font-black mb-4">{t('confirmTitle')}</h2>
      <button onClick={onClose} className="btn-emerald w-full">
        {t('submit')}
      </button>
    </div>
  );
};
```

### 3. Golden Rules Overlay PayU:
- **Backdrop**: WAJIB `bg-black/60 backdrop-blur-sm` (Glassmorphism).
- **Modal Container**: `bg-card rounded-3xl shadow-2xl overflow-hidden`.
- **Animation**: Spring physics (`stiffness: 300, damping: 25`) via Framer Motion.
- **i18n**: Gunakan `useTranslations` di dalam komponen konten untuk menjaga context.
- **Seamless Exit**: Pastikan menggunakan `AnimatePresence` untuk menghindari elemen "terputus" saat tutup.

---

## 🎭 Animation Strategy: Framer Motion vs GSAP

PayU menggunakan pendekatan sistem ganda untuk animasi guna menjamin performa dan kualitas visual:

### 1. Framer Motion (Default UI/UX)
**Gunakan untuk**: Micro-interactions, Page transitions, Modals, Sidebars, dan List animations.
- **Kelebihan**: Integrasi native dengan React lifecycle (`AnimatePresence`), deklaratif, dan performa tinggi untuk layout transitions.

### 2. GSAP (Complex Visuals & Timelines)
**Gunakan untuk**: Landing pages artistik, marketing storytelling yang berat, visualisasi data finansial yang kompleks, dan koordinasi animasi multi-elemen yang butuh sinkronisasi timeline ketat.
- **Kelebihan**: Kontrol timeline imperatif yang absolut, performa ekstrim untuk ribuan objek, dan toolset melimpah untuk SVG/Canvas.

#### 💡 GSAP Usage Pattern (React)
Selalu gunakan `useGSAP` hook (atau `useLayoutEffect` dengan `gsap.context()`) untuk pembersihan memory otomatis.

```tsx
'use client';

import { useRef } from 'react';
import { gsap } from 'gsap';
import { useGSAP } from '@gsap/react';

export const MarketingTeaser = () => {
  const container = useRef<HTMLDivElement>(null);

  useGSAP(() => {
    // Definisi Timeline yang kompleks
    const tl = gsap.timeline({ repeat: -1, yoyo: true });
    tl.to(".coin", { y: -20, rotation: 360, duration: 2, stagger: 0.2 })
      .to(".glow", { opacity: 0.8, scale: 1.2, duration: 1 }, "-=1");
  }, { scope: container });

  return (
    <div ref={container} className="relative overflow-hidden">
      {/* Visual elements */}
    </div>
  );
};
```

---

## Component Design Patterns

### 1. Form Components (React Hook Form + Zod)

```typescript
// components/forms/TransferForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button, Input, Select } from '@/components/ui';

const transferSchema = z.object({
  recipientAccount: z.string().min(10, 'Invalid account number'),
  amount: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Invalid amount'),
  description: z.string().max(100).optional(),
  sourceAccount: z.string().uuid('Invalid account ID'),
});

type TransferFormData = z.infer<typeof transferSchema>;

export const TransferForm = () => {
  const { data: accounts } = useAccounts();
  const createTransfer = useCreateTransfer();
  
  const form = useForm<TransferFormData>({
    resolver: zodResolver(transferSchema),
    defaultValues: {
      amount: '',
      description: '',
    },
  });
  
  const onSubmit = async (data: TransferFormData) => {
    try {
      await createTransfer.mutateAsync({
        recipientAccount: data.recipientAccount,
        amount: parseFloat(data.amount),
        description: data.description,
        sourceAccountId: data.sourceAccount,
      });
      form.reset();
    } catch (error) {
      // Error handling
    }
  };
  
  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
      <Select {...form.register('sourceAccount')}>
        {accounts?.map((account) => (
          <option key={account.id} value={account.id}>
            {account.accountNumber} - {account.name}
          </option>
        ))}
      </Select>
      
      <Input 
        {...form.register('recipientAccount')}
        placeholder="Recipient Account Number"
        error={form.formState.errors.recipientAccount?.message}
      />
      
      <Input 
        {...form.register('amount')}
        type="number"
        placeholder="Amount"
        error={form.formState.errors.amount?.message}
      />
      
      <Input 
        {...form.register('description')}
        placeholder="Description (optional)"
      />
      
      <Button 
        type="submit" 
        loading={createTransfer.isPending}
        disabled={!form.formState.isValid}
      >
        Transfer
      </Button>
    </form>
  );
};
```

### 2. Loading & Error States

```typescript
// components/common/DataTable.tsx
export const DataTable = ({ query, columns }: DataTableProps) => {
  if (query.isLoading) {
    return <DataTableSkeleton rows={10} />;
  }
  
  if (query.isError) {
    return (
      <ErrorState
        message="Failed to load data"
        onRetry={() => query.refetch()}
      />
    );
  }
  
  return (
    <Table>
      <TableHeader>
        {columns.map((col) => (
          <TableHead key={col.key}>{col.label}</TableHead>
        ))}
      </TableHeader>
      <TableBody>
        {query.data?.map((row) => (
          <TableRow key={row.id}>
            {columns.map((col) => (
              <TableCell key={col.key}>{col.render?.(row) ?? row[col.key]}</TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
};
```

---

## Testing Strategy

### 1. Unit Tests (Vitest)

```typescript
// tests/unit/components/TransferForm.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TransferForm } from '@/components/forms/TransferForm';

describe('TransferForm', () => {
  it('should validate required fields', async () => {
    const user = userEvent.setup();
    render(<TransferForm />);
    
    const submitButton = screen.getByRole('button', { name: /transfer/i });
    await user.click(submitButton);
    
    expect(screen.getByText('Invalid account number')).toBeInTheDocument();
    expect(screen.getByText('Invalid amount')).toBeInTheDocument();
  });
  
  it('should submit valid form data', async () => {
    const user = userEvent.setup();
    const mockCreateTransfer = vi.fn().mockResolvedValue({});
    
    render(<TransferForm />);
    
    await user.type(screen.getByPlaceholderText('Recipient Account Number'), '1234567890');
    await user.type(screen.getByPlaceholderText('Amount'), '100.50');
    await user.click(screen.getByRole('button', { name: /transfer/i }));
    
    expect(mockCreateTransfer).toHaveBeenCalledWith({
      recipientAccount: '1234567890',
      amount: 100.50,
    });
  });
});
```

### 2. E2E Tests (Playwright)

```typescript
// tests/e2e/transfer.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Money Transfer Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/login');
    await page.fill('input[name="phone"]', '+6281234567890');
    await page.fill('input[name="pin"]', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });
  
  test('should transfer money successfully', async ({ page }) => {
    await page.click('text=Transfer');
    
    await page.selectOption('select[name="sourceAccount"]', 'account-1');
    await page.fill('input[name="recipientAccount"]', '1234567890');
    await page.fill('input[name="amount"]', '100');
    await page.fill('input[name="description"]', 'Test transfer');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Transfer successful')).toBeVisible();
  });
  
  test('should show error for insufficient balance', async ({ page }) => {
    await page.click('text=Transfer');
    await page.selectOption('select[name="sourceAccount"]', 'account-1');
    await page.fill('input[name="recipientAccount"]', '1234567890');
    await page.fill('input[name="amount"]', '999999999');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Insufficient balance')).toBeVisible();
  });
});
```

---

## Performance Optimization

### 1. Code Splitting

```typescript
// Lazy load components
const TransferScreen = dynamic(() => import('@/screens/transactions/TransferScreen'), {
  loading: () => <ScreenSkeleton />,
});

const ChartComponent = dynamic(() => import('@/components/charts/LineChart'), {
  loading: () => <ChartSkeleton />,
});
```

### 2. Image Optimization

```typescript
import Image from 'next/image';

// Next.js Image component (automatic optimization)
<Image
  src="/logo.png"
  alt="PayU Logo"
  width={200}
  height={50}
  priority // For above-the-fold images
/>
```

### 3. Virtualization (Long Lists)

```typescript
import { useVirtualizer } from '@tanstack/react-virtual';

export const TransactionList = ({ transactions }) => {
  const parentRef = useRef<HTMLDivElement>(null);
  
  const rowVirtualizer = useVirtualizer({
    count: transactions.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 60,
    overscan: 5,
  });
  
  return (
    <div ref={parentRef} style={{ height: '500px', overflow: 'auto' }}>
      <div style={{ height: `${rowVirtualizer.getTotalSize()}px` }}>
        {rowVirtualizer.getVirtualItems().map((virtualRow) => (
          <TransactionItem
            key={virtualRow.key}
            transaction={transactions[virtualRow.index]}
            style={{ position: 'absolute', top: 0, left: 0, width: '100%' }}
          />
        ))}
      </div>
    </div>
  );
};
```

---

## Accessibility (A11y)

### WCAG 2.1 Level AA Compliance

| Requirement | Implementation |
|-------------|----------------|
| **Keyboard Navigation** | All interactive elements focusable, proper tab order |
| **Screen Readers** | ARIA labels, semantic HTML, alt text |
| **Color Contrast** | Minimum 4.5:1 for normal text, 3:1 for large text |
| **Form Validation** | Descriptive error messages linked to inputs |
| **Focus Indicators** | Visible focus rings, skip links |
| **Dynamic Content** | Live regions for status updates |

```typescript
// Accessible button with aria-label
<button
  aria-label="Close dialog"
  onClick={onClose}
>
  <XIcon aria-hidden="true" />
</button>

// Accessible form with error announcement
<div role="alert" aria-live="polite">
  {error && <p className="error-message">{error}</p>}
</div>
```

---

## Internationalization (i18n)

```typescript
// Using next-intl
import { useTranslations } from 'next-intl';

export const DashboardScreen = () => {
  const t = useTranslations('dashboard');
  
  return (
    <div>
      <h1>{t('welcome', { name: user.name })}</h1>
      <p>{t('accountBalance', { balance: formatCurrency(balance) })}</p>
    </div>
  );
};

// messages/en.json
{
  "dashboard": {
    "welcome": "Welcome, {name}!",
    "accountBalance": "Your account balance is {balance}"
  }
}
```

---

## Error Handling

### Global Error Boundary

```typescript
// components/ErrorBoundary.tsx
'use client';

import { Component, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }
  
  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }
  
  componentDidCatch(error: Error, errorInfo: any) {
    // Log error to monitoring service
    console.error('Error caught by boundary:', error, errorInfo);
  }
  
    render() {
    if (this.state.hasError) {
      return this.props.fallback || <ErrorFallback error={this.state.error} />;
    }
    
    return this.props.children;
  }
}
```

## 🔴 Centralized Error Code Mapping
Following the PayU engineering standard, frontends must map backend error codes to user-friendly messages.

### 1. Error Mapping Strategy
Instead of hardcoding strings, use the extracted error JSON from the backend.

```typescript
// lib/utils/error-mapper.ts
import errorCatalog from '@/constants/errors.json';

export const mapErrorCode = (code: string, locale: string = 'id'): string => {
  const entry = errorCatalog[code];
  if (!entry) return errorCatalog['GEN_500'][locale];
  return entry[locale] || entry['en'];
};
```

### 2. Implementation with i18n
Integrate your mapping with `next-intl` or your i18n framework to ensure localized error displays.

### 3. Unknown Code Logging
Always log the original `errorCode` and `traceId` to the console (development) or a monitoring service (production) if a mapping is missing.

---

## Deployment

### Next.js Deployment

```bash
# Build for production
npm run build

# Environment variables
# .env.production
NEXT_PUBLIC_API_URL=https://api.payu.id/v1
NEXT_PUBLIC_KEYCLOAK_URL=https://auth.payu.id
NEXT_PUBLIC_SENTRY_DSN=...
NEXT_PUBLIC_GA_ID=...
```

### React Native Deployment

```bash
# iOS
npm run ios:build:release

# Android
npm run android:build:release

# Environment config
# app.config.js
export default {
  expo: {
    extra: {
      apiUrl: process.env.EXPO_PUBLIC_API_URL,
      keycloakUrl: process.env.EXPO_PUBLIC_KEYCLOAK_URL,
    },
  },
};
```

---

## 🚀 Vercel Performance Shield (PayU Engineering Standard)

Standard performas kritis yang diadopsi dari **Vercel Engineering** untuk memastikan aplikasi PayU memiliki LCP dan TTI kelas dunia.

### 1. Eliminating Waterfalls (CRITICAL)
Waterfalls adalah pembunuh performa nomor satu. Setiap `await` sekuensial menambah latensi jaringan penuh.

- **Defer Await**: Pindahkan `await` ke dalam cabang logika yang benar-benar membutuhkannya.
- **Parallel All**: Gunakan `Promise.all()` untuk operasi independen.
- **Strategic Suspense**: Jangan biarkan seluruh halaman menunggu data. Gunakan `<Suspense>` untuk melakukan *streaming* konten.

```tsx
// ✅ Correct: Header dan Sidebar fetch secara simultan via komposisi
export default function Page() {
  return (
    <div>
      <Suspense fallback={<Skeleton />}><Header /></Suspense>
      <Suspense fallback={<Skeleton />}><Sidebar /></Suspense>
    </div>
  )
}
```

### 2. Bundle Size Optimization (CRITICAL)
- **No Barrel Files**: JANGAN impor dari file aggregator `index.ts` besar (seperti `lucide-react` atau `@mui/material`). Impor langsung dari path file aslinya untuk menjaga *tree-shaking*.
- **Dynamic Imports**: Gunakan `next/dynamic` untuk komponen berat (Charts, PDF, Editors) agar tidak masuk ke *initial bundle*.

### 3. Server-Side Hygiene (HIGH)
- **Minimize Serialization**: Jangan melewatkan objek besar (seperti seluruh objek `User` dengan 50 field) ke Client Component. Hanya kirim field yang digunakan (misal `name` dan `avatar`).
- **React.cache()**: Gunakan untuk deduplikasi query database atau perhitungan berat di dalam satu siklus *request*.
- **Immutability with `.toSorted()`**: Gunakan `.toSorted()`, `.toReversed()`, atau `.toSpliced()` (ES2023) untuk manipulasi array tanpa merusak state asli (immutability native).

```typescript
// ✅ Correct: Immutability tanpa copy manual
const sortedTransactions = transactions.toSorted((a, b) => b.date - a.date);
```

---

## Frontend Code Review Checklist

- [ ] **TypeScript**: Strict mode enabled, no `any` types
- [ ] **Performance**: Code splitting, lazy loading, memoization where needed
- [ ] **Accessibility**: WCAG AA compliant, keyboard navigation, ARIA labels
- [ ] **Error Handling**: Proper error boundaries, user-friendly error messages
- [ ] **Security**: No sensitive data in localStorage, proper token handling
- [ ] **Testing**: Unit tests for components, E2E tests for critical flows
- [ ] **Responsive**: Mobile-first design, tested on breakpoints
- [ ] **Form Validation**: Zod schemas, proper error messages
- [ ] **API Integration**: React Query configured, proper error handling
- [ ] **Internationalization**: All user-facing text uses translation keys

## 🤖 Agent Delegation & Parallel Execution

Untuk hasil terbaik dan tercepat, gunakan pola delegasi paralel (Swarm Mode):

- **Visual Aesthetics**: Delegasikan ke **`@styler`** untuk implementasi CSS, Gradients, dan "Premium Emerald" look.
- **Complex UI Components**: Gunakan **`@web-artifacts-builder`** -> **`@builder`** untuk membuat POC atau demo komponen interaktif secara terpisah.
- **Project Documentation/Training**: Jika butuh deck presentasi untuk fitur frontend baru, delegasikan ke **`@slidev`** -> **`@styler`**.
- **Backend Sync**: Jika butuh penyesuaian API DTO, aktifkan **`@logic-builder`** secara paralel dengan pengerjaan UI.

---

## Related Resources

| Resource | Path |
|----------|------|
| PayU Development Skill | `.agent/skills/payu-development/SKILL.md` |
| Tailwind Design System | `.agent/skills/tailwind-design-system/SKILL.md` |
| Frontend Patterns | `.agent/skills/frontend-patterns/SKILL.md` |
| Security Specialist | `.agent/skills/security-specialist/SKILL.md` |
| QA Expert | `.agent/skills/qa-expert/SKILL.md` |
| Architecture | `ARCHITECTURE.md` |

---

_Last Updated: January 2026_
