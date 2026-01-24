import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '@/hooks/useAuth';
import { useBiometrics } from '@/hooks/useBiometrics';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { validatePhoneNumber } from '@/utils/validation';

type LoginStep = 'phone' | 'otp' | 'password' | 'biometric';

export default function LoginScreen() {
  const router = useRouter();
  const { login, isLoading } = useAuth();
  const { checkAvailability, authenticate } = useBiometrics();

  const [step, setStep] = useState<LoginStep>('phone');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{
    phoneNumber?: string;
    otp?: string;
    password?: string;
  }>({});
  const [biometricAvailable, setBiometricAvailable] = useState(false);
  const [countdown, setCountdown] = useState(0);

  // Check biometric availability on mount
  React.useEffect(() => {
    checkBiometricAvailability();
  }, []);

  // OTP countdown timer
  React.useEffect(() => {
    let interval: NodeJS.Timeout;
    if (countdown > 0) {
      interval = setInterval(() => {
        setCountdown((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [countdown]);

  const checkBiometricAvailability = async () => {
    const available = await checkAvailability();
    setBiometricAvailable(available);
  };

  const validatePhoneForm = () => {
    const newErrors: typeof errors = {};

    if (!phoneNumber.trim()) {
      newErrors.phoneNumber = 'Phone number is required';
    } else if (!validatePhoneNumber(phoneNumber)) {
      newErrors.phoneNumber = 'Please enter a valid phone number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validateOtpForm = () => {
    const newErrors: typeof errors = {};

    if (!otp.trim()) {
      newErrors.otp = 'OTP is required';
    } else if (otp.length !== 6) {
      newErrors.otp = 'OTP must be 6 digits';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validatePasswordForm = () => {
    const newErrors: typeof errors = {};

    if (!password) {
      newErrors.password = 'Password is required';
    } else if (password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleRequestOTP = async () => {
    if (!validatePhoneForm()) return;

    try {
      // Request OTP from API
      // await authService.requestOTP(phoneNumber);

      setCountdown(60); // 60 second countdown
      setStep('otp');
    } catch (error: any) {
      Alert.alert('Error', error.response?.data?.message || 'Failed to send OTP');
    }
  };

  const handleVerifyOTP = async () => {
    if (!validateOtpForm()) return;

    try {
      // Verify OTP and check if user has password
      // const response = await authService.verifyOTP(phoneNumber, otp);

      // For demo, proceed to password or biometric
      if (biometricAvailable) {
        const success = await authenticate('Authenticate to continue');
        if (success) {
          await login(phoneNumber, '');
          router.replace('/(tabs)');
        }
      } else {
        setStep('password');
      }
    } catch (error: any) {
      Alert.alert('Error', error.response?.data?.message || 'Invalid OTP');
    }
  };

  const handlePasswordLogin = async () => {
    if (!validatePasswordForm()) return;

    try {
      await login(phoneNumber, password);
      router.replace('/(tabs)');
    } catch (error: any) {
      Alert.alert('Login Failed', error.response?.data?.message || 'Invalid credentials');
    }
  };

  const handleBiometricLogin = async () => {
    const success = await authenticate('Authenticate to continue');
    if (success) {
      // Proceed with biometric login
      // await authService.biometricLogin(phoneNumber);
      router.replace('/(tabs)');
    }
  };

  const renderPhoneStep = () => (
    <View>
      <View style={styles.logoContainer}>
        <View style={styles.logo}>
          <Text style={styles.logoText}>PayU</Text>
        </View>
        <Text style={styles.tagline}>Digital Banking Made Simple</Text>
      </View>

      <Card padding="lg">
        <Text style={styles.title}>Welcome Back</Text>
        <Text style={styles.subtitle}>Enter your phone number to continue</Text>

        <Input
          label="Phone Number"
          value={phoneNumber}
          onChangeText={setPhoneNumber}
          placeholder="+62 812 3456 7890"
          keyboardType="phone-pad"
          autoCapitalize="none"
          error={errors.phoneNumber}
          onSubmitEditing={handleRequestOTP}
          returnKeyType="next"
        />

        <Button
          title="Continue"
          onPress={handleRequestOTP}
          loading={isLoading}
          fullWidth
          style={styles.continueButton}
        />

        <View style={styles.footer}>
          <Text style={styles.footerText}>Don't have an account? </Text>
          <TouchableOpacity onPress={() => router.push('/(auth)/register')}>
            <Text style={styles.link}>Sign Up</Text>
          </TouchableOpacity>
        </View>
      </Card>

      {biometricAvailable && (
        <TouchableOpacity
          style={styles.biometricButton}
          onPress={handleBiometricLogin}
        >
          <Text style={styles.biometricIcon}>👆</Text>
          <Text style={styles.biometricText}>Use Biometric Login</Text>
        </TouchableOpacity>
      )}
    </View>
  );

  const renderOTPStep = () => (
    <View>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => setStep('phone')}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Card padding="lg">
        <Text style={styles.title}>Enter OTP</Text>
        <Text style={styles.subtitle}>
          We've sent a 6-digit code to {phoneNumber}
        </Text>

        <Input
          label="OTP Code"
          value={otp}
          onChangeText={setOtp}
          placeholder="000000"
          keyboardType="number-pad"
          maxLength={6}
          error={errors.otp}
          onSubmitEditing={handleVerifyOTP}
          returnKeyType="done"
          textAlign="center"
          style={{ letterSpacing: 8 }}
        />

        <TouchableOpacity
          onPress={handleRequestOTP}
          disabled={countdown > 0}
          style={styles.resendButton}
        >
          <Text style={[
            styles.resendText,
            countdown > 0 && styles.resendTextDisabled
          ]}>
            {countdown > 0
              ? `Resend in ${countdown}s`
              : "Didn't receive? Resend OTP"}
          </Text>
        </TouchableOpacity>

        <Button
          title="Verify"
          onPress={handleVerifyOTP}
          loading={isLoading}
          fullWidth
          style={styles.continueButton}
        />
      </Card>
    </View>
  );

  const renderPasswordStep = () => (
    <View>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => setStep('otp')}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Card padding="lg">
        <Text style={styles.title}>Enter Password</Text>
        <Text style={styles.subtitle}>Complete your login</Text>

        <Input
          label="Password"
          value={password}
          onChangeText={setPassword}
          placeholder="Enter your password"
          secureTextEntry
          error={errors.password}
          onSubmitEditing={handlePasswordLogin}
          returnKeyType="done"
        />

        <TouchableOpacity style={styles.forgotPassword}>
          <Text style={styles.forgotPasswordText}>Forgot Password?</Text>
        </TouchableOpacity>

        <Button
          title="Sign In"
          onPress={handlePasswordLogin}
          loading={isLoading}
          fullWidth
          style={styles.continueButton}
        />
      </Card>
    </View>
  );

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {step === 'phone' && renderPhoneStep()}
        {step === 'otp' && renderOTPStep()}
        {step === 'password' && renderPasswordStep()}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
    paddingVertical: 40,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 32,
  },
  logo: {
    width: 80,
    height: 80,
    borderRadius: 20,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  logoText: {
    fontSize: 32,
    fontWeight: '900',
    color: '#ffffff',
  },
  tagline: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: '900',
    color: '#111827',
    marginBottom: 8,
    letterSpacing: -1,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 14,
    color: '#6b7280',
    marginBottom: 24,
    textAlign: 'center',
  },
  continueButton: {
    marginTop: 16,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 24,
  },
  footerText: {
    fontSize: 14,
    color: '#6b7280',
  },
  link: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
  },
  backButton: {
    alignSelf: 'flex-start',
    marginBottom: 16,
    paddingVertical: 8,
  },
  backText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#10b981',
  },
  resendButton: {
    alignSelf: 'center',
    marginTop: 16,
    paddingVertical: 8,
  },
  resendText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
  },
  resendTextDisabled: {
    color: '#9ca3af',
  },
  forgotPassword: {
    alignSelf: 'flex-end',
    marginTop: 12,
  },
  forgotPasswordText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
  },
  biometricButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    backgroundColor: '#f9fafb',
  },
  biometricIcon: {
    fontSize: 20,
    marginRight: 8,
  },
  biometricText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
  },
});
