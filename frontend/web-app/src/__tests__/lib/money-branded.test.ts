import { describe, it, expect } from 'vitest';
// eslint-disable-next-line @typescript-eslint/no-unused-vars -- retained for branded type test
import { asMoney, isMoney, asAccountId, asUserId, asTransactionId } from '@/types';
import { asMoney as asMoneyCur, isMoney as isMoneyCur, addCurrency, compareCurrency, divideCurrency, parseCurrencyExact, formatExactDecimal } from '@/lib/currency';

describe('ADR-0047 strict branded types', () => {
  it('Money asMoney validates DECIMAL(19,4) and returns branded string', () => {
    expect(asMoney('0')).toBe('0');
    expect(asMoney('100000.00')).toBe('100000.00');
    expect(asMoney('0.1')).toBe('0.1');
    expect(() => asMoney('1.23456')).toThrow(); // >4 decimals
    expect(() => asMoney('abc')).toThrow();
    expect(() => asMoney('')).toThrow();
  });

  it('isMoney guard', () => {
    expect(isMoney('0.00')).toBe(true);
    expect(isMoney('100.1234')).toBe(true);
    expect(isMoney('100.12345')).toBe(false);
    expect(isMoney('')).toBe(false);
  });

  it('currency asMoney/isMoney parity', () => {
    expect(asMoneyCur('123.45')).toBe('123.45');
    expect(isMoneyCur('123.45')).toBe(true);
    expect(() => asMoneyCur('12a')).toThrow();
  });

  it('branded Id helpers return same string with brand', () => {
    const id = asAccountId('550e8400-e29b-41d4-a716-446655440000');
    expect(id).toBe('550e8400-e29b-41d4-a716-446655440000');
    // @ts-expect-error — plain string not assignable to branded AccountId
    const _plain: import('@/types').AccountId = 'plain';
    // bypass via helper
    const ok: import('@/types').AccountId = asAccountId('plain');
    expect(ok).toBe('plain');
  });

  it('Money arithmetic preserves string precision (no float)', () => {
    // 0.1 + 0.2 = 0.3 not 0.3000000004
    expect(addCurrency('0.1', '0.2')).toBe('0.3');
    expect(addCurrency('100.1234', '0.0001')).toBe('100.1235');
    expect(compareCurrency('100.1234', '100.1235')).toBe(-1);
    expect(compareCurrency('100.1234', '100.1234')).toBe(0);
    expect(divideCurrency('100', 3, 4)).toBe('33.3333'); // HALF_EVEN
    expect(divideCurrency('10', 4, 4)).toBe('2.5'); // trims trailing zeros
  });

  it('parseCurrencyExact retains decimal string', () => {
    expect(parseCurrencyExact('Rp 1.000.000,50')).toBe('1000000.5');
    expect(parseCurrencyExact('1,234.56')).toBe('1234.56');
  });

  it('formatExactDecimal groups correctly', () => {
    expect(formatExactDecimal('1234567.89', 2, 'en-US')).toBe('1,234,567.89');
    expect(formatExactDecimal('1234567.89', 2, 'id-ID')).toBe('1.234.567,89');
  });

  it('transfer amount validation uses compareCurrency not Number', () => {
    expect(compareCurrency('0', '0')).toBe(0);
    expect(compareCurrency('0.00', '0')).toBe(0);
    expect(compareCurrency('0.01', '0')).toBe(1);
    expect(compareCurrency('-5', '0')).toBe(-1);
  });
});