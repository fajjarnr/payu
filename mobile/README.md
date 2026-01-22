# PayU Mobile Apps

Native mobile applications for the PayU Digital Banking Platform.

## Architecture

### iOS (Swift/SwiftUI)
- **Language**: Swift 5.9+
- **UI Framework**: SwiftUI
- **Minimum iOS Version**: iOS 16.0+
- **Architecture**: MVVM with SwiftUI
- **Networking**: URLSession with async/await

### Android (Kotlin/Jetpack Compose)
- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 35 (Android 15)
- **Architecture**: MVVM with Hilt DI
- **Networking**: Retrofit + OkHttp

## Features

- **Home Dashboard**: View balance, quick actions, and recent transactions
- **Accounts Management**: View and manage multiple accounts
- **Transfers**: Send money to other accounts
- **Virtual Cards**: Manage virtual payment cards
- **Profile**: User settings and preferences

## Project Structure

```
mobile/
├── ios/
│   ├── PayU/                    # iOS App
│   │   ├── PayUApp.swift       # App entry point
│   │   ├── ContentView.swift   # Main view
│   │   ├── Views/               # SwiftUI Views
│   │   │   ├── HomeView.swift
│   │   │   ├── AccountsView.swift
│   │   │   ├── TransfersView.swift
│   │   │   ├── CardsView.swift
│   │   │   └── ProfileView.swift
│   │   ├── Models/             # Data Models
│   │   │   └── Models.swift
│   │   ├── Services/           # Business Logic
│   │   │   ├── APIClient.swift
│   │   │   └── AppState.swift
│   │   └── Info.plist
│   └── PayUTests/              # Unit Tests
│
└── android/
    ├── app/
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/payu/mobile/
    │       │   │   ├── MainActivity.kt
    │       │   │   ├── PayUApplication.kt
    │       │   │   ├── data/
    │       │   │   │   ├── api/
    │       │   │   │   ├── model/
    │       │   │   │   └── repository/
    │       │   │   ├── di/
    │       │   │   ├── ui/
    │       │   │   │   ├── screens/
    │       │   │   │   ├── theme/
    │       │   │   │   └── viewmodel/
    │       │   │   └── PayUNavigation.kt
    │       │   ├── res/
    │       │   └── AndroidManifest.xml
    │       ├── test/            # Unit Tests
    │       └── androidTest/     # Instrumentation Tests
    ├── build.gradle.kts
    └── settings.gradle.kts
```

## Getting Started

### iOS Prerequisites
- Xcode 15.0+
- iOS 16.0+ Simulator or Device
- CocoaPods (if using pods)

### Android Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17
- Android SDK API 35

### Running iOS App

```bash
cd mobile/ios/PayU
open PayU.xcodeproj
```

Or using Xcode command line:
```bash
xcodebuild -scheme PayU -destination 'platform=iOS Simulator,name=iPhone 15' build
```

### Running Android App

```bash
cd mobile/android
./gradlew assembleDebug
```

Or run directly:
```bash
./gradlew installDebug
```

## API Configuration

### iOS
Set the API base URL in `Info.plist`:
```xml
<key>api_base_url</key>
<string>http://localhost:8080/api/v1</string>
```

Or configure at runtime:
```swift
UserDefaults.standard.set("http://your-api-url", forKey: "api_base_url")
```

### Android
Configure in `NetworkModule.kt`:
```kotlin
.baseUrl("http://10.0.2.2:8080/api/v1/")
```

Note: Use `10.0.2.2` for Android emulator to access localhost.

## Testing

### iOS Tests
```bash
xcodebuild test -scheme PayU -destination 'platform=iOS Simulator,name=iPhone 15'
```

### Android Tests
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

## Building for Production

### iOS
```bash
xcodebuild -scheme PayU -configuration Release archive -archivePath build/PayU.xcarchive
xcodebuild -exportArchive -archivePath build/PayU.xcarchive -exportPath build/export
```

### Android
```bash
./gradlew assembleRelease
```

## Dependencies

### iOS
- SwiftUI (native)
- URLSession (native)

### Android
- Jetpack Compose
- Hilt for DI
- Retrofit for networking
- OkHttp for HTTP client
- DataStore for preferences
- Kotlinx Coroutines

## Security Best Practices

1. **Certificate Pinning**: Implement SSL pinning for production
2. **Token Storage**: Use Keychain (iOS) / EncryptedSharedPreferences (Android)
3. **Biometric Auth**: Face ID / Touch ID (iOS), BiometricPrompt (Android)
4. **Root/Jailbreak Detection**: Detect compromised devices
5. **Code Obfuscation**: ProGuard/R8 for Android

## Observability

Both apps are configured to emit metrics:

### iOS
- Structured logging with JSON format
- Custom metrics for API calls
- Crash reporting integration ready

### Android
- Micrometer integration ready
- Structured logging with Timber/logcat
- Crashlytics integration ready

## Contributing

1. Follow platform-specific coding standards
2. Ensure all tests pass before submitting PR
3. Add tests for new features
4. Update documentation

## License

© 2026 PayU Digital Banking | Proprietary
