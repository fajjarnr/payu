import { describe, it, expect } from 'vitest';
import { locales, defaultLocale } from '@/i18n/config';

describe('i18n Configuration', () => {
  describe('Locale Configuration', () => {
    it('should have correct locales', () => {
      expect(locales).toEqual(['id', 'en']);
    });

    it('should have default locale set to Indonesian', () => {
      expect(defaultLocale).toBe('id');
    });

    it('should include all locales in the type definition', () => {
      expect(locales).toContain('id');
      expect(locales).toContain('en');
    });
  });
});
