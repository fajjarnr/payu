# Mobile Architecture Patterns Reference

## Offline-First Architecture

### Local Database Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                    OFFLINE-FIRST ARCHITECTURE                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐    │
│  │    UI Layer  │────►│  Repository  │────►│    Local     │    │
│  │   (React)    │     │   Pattern    │     │   Database   │    │
│  └──────────────┘     └──────────────┘     │  (SQLite/    │    │
│                              │              │  WatermelonDB)│   │
│                              ▼              └──────────────┘    │
│                       ┌──────────────┐             │            │
│                       │    Sync      │             │            │
│                       │   Manager    │◄────────────┘            │
│                       └──────┬───────┘                          │
│                              │                                   │
│                              ▼                                   │
│                       ┌──────────────┐                          │
│                       │   Backend    │                          │
│                       │     API      │                          │
│                       └──────────────┘                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### WatermelonDB Implementation (React Native)

```typescript
// schema.ts - Database Schema Definition
import { appSchema, tableSchema } from '@nozbe/watermelondb';

export const schema = appSchema({
  version: 1,
  tables: [
    tableSchema({
      name: 'transactions',
      columns: [
        { name: 'transaction_id', type: 'string', isIndexed: true },
        { name: 'account_id', type: 'string', isIndexed: true },
        { name: 'amount', type: 'number' },
        { name: 'type', type: 'string' }, // DEBIT, CREDIT
        { name: 'description', type: 'string' },
        { name: 'status', type: 'string' }, // PENDING, SYNCED, FAILED
        { name: 'created_at', type: 'number' },
        { name: 'synced_at', type: 'number', isOptional: true },
        { name: 'server_id', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'accounts',
      columns: [
        { name: 'account_number', type: 'string', isIndexed: true },
        { name: 'balance', type: 'number' },
        { name: 'balance_updated_at', type: 'number' },
        { name: 'sync_status', type: 'string' },
      ],
    }),
    tableSchema({
      name: 'sync_queue',
      columns: [
        { name: 'entity_type', type: 'string' },
        { name: 'entity_id', type: 'string' },
        { name: 'operation', type: 'string' }, // CREATE, UPDATE, DELETE
        { name: 'payload', type: 'string' },
        { name: 'retry_count', type: 'number' },
        { name: 'created_at', type: 'number' },
      ],
    }),
  ],
});

// models/Transaction.ts
import { Model } from '@nozbe/watermelondb';
import { field, date, readonly } from '@nozbe/watermelondb/decorators';

export default class Transaction extends Model {
  static table = 'transactions';
  
  @field('transaction_id') transactionId!: string;
  @field('account_id') accountId!: string;
  @field('amount') amount!: number;
  @field('type') type!: 'DEBIT' | 'CREDIT';
  @field('description') description!: string;
  @field('status') status!: 'PENDING' | 'SYNCED' | 'FAILED';
  @readonly @date('created_at') createdAt!: Date;
  @date('synced_at') syncedAt?: Date;
  @field('server_id') serverId?: string;
  
  async markAsSynced(serverId: string) {
    await this.update(txn => {
      txn.status = 'SYNCED';
      txn.serverId = serverId;
      txn.syncedAt = new Date();
    });
  }
}
```

### Sync Manager

```typescript
// services/SyncManager.ts
import NetInfo from '@react-native-community/netinfo';
import { database } from './database';
import { apiClient } from './apiClient';

interface SyncResult {
  success: boolean;
  synced: number;
  failed: number;
  errors: string[];
}

export class SyncManager {
  private isSyncing = false;
  private syncInterval: NodeJS.Timeout | null = null;
  
  async startBackgroundSync(intervalMs = 30000): Promise<void> {
    // Listen for network changes
    NetInfo.addEventListener(state => {
      if (state.isConnected && !this.isSyncing) {
        this.syncPendingChanges();
      }
    });
    
    // Periodic sync
    this.syncInterval = setInterval(() => {
      this.syncPendingChanges();
    }, intervalMs);
  }
  
  async syncPendingChanges(): Promise<SyncResult> {
    if (this.isSyncing) {
      return { success: false, synced: 0, failed: 0, errors: ['Sync already in progress'] };
    }
    
    const netInfo = await NetInfo.fetch();
    if (!netInfo.isConnected) {
      return { success: false, synced: 0, failed: 0, errors: ['No network connection'] };
    }
    
    this.isSyncing = true;
    const result: SyncResult = { success: true, synced: 0, failed: 0, errors: [] };
    
    try {
      // 1. Push local changes to server
      await this.pushChanges(result);
      
      // 2. Pull remote changes from server
      await this.pullChanges(result);
      
    } catch (error) {
      result.success = false;
      result.errors.push(error instanceof Error ? error.message : 'Unknown error');
    } finally {
      this.isSyncing = false;
    }
    
    return result;
  }
  
  private async pushChanges(result: SyncResult): Promise<void> {
    const syncQueue = database.get<SyncQueueItem>('sync_queue');
    const pendingItems = await syncQueue.query().fetch();
    
    for (const item of pendingItems) {
      try {
        const response = await apiClient.syncEntity({
          entityType: item.entityType,
          entityId: item.entityId,
          operation: item.operation,
          payload: JSON.parse(item.payload),
        });
        
        // Mark original entity as synced
        await this.markEntitySynced(item.entityType, item.entityId, response.serverId);
        
        // Remove from sync queue
        await item.destroyPermanently();
        result.synced++;
        
      } catch (error) {
        await item.update(i => {
          i.retryCount++;
        });
        
        if (item.retryCount >= 5) {
          // Move to dead letter queue or notify user
          await this.handleSyncFailure(item, error);
        }
        
        result.failed++;
        result.errors.push(`Failed to sync ${item.entityType}:${item.entityId}`);
      }
    }
  }
  
  private async pullChanges(result: SyncResult): Promise<void> {
    const lastSyncTimestamp = await this.getLastSyncTimestamp();
    
    const changes = await apiClient.getChanges({
      since: lastSyncTimestamp,
      entityTypes: ['transactions', 'accounts'],
    });
    
    await database.write(async () => {
      for (const change of changes.items) {
        await this.applyChange(change);
        result.synced++;
      }
    });
    
    await this.setLastSyncTimestamp(changes.timestamp);
  }
  
  private async applyChange(change: RemoteChange): Promise<void> {
    const collection = database.get(change.entityType);
    
    switch (change.operation) {
      case 'CREATE':
      case 'UPDATE':
        try {
          const existing = await collection.find(change.entityId);
          await existing.update(record => {
            Object.assign(record, change.data);
            record.syncStatus = 'SYNCED';
          });
        } catch {
          // Record doesn't exist, create it
          await collection.create(record => {
            Object.assign(record, change.data);
            record.syncStatus = 'SYNCED';
          });
        }
        break;
        
      case 'DELETE':
        try {
          const toDelete = await collection.find(change.entityId);
          await toDelete.markAsDeleted();
        } catch {
          // Already deleted, ignore
        }
        break;
    }
  }
}
```

---

## Conflict Resolution Strategies

### Last-Write-Wins (LWW)

```typescript
// Simple but may lose data
function resolveConflictLWW(local: Entity, remote: Entity): Entity {
  return local.updatedAt > remote.updatedAt ? local : remote;
}
```

### Server-Wins

```typescript
// Server is always authoritative
function resolveConflictServerWins(local: Entity, remote: Entity): Entity {
  return {
    ...remote,
    localChanges: local.updatedAt > remote.syncedAt ? local : undefined,
  };
}
```

### Merge Strategy (Recommended for Financial Apps)

```typescript
interface ConflictResolution {
  resolved: Entity;
  conflicts: ConflictDetail[];
  requiresUserAction: boolean;
}

function resolveTransactionConflict(
  local: Transaction,
  remote: Transaction
): ConflictResolution {
  // For financial transactions, NEVER auto-resolve
  if (local.amount !== remote.amount) {
    return {
      resolved: remote, // Server is authoritative for amounts
      conflicts: [{
        field: 'amount',
        localValue: local.amount,
        remoteValue: remote.amount,
        resolution: 'SERVER_WINS',
      }],
      requiresUserAction: true, // Show notification to user
    };
  }
  
  // For non-critical fields, merge
  return {
    resolved: {
      ...remote,
      description: local.description || remote.description, // Prefer local description
    },
    conflicts: [],
    requiresUserAction: false,
  };
}
```

---

## Biometric Authentication

### Expo SecureStore + Biometrics

```typescript
// services/BiometricAuth.ts
import * as LocalAuthentication from 'expo-local-authentication';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

export interface BiometricCapability {
  isAvailable: boolean;
  biometricType: 'fingerprint' | 'face' | 'iris' | 'none';
  isEnrolled: boolean;
}

export class BiometricAuthService {
  
  async checkCapability(): Promise<BiometricCapability> {
    const isAvailable = await LocalAuthentication.hasHardwareAsync();
    const isEnrolled = await LocalAuthentication.isEnrolledAsync();
    const supportedTypes = await LocalAuthentication.supportedAuthenticationTypesAsync();
    
    let biometricType: BiometricCapability['biometricType'] = 'none';
    if (supportedTypes.includes(LocalAuthentication.AuthenticationType.FACIAL_RECOGNITION)) {
      biometricType = 'face';
    } else if (supportedTypes.includes(LocalAuthentication.AuthenticationType.FINGERPRINT)) {
      biometricType = 'fingerprint';
    } else if (supportedTypes.includes(LocalAuthentication.AuthenticationType.IRIS)) {
      biometricType = 'iris';
    }
    
    return { isAvailable, biometricType, isEnrolled };
  }
  
  async authenticate(reason: string): Promise<{ success: boolean; error?: string }> {
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: reason,
        fallbackLabel: 'Use PIN',
        cancelLabel: 'Cancel',
        disableDeviceFallback: false,
      });
      
      return {
        success: result.success,
        error: result.error,
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Biometric authentication failed',
      };
    }
  }
  
  // Store sensitive data with biometric protection
  async storeSecure(key: string, value: string): Promise<void> {
    const options: SecureStore.SecureStoreOptions = {
      keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
    };
    
    await SecureStore.setItemAsync(key, value, options);
  }
  
  // Retrieve data with biometric verification
  async getSecureWithBiometric(key: string, reason: string): Promise<string | null> {
    const authResult = await this.authenticate(reason);
    if (!authResult.success) {
      throw new Error(authResult.error || 'Authentication failed');
    }
    
    return await SecureStore.getItemAsync(key);
  }
}

// Token storage with biometric protection
export class SecureTokenStorage {
  private biometricAuth = new BiometricAuthService();
  
  async storeTokens(accessToken: string, refreshToken: string): Promise<void> {
    await this.biometricAuth.storeSecure('access_token', accessToken);
    await this.biometricAuth.storeSecure('refresh_token', refreshToken);
  }
  
  async getAccessToken(): Promise<string | null> {
    return await this.biometricAuth.getSecureWithBiometric(
      'access_token',
      'Authenticate to access your account'
    );
  }
  
  async clearTokens(): Promise<void> {
    await SecureStore.deleteItemAsync('access_token');
    await SecureStore.deleteItemAsync('refresh_token');
  }
}
```

### Device Binding

```typescript
// services/DeviceBinding.ts
import * as Application from 'expo-application';
import * as Crypto from 'expo-crypto';
import * as Device from 'expo-device';

export interface DeviceInfo {
  deviceId: string;
  deviceName: string;
  platform: 'ios' | 'android';
  osVersion: string;
  appVersion: string;
  isPhysicalDevice: boolean;
}

export class DeviceBindingService {
  
  async getDeviceInfo(): Promise<DeviceInfo> {
    const installationId = await Application.getInstallationIdAsync();
    
    return {
      deviceId: await this.generateDeviceFingerprint(installationId),
      deviceName: Device.deviceName || 'Unknown Device',
      platform: Device.osName?.toLowerCase() === 'ios' ? 'ios' : 'android',
      osVersion: Device.osVersion || 'unknown',
      appVersion: Application.nativeApplicationVersion || '1.0.0',
      isPhysicalDevice: Device.isDevice,
    };
  }
  
  private async generateDeviceFingerprint(installationId: string): Promise<string> {
    // Combine multiple device attributes for stable fingerprint
    const components = [
      installationId,
      Device.brand,
      Device.modelName,
      Device.osName,
    ].filter(Boolean).join('|');
    
    const hash = await Crypto.digestStringAsync(
      Crypto.CryptoDigestAlgorithm.SHA256,
      components
    );
    
    return hash.substring(0, 32);
  }
  
  async registerDevice(): Promise<{ deviceToken: string }> {
    const deviceInfo = await this.getDeviceInfo();
    
    // Register with backend
    const response = await apiClient.post('/devices/register', {
      ...deviceInfo,
      pushToken: await this.getPushToken(),
    });
    
    // Store device token securely
    await SecureStore.setItemAsync('device_token', response.deviceToken);
    
    return { deviceToken: response.deviceToken };
  }
  
  async validateDeviceBinding(): Promise<boolean> {
    const storedToken = await SecureStore.getItemAsync('device_token');
    const currentDeviceInfo = await this.getDeviceInfo();
    
    try {
      const response = await apiClient.post('/devices/validate', {
        deviceToken: storedToken,
        currentDeviceId: currentDeviceInfo.deviceId,
      });
      
      return response.isValid;
    } catch {
      return false;
    }
  }
}
```

---

## Security Best Practices

### Certificate Pinning

```typescript
// React Native with Axios
import axios from 'axios';
import RNSSLPinning from 'react-native-ssl-pinning';

const createPinnedClient = () => {
  return {
    async fetch(url: string, options: RequestInit = {}): Promise<Response> {
      const response = await RNSSLPinning.fetch(url, {
        method: options.method || 'GET',
        headers: options.headers as Record<string, string>,
        body: options.body as string,
        sslPinning: {
          certs: ['api_payu_id'], // Certificate name in assets
        },
        timeoutInterval: 30000,
      });
      
      return new Response(response.bodyString, {
        status: response.status,
        headers: response.headers,
      });
    },
  };
};
```

### Jailbreak/Root Detection

```typescript
// services/SecurityChecks.ts
import JailMonkey from 'jail-monkey';

export class SecurityChecks {
  
  static async validateDeviceSecurity(): Promise<{
    isSecure: boolean;
    issues: string[];
  }> {
    const issues: string[] = [];
    
    // Check for jailbreak/root
    if (JailMonkey.isJailBroken()) {
      issues.push('DEVICE_ROOTED');
    }
    
    // Check for debugging
    if (JailMonkey.isDebuggedMode()) {
      issues.push('DEBUG_MODE_ENABLED');
    }
    
    // Check for mock location
    if (await JailMonkey.canMockLocation()) {
      issues.push('MOCK_LOCATION_ENABLED');
    }
    
    // Check for ADB (Android only)
    if (JailMonkey.AdbEnabled()) {
      issues.push('ADB_ENABLED');
    }
    
    return {
      isSecure: issues.length === 0,
      issues,
    };
  }
  
  static async enforceSecurityPolicy(): Promise<void> {
    const { isSecure, issues } = await this.validateDeviceSecurity();
    
    if (!isSecure) {
      // Log to analytics
      await analytics.track('security_policy_violation', { issues });
      
      // For banking apps, may want to restrict functionality
      if (issues.includes('DEVICE_ROOTED')) {
        throw new Error('This app cannot run on rooted/jailbroken devices');
      }
    }
  }
}
```

### Secure Keyboard

```typescript
// Prevent screenshot and screen recording for sensitive inputs
import { Platform } from 'react-native';
import RNPreventScreenshot from 'react-native-prevent-screenshot';

export const useSecureScreen = (isSecure: boolean) => {
  useEffect(() => {
    if (isSecure) {
      // Android: Prevent screenshots
      if (Platform.OS === 'android') {
        RNPreventScreenshot.enabled(true);
      }
      
      return () => {
        if (Platform.OS === 'android') {
          RNPreventScreenshot.enabled(false);
        }
      };
    }
  }, [isSecure]);
};

// Custom PIN input that uses secure text entry
export const SecurePinInput: React.FC<{
  onComplete: (pin: string) => void;
  length?: number;
}> = ({ onComplete, length = 6 }) => {
  const [pin, setPin] = useState('');
  
  useSecureScreen(true);
  
  return (
    <View>
      <TextInput
        value={pin}
        onChangeText={(text) => {
          const cleaned = text.replace(/[^0-9]/g, '').slice(0, length);
          setPin(cleaned);
          if (cleaned.length === length) {
            onComplete(cleaned);
          }
        }}
        keyboardType="numeric"
        secureTextEntry
        maxLength={length}
        autoComplete="off"
        textContentType="oneTimeCode"
        importantForAutofill="no"
        autoCorrect={false}
      />
      {/* Visual PIN dots */}
      <View style={styles.dotsContainer}>
        {Array.from({ length }).map((_, i) => (
          <View
            key={i}
            style={[
              styles.dot,
              i < pin.length && styles.dotFilled,
            ]}
          />
        ))}
      </View>
    </View>
  );
};
```

---

## Performance Optimization

### Image Caching

```typescript
import FastImage from 'react-native-fast-image';

// Preload important images
FastImage.preload([
  { uri: 'https://cdn.payu.fajjjar.my.id/assets/logo.png' },
  { uri: 'https://cdn.payu.fajjjar.my.id/assets/promo-banner.jpg' },
]);

// Usage with caching
<FastImage
  style={{ width: 200, height: 200 }}
  source={{
    uri: imageUrl,
    priority: FastImage.priority.high,
    cache: FastImage.cacheControl.immutable,
  }}
  resizeMode={FastImage.resizeMode.cover}
/>
```

### List Virtualization

```typescript
import { FlashList } from "@shopify/flash-list";

const TransactionList: React.FC<{ transactions: Transaction[] }> = ({ 
  transactions 
}) => {
  return (
    <FlashList
      data={transactions}
      renderItem={({ item }) => <TransactionItem transaction={item} />}
      estimatedItemSize={80}
      keyExtractor={(item) => item.id}
      getItemType={(item) => item.type} // Optimize for different item types
      onEndReached={loadMore}
      onEndReachedThreshold={0.5}
    />
  );
};
```

### Bundle Optimization

```javascript
// metro.config.js
module.exports = {
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: true, // Enable inline requires
      },
    }),
  },
};
```
