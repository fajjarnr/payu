import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useTranslations } from 'next-intl';

// Mock next-intl
vi.mock('next-intl', () => ({
  useTranslations: vi.fn(),
}));

describe('i18n Configuration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('useTranslations hook', () => {
    it('should return translation function', () => {
      const mockT = vi.fn();
      (useTranslations as any).mockReturnValue(mockT);

      const { result } = renderHook(() => useTranslations('hero'));
      expect(result.current).toBe(mockT);
    });

    it('should call translation function with key', () => {
      const mockT = vi.fn().mockReturnValue('Dokumentasi');
      (useTranslations as any).mockReturnValue(mockT);

      const { result } = renderHook(() => useTranslations('hero'));
      result.current('title');

      expect(mockT).toHaveBeenCalledWith('title');
    });
  });
});
