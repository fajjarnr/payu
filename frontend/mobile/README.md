# PayU Mobile App (Expo)

Digital banking mobile application for the PayU Platform built with Expo and React Native.

## 🚀 Architecture

- **Framework**: [Expo](https://expo.dev/) (SDK 52+)
- **Language**: TypeScript
- **Navigation**: [Expo Router](https://docs.expo.dev/router/introduction/) (File-based routing)
- **Styling**: NativeWind (Tailwind CSS for React Native)
- **State Management**: React Query (TanStack Query) for server state, Zustand for client state
- **API Client**: Axios with interceptors for JWT management
- **Design System**: Premium Emerald (Emerald Green #10b981)

## 📁 Project Structure

```text
mobile/
├── app/                  # Expo Router directory (screens & layouts)
│   ├── (auth)/           # Authentication flow (Login, Register)
│   ├── (tabs)/           # Main dashboard tabs (Home, Transfers, Cards, History)
│   ├── _layout.tsx       # Root layout & providers
│   ├── qris.tsx          # QRIS scanner
│   └── feedback.tsx      # Feedback screen
├── components/           # Reusable UI components (Emerald Design System)
│   ├── ui/               # Primary UI elements (Buttons, Inputs, Cards, Modal, Badge, Avatar)
│   └── shared/           # Business-specific shared components (BalanceCard, QuickActions, TransactionItem, CardPreview)
├── constants/            # Theme, Colors, and Config
├── context/              # React Context providers (Theme, Auth, Notification)
├── hooks/                # Custom React hooks (useAuth, useCards, useBiometrics, useNotifications, useCamera)
├── src/hooks/            # React Query hooks (useWalletQuery, useTransactionQuery, useAuthQuery)
├── services/             # API services (api, auth, wallet, transaction, card, notification, feedback)
├── store/                # Client state only (authStore, cardStore, uiStore)
│                        # Note: walletStore and transactionStore are deprecated
├── types/                # TypeScript definitions
└── utils/                # Helper functions (currency, date, validation, storage)
```

## 🛠️ Getting Started

### Prerequisites

- Node.js 20+
- pnpm or yarn or npm
- [Expo Go](https://expo.dev/go) app on your mobile device OR Android/iOS Emulator

### Installation

1. Navigate to the mobile directory:
   ```bash
   cd mobile
   ```

2. Install dependencies:
   ```bash
   pnpm install
   ```

3. Start the development server:
   ```bash
   pnpm start
   ```

4. Open the app:
   - Scan the QR code with **Expo Go** (Android/iOS)
   - Press `i` for iOS simulator
   - Press `a` for Android emulator

## 🔒 Security Features

- **Secure Storage**: JWT and sensitive data stored via `expo-secure-store`.
- **Bio-Authentication**: Ready for FaceID/Fingerprint integration via `expo-local-authentication`.
- **SSL Pinning**: Configured for production environments.

## ⚡ Performance Optimizations (P2-C5, P2-C6)

### List Virtualization
- **FlashList** (@shopify/flash-list) for all transaction lists and card lists
- Superior performance compared to FlatList with efficient cell recycling
- Configured `estimatedItemSize`, `getItemType`, and `keyExtractor` for optimal rendering

### Component Memoization
All UI and shared components use `React.memo` with custom comparison functions:
- `BalanceCard`, `TransactionItem`, `QuickActions`, `CardPreview`
- `Button`, `Input`, `Card`, `Badge`, `Avatar`, `Modal`

### Hook Optimizations
- `useMemo` for expensive computations (currency formatting, style objects)
- `useCallback` for event handlers and render props
- Mount checks to prevent state updates after unmount

### Request Cancellation
- Automatic request deduplication via `AbortController`
- `cancelAllRequests()` method for cleanup
- Idempotency key tracking for financial operations

### SecureStore Optimization
- Parallel operations with `Promise.all` (`getMany`, `setMany`, `removeMany`)
- Key tracking for efficient bulk operations

See [docs/PERFORMANCE-OPTIMIZATIONS.md](./docs/PERFORMANCE-OPTIMIZATIONS.md) for details.

## 🎨 Design System

Following the **PayU Premium Emerald** guidelines:
- **Primary**: `#10b981` (bank-green)
- **Secondary**: `#059669` (bank-emerald)
- **Background**: White (Light) / Gray-950 (Dark)
- **Typography**: System fonts
- **Border Radius**: 12-20px for cards, 16px for buttons
- **Shadows**: Emerald-tinted shadows for depth

## 🧪 Testing

```bash
# Run unit tests
pnpm test

# Run linting
pnpm lint
```

## 📱 Screens

### Authentication Flow
- **Login** - Email/phone login with password
- **Register** - User registration with validation

### Main App (Tabs)
- **Home** - Dashboard with balance, quick actions, recent transactions
- **Transfers** - Bank transfer functionality
- **Cards** - Virtual card management
- **History** - Transaction history with date grouping

### Additional Screens
- **QRIS Scanner** - Scan QR codes for payments
- **Feedback** - In-app feedback widget

## 🔑 Key Features

### State Management
**Server State (React Query/TanStack Query)**
- `useWalletQuery` - Primary wallet, wallets list, pocket operations
- `useTransactionQuery` - Transaction history, transfers, top-up, QRIS payments
- `useAuthQuery` - Authentication operations

**Client State (Zustand)**
- **Auth Store** - User authentication, tokens, session management
- **Card Store** - Virtual cards, freeze/unfreeze, spending limits
- **UI Store** - Theme, language, client-side UI settings

**Deprecated (v1.2.0 - will be removed in v2.0.0)**
- ~~Wallet Store~~ - Use `useWalletQuery` instead
- ~~Transaction Store~~ - Use `useTransactionQuery` instead

### Custom Hooks (Client-side)
- `useAuth()` - Authentication operations
- `useCards()` - Virtual card management
- `useBiometrics()` - Biometric authentication
- `useNotifications()` - Push notifications
- `useCamera()` - Camera operations

### React Query Hooks (Server-side)
- `usePrimaryWallet()` - Fetch primary wallet with caching
- `useWallets()` - Fetch all wallets
- `useCreatePocket()` - Create new pocket (with optimistic update)
- `useTransferToPocket()` - Transfer between pockets (with optimistic update)
- `useInfiniteTransactions()` - Paginated transaction history
- `useCreateTransfer()` - Create transfer (with optimistic update)
- `useTopUp()` - Top-up wallet (with optimistic update)
- `usePayQRIS()` - QRIS payment (with optimistic update)

### Services
- **API Client** - Axios with JWT refresh interceptor
- **Auth Service** - Login, register, logout, password reset
- **Wallet Service** - Wallet operations, pocket management
- **Transaction Service** - Transfers, payments, QRIS
- **Card Service** - Virtual card CRUD, freeze/unfreeze
- **Notification Service** - Push notification registration
- **Feedback Service** - Feedback submission

### Components

#### UI Components
- `Button` - Primary, secondary, outline, ghost variants
- `Input` - Text input with validation
- `Card` - Elevated, outlined, flat variants
- `Modal` - Bottom sheet modal
- `Badge` - Status badges
- `Avatar` - User avatar with initials

#### Shared Components
- `BalanceCard` - Balance display with show/hide toggle
- `QuickActions` - Action grid for common operations
- `TransactionItem` - Transaction list item
- `CardPreview` - Virtual card preview

## 🔗 API Integration

The app connects to the PayU backend API:
- **Base URL**: Configurable via `API_CONFIG.BASE_URL`
- **Authentication**: JWT with automatic refresh
- **Endpoints**: Auth, Wallet, Transaction, Card, Notification, Feedback

## 📝 Environment Configuration

Update `constants/config.ts` for your environment:
```typescript
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api', // Development
  // BASE_URL: 'https://api.payu.fajjjar.my.id/api', // Production
  TIMEOUT: 30000,
};
```

---

© 2026 PayU Digital Banking | All Rights Reserved
