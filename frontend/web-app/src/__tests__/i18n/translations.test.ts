import { describe, it, expect } from 'vitest';
import idMessages from '../../../messages/id.json';
import enMessages from '../../../messages/en.json';

interface TranslationMessages {
  [key: string]: string | TranslationMessages;
}

describe('Translation Files', () => {
  it('should have identical keys in both languages', () => {
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

    const idKeys = new Set(getKeys(idMessages));
    const enKeys = new Set(getKeys(enMessages));

    const missingInEn = [...idKeys].filter(k => !enKeys.has(k));
    const missingInId = [...enKeys].filter(k => !idKeys.has(k));

    expect(missingInEn).toHaveLength(0);
    expect(missingInId).toHaveLength(0);
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
      expect(enMessages).toHaveProperty(key);
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
      expect(enMessages).toHaveProperty(key);
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
    checkNonEmpty(enMessages);
  });

  it('should support interpolation parameters', () => {
    expect(idMessages.dashboard.welcome).toContain('{name}');
    expect(enMessages.dashboard.welcome).toContain('{name}');
  });
});
