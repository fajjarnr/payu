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
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import {
  validateEmail,
  validatePhoneNumber,
  validatePassword,
} from '@/utils/validation';

type RegistrationStep = 'personal' | 'phone' | 'otp' | 'password' | 'pin' | 'complete';

export default function RegisterScreen() {
  const router = useRouter();
  const { register, isLoading } = useAuth();

  const [step, setStep] = useState<RegistrationStep>('personal');
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
    otp: '',
    password: '',
    confirmPassword: '',
    pin: '',
    confirmPin: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [countdown, setCountdown] = useState(0);
  const [agreedToTerms, setAgreedToTerms] = useState(false);

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

  const updateFormData = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // Clear error for this field
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validatePersonalInfo = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Full name is required';
    } else if (formData.fullName.trim().length < 2) {
      newErrors.fullName = 'Name must be at least 2 characters';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Please enter a valid email';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validatePhoneStep = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = 'Phone number is required';
    } else if (!validatePhoneNumber(formData.phoneNumber)) {
      newErrors.phoneNumber = 'Please enter a valid phone number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validateOtpStep = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.otp.trim()) {
      newErrors.otp = 'OTP is required';
    } else if (formData.otp.length !== 6) {
      newErrors.otp = 'OTP must be 6 digits';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validatePasswordStep = () => {
    const newErrors: Record<string, string> = {};

    const passwordValidation = validatePassword(formData.password);
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (!passwordValidation.isValid) {
      newErrors.password = passwordValidation.errors[0];
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validatePinStep = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.pin) {
      newErrors.pin = 'PIN is required';
    } else if (formData.pin.length !== 6) {
      newErrors.pin = 'PIN must be 6 digits';
    }

    if (formData.pin !== formData.confirmPin) {
      newErrors.confirmPin = 'PINs do not match';
    }

    if (!agreedToTerms) {
      newErrors.terms = 'You must agree to the terms and conditions';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handlePersonalNext = () => {
    if (validatePersonalInfo()) {
      setStep('phone');
    }
  };

  const handlePhoneNext = async () => {
    if (!validatePhoneStep()) return;

    try {
      // Request OTP from API
      // await authService.requestOTP(formData.phoneNumber);
      setCountdown(60);
      setStep('otp');
    } catch (error: any) {
      Alert.alert('Error', error.response?.data?.message || 'Failed to send OTP');
    }
  };

  const handleOtpNext = () => {
    if (validateOtpStep()) {
      setStep('password');
    }
  };

  const handlePasswordNext = () => {
    if (validatePasswordStep()) {
      setStep('pin');
    }
  };

  const handleCompleteRegistration = async () => {
    if (!validatePinStep()) return;

    try {
      await register({
        email: formData.email,
        phoneNumber: formData.phoneNumber,
        fullName: formData.fullName,
        password: formData.password,
        confirmPassword: formData.confirmPassword,
      });

      setStep('complete');
    } catch (error: any) {
      Alert.alert(
        'Registration Failed',
        error.response?.data?.message || 'Something went wrong'
      );
    }
  };

  const renderStepIndicator = () => (
    <View style={styles.stepIndicator}>
      {['personal', 'phone', 'otp', 'password', 'pin'].map((s, index) => (
        <View
          key={s}
          style={[
            styles.stepDot,
            step === s || ['personal', 'phone', 'otp', 'password', 'pin'].indexOf(step) > index
              ? styles.stepDotActive
              : styles.stepDotInactive,
          ]}
        />
      ))}
    </View>
  );

  const renderPersonalStep = () => (
    <View>
      <Text style={styles.title}>Create Account</Text>
      <Text style={styles.subtitle}>Let's start with your personal information</Text>

      <Input
        label="Full Name"
        value={formData.fullName}
        onChangeText={(value) => updateFormData('fullName', value)}
        placeholder="John Doe"
        autoCapitalize="words"
        error={errors.fullName}
      />

      <Input
        label="Email Address"
        value={formData.email}
        onChangeText={(value) => updateFormData('email', value)}
        placeholder="john.doe@example.com"
        keyboardType="email-address"
        autoCapitalize="none"
        error={errors.email}
      />

      <Button
        title="Continue"
        onPress={handlePersonalNext}
        fullWidth
        style={styles.continueButton}
      />
    </View>
  );

  const renderPhoneStep = () => (
    <View>
      <Text style={styles.title}>Verify Phone</Text>
      <Text style={styles.subtitle}>Enter your phone number for verification</Text>

      <Input
        label="Phone Number"
        value={formData.phoneNumber}
        onChangeText={(value) => updateFormData('phoneNumber', value)}
        placeholder="+62 812 3456 7890"
        keyboardType="phone-pad"
        error={errors.phoneNumber}
      />

      <Button
        title="Send OTP"
        onPress={handlePhoneNext}
        loading={isLoading}
        fullWidth
        style={styles.continueButton}
      />
    </View>
  );

  const renderOtpStep = () => (
    <View>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => setStep('phone')}
      >
        <Text style={styles.backText}>← Change Phone</Text>
      </TouchableOpacity>

      <Text style={styles.title}>Enter OTP</Text>
      <Text style={styles.subtitle}>
        We've sent a 6-digit code to {formData.phoneNumber}
      </Text>

      <Input
        label="OTP Code"
        value={formData.otp}
        onChangeText={(value) => updateFormData('otp', value)}
        placeholder="000000"
        keyboardType="number-pad"
        maxLength={6}
        error={errors.otp}
        textAlign="center"
        style={{ letterSpacing: 8 }}
      />

      <TouchableOpacity
        onPress={handlePhoneNext}
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
        onPress={handleOtpNext}
        fullWidth
        style={styles.continueButton}
      />
    </View>
  );

  const renderPasswordStep = () => (
    <View>
      <Text style={styles.title}>Create Password</Text>
      <Text style={styles.subtitle}>Choose a strong password for your account</Text>

      <Input
        label="Password"
        value={formData.password}
        onChangeText={(value) => updateFormData('password', value)}
        placeholder="••••••••"
        secureTextEntry
        error={errors.password}
      />

      <Input
        label="Confirm Password"
        value={formData.confirmPassword}
        onChangeText={(value) => updateFormData('confirmPassword', value)}
        placeholder="••••••••"
        secureTextEntry
        error={errors.confirmPassword}
      />

      <View style={styles.passwordRequirements}>
        <Text style={styles.requirementTitle}>Password requirements:</Text>
        <Text style={styles.requirement}>• At least 8 characters</Text>
        <Text style={styles.requirement}>• One uppercase letter</Text>
        <Text style={styles.requirement}>• One lowercase letter</Text>
        <Text style={styles.requirement}>• One number</Text>
        <Text style={styles.requirement}>• One special character</Text>
      </View>

      <Button
        title="Continue"
        onPress={handlePasswordNext}
        fullWidth
        style={styles.continueButton}
      />
    </View>
  );

  const renderPinStep = () => (
    <View>
      <Text style={styles.title}>Create PIN</Text>
      <Text style={styles.subtitle}>Create a 6-digit PIN for quick access</Text>

      <Input
        label="PIN"
        value={formData.pin}
        onChangeText={(value) => updateFormData('pin', value)}
        placeholder="••••••"
        keyboardType="number-pad"
        maxLength={6}
        secureTextEntry
        error={errors.pin}
        textAlign="center"
        style={{ letterSpacing: 8 }}
      />

      <Input
        label="Confirm PIN"
        value={formData.confirmPin}
        onChangeText={(value) => updateFormData('confirmPin', value)}
        placeholder="••••••"
        keyboardType="number-pad"
        maxLength={6}
        secureTextEntry
        error={errors.confirmPin}
        textAlign="center"
        style={{ letterSpacing: 8 }}
      />

      <TouchableOpacity
        style={styles.termsContainer}
        onPress={() => setAgreedToTerms(!agreedToTerms)}
      >
        <View style={[
          styles.checkbox,
          agreedToTerms && styles.checkboxChecked
        ]}>
          {agreedToTerms && <Text style={styles.checkmark}>✓</Text>}
        </View>
        <Text style={styles.termsText}>
          I agree to the Terms of Service and Privacy Policy
        </Text>
      </TouchableOpacity>

      {errors.terms && (
        <Text style={styles.errorText}>{errors.terms}</Text>
      )}

      <Button
        title="Complete Registration"
        onPress={handleCompleteRegistration}
        loading={isLoading}
        fullWidth
        style={styles.continueButton}
      />
    </View>
  );

  const renderCompleteStep = () => (
    <View style={styles.completeContainer}>
      <View style={styles.successIcon}>
        <Text style={styles.successIconText}>✓</Text>
      </View>
      <Text style={styles.completeTitle}>Welcome to PayU!</Text>
      <Text style={styles.completeSubtitle}>
        Your account has been created successfully
      </Text>
      <Button
        title="Get Started"
        onPress={() => router.replace('/(tabs)')}
        fullWidth
        style={styles.getStartedButton}
      />
    </View>
  );

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.logoContainer}>
        <View style={styles.logo}>
          <Text style={styles.logoText}>PayU</Text>
        </View>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <Card padding="lg">
          {step !== 'complete' && renderStepIndicator()}

          {step === 'personal' && renderPersonalStep()}
          {step === 'phone' && renderPhoneStep()}
          {step === 'otp' && renderOtpStep()}
          {step === 'password' && renderPasswordStep()}
          {step === 'pin' && renderPinStep()}
          {step === 'complete' && renderCompleteStep()}

          {step !== 'complete' && (
            <View style={styles.footer}>
              <Text style={styles.footerText}>Already have an account? </Text>
              <TouchableOpacity onPress={() => router.push('/(auth)/login')}>
                <Text style={styles.link}>Sign In</Text>
              </TouchableOpacity>
            </View>
          )}
        </Card>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  logoContainer: {
    alignItems: 'center',
    marginTop: 40,
    marginBottom: 24,
  },
  logo: {
    width: 64,
    height: 64,
    borderRadius: 16,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoText: {
    fontSize: 24,
    fontWeight: '900',
    color: '#ffffff',
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingVertical: 20,
  },
  stepIndicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginBottom: 24,
  },
  stepDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginHorizontal: 4,
  },
  stepDotActive: {
    backgroundColor: '#10b981',
  },
  stepDotInactive: {
    backgroundColor: '#d1d5db',
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
  backButton: {
    alignSelf: 'flex-start',
    marginBottom: 16,
  },
  backText: {
    fontSize: 14,
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
  passwordRequirements: {
    backgroundColor: '#f9fafb',
    padding: 16,
    borderRadius: 12,
    marginTop: 16,
    marginBottom: 16,
  },
  requirementTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  requirement: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 4,
  },
  termsContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 16,
    marginBottom: 8,
  },
  checkbox: {
    width: 20,
    height: 20,
    borderRadius: 4,
    borderWidth: 2,
    borderColor: '#d1d5db',
    marginRight: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxChecked: {
    backgroundColor: '#10b981',
    borderColor: '#10b981',
  },
  checkmark: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '700',
  },
  termsText: {
    flex: 1,
    fontSize: 12,
    color: '#6b7280',
  },
  errorText: {
    fontSize: 12,
    color: '#ef4444',
    marginBottom: 16,
  },
  completeContainer: {
    alignItems: 'center',
    paddingVertical: 24,
  },
  successIcon: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  successIconText: {
    fontSize: 48,
    color: '#ffffff',
    fontWeight: '700',
  },
  completeTitle: {
    fontSize: 24,
    fontWeight: '900',
    color: '#111827',
    marginBottom: 8,
  },
  completeSubtitle: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
    marginBottom: 32,
  },
  getStartedButton: {
    minWidth: 200,
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
});
