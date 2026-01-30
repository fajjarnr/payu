import { describe, it, expect } from 'vitest';
import idMessages from '@/i18n/id/messages';
import enMessages from '@/i18n/en/messages';

describe('i18n Messages', () => {
  describe('Indonesian Messages', () => {
    it('should have correct locale', () => {
      expect(idMessages.locale).toBe('id');
    });

    it('should have nav translations', () => {
      expect(idMessages.messages.nav).toBeDefined();
      expect(idMessages.messages.nav.home).toBe('Beranda');
      expect(idMessages.messages.nav.gettingStarted).toBe('Memulai');
      expect(idMessages.messages.nav.guides).toBe('Panduan Integrasi');
      expect(idMessages.messages.nav.sdk).toBe('SDK Contoh');
      expect(idMessages.messages.nav.api).toBe('Referensi API');
    });

    it('should have hero translations', () => {
      expect(idMessages.messages.hero).toBeDefined();
      expect(idMessages.messages.hero.title).toBe('Dokumentasi Developer PayU');
      expect(idMessages.messages.hero.subtitle).toContain('Panduan lengkap');
      expect(idMessages.messages.hero.getStarted).toBe('Mulai Sekarang');
      expect(idMessages.messages.hero.viewDocs).toBe('Lihat Dokumentasi');
    });

    it('should have features translations', () => {
      expect(idMessages.messages.features).toBeDefined();
      expect(idMessages.messages.features.easyIntegration).toBeDefined();
      expect(idMessages.messages.features.comprehensive).toBeDefined();
      expect(idMessages.messages.features.sandbox).toBeDefined();
    });

    it('should have sidebar translations', () => {
      expect(idMessages.messages.sidebar).toBeDefined();
      expect(idMessages.messages.sidebar.quickStart).toBe('Mulai Cepat');
      expect(idMessages.messages.sidebar.gettingStarted).toBe('Memulai');
      expect(idMessages.messages.sidebar.guides).toBe('Panduan Integrasi');
      expect(idMessages.messages.sidebar.partnerPayments).toBe('Pembayaran Partner');
      expect(idMessages.messages.sidebar.qrisPayments).toBe('Pembayaran QRIS');
      expect(idMessages.messages.sidebar.biFastTransfers).toBe('Transfer BI-FAST');
      expect(idMessages.messages.sidebar.sdkExamples).toBe('SDK Contoh');
      expect(idMessages.messages.sidebar.java).toBe('Java SDK');
      expect(idMessages.messages.sidebar.python).toBe('Python SDK');
      expect(idMessages.messages.sidebar.typescript).toBe('TypeScript SDK');
    });

    it('should have common translations', () => {
      expect(idMessages.messages.common).toBeDefined();
      expect(idMessages.messages.common.viewMore).toBe('Lihat Selengkapnya');
      expect(idMessages.messages.common.backToTop).toBe('Kembali ke Atas');
      expect(idMessages.messages.common.lastUpdated).toBe('Terakhir diperbarui');
    });
  });

  describe('English Messages', () => {
    it('should have correct locale', () => {
      expect(enMessages.locale).toBe('en');
    });

    it('should have nav translations', () => {
      expect(enMessages.messages.nav).toBeDefined();
      expect(enMessages.messages.nav.home).toBe('Home');
      expect(enMessages.messages.nav.gettingStarted).toBe('Getting Started');
      expect(enMessages.messages.nav.guides).toBe('Integration Guides');
      expect(enMessages.messages.nav.sdk).toBe('SDK Examples');
      expect(enMessages.messages.nav.api).toBe('API Reference');
    });

    it('should have hero translations', () => {
      expect(enMessages.messages.hero).toBeDefined();
      expect(enMessages.messages.hero.title).toBe('PayU Developer Documentation');
      expect(enMessages.messages.hero.subtitle).toContain('Complete guide');
      expect(enMessages.messages.hero.getStarted).toBe('Get Started');
      expect(enMessages.messages.hero.viewDocs).toBe('View Documentation');
    });

    it('should have features translations', () => {
      expect(enMessages.messages.features).toBeDefined();
      expect(enMessages.messages.features.easyIntegration).toBeDefined();
      expect(enMessages.messages.features.comprehensive).toBeDefined();
      expect(enMessages.messages.features.sandbox).toBeDefined();
    });

    it('should have sidebar translations', () => {
      expect(enMessages.messages.sidebar).toBeDefined();
      expect(enMessages.messages.sidebar.quickStart).toBe('Quick Start');
      expect(enMessages.messages.sidebar.gettingStarted).toBe('Getting Started');
      expect(enMessages.messages.sidebar.guides).toBe('Integration Guides');
      expect(enMessages.messages.sidebar.partnerPayments).toBe('Partner Payments');
      expect(enMessages.messages.sidebar.qrisPayments).toBe('QRIS Payments');
      expect(enMessages.messages.sidebar.biFastTransfers).toBe('BI-FAST Transfers');
      expect(enMessages.messages.sidebar.sdkExamples).toBe('SDK Examples');
      expect(enMessages.messages.sidebar.java).toBe('Java SDK');
      expect(enMessages.messages.sidebar.python).toBe('Python SDK');
      expect(enMessages.messages.sidebar.typescript).toBe('TypeScript SDK');
    });

    it('should have common translations', () => {
      expect(enMessages.messages.common).toBeDefined();
      expect(enMessages.messages.common.viewMore).toBe('View More');
      expect(enMessages.messages.common.backToTop).toBe('Back to Top');
      expect(enMessages.messages.common.lastUpdated).toBe('Last updated');
    });
  });

  describe('Message Structure Consistency', () => {
    it('should have same keys in both locales', () => {
      const getKeys = (obj: any, prefix = ''): string[] => {
        return Object.keys(obj).flatMap(key => {
          const newKey = prefix ? `${prefix}.${key}` : key;
          if (typeof obj[key] === 'object' && obj[key] !== null) {
            return getKeys(obj[key], newKey);
          }
          return newKey;
        });
      };

      const idKeys = getKeys(idMessages.messages).sort();
      const enKeys = getKeys(enMessages.messages).sort();

      expect(idKeys).toEqual(enKeys);
    });

    it('should have non-empty string values for all translations', () => {
      const checkValues = (obj: any) => {
        Object.values(obj).forEach(value => {
          if (typeof value === 'object' && value !== null) {
            checkValues(value);
          } else if (typeof value === 'string') {
            expect(value.length).toBeGreaterThan(0);
          }
        });
      };

      checkValues(idMessages.messages);
      checkValues(enMessages.messages);
    });
  });
});
