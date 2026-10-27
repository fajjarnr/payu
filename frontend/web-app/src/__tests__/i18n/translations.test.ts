import { describe, it, expect } from 'vitest';
import idMessages from '../../../messages/id.json';

interface TranslationMessages {
  [key: string]: string | TranslationMessages;
}

// Single-locale app (Indonesian only): the catalog must be complete and valid.
describe('Translation Files', () => {
  it('should have a non-empty key set', () => {
    const getKeys = (obj: TranslationMessages, prefix = ''): string[] => {
      let keys: string[] = [];
      for (const key in obj) {
        const fullKey = prefix ? `${prefix}.${key}` : key;
        if (typeof obj[key] === 'object' && obj[key] !== null) {
          keys = keys.concat(getKeys(obj[key], fullKey));
        } else {
          keys.push(fullKey);
        }
      }
      return keys;
    };

    expect(getKeys(idMessages).length).toBeGreaterThan(1000);
  });

  it('should have all required common translations', () => {
    const requiredCommonKeys = [
      'common.loading',
      'common.error',
      'common.success',
      'common.retry',
    ];

    requiredCommonKeys.forEach(key => {
      expect(idMessages).toHaveProperty(key);
    });
  });

  it('should have all required navigation translations', () => {
    const requiredNavKeys = [
      'nav.dashboard',
      'nav.accounts',
      'nav.transactions',
      'nav.transfers',
    ];

    requiredNavKeys.forEach(key => {
      expect(idMessages).toHaveProperty(key);
    });
  });

  it('should have non-empty string values for all translations', () => {
    const checkNonEmpty = (obj: TranslationMessages): void => {
      for (const key in obj) {
        if (typeof obj[key] === 'object' && obj[key] !== null) {
          checkNonEmpty(obj[key]);
        } else {
          expect(typeof obj[key]).toBe('string');
          expect(obj[key].trim()).not.toBe('');
        }
      }
    };

    checkNonEmpty(idMessages);
  });

  it('should support interpolation parameters', () => {
    expect(idMessages.dashboard.welcome).toContain('{name}');
  });
});
