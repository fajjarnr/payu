import { useState, useEffect } from 'react';
import { CameraView, CameraType, Camera } from 'expo-camera';

export const useCamera = () => {
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [facing, setFacing] = useState<CameraType>('back');

  useEffect(() => {
    (async () => {
      const { status } = await Camera.requestCameraPermissionsAsync();
      setHasPermission(status === 'granted');
    })();
  }, []);

  const toggleCameraFacing = () => {
    setFacing((current) => (current === 'back' ? 'front' : 'back'));
  };

  return {
    hasPermission,
    facing,
    toggleCameraFacing,
  };
};

export const useQRScanner = () => {
  const [scanned, setScanned] = useState(false);

  const handleBarCodeScanned = ({
    data,
  }: {
    data: string;
    type: string;
  }) => {
    if (scanned) return;

    setScanned(true);
    return data;
  };

  const resetScanner = () => {
    setScanned(false);
  };

  return {
    scanned,
    handleBarCodeScanned,
    resetScanner,
  };
};
