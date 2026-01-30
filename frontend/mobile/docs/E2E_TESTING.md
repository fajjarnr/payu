# PayU Mobile E2E Testing with Maestro

This document provides comprehensive guidance for running and maintaining End-to-End (E2E) tests for the PayU Mobile application using [Maestro](https://maestro.mobile.dev/).

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [Environment Configuration](#environment-configuration)
- [Test Flows](#test-flows)
- [CI/CD Integration](#cicd-integration)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview

Maestro is a modern mobile UI testing framework that provides:

- **Cross-platform support**: Single test suite for iOS and Android
- **No-code/Low-code**: YAML-based test definitions
- **Fast execution**: Optimized for speed and reliability
- **Easy CI/CD integration**: Works seamlessly with GitHub Actions, Jenkins, etc.
- **Built-in assertions**: Rich set of assertion types
- **Network mocking**: Simulate offline/online states

## Prerequisites

### Required Software

- **Node.js** 18+ (for running the mobile app)
- **Maestro CLI** (latest version)
- **Android SDK** or **Xcode** (for running emulators/simulators)
- **Java** 11+ (for Android testing)

### Mobile App Requirements

- App must be built and installed on the device/emulator
- Test IDs must be added to UI components (see [Adding Test IDs](#adding-test-ids))

## Installation

### 1. Install Maestro CLI

```bash
# macOS/Linux
curl -fsSL "https://get.maestro.mobile.dev" | bash

# Or using Homebrew (macOS)
brew tap mobile-dev-inc/tap
brew install maestro

# Verify installation
maestro --version
```

### 2. Install Maestro Studio (Optional but Recommended)

Maestro Studio provides an interactive UI for writing and debugging tests:

```bash
maestro studio
```

## Project Structure

```
frontend/mobile/
├── .maestro/
│   ├── config.yaml              # Global Maestro configuration
│   ├── flows/                   # Test flow definitions
│   │   ├── login.yaml           # Login flow tests
│   │   ├── transfer.yaml        # Transfer flow tests
│   │   ├── biometric-auth.yaml  # Biometric authentication tests
│   │   └── offline-mode.yaml    # Offline functionality tests
│   └── scripts/                 # Helper scripts
│       └── mock-biometric.js    # Biometric mocking script
├── docs/
│   └── E2E_TESTING.md           # This documentation
└── package.json                 # NPM scripts for E2E
```

## Running Tests

### Available NPM Scripts

```bash
# Run all E2E tests
npm run test:e2e

# Run tests in CI mode (headless, with reporting)
npm run test:e2e:ci

# Run only smoke tests
npm run test:e2e:smoke

# Run only critical tests
npm run test:e2e:critical

# Open Maestro Studio for interactive test development
npm run maestro:studio

# Record test execution video
npm run maestro:record
```

### Direct Maestro Commands

```bash
# Run specific flow
maestro test .maestro/flows/login.yaml

# Run with environment variables
TEST_PHONE=081234567890 TEST_PIN=123456 maestro test .maestro/flows

# Run with tags
maestro test .maestro/flows --include-tags=smoke

# Run with retry
maestro test .maestro/flows --retries=3

# Generate JUnit report
maestro test .maestro/flows --format=junit --output=report.xml
```

## Environment Configuration

### Default Configuration (`.maestro/config.yaml`)

```yaml
appId: com.payu.mobile
timeouts:
  implicitWait: 5000
  explicitWait: 10000
env:
  TEST_PHONE: ${TEST_PHONE:-081234567890}
  TEST_PIN: ${TEST_PIN:-123456}
  TEST_RECIPIENT_ACCOUNT: ${TEST_RECIPIENT_ACCOUNT:-9876543210}
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `TEST_PHONE` | Phone number for login tests | `081234567890` |
| `TEST_PIN` | PIN for authentication | `123456` |
| `TEST_RECIPIENT_ACCOUNT` | Recipient account for transfers | `9876543210` |
| `BASE_URL` | API base URL | `http://localhost:8080` |

### Setting Environment Variables

```bash
# Linux/macOS
export TEST_PHONE=089876543210
export TEST_PIN=654321

# Windows (PowerShell)
$env:TEST_PHONE="089876543210"
$env:TEST_PIN="654321"
```

## Test Flows

### 1. Login Flow (`flows/login.yaml`)

Tests the complete authentication journey:

- Splash screen display
- Phone number input
- PIN entry
- Home screen navigation
- Balance visibility check

**Tags**: `smoke`, `critical`, `auth`

### 2. Transfer Flow (`flows/transfer.yaml`)

Tests money transfer functionality:

- Navigate to transfer screen
- Input amount (with validation)
- Select recipient bank
- Enter account number
- Confirm transfer details
- Success state verification

**Tags**: `smoke`, `critical`, `transfer`, `payment`

### 3. Biometric Authentication (`flows/biometric-auth.yaml`)

Tests Face ID/Touch ID/Fingerprint:

- Biometric availability check
- Enable/disable biometric in settings
- Biometric login flow
- Fallback to PIN on failure

**Tags**: `smoke`, `biometric`, `auth`, `security`

### 4. Offline Mode (`flows/offline-mode.yaml`)

Tests offline functionality:

- Network status detection
- Offline indicator display
- Cached data accessibility
- Offline queue for pending actions
- Sync when back online

**Tags**: `smoke`, `offline`, `resilience`, `critical`

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  e2e-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Maestro
        run: curl -fsSL "https://get.maestro.mobile.dev" | bash

      - name: Add Maestro to PATH
        run: echo "$HOME/.maestro/bin" >> $GITHUB_PATH

      - name: Start Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          target: google_apis
          arch: x86_64
          script: |
            # Build and install app
            cd frontend/mobile
            npm install
            npx expo prebuild
            cd android && ./gradlew assembleDebug && cd ..
            adb install android/app/build/outputs/apk/debug/app-debug.apk

            # Run E2E tests
            npm run test:e2e:ci

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: e2e-test-results
          path: frontend/mobile/maestro-reports/
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any

    environment {
        TEST_PHONE = credentials('test-phone')
        TEST_PIN = credentials('test-pin')
    }

    stages {
        stage('Setup') {
            steps {
                sh 'curl -fsSL "https://get.maestro.mobile.dev" | bash'
            }
        }

        stage('Build App') {
            steps {
                dir('frontend/mobile') {
                    sh 'npm install'
                    sh 'npx expo prebuild'
                    sh 'cd android && ./gradlew assembleDebug'
                }
            }
        }

        stage('Run E2E Tests') {
            steps {
                dir('frontend/mobile') {
                    sh 'npm run test:e2e:ci'
                }
            }
        }
    }

    post {
        always {
            publishTestResults testResultsPattern: 'frontend/mobile/maestro-reports/*.xml'
        }
    }
}
```

## Best Practices

### 1. Adding Test IDs

Always add test IDs to interactive elements:

```tsx
// Good
<TextInput
  testID="phone-input"
  value={phone}
  onChangeText={setPhone}
/>

<Button
  testID="login-button"
  title="Login"
  onPress={handleLogin}
/>

// Bad - no testID
<TextInput value={phone} onChangeText={setPhone} />
```

### 2. Writing Reliable Tests

- Use `waitForAnimationToEnd` after navigation
- Prefer `testID` over text assertions when possible
- Use `extendedWaitUntil` for dynamic content
- Add meaningful labels to assertions

```yaml
# Good
- waitForAnimationToEnd:
    timeout: 10000

- assertVisible:
    id: "home-screen"
    label: "Home screen is displayed after login"

# Bad
- assertVisible: "Home"  # May fail if text changes
```

### 3. Test Data Management

- Use environment variables for test credentials
- Create dedicated test users for E2E
- Clean up test data after tests
- Use unique identifiers for test transactions

### 4. Test Organization

- Group related tests with tags
- Use `onFlowStart` for common setup
- Keep flows focused on single user journeys
- Reuse flows with `runFlow`

### 5. Handling Flakiness

```yaml
# Add retry for flaky operations
- retry:
    maxRetries: 3
    commands:
      - tapOn:
          id: "dynamic-element"
      - assertVisible:
          id: "success-indicator"
```

## Troubleshooting

### Common Issues

#### 1. "App not installed" Error

```bash
# Ensure app is built and installed
npx expo prebuild
cd android && ./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### 2. Elements Not Found

- Verify `testID` is set correctly
- Check if element is within scroll view (may need to scroll)
- Increase timeout for slow loading elements
- Use `maestro studio` to inspect element hierarchy

#### 3. Biometric Tests Failing on Simulator

Biometric tests require special setup on simulators:

```bash
# iOS Simulator - Enable biometrics
xcrun simctl biometric enrolled

# Android Emulator - Register fingerprint
adb emu finger touch 1
```

#### 4. Network Tests Failing

Ensure airplane mode toggle is supported:

```bash
# Check if device supports airplane mode toggle
maestro test --help | grep airplane
```

### Debug Commands

```bash
# View device logs
maestro logs

# Take screenshot
maestro screenshot

# View hierarchy
maestro hierarchy

# Record video of test
maestro record .maestro/flows/login.yaml
```

### Getting Help

- [Maestro Documentation](https://maestro.mobile.dev/)
- [Maestro Slack Community](https://mobile-dev-inc.slack.com/)
- [GitHub Issues](https://github.com/mobile-dev-inc/maestro/issues)

## Contributing

When adding new E2E tests:

1. Create flow file in `.maestro/flows/`
2. Add appropriate tags
3. Update this documentation
4. Test on both iOS and Android
5. Add to CI pipeline

## License

Proprietary - PayU Digital Banking Platform
