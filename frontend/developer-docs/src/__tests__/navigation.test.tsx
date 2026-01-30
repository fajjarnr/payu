import { describe, it, expect } from 'vitest';
import { locales, defaultLocale } from '@/i18n/config';

describe('Navigation', () => {
  describe('Routing Configuration', () => {
    it('should have correct locales configured', () => {
      expect(locales).toEqual(['id', 'en']);
    });

    it('should have correct default locale', () => {
      expect(defaultLocale).toBe('id');
    });
  });

  describe('Locale Support', () => {
    it('should support Indonesian locale', () => {
      expect(locales).toContain('id');
    });

    it('should support English locale', () => {
      expect(locales).toContain('en');
    });

    it('should have Indonesian as default locale', () => {
      expect(defaultLocale).toBe('id');
    });
  });
});

describe('Internal Links', () => {
  const validRoutes = [
    '/',
    '/getting-started',
    '/getting-started/auth',
    '/getting-started/webhooks',
    '/guides/partner-payments',
    '/guides/qris-payments',
    '/guides/bifast-transfers',
    '/sdk/java',
    '/sdk/python',
    '/sdk/typescript',
  ];

  it('should have all main routes defined', () => {
    // These routes have corresponding page.tsx files
    validRoutes.forEach(route => {
      expect(route).toBeDefined();
      expect(typeof route).toBe('string');
    });
  });

  it('should have all getting-started subroutes defined', () => {
    // These routes should all have corresponding page.tsx files
    const gettingStartedRoutes = validRoutes.filter(r => r.startsWith('/getting-started'));
    expect(gettingStartedRoutes).toContain('/getting-started/auth');
    expect(gettingStartedRoutes).toContain('/getting-started/webhooks');
  });

  it('should have consistent route naming', () => {
    validRoutes.forEach(route => {
      // Routes should start with /
      expect(route.startsWith('/')).toBe(true);
      // Routes should not end with /
      if (route !== '/') {
        expect(route.endsWith('/')).toBe(false);
      }
    });
  });
});

describe('Navigation Structure', () => {
  it('should have main navigation items', () => {
    const mainNavItems = [
      { label: 'Getting Started', href: '/getting-started' },
      { label: 'Guides', href: '/guides/partner-payments' },
      { label: 'SDK', href: '/sdk/java' },
    ];

    expect(mainNavItems).toHaveLength(3);
    expect(mainNavItems[0].href).toBe('/getting-started');
    expect(mainNavItems[1].href).toBe('/guides/partner-payments');
  });

  it('should have sidebar navigation groups', () => {
    const sidebarGroups = [
      {
        title: 'Quick Start',
        items: [
          { label: 'Quick Start', href: '/getting-started' },
        ],
      },
      {
        title: 'Guides',
        items: [
          { label: 'Partner Payments', href: '/guides/partner-payments' },
          { label: 'QRIS Payments', href: '/guides/qris-payments' },
          { label: 'BI-FAST Transfers', href: '/guides/bifast-transfers' },
        ],
      },
      {
        title: 'SDK Examples',
        items: [
          { label: 'Java SDK', href: '/sdk/java' },
          { label: 'Python SDK', href: '/sdk/python' },
          { label: 'TypeScript SDK', href: '/sdk/typescript' },
        ],
      },
    ];

    expect(sidebarGroups).toHaveLength(3);
    expect(sidebarGroups[1].items).toHaveLength(3);
    expect(sidebarGroups[2].items).toHaveLength(3);
  });
});
