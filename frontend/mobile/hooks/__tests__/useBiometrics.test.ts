import { renderHook, act } from '@testing-library/react-native';
import { useBiometrics } from '../useBiometrics';
import * as LocalAuthentication from 'expo-local-authentication';

// Mock expo-local-authentication
jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn(),
  isEnrolledAsync: jest.fn(),
  authenticateAsync: jest.fn(),
  supportedAuthenticationTypesAsync: jest.fn(),
  AuthenticationType: {
    FINGERPRINT: 1,
    FACIAL_RECOGNITION: 2,
    IRIS: 3,
  },
}));

describe('useBiometrics', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('checkAvailability', () => {
    it('should return true when hardware and enrollment are available', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(true);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(true);

      const { result } = renderHook(() => useBiometrics());

      let availabilityResult: boolean | undefined;
      await act(async () => {
        availabilityResult = await result.current.checkAvailability();
      });

      expect(availabilityResult).toBe(true);
      expect(LocalAuthentication.hasHardwareAsync).toHaveBeenCalled();
      expect(LocalAuthentication.isEnrolledAsync).toHaveBeenCalled();
    });

    it('should return false when hardware is not available', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(false);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(true);

      const { result } = renderHook(() => useBiometrics());

      let availabilityResult: boolean | undefined;
      await act(async () => {
        availabilityResult = await result.current.checkAvailability();
      });

      expect(availabilityResult).toBe(false);
    });

    it('should return false when user is not enrolled', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(true);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(false);

      const { result } = renderHook(() => useBiometrics());

      let availabilityResult: boolean | undefined;
      await act(async () => {
        availabilityResult = await result.current.checkAvailability();
      });

      expect(availabilityResult).toBe(false);
    });

    it('should return false when both hardware and enrollment are unavailable', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(false);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(false);

      const { result } = renderHook(() => useBiometrics());

      let availabilityResult: boolean | undefined;
      await act(async () => {
        availabilityResult = await result.current.checkAvailability();
      });

      expect(availabilityResult).toBe(false);
    });

    it('should handle errors gracefully', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockRejectedValue(new Error('Hardware check failed'));

      const { result } = renderHook(() => useBiometrics());

      let availabilityResult: boolean | undefined;
      await act(async () => {
        availabilityResult = await result.current.checkAvailability();
      });

      expect(availabilityResult).toBe(false);
      expect(consoleSpy).toHaveBeenCalledWith('Error checking biometric availability:', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });

  describe('authenticate', () => {
    it('should return true when authentication succeeds', async () => {
      (LocalAuthentication.authenticateAsync as jest.Mock).mockResolvedValue({
        success: true,
      });

      const { result } = renderHook(() => useBiometrics());

      let authResult: boolean | undefined;
      await act(async () => {
        authResult = await result.current.authenticate('Authenticate to continue');
      });

      expect(authResult).toBe(true);
      expect(LocalAuthentication.authenticateAsync).toHaveBeenCalledWith({
        promptMessage: 'Authenticate to continue',
        fallbackLabel: 'Use Passcode',
        cancelLabel: 'Cancel',
        disableDeviceFallback: false,
      });
    });

    it('should return false when authentication fails', async () => {
      (LocalAuthentication.authenticateAsync as jest.Mock).mockResolvedValue({
        success: false,
        error: 'User cancelled',
      });

      const { result } = renderHook(() => useBiometrics());

      let authResult: boolean | undefined;
      await act(async () => {
        authResult = await result.current.authenticate();
      });

      expect(authResult).toBe(false);
    });

    it('should use default prompt message when not provided', async () => {
      (LocalAuthentication.authenticateAsync as jest.Mock).mockResolvedValue({
        success: true,
      });

      const { result } = renderHook(() => useBiometrics());

      await act(async () => {
        await result.current.authenticate();
      });

      expect(LocalAuthentication.authenticateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          promptMessage: 'Authenticate to continue',
        })
      );
    });

    it('should handle authentication errors gracefully', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      (LocalAuthentication.authenticateAsync as jest.Mock).mockRejectedValue(new Error('Auth failed'));

      const { result } = renderHook(() => useBiometrics());

      let authResult: boolean | undefined;
      await act(async () => {
        authResult = await result.current.authenticate();
      });

      expect(authResult).toBe(false);
      expect(consoleSpy).toHaveBeenCalledWith('Biometric authentication error:', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });

  describe('getSupportedTypes', () => {
    it('should return supported authentication types', async () => {
      const mockTypes = [1, 2]; // FINGERPRINT and FACIAL_RECOGNITION
      (LocalAuthentication.supportedAuthenticationTypesAsync as jest.Mock).mockResolvedValue(mockTypes);

      const { result } = renderHook(() => useBiometrics());

      let types: number[] | undefined;
      await act(async () => {
        types = await result.current.getSupportedTypes();
      });

      expect(types).toEqual(mockTypes);
      expect(LocalAuthentication.supportedAuthenticationTypesAsync).toHaveBeenCalled();
    });

    it('should return empty array when no types are supported', async () => {
      (LocalAuthentication.supportedAuthenticationTypesAsync as jest.Mock).mockResolvedValue([]);

      const { result } = renderHook(() => useBiometrics());

      let types: number[] | undefined;
      await act(async () => {
        types = await result.current.getSupportedTypes();
      });

      expect(types).toEqual([]);
    });

    it('should handle errors gracefully', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      (LocalAuthentication.supportedAuthenticationTypesAsync as jest.Mock).mockRejectedValue(new Error('Failed to get types'));

      const { result } = renderHook(() => useBiometrics());

      let types: number[] | undefined;
      await act(async () => {
        types = await result.current.getSupportedTypes();
      });

      expect(types).toEqual([]);
      expect(consoleSpy).toHaveBeenCalledWith('Error getting supported biometric types:', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });
});
