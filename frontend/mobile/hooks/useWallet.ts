import { useEffect } from 'react';
import { useWalletStore } from '@/store/walletStore';

export const useWallet = () => {
  const {
    primaryWallet,
    pockets,
    balance,
    isLoading,
    error,
    loadWallet,
    loadPockets,
    createPocket,
    transferToPocket,
    clearError,
  } = useWalletStore();

  useEffect(() => {
    // Load wallet on mount
    loadWallet();
    loadPockets();
  }, []);

  return {
    primaryWallet,
    pockets,
    balance,
    isLoading,
    error,
    loadWallet,
    loadPockets,
    createPocket,
    transferToPocket,
    clearError,
  };
};
