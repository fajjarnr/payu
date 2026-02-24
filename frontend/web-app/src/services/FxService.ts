import api from '@/lib/api';

// FX Rate Types
export interface FxRate {
  id: string;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  inverseRate: number;
  validFrom: string;
  validUntil: string;
}

export interface FxRateResponse {
  id: string;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  inverseRate: number;
  validFrom: string;
  validUntil: string;
}

// FX Conversion Types
export type FxConversionStatus = 'PENDING' | 'COMPLETED' | 'REVERSED' | 'FAILED';

export interface FxConversion {
  id: string;
  accountId: string;
  fromCurrency: string;
  toCurrency: string;
  fromAmount: number;
  toAmount: number;
  exchangeRate: number;
  fee: number;
  effectiveAmount: number;
  conversionDate: string;
  status: FxConversionStatus;
}

export interface FxConversionResponse {
  id: string;
  accountId: string;
  fromCurrency: string;
  toCurrency: string;
  fromAmount: number;
  toAmount: number;
  exchangeRate: number;
  fee: number;
  effectiveAmount: number;
  conversionDate: string;
  status: string;
}

// Request Types
export interface ConvertCurrencyRequest {
  fromCurrency: string;
  toCurrency: string;
  amount: number;
}

export interface FxConversionRequest {
  fromCurrency: string;
  toCurrency: string;
  amount: number;
}

// Currency Info
export interface CurrencyInfo {
  code: string;
  name: string;
  symbol: string;
  flag: string;
  decimalPlaces: number;
}

export const SUPPORTED_CURRENCIES: Record<string, CurrencyInfo> = {
  IDR: {
    code: 'IDR',
    name: 'Indonesian Rupiah',
    symbol: 'Rp',
    flag: '🇮🇩',
    decimalPlaces: 0,
  },
  USD: {
    code: 'USD',
    name: 'US Dollar',
    symbol: '$',
    flag: '🇺🇸',
    decimalPlaces: 2,
  },
  EUR: {
    code: 'EUR',
    name: 'Euro',
    symbol: '€',
    flag: '🇪🇺',
    decimalPlaces: 2,
  },
  SGD: {
    code: 'SGD',
    name: 'Singapore Dollar',
    symbol: 'S$',
    flag: '🇸🇬',
    decimalPlaces: 2,
  },
  JPY: {
    code: 'JPY',
    name: 'Japanese Yen',
    symbol: '¥',
    flag: '🇯🇵',
    decimalPlaces: 0,
  },
  GBP: {
    code: 'GBP',
    name: 'British Pound',
    symbol: '£',
    flag: '🇬🇧',
    decimalPlaces: 2,
  },
  AUD: {
    code: 'AUD',
    name: 'Australian Dollar',
    symbol: 'A$',
    flag: '🇦🇺',
    decimalPlaces: 2,
  },
  CNY: {
    code: 'CNY',
    name: 'Chinese Yuan',
    symbol: '¥',
    flag: '🇨🇳',
    decimalPlaces: 2,
  },
};

export class FxService {
  private static instance: FxService;
  // BUG-BE-078 Fix: Changed from '/fx-api/v1' to '/api/v1/fx' to match
  // standard service routing pattern. The old prefix didn't match any BFF route.
  private baseUrl = '/api/v1/fx';

  private constructor() {}

  static getInstance(): FxService {
    if (!FxService.instance) {
      FxService.instance = new FxService();
    }
    return FxService.instance;
  }

  async getCurrentRate(fromCurrency: string, toCurrency: string): Promise<FxRateResponse> {
    const response = await api.get<FxRateResponse>(
      `${this.baseUrl}/rates/${fromCurrency}/${toCurrency}`
    );
    return response.data;
  }

  async getAllRates(): Promise<FxRateResponse[]> {
    const response = await api.get<FxRateResponse[]>(`${this.baseUrl}/rates`);
    return response.data;
  }

  async estimateConversion(request: ConvertCurrencyRequest): Promise<FxConversionResponse> {
    const response = await api.post<FxConversionResponse>(
      `${this.baseUrl}/conversions/estimate`,
      request
    );
    return response.data;
  }

  async createConversion(request: FxConversionRequest): Promise<FxConversionResponse> {
    const response = await api.post<FxConversionResponse>(
      `${this.baseUrl}/conversions`,
      request
    );
    return response.data;
  }

  async getConversion(conversionId: string): Promise<FxConversionResponse> {
    const response = await api.get<FxConversionResponse>(
      `${this.baseUrl}/conversions/${conversionId}`
    );
    return response.data;
  }

  async getConversions(): Promise<FxConversionResponse[]> {
    const response = await api.get<FxConversionResponse[]>(`${this.baseUrl}/conversions`);
    return response.data;
  }

  async reverseConversion(conversionId: string): Promise<FxConversionResponse> {
    const response = await api.post<FxConversionResponse>(
      `${this.baseUrl}/conversions/${conversionId}/reverse`
    );
    return response.data;
  }

  formatCurrency(amount: number, currencyCode: string): string {
    const currency = SUPPORTED_CURRENCIES[currencyCode];
    if (!currency) {
      return `${amount.toFixed(2)} ${currencyCode}`;
    }

    const formattedAmount = amount.toLocaleString('en-US', {
      minimumFractionDigits: currency.decimalPlaces,
      maximumFractionDigits: currency.decimalPlaces,
    });

    return `${currency.symbol}${formattedAmount}`;
  }

  getCurrencyInfo(code: string): CurrencyInfo | undefined {
    return SUPPORTED_CURRENCIES[code];
  }
}

export default FxService.getInstance();
