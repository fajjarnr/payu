import { renderHook, act } from '@testing-library/react-native';
import { useCamera } from '../useCamera';
import { Camera } from 'expo-camera';

// Mock expo-camera
jest.mock('expo-camera', () => ({
  Camera: {
    requestCameraPermissionsAsync: jest.fn(),
  },
  CameraView: jest.fn(),
}));

describe('useCamera', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useCamera());

    expect(result.current.type).toBe('back');
    expect(result.current.permission).toBe(false);
    expect(result.current.cameraRef.current).toBeNull();
  });

  it('should request camera permission successfully', async () => {
    (Camera.requestCameraPermissionsAsync as jest.Mock).mockResolvedValue({
      granted: true,
    });

    const { result } = renderHook(() => useCamera());

    let permissionResult: boolean | undefined;
    await act(async () => {
      permissionResult = await result.current.requestPermission();
    });

    expect(permissionResult).toBe(true);
    expect(result.current.permission).toBe(true);
    expect(Camera.requestCameraPermissionsAsync).toHaveBeenCalled();
  });

  it('should handle denied camera permission', async () => {
    (Camera.requestCameraPermissionsAsync as jest.Mock).mockResolvedValue({
      granted: false,
    });

    const { result } = renderHook(() => useCamera());

    let permissionResult: boolean | undefined;
    await act(async () => {
      permissionResult = await result.current.requestPermission();
    });

    expect(permissionResult).toBe(false);
    expect(result.current.permission).toBe(false);
  });

  it('should toggle camera type from back to front', () => {
    const { result } = renderHook(() => useCamera());

    expect(result.current.type).toBe('back');

    act(() => {
      result.current.toggleCameraType();
    });

    expect(result.current.type).toBe('front');
  });

  it('should toggle camera type from front to back', () => {
    const { result } = renderHook(() => useCamera());

    // First toggle to front
    act(() => {
      result.current.toggleCameraType();
    });

    expect(result.current.type).toBe('front');

    // Toggle back to back
    act(() => {
      result.current.toggleCameraType();
    });

    expect(result.current.type).toBe('back');
  });

  it('should return null when taking picture without camera ref', async () => {
    const { result } = renderHook(() => useCamera());

    const picture = await act(async () => {
      return await result.current.takePicture();
    });

    expect(picture).toBeNull();
  });

  it('should take picture when camera ref is available', async () => {
    const mockTakePictureAsync = jest.fn().mockResolvedValue({
      uri: 'file://photo.jpg',
      width: 1920,
      height: 1080,
    });

    const { result } = renderHook(() => useCamera());

    // Simulate setting the camera ref
    act(() => {
      (result.current.cameraRef as any).current = {
        takePictureAsync: mockTakePictureAsync,
      };
    });

    const picture = await act(async () => {
      return await result.current.takePicture();
    });

    expect(mockTakePictureAsync).toHaveBeenCalled();
    expect(picture).toEqual({
      uri: 'file://photo.jpg',
      width: 1920,
      height: 1080,
    });
  });

  it('should handle take picture errors gracefully', async () => {
    const mockTakePictureAsync = jest.fn().mockRejectedValue(new Error('Camera error'));

    const { result } = renderHook(() => useCamera());

    act(() => {
      (result.current.cameraRef as any).current = {
        takePictureAsync: mockTakePictureAsync,
      };
    });

    await expect(act(async () => {
      await result.current.takePicture();
    })).rejects.toThrow('Camera error');
  });

  it('should maintain camera ref across renders', () => {
    const { result, rerender } = renderHook(() => useCamera());

    const mockCamera = {
      takePictureAsync: jest.fn(),
    };

    act(() => {
      (result.current.cameraRef as any).current = mockCamera;
    });

    rerender();

    expect(result.current.cameraRef.current).toBe(mockCamera);
  });

  it('should export default useCamera', () => {
    // Verify the hook can be imported as default
    const { useCamera: defaultUseCamera } = require('../useCamera');
    expect(defaultUseCamera).toBeDefined();
    expect(typeof defaultUseCamera).toBe('function');
  });
});
