import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Switch,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { useAuth } from '@/hooks/useAuth';
import { useAppLock } from '@/hooks/useAppLock';
import { useBiometrics } from '@/hooks/useBiometrics';
import { useFeedback } from '@/hooks/useFeedback';
import { useNotifications } from '@/hooks/useNotifications';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';

type Language = 'en' | 'id';

interface SettingItem {
  id: string;
  label: string;
  type: 'navigation' | 'toggle' | 'action';
  value?: boolean;
  onPress?: () => void;
  icon?: string;
}

export default function ProfileScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const { user, logout } = useAuth();
  const { lockEnabled, toggleAppLock, sessionTimeout, setSessionTimeout, checkJailbreak } = useAppLock();
  const { checkAvailability: checkBiometric } = useBiometrics();
  const { showFeedback } = useFeedback();
  const { permissionGranted: notificationPermission } = useNotifications();

  const [language, setLanguage] = useState<Language>('en');
  const [notificationsEnabled, setNotificationsEnabled] = useState(notificationPermission);
  const [biometricEnabled, setBiometricEnabled] = useState(false);
  const [faceIdEnabled, setFaceIdEnabled] = useState(false);

  const handleLogout = () => {
    Alert.alert(
      'Logout',
      'Are you sure you want to log out?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Logout',
          style: 'destructive',
          onPress: async () => {
            await logout();
          },
        },
      ]
    );
  };

  const handleChangePIN = () => {
    router.push('/profile/change-pin');
  };

  const handleSecurityCheck = async () => {
    const isJailbroken = await checkJailbreak();
    if (isJailbroken) {
      Alert.alert(
        'Security Warning',
        'Your device appears to be jailbroken or rooted. This may compromise the security of your account.',
        [{ text: 'OK' }]
      );
    } else {
      Alert.alert('Security Check', 'Your device appears to be secure.');
    }
  };

  const toggleNotifications = async (value: boolean) => {
    if (value && !notificationPermission) {
      Alert.alert(
        'Enable Notifications',
        'Please enable notifications in your device settings to receive transaction alerts and updates.',
        [{ text: 'OK' }]
      );
    }
    setNotificationsEnabled(value);
  };

  const toggleBiometrics = async (value: boolean) => {
    if (value) {
      const available = await checkBiometric();
      if (available) {
        setBiometricEnabled(true);
      } else {
        Alert.alert(
          'Biometric Not Available',
          'Biometric authentication is not available on this device.',
          [{ text: 'OK' }]
        );
      }
    } else {
      setBiometricEnabled(false);
    }
  };

  const settingsSections: SettingItem[][] = [
    // Account Settings
    [
      {
        id: 'personal-info',
        label: 'Personal Information',
        type: 'navigation',
        icon: '👤',
        onPress: () => router.push('/profile/personal-info'),
      },
      {
        id: 'change-pin',
        label: 'Change PIN',
        type: 'navigation',
        icon: '🔒',
        onPress: handleChangePIN,
      },
      {
        id: 'security-check',
        label: 'Security Check',
        type: 'action',
        icon: '🛡️',
        onPress: handleSecurityCheck,
      },
    ],
    // Security Settings
    [
      {
        id: 'app-lock',
        label: 'App Lock',
        type: 'toggle',
        value: lockEnabled,
        icon: '🔐',
        onPress: () => toggleAppLock(!lockEnabled),
      },
      {
        id: 'biometric',
        label: 'Biometric Login',
        type: 'toggle',
        value: biometricEnabled,
        icon: '👆',
        onPress: () => toggleBiometrics(!biometricEnabled),
      },
      {
        id: 'session-timeout',
        label: 'Auto-lock Timeout',
        type: 'navigation',
        icon: '⏱️',
        onPress: () => {
          Alert.alert(
            'Session Timeout',
            'Select auto-lock duration',
            [
              { text: '5 minutes', onPress: () => setSessionTimeout(5) },
              { text: '15 minutes', onPress: () => setSessionTimeout(15) },
              { text: '30 minutes', onPress: () => setSessionTimeout(30) },
              { text: 'Cancel', style: 'cancel' },
            ]
          );
        },
      },
    ],
    // Notification Settings
    [
      {
        id: 'notifications',
        label: 'Push Notifications',
        type: 'toggle',
        value: notificationsEnabled,
        icon: '🔔',
        onPress: () => toggleNotifications(!notificationsEnabled),
      },
      {
        id: 'notification-prefs',
        label: 'Notification Preferences',
        type: 'navigation',
        icon: '⚙️',
        onPress: () => router.push('/profile/notification-prefs'),
      },
    ],
    // App Settings
    [
      {
        id: 'language',
        label: 'Language',
        type: 'navigation',
        icon: '🌐',
        onPress: () => {
          Alert.alert(
            'Select Language',
            '',
            [
              {
                text: 'English',
                onPress: () => setLanguage('en'),
              },
              {
                text: 'Bahasa Indonesia',
                onPress: () => setLanguage('id'),
              },
              { text: 'Cancel', style: 'cancel' },
            ]
          );
        },
      },
      {
        id: 'feedback',
        label: 'Send Feedback',
        type: 'action',
        icon: '💬',
        onPress: showFeedback,
      },
      {
        id: 'help',
        label: 'Help & Support',
        type: 'navigation',
        icon: '❓',
        onPress: () => router.push('/profile/help'),
      },
      {
        id: 'about',
        label: 'About',
        type: 'navigation',
        icon: 'ℹ️',
        onPress: () => router.push('/profile/about'),
      },
    ],
  ];

  const renderSettingItem = (item: SettingItem) => (
    <TouchableOpacity
      key={item.id}
      style={[styles.settingItem, { borderBottomColor: colors.border }]}
      onPress={item.onPress}
      activeOpacity={0.7}
    >
      <View style={styles.settingLeft}>
        {item.icon && <Text style={styles.settingIcon}>{item.icon}</Text>}
        <Text style={[styles.settingLabel, { color: colors.text }]}>
          {item.label}
        </Text>
      </View>

      <View style={styles.settingRight}>
        {item.type === 'toggle' && (
          <Switch
            value={item.value}
            onValueChange={item.onPress}
            trackColor={{ false: '#d1d5db', true: '#10b981' }}
            thumbColor="#ffffff"
          />
        )}
        {item.type === 'navigation' && (
          <Text style={[styles.settingArrow, { color: colors.textSecondary }]}>
            ›
          </Text>
        )}
      </View>
    </TouchableOpacity>
  );

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      {/* Profile Header */}
      <Card padding="lg" style={styles.profileCard}>
        <View style={styles.profileHeader}>
          <Avatar
            src={user?.avatar}
            name={user?.fullName || 'User'}
            size={80}
          />
          <View style={styles.profileInfo}>
            <Text style={[styles.profileName, { color: colors.text }]}>
              {user?.fullName || 'User Name'}
            </Text>
            <Text style={[styles.profileEmail, { color: colors.textSecondary }]}>
              {user?.email || 'user@email.com'}
            </Text>
            <Text style={[styles.profilePhone, { color: colors.textSecondary }]}>
              {user?.phoneNumber || '+62 812 3456 7890'}
            </Text>
          </View>
        </View>

        <View style={styles.profileStats}>
          <View style={styles.statItem}>
            <Text style={[styles.statValue, { color: colors.text }]}>
              {user?.phoneNumber ? `•••• ${user.phoneNumber.slice(-4)}` : '•••• 1234'}
            </Text>
            <Text style={[styles.statLabel, { color: colors.textSecondary }]}>
              Account Number
            </Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={[styles.statValue, { color: colors.text }]}>
              {language.toUpperCase()}
            </Text>
            <Text style={[styles.statLabel, { color: colors.textSecondary }]}>
              Language
            </Text>
          </View>
        </View>
      </Card>

      {/* Settings Sections */}
      {settingsSections.map((section, sectionIndex) => (
        <Card key={sectionIndex} padding="none" style={styles.settingsCard}>
          {section.map((item) => renderSettingItem(item))}
        </Card>
      ))}

      {/* Logout Button */}
      <Button
        title="Log Out"
        onPress={handleLogout}
        variant="outline"
        fullWidth
        style={styles.logoutButton}
      />

      {/* Version Info */}
      <Text style={[styles.version, { color: colors.textSecondary }]}>
        PayU Mobile v1.0.0
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  content: {
    padding: 20,
  },
  profileCard: {
    marginBottom: 24,
  },
  profileHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 24,
  },
  profileInfo: {
    flex: 1,
    marginLeft: 16,
  },
  profileName: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 4,
  },
  profileEmail: {
    fontSize: 14,
    marginBottom: 2,
  },
  profilePhone: {
    fontSize: 14,
  },
  profileStats: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
  },
  statDivider: {
    width: 1,
    backgroundColor: '#e5e7eb',
  },
  settingsCard: {
    marginBottom: 16,
    overflow: 'hidden',
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
  },
  settingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  settingIcon: {
    fontSize: 20,
    marginRight: 12,
  },
  settingLabel: {
    fontSize: 16,
    fontWeight: '500',
  },
  settingRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  settingArrow: {
    fontSize: 20,
    fontWeight: '300',
  },
  logoutButton: {
    marginTop: 8,
    marginBottom: 16,
  },
  version: {
    fontSize: 12,
    textAlign: 'center',
    marginBottom: 24,
  },
});
