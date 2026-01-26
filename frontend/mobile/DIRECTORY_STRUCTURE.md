# PayU Mobile App - Directory Structure

```
/home/ubuntu/payu/mobile/
├── app/                                    # Expo Router screens
│   ├── (auth)/                            # Authentication group
│   │   ├── _layout.tsx                    # Auth layout with redirect logic
│   │   ├── login.tsx                      # Login with OTP + Biometric
│   │   └── register.tsx                   # Progressive 5-step registration
│   ├── (tabs)/                            # Main tab navigation
│   │   ├── _layout.tsx                    # Tab bar layout
│   │   ├── index.tsx                      # Dashboard (Home)
│   │   ├── cards.tsx                      # Virtual cards management
│   │   ├── transfers.tsx                  # Transfer input screen
│   │   └── history.tsx                    # Transaction history
│   ├── _layout.tsx                        # Root layout with providers
│   ├── root.tsx                           # Root error boundary
│   ├── qris.tsx                           # QRIS scanner
│   ├── bills.tsx                          # Bill payments
│   ├── profile.tsx                        # User profile & settings
│   ├── transfer-confirm.tsx               # Transfer confirmation & PIN
│   └── feedback.tsx                       # In-app feedback
│
├── components/                             # Reusable components
│   ├── shared/                            # Shared feature components
│   │   ├── BalanceCard.tsx                # Balance display card
│   │   ├── CardPreview.tsx                # Virtual card preview
│   │   ├── QuickActions.tsx               # Quick action buttons
│   │   └── TransactionItem.tsx            # Transaction list item
│   └── ui/                                # UI components
│       ├── Avatar.tsx                     # User avatar
│       ├── Badge.tsx                      # Status badges
│       ├── Button.tsx                     # Buttons (variants)
│       ├── Card.tsx                       # Cards (elevated/outlined)
│       ├── Input.tsx                      # Form inputs
│       └── Modal.tsx                      # Modal dialogs
│
├── hooks/                                 # Custom React hooks
│   ├── useAnalytics.ts                    # Analytics & event tracking
│   ├── useAppLock.ts                      # App lock & session management
│   ├── useAuth.ts                         # Authentication state & actions
│   ├── useBiometrics.ts                   # Biometric authentication
│   ├── useCamera.ts                       # Camera & QR scanner
│   ├── useCards.ts                        # Virtual cards management
│   ├── useFeedback.ts                     # Feedback widget & surveys
│   ├── useNotifications.ts                # Push notifications
│   ├── useOfflineMode.ts                  # Offline support & sync
│   ├── useTransactions.ts                 # Transaction management
│   └── useWallet.ts                       # Wallet & pockets
│
├── store/                                 # Zustand state stores
│   ├── authStore.ts                       # Authentication state
│   ├── cardStore.ts                       # Virtual cards state
│   ├── transactionStore.ts                # Transactions state
│   └── walletStore.ts                     # Wallet & pockets state
│
├── services/                              # API services
│   ├── api.ts                             # Axios configuration
│   ├── auth.service.ts                    # Authentication API
│   ├── card.service.ts                    # Virtual cards API
│   ├── feedback.service.ts                # Feedback API
│   ├── notification.service.ts            # Notifications API
│   └── transaction.service.ts             # Transactions API
│
├── context/                               # React contexts
│   ├── AuthContext.tsx                    # Authentication provider
│   ├── NotificationContext.tsx            # Notifications provider
│   └── ThemeContext.tsx                   # Theme provider
│
├── utils/                                 # Utility functions
│   ├── currency.ts                        # IDR formatting
│   ├── date.ts                            # Date formatting
│   ├── storage.ts                         # Storage helpers
│   └── validation.ts                      # Form validation
│
├── constants/                             # App constants
│   ├── config.ts                          # API & app config
│   └── theme.ts                           # Theme colors
│
├── types/                                 # TypeScript types
│   └── index.ts                           # Type definitions
│
├── assets/                                # Static assets
│   ├── images/                            # Images
│   ├── icons/                             # Icons
│   └── fonts/                             # Custom fonts
│
├── app.json                               # Expo configuration
├── package.json                           # Dependencies
├── tsconfig.json                          # TypeScript config
├── tailwind.config.js                     # Tailwind config
├── nativewind-env.d.ts                    # NativeWind types
├── PHASE4_IMPLEMENTATION.md               # Implementation summary
└── DIRECTORY_STRUCTURE.md                 # This file
```

## File Count Summary

| Category | Count |
|----------|-------|
| Screens (app/) | 16 |
| Components | 12 |
| Hooks | 11 |
| Stores | 4 |
| Services | 6 |
| Contexts | 3 |
| Utils | 4 |
| **Total** | **56+** |

## Key Features by Directory

### `/app/` - Screens
- Authentication (login, register)
- Dashboard with balance & transactions
- Transfers with confirmation
- QRIS scanner
- Bill payments
- Virtual cards
- Profile & settings
- Feedback

### `/components/` - UI Components
- Avatar component with fallbacks
- Button variants (primary, outline, ghost)
- Card variants (elevated, outlined, flat)
- Form inputs with validation
- Modal dialogs
- Badges for status
- Balance card with animations
- Transaction items
- Quick actions grid
- Card preview with flip

### `/hooks/` - Custom Hooks
- useAuth - Authentication state
- useBiometrics - FaceID/TouchID
- useAppLock - App lock & session
- useOfflineMode - Offline support
- useAnalytics - Event tracking
- useFeedback - Feedback widget
- useNotifications - Push notifications
- useCamera - QR scanner
- useWallet - Wallet management
- useCards - Card management
- useTransactions - Transaction history

### `/store/` - State Management
- authStore - User & tokens
- walletStore - Balance & pockets
- transactionStore - Transaction history
- cardStore - Virtual cards

### `/services/` - API Integration
- auth.service - Login, register, token refresh
- transaction.service - Transfers, QRIS, bills
- card.service - Card CRUD operations
- notification.service - Push notifications
- feedback.service - User feedback
- api.ts - Axios setup with interceptors

### `/utils/` - Utilities
- currency.ts - IDR formatting
- date.ts - Date formatting
- storage.ts - Secure & regular storage
- validation.ts - Form validators

## Routing Structure

### Authentication Flow
```
/(auth)/login → Phone → OTP → Password/Biometric → /(tabs)
```

### Registration Flow
```
/(auth)/register → Personal → Phone → OTP → Password → PIN → Complete → /(tabs)
```

### Main Navigation
```
/(tabs)/index (Dashboard)
/(tabs)/transfers → /transfer-confirm
/(tabs)/cards
/(tabs)/history
/qris
/bills
/profile
/feedback
```

## State Flow

```
User Action → Hook → Store → Service → API
                    ↓
                 Component Re-render
```

## Data Flow

```
API Response → Service → Store → Hook → Component
                        ↓
                   Secure Storage
```
