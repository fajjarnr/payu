import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FxService, SUPPORTED_CURRENCIES, type FxRateResponse, type FxConversionResponse, type ConvertCurrencyRequest, type FxConversionRequest } from '@/services/FxService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockRate: FxRateResponse = {
  id: 'rate_001',
  fromCurrency: 'USD',
  toCurrency: 'IDR',
  rate: '15750',
  inverseRate: '0.0000635',
  validFrom: '2026-02-18T00:00:00Z',
  validUntil: '2026-02-18T23:59:59Z',
};

const mockConversion: FxConversionResponse = {
  id: 'conv_001',
  accountId: 'acc_123',
  fromCurrency: 'USD',
  toCurrency: 'IDR',
  fromAmount: '100',
  toAmount: '1575000',
  exchangeRate: '15750',
  fee: '5000',
  effectiveAmount: '1570000',
  conversionDate: '2026-02-18T10:00:00Z',
  status: 'COMPLETED',
};

describe('FxService', () => {
  let service: FxService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = FxService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = FxService.getInstance();
    const instance2 = FxService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('getCurrentRate', () => {
    it('should fetch current rate for currency pair', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockRate });

      const result = await service.getCurrentRate('USD', 'IDR');

      expect(api.get).toHaveBeenCalledWith('/fx/rates/USD/IDR');
      expect(result.rate).toBe('15750');
      expect(result.fromCurrency).toBe('USD');
      expect(result.toCurrency).toBe('IDR');
    });
  });

  describe('getAllRates', () => {
    it('should fetch all available rates', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockRate] });

      const result = await service.getAllRates();

      expect(api.get).toHaveBeenCalledWith('/fx/rates');
      expect(result).toHaveLength(1);
    });
  });

  describe('estimateConversion', () => {
    it('should estimate currency conversion', async () => {
      const request: ConvertCurrencyRequest = {
        fromCurrency: 'USD',
        toCurrency: 'IDR',
        amount: '100',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockConversion });

      const result = await service.estimateConversion(request);

      expect(api.post).toHaveBeenCalledWith('/fx/conversions/estimate', request);
      expect(result.toAmount).toBe('1575000');
    });
  });

  describe('createConversion', () => {
    it('should create a currency conversion', async () => {
      const request: FxConversionRequest = {
        fromCurrency: 'USD',
        toCurrency: 'IDR',
        amount: '100',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockConversion });

      const result = await service.createConversion(request);

      expect(api.post).toHaveBeenCalledWith(
        '/fx/conversions',
        request,
        expect.objectContaining({ headers: expect.objectContaining({ 'X-Idempotency-Key': expect.any(String) }) }),
      );
      expect(result.status).toBe('COMPLETED');
    });
  });

  describe('getConversion', () => {
    it('should fetch conversion by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockConversion });

      const result = await service.getConversion('conv_001');

      expect(api.get).toHaveBeenCalledWith('/fx/conversions/conv_001');
      expect(result.id).toBe('conv_001');
    });
  });

  describe('getConversions', () => {
    it('should fetch all conversions', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockConversion] });

      const result = await service.getConversions();

      expect(api.get).toHaveBeenCalledWith('/fx/conversions');
      expect(result).toHaveLength(1);
    });
  });

  describe('reverseConversion', () => {
    it('should reverse a conversion', async () => {
      const reversedConversion = { ...mockConversion, status: 'REVERSED' };
      vi.mocked(api.post).mockResolvedValue({ data: reversedConversion });

      const result = await service.reverseConversion('conv_001');

      expect(api.post).toHaveBeenCalledWith(
        '/fx/conversions/conv_001/reverse',
        {},
        expect.objectContaining({ headers: expect.objectContaining({ 'X-Idempotency-Key': expect.any(String) }) }),
      );
      expect(result.status).toBe('REVERSED');
    });
  });

  describe('formatCurrency', () => {
    it('should format IDR with no decimals', () => {
      const result = service.formatCurrency(1575000, 'IDR');
      expect(result).toBe('Rp1,575,000');
    });

    it('should format USD with 2 decimals', () => {
      const result = service.formatCurrency(100.5, 'USD');
      expect(result).toBe('$100.50');
    });

    it('should handle unknown currency', () => {
      const result = service.formatCurrency(100, 'XYZ');
      expect(result).toBe('100 XYZ');
    });
  });

  describe('getCurrencyInfo', () => {
    it('should return currency info for known code', () => {
      const info = service.getCurrencyInfo('IDR');
      expect(info).toBeDefined();
      expect(info?.symbol).toBe('Rp');
      expect(info?.decimalPlaces).toBe(0);
    });

    it('should return undefined for unknown code', () => {
      const info = service.getCurrencyInfo('UNKNOWN');
      expect(info).toBeUndefined();
    });
  });

  describe('SUPPORTED_CURRENCIES', () => {
    it('should have 8 supported currencies', () => {
      expect(Object.keys(SUPPORTED_CURRENCIES)).toHaveLength(8);
    });

    it('should include IDR, USD, EUR, SGD, JPY, GBP, AUD, CNY', () => {
      const codes = Object.keys(SUPPORTED_CURRENCIES);
      expect(codes).toContain('IDR');
      expect(codes).toContain('USD');
      expect(codes).toContain('EUR');
      expect(codes).toContain('SGD');
    });
  });
});
