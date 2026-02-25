# PayU Digital Banking Web Application

> **Modern Next.js 15 banking frontend with Premium Emerald design system**

---

## 🚀 Quick Start

### Prerequisites

- **Node.js** 22+ LTS
- **npm** or **pnpm**
- **PayU backend services** running (see [Backend Setup](#backend-setup))

### Development

```bash
# Install dependencies
npm install --legacy-peer-deps

# Start development server
npm run dev

# Open browser
# http://localhost:3001
```

### Production Build

```bash
npm run build
npm start
```

---

## 📁 Project Structure

```
web-app/
├── app/                      # Next.js App Router
│   ├── (auth)/              # Auth routes (login, register)
│   ├── dashboard/           # Dashboard
│   ├── transfer/            # Transfer & payments
│   ├── investments/         # Investment products
│   ├── lending/             # Loans & PayLater
│   ├── bills/               # Bill payments
│   ├── cards/               # Card management
│   └── settings/            # User settings
├── components/              # React components
│   ├── ui/                  # shadcn/ui components
│   ├── dashboard/           # Dashboard widgets
│   ├── forms/               # Form components
│   └── layouts/             # Layout wrappers
├── lib/                     # Utilities
│   ├── api/                 # API client functions
│   ├── hooks/               # Custom React hooks
│   └── utils/              # Helper functions
├── styles/                  # Global styles
├── public/                  # Static assets
└── e2e/                     # Playwright E2E tests
```

---

## 🎨 Design System

### Premium Emerald Theme

| Category | Token | Value |
|:---------|:------|:------|
| **Primary** | `--bank-green` | `#10b981` |
| **Background** | `--bg-gray-950` | `#0a0a0a` |
| **Surface** | `--surface-gray` | `#1a1a1a` |
| **Text Primary** | `--text-primary` | `#f3f4f6` |
| **Text Secondary** | `--text-secondary` | `#9ca3af` |
| **Border** | `--border-gray` | `#374151` |

### Typography

| Usage | Font | Weight |
|:------|:-----|:-------:|
| **Headers** | Outfit | 600-700 |
| **Body** | Inter | 400-500 |
| **Monospace** | JetBrains Mono | 400 |

### Components

Built with **shadcn/ui** + **Radix UI** primitives:
- Tabs, Switch, Slider, Stepper
- Dialog, Dropdown, Select
- Form inputs with validation
- Accessible by default (A11y compliant)

---

## 🔌 API Integration

### Environment Variables

Create `.env.local`:

```bash
# API Gateway
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080

# Feature Flags
NEXT_PUBLIC_ENABLE_INVESTMENTS=false
NEXT_PUBLIC_ENABLE_LENDING=false
```

### API Client

```typescript
// lib/api/client.ts
import { apiClient } from '@/lib/api/client';

// Example: Get account balance
const balance = await apiClient.get('/api/v1/accounts/balance');
```

### Authentication

Uses **Keycloak OAuth2**:

```typescript
// Login flow
const login = async (username: string, password: string) => {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  // Returns: { success, data: { mfa_token, ... } }
};
```

---

## 🧪 Testing

### Unit Tests

```bash
npm test                    # Jest + React Testing Library
npm test -- --watch       # Watch mode
npm test -- --coverage     # With coverage
```

### E2E Tests (Playwright)

```bash
# Install browsers first
npx playwright install --with-deps

# Run E2E tests
npm run test:e2e           # Headless
npx playwright test --ui   # With UI

# Run specific test
npx playwright test login-flow.spec.ts
```

### Accessibility Tests

```bash
# Run a11y audit
npm run test:a11y          # Using axe-core
```

---

## 📱 Responsive Design

### Breakpoints

| Device | Width | Components |
|:-------|:------|:-----------:|
| **Mobile** | < 640px | Bottom nav, card layouts |
| **Tablet** | 640px - 1024px | Adjusted grid, sidebar |
| **Desktop** | > 1024px | Full navigation, multi-column |

### Mobile-First

All components designed mobile-first with progressive enhancement for larger screens.

---

## 🔧 Development Workflow

### Code Quality

```bash
# Lint
npm run lint               # ESLint check
npm run lint:fix           # Auto-fix issues

# Format
npm run format             # Prettier format
```

### Git Hooks

Pre-commit hooks run automatically:
- ESLint
- Prettier
- Type checking

```bash
# Run manually
npm run lint-staged
```

### Type Safety

```bash
# Type check
npm run type-check         # TypeScript check
```

---

## 🐛 Troubleshooting

### Dev Server Issues

```bash
# Clear cache
rm -rf .next
rm -rf node_modules/.cache

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

### Port Already in Use

```bash
# Find process on port 3001
lsof -i :3001

# Kill process
kill -9 <PID>

# Or use different port
npm run dev -- -p 3002
```

### API Connection Issues

```bash
# Verify backend is running
curl http://localhost:8080/actuator/health

# Check CORS configuration
curl -H "Origin: http://localhost:3001" \
  -X OPTIONS http://localhost:8080/api/v1/accounts
```

---

## 📦 Deployment

### Container Deployment

```bash
# Build image
docker build -t payu-web-app .

# Run container
docker run -p 3001:3000 payu-web-app
```

### Environment Variables

Production (set in deployment platform):

```bash
NODE_ENV=production
NEXT_PUBLIC_API_URL=https://api.payu.fajjjar.my.id
```

---

## 🔗 Related Docs

| Document | Path |
|:---------|:-----|
| **Developer Onboarding** | `/docs/DEVELOPER_ONBOARDING.md` |
| **Troubleshooting** | `/docs/TROUBLESHOOTING.md` |
| **API Documentation** | http://localhost:8080/api-docs |
| **Design System** | `/docs/design/PREMIUM_EMERALD.md` |

---

## 🤝 Contributing

1. Follow **Premium Emerald** design system
2. Use **shadcn/ui** components when possible
3. Ensure **WCAG 2.1 AA** accessibility
4. Write **E2E tests** for new features
5. Follow **Conventional Commits**

---

**Built with Next.js 15, React 19, TypeScript, and Tailwind CSS**

*PayU Digital Banking Platform*
