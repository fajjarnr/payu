import * as LocalAuthentication from 'expo-local-authentication';
import { Logger } from '@/utils/logger';

export const useBiometrics = () => {
  const checkAvailability = async (): Promise<boolean> => {
    try {
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      const isEnrolled = await LocalAuthentication.isEnrolledAsync();

      Logger.debug('Biometrics', 'Hardware availability checked', {
        hasHardware,
        isEnrolled,
      });

      return hasHardware && isEnrolled;
    } catch (error) {
      Logger.error('Biometrics', 'Error checking biometric availability', error);
      return false;
    }
  };

  const authenticate = async (
    promptMessage: string = 'Authenticate to continue'
  ): Promise<boolean> => {
    try {
      Logger.debug('Biometrics', 'Starting biometric authentication');

      const result = await LocalAuthentication.authenticateAsync({
        promptMessage,
        fallbackLabel: 'Use Passcode',
        cancelLabel: 'Cancel',
        disableDeviceFallback: false,
      });

      Logger.debug('Biometrics', 'Authentication result', { success: result.success });

      return result.success;
    } catch (error) {
      Logger.error('Biometrics', 'Biometric authentication error', error);
      return false;
    }
  };

  const getSupportedTypes = async (): Promise<LocalAuthentication.AuthenticationType[]> => {
    try {
      const types = await LocalAuthentication.supportedAuthenticationTypesAsync();

      Logger.debug('Biometrics', 'Supported biometric types', {
        types,
        count: types.length,
      });

      return types;
    } catch (error) {
      Logger.error('Biometrics', 'Error getting supported biometric types', error);
      return [];
    }
  };

  return {
    checkAvailability,
    authenticate,
    getSupportedTypes,
  };
};
