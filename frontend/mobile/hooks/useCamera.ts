import { useState, useRef } from 'react';
import { CameraView, CameraType, Camera } from 'expo-camera';

export const useCamera = () => {
  const [type, setType] = useState<CameraType>('back');
  const [permissions, requestPermission] = Camera.useCameraPermissions();
  const cameraRef = useRef<any>(null);

  const toggleCameraType = () => {
    setType(current => (current === 'back' ? 'front' : 'back'));
  };

  const takePicture = async () => {
    if (!cameraRef.current) return null;
    return await cameraRef.current.takePictureAsync();
  };

  return {
    type,
    permissions,
    requestPermission,
    toggleCameraType,
    takePicture,
    cameraRef,
  };
};

export default useCamera;
