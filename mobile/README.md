# PayU Mobile App (Expo)

Digital banking mobile application for the PayU Platform built with Expo and React Native.

## 🚀 Architecture

- **Framework**: [Expo](https://expo.dev/) (SDK 52+)
- **Language**: TypeScript
- **Navigation**: [Expo Router](https://docs.expo.dev/router/introduction/) (File-based routing)
- **Styling**: NativeWind (Tailwind CSS for React Native)
- **State Management**: Zustand
- **API Client**: Axios with interceptors for JWT management
- **Design System**: Premium Emerald (Emerald Green #10b981)

## 📁 Project Structure

```text
mobile/
├── app/                  # Expo Router directory (screens & layouts)
│   ├── (auth)/           # Authentication flow (Login, Register, eKYC)
│   ├── (tabs)/           # Main dashboard tabs (Home, Transfers, Cards, History)
│   ├── _layout.tsx       # Root layout & providers
│   └── index.tsx          # Initial entry point
├── components/           # Reusable UI components (Emerald Design System)
│   ├── ui/               # Primary UI elements (Buttons, Inputs, Cards)
│   └── shared/           # Business-specific shared components
├── constants/            # Theme, Colors, and Config
├── hooks/                # Custom React hooks (useAuth, useWallet)
├── services/             # API services (api.ts, auth.service.ts)
├── store/                # Global state (authStore, walletStore)
├── types/                # TypeScript definitions
└── utils/                # Helper functions (formatting, validation)
```

## 🛠️ Getting Started

### Prerequisites

- Node.js 20+
- pnpm or yarn
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

## 🎨 Design System

Following the **PayU Premium Emerald** guidelines:
- **Primary**: `#10b981` (bank-green)
- **Secondary**: `#059669` (bank-emerald)
- **Background**: White (Light) / Gray-950 (Dark)
- **Typography**: Inter / Outfit via Google Fonts

## 🧪 Testing

```bash
# Run unit tests
pnpm test

# Run linting
pnpm lint
```

---

© 2026 PayU Digital Banking | All Rights Reserved
