/**
 * Deeplink Handler Hook for PayU Mobile App
 *
 * Handles incoming deep links with URL scheme: payu://
 * Supported actions:
 * - payu://pay?token=xxx - Navigate to payment flow
 * - payu://topup?amount=xxx - Navigate to topup flow
 * - payu://transfer?to=xxx&amount=xxx - Navigate to transfer flow
 *
 * Part of E-15 IMP-046: Checkout Deeplink
 */

import { useEffect, useCallback } from 'react';
import { Linking, Platform } from 'react-native';
import * as LinkingExpo from 'expo-linking';
import { useRouter } from 'expo-router';
import { useAuth } from '@/context/AuthContext';

interface DeeplinkData {
  action: 'pay' | 'topup' | 'transfer' | 'unknown';
  params: Record<string, string>;
  url: string;
}

export function useDeeplinkHandler() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();

  const parseDeeplink = useCallback((url: string): DeeplinkData => {
    // Parse payu:// URLs
    if (!url.startsWith('payu://')) {
      return { action: 'unknown', params: {}, url };
    }

    // Extract path and query parameters
    const urlWithoutScheme = url.replace('payu://', '');
    const [path, queryString] = urlWithoutScheme.split('?');
    const action = path as DeeplinkData['action'];

    // Parse query parameters
    const params: Record<string, string> = {};
    if (queryString) {
      const searchParams = new URLSearchParams(queryString);
      searchParams.forEach((value, key) => {
        params[key] = decodeURIComponent(value);
      });
    }

    return { action, params, url };
  }, []);

  const handleDeeplink = useCallback(async (url: string | null) => {
    if (!url) return;

    console.log('[Deeplink] Received URL:', url);

    const data = parseDeeplink(url);

    // Check authentication for protected routes
    if (!isAuthenticated && data.action !== 'unknown') {
      console.log('[Deeplink] User not authenticated, redirecting to login');
      router.push({
        pathname: '/(auth)/login',
        params: { redirectUrl: url }
      });
      return;
    }

    switch (data.action) {
      case 'pay':
        handlePayDeeplink(data.params);
        break;
      case 'topup':
        handleTopupDeeplink(data.params);
        break;
      case 'transfer':
        handleTransferDeeplink(data.params);
        break;
      default:
        console.log('[Deeplink] Unknown action:', data.action);
    }
  }, [isAuthenticated, router, parseDeeplink]);

  const handlePayDeeplink = useCallback((params: Record<string, string>) => {
    const { token, amount, orderId, partnerId } = params;

    console.log('[Deeplink] Processing pay action:', { token, amount, orderId });

    if (!token) {
      console.error('[Deeplink] Missing token for pay action');
      return;
    }

    // Navigate to payment confirmation screen
    router.push({
      pathname: '/payment-confirm',
      params: {
        token,
        amount: amount || '',
        orderId: orderId || '',
        partnerId: partnerId || '',
        source: 'deeplink'
      }
    });
  }, [router]);

  const handleTopupDeeplink = useCallback((params: Record<string, string>) => {
    const { amount, method } = params;

    console.log('[Deeplink] Processing topup action:', { amount, method });

    // Navigate to topup screen with pre-filled amount
    router.push({
      pathname: '/(tabs)/topup',
      params: {
        amount: amount || '',
        method: method || '',
        source: 'deeplink'
      }
    });
  }, [router]);

  const handleTransferDeeplink = useCallback((params: Record<string, string>) => {
    const { to, amount, note } = params;

    console.log('[Deeplink] Processing transfer action:', { to, amount });

    if (!to) {
      console.error('[Deeplink] Missing recipient for transfer action');
      return;
    }

    // Navigate to transfer screen with pre-filled data
    router.push({
      pathname: '/transfer-confirm',
      params: {
        recipientId: to,
        amount: amount || '',
        note: note || '',
        source: 'deeplink'
      }
    });
  }, [router]);

  useEffect(() => {
    // Handle initial URL (app opened via deeplink)
    const getInitialUrl = async () => {
      const initialUrl = await Linking.getInitialURL();
      if (initialUrl) {
        handleDeeplink(initialUrl);
      }
    };

    getInitialUrl();

    // Listen for deeplinks while app is running
    const subscription = Linking.addEventListener('url', ({ url }) => {
      handleDeeplink(url);
    });

    return () => {
      subscription.remove();
    };
  }, [handleDeeplink]);

  // Generate a deeplink URL (for sharing)
  const generateDeeplink = useCallback((
    action: 'pay' | 'topup' | 'transfer',
    params: Record<string, string>
  ): string => {
    const queryString = Object.entries(params)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');

    return `payu://${action}${queryString ? `?${queryString}` : ''}`;
  }, []);

  return {
    parseDeeplink,
    handleDeeplink,
    generateDeeplink
  };
}

export default useDeeplinkHandler;
