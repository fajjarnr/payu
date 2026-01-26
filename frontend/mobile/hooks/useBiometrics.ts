import * as LocalAuthentication from 'expo-local-authentication';

export const useBiometrics = () => {
  const checkAvailability = async (): Promise<boolean> => {
    try {
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      const isEnrolled = await LocalAuthentication.isEnrolledAsync();

      return hasHardware && isEnrolled;
    } catch (error) {
      console.error('Error checking biometric availability:', error);
      return false;
    }
  };

  const authenticate = async (
    promptMessage: string = 'Authenticate to continue'
  ): Promise<boolean> => {
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage,
        fallbackLabel: 'Use Passcode',
        cancelLabel: 'Cancel',
        disableDeviceFallback: false,
      });

      return result.success;
    } catch (error) {
      console.error('Biometric authentication error:', error);
      return false;
    }
  };

  const getSupportedTypes = async (): Promise<LocalAuthentication.AuthenticationType[]> => {
    try {
      const types = await LocalAuthentication.supportedAuthenticationTypesAsync();
      return types;
    } catch (error) {
      console.error('Error getting supported biometric types:', error);
      return [];
    }
  };

  return {
    checkAvailability,
    authenticate,
    getSupportedTypes,
  };
};
