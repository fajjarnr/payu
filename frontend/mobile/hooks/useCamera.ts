import { useState, useRef } from 'react';
import { CameraView, CameraType, Camera } from 'expo-camera';

export const useCamera = () => {
  const [type, setType] = useState<CameraType>('back');
  const [permission, setPermission] = useState<boolean>(false);
  const cameraRef = useRef<any>(null);

  const requestPermission = async () => {
    const result = await Camera.requestCameraPermissionsAsync();
    setPermission(result.granted);
    return result.granted;
  };

  const toggleCameraType = () => {
    setType(current => (current === 'back' ? 'front' : 'back'));
  };

  const takePicture = async () => {
    if (!cameraRef.current) return null;
    return await cameraRef.current.takePictureAsync();
  };

  return {
    type,
    permission,
    requestPermission,
    toggleCameraType,
    takePicture,
    cameraRef,
  };
};

export default useCamera;
