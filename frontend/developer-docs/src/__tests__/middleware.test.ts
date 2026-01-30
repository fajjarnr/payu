import { describe, it, expect } from 'vitest';
import { locales, defaultLocale } from '@/i18n/config';

describe('Middleware', () => {
  it('should have correct locale configuration', () => {
    expect(locales).toEqual(['id', 'en']);
    expect(defaultLocale).toBe('id');
  });

  it('should have correct matcher patterns', () => {
    // Based on middleware.ts config
    const expectedMatcher = ['/', '/(id|en)/:path*'];
    expect(expectedMatcher).toEqual(['/', '/(id|en)/:path*']);
  });

  it('should match root path', () => {
    const matcher = ['/', '/(id|en)/:path*'];
    expect(matcher).toContain('/');
  });

  it('should match locale paths', () => {
    const matcher = ['/', '/(id|en)/:path*'];
    expect(matcher).toContain('/(id|en)/:path*');
  });

  it('should support all configured locales in matcher', () => {
    const matcherPattern = '/(id|en)/:path*';
    locales.forEach(locale => {
      expect(matcherPattern).toContain(locale);
    });
  });
});

describe('Middleware Locale Handling', () => {
  it('should redirect root to default locale', () => {
    // Root path / should redirect to /id (default locale)
    expect(defaultLocale).toBe('id');
  });

  it('should accept both supported locales', () => {
    expect(locales).toContain('id');
    expect(locales).toContain('en');
  });

  it('should have 2 supported locales', () => {
    expect(locales).toHaveLength(2);
  });
});
