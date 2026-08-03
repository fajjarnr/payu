import { beforeEach, describe, expect, it, vi } from 'vitest';

const LEGACY_AUTH_STORAGE_KEY = 'payu-auth-storage';

describe('auth store persistence migration', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  it('removes legacy persisted auth state when the store loads', async () => {
    localStorage.setItem(LEGACY_AUTH_STORAGE_KEY, JSON.stringify({
      state: {
        username: 'stale-user',
        accountId: 'stale-account',
        roles: ['customer'],
        isAuthenticated: true,
      },
      version: 0,
    }));

    await import('@/stores/authStore');

    expect(localStorage.getItem(LEGACY_AUTH_STORAGE_KEY)).toBeNull();
  });

  it('does not write auth state to browser storage after login state changes', async () => {
    const { useAuthStore } = await import('@/stores/authStore');

    useAuthStore.getState().setAuth(
      { id: 'user-1', username: 'user-1', roles: ['customer'] },
      'account-1',
    );

    expect(localStorage.getItem(LEGACY_AUTH_STORAGE_KEY)).toBeNull();
  });
});
