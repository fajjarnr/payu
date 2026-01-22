import { useBalance } from '@/hooks/useWallet';

describe('useBalance hook', () => {
  test('hook should be defined', () => {
    expect(useBalance).toBeDefined();
  });

  test('hook should be a function', () => {
    expect(typeof useBalance).toBe('function');
  });
});
