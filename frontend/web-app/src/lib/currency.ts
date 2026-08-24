/**
 * Currency Utility Functions for PayU Digital Banking
 * Handles Indonesian Rupiah (IDR) formatting and parsing
 * ADR-0047: Money is branded string "0.00" HALF_EVEN, never float (GAP-047)
 */
export type Money = string & { readonly __brand: 'Money' };
const MONEY_RE = /^-?\d+(?:\.\d{1,4})?$/;
export function isMoney(value: string): value is Money { return MONEY_RE.test(value); }
export function asMoney(value: string): Money {
  if (!MONEY_RE.test(value)) throw new Error(`Invalid Money: ${value}`);
  return value as Money;
}
type CurrencyInput = Money | number | string;
function expandExponential(value: string): string {
  const match = value.toLowerCase().match(/^(-?)(\d+)(?:\.(\d+))?e([+-]?\d+)$/);
  if (!match) return value;
  const [, sign, integer, fraction = '', exponentText] = match;
  const digits = integer + fraction;
  const decimalIndex = integer.length + Number(exponentText);
  if (decimalIndex <= 0) return `${sign}0.${'0'.repeat(-decimalIndex)}${digits}`;
  if (decimalIndex >= digits.length) return `${sign}${digits}${'0'.repeat(decimalIndex - digits.length)}`;
  return `${sign}${digits.slice(0, decimalIndex)}.${digits.slice(decimalIndex)}`;
}

function decimalString(value: CurrencyInput): string | null {
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) return null;
    return expandExponential(String(value));
  }
  const invalidCharacters = value.replace(/Rp/gi, '').replace(/[\d.,\s$€¥£-]/g, '');
  if (invalidCharacters) return null;
  let cleaned = value.trim().replace(/[^\d.,-]/g, '');
  if (!cleaned || (cleaned.match(/-/g) || []).length > 1) return null;
  const negative = cleaned.startsWith('-');
  cleaned = cleaned.replace(/-/g, '');
  const commaCount = (cleaned.match(/,/g) || []).length;
  const dotCount = (cleaned.match(/\./g) || []).length;
  if (commaCount && dotCount) {
    cleaned = cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')
      ? cleaned.replace(/\./g, '').replace(',', '.')
      : cleaned.replace(/,/g, '');
  } else if (commaCount > 1) {
    cleaned = cleaned.replace(/,/g, '');
  } else if (commaCount === 1) {
    cleaned = cleaned.replace(',', '.');
  } else if (dotCount > 1) {
    cleaned = cleaned.replace(/\./g, '');
  }
  if (!/^\d+(?:\.\d+)?$/.test(cleaned)) return null;
  let [integer, fraction = ''] = cleaned.split('.');
  integer = integer.replace(/^0+(?=\d)/, '');
  fraction = fraction.replace(/0+$/, '');
  return `${negative ? '-' : ''}${integer || '0'}${fraction ? `.${fraction}` : ''}`;
}

function roundDecimal(value: string, decimals: number): string {
  const negative = value.startsWith('-');
  const unsigned = negative ? value.slice(1) : value;
  let [integer, fraction = ''] = unsigned.split('.');
  if (decimals < 0 || !Number.isInteger(decimals)) return value;
  if (fraction.length > decimals) {
    const retained = fraction.slice(0, decimals);
    const discarded = fraction.slice(decimals);
    const first = discarded[0];
    const hasMore = /[1-9]/.test(discarded.slice(1));
    const lastRetained = decimals > 0 ? retained[retained.length - 1] : integer[integer.length - 1];
    const increment = first > '5' || (first === '5' && (hasMore || Number(lastRetained || '0') % 2 === 1));
    let digits = `${integer}${retained}`.replace(/^0+(?=\d)/, '') || '0';
    if (increment) {
      const chars: string[] = digits.split('');
      for (let i = chars.length - 1; i >= 0; i--) {
        if (chars[i] === '9') chars[i] = '0';
        else { chars[i] = String(Number(chars[i]) + 1); break; }
      }
      digits = chars.every((digit) => digit === '0') ? `1${chars.join('')}` : chars.join('');
    }
    integer = decimals === 0 ? digits : digits.slice(0, -decimals) || '0';
    fraction = decimals === 0 ? '' : digits.slice(-decimals).padStart(decimals, '0');
  } else {
    fraction = fraction.padEnd(decimals, '0');
  }
  const result = `${integer}${decimals ? `.${fraction}` : ''}`;
  return negative && result !== '0' && !/^0\.0+$/.test(result) ? `-${result}` : result;
}
function formatExact(value: CurrencyInput, decimals: number, locale: string): string | null {
  const normalized = decimalString(value);
  if (!normalized) return null;
  const rounded = roundDecimal(normalized, decimals);
  const negative = rounded.startsWith('-');
  const unsigned = negative ? rounded.slice(1) : rounded;
  const [integer, fraction = ''] = unsigned.split('.');
  const groupedInteger = integer.replace(/\B(?=(\d{3})+(?!\d))/g, locale === 'id-ID' ? '.' : ',');
  const decimalSeparator = locale === 'id-ID' ? ',' : '.';
  return `${negative ? '-' : ''}${groupedInteger}${fraction ? decimalSeparator + fraction : ''}`;
}

/**
 * Format a decimal money string with grouping and fixed decimals, no symbol
 * (locale: 'id-ID' → 1.234,56 ; default en-US → 1,234.56). Returns '0' on invalid input.
 */
export function formatExactDecimal(value: CurrencyInput | null | undefined, decimals = 0, locale = 'en-US'): string {
  if (value === null || value === undefined) return '0';
  return formatExact(value, decimals, locale) ?? '0';
}

/**
 * Format a number to Indonesian Rupiah currency string
 * @param amount - The amount to format
 * @param options - Formatting options
 * @returns Formatted currency string (e.g., "Rp 1.000.000" or "Rp 1.000.000,00")
 */
export function formatCurrency(
  amount: CurrencyInput | null | undefined,
  options: {
    withDecimals?: boolean;
    symbol?: string;
    locale?: string;
    compact?: boolean;
  } = {}
): string {
  const {
    withDecimals = false,
    symbol = 'Rp',
    locale = 'id-ID',
    compact = false,
  } = options;

  // Handle null/undefined
  if (amount === null || amount === undefined) {
    return `${symbol} 0`;
  }

  if (compact) {
    const normalized = decimalString(amount);
    if (!normalized) return `${symbol} 0`;
    const formatter = new Intl.NumberFormat(locale, {
      style: 'currency', currency: 'IDR', minimumFractionDigits: withDecimals ? 2 : 0,
      maximumFractionDigits: withDecimals ? 2 : 0, notation: 'compact', compactDisplay: 'short',
    });
    return formatter.format(Number(normalized)).replace('Rp', symbol);
  }

  const formattedAmount = formatExact(amount, withDecimals ? 2 : 0, locale);
  if (!formattedAmount) return `${symbol} 0`;
  return `${formattedAmount.startsWith('-') ? '-' : ''}${symbol}\u00A0${formattedAmount.replace(/^-/, '')}`;
}

/**
 * Format currency amount without symbol (for editable inputs)
 * @param amount - The amount to format
 * @param options - Formatting options
 * @returns Formatted number string (e.g., "1.000.000")
 */
export function formatCurrencyWithoutSymbol(
  amount: CurrencyInput | null | undefined,
  options: { withDecimals?: boolean; locale?: string } = {}
): string {
  const { withDecimals = false, locale = 'id-ID' } = options;

  if (amount === null || amount === undefined) {
    return '0';
  }

  return formatExact(amount, withDecimals ? 2 : 0, locale) ?? '0';
}

/** Parse a formatted amount while retaining its decimal string representation. */
export function parseCurrencyExact(value: CurrencyInput | null | undefined): Money {
  if (value === null || value === undefined) return '0';
  return decimalString(value) ?? '0';
}

/** Add decimal money values without passing through IEEE-754 numbers. */
export function addCurrency(left: CurrencyInput, right: CurrencyInput): Money {
  const leftValue = decimalString(left) ?? '0';
  const rightValue = decimalString(right) ?? '0';
  const scale = Math.max(leftValue.split('.')[1]?.length ?? 0, rightValue.split('.')[1]?.length ?? 0);
  const scaled = (value: string): bigint => {
    const negative = value.startsWith('-');
    const unsigned = negative ? value.slice(1) : value;
    const [integer, fraction = ''] = unsigned.split('.');
    const result = BigInt(`${integer}${fraction.padEnd(scale, '0')}`);
    return negative ? -result : result;
  };
  const sum = scaled(leftValue) + scaled(rightValue);
  const negative = sum < BigInt(0);
  const digits = (negative ? -sum : sum).toString().padStart(scale + 1, '0');
  const integer = scale ? digits.slice(0, -scale) : digits;
  const fraction = scale ? digits.slice(-scale).replace(/0+$/, '') : '';
  return `${negative ? '-' : ''}${integer}${fraction ? `.${fraction}` : ''}`;
}

/** Compare decimal money values without passing through IEEE-754 numbers. */
export function compareCurrency(left: CurrencyInput, right: CurrencyInput): -1 | 0 | 1 {
  const leftValue = decimalString(left) ?? '0';
  const rightValue = decimalString(right) ?? '0';
  const scale = Math.max(leftValue.split('.')[1]?.length ?? 0, rightValue.split('.')[1]?.length ?? 0);
  const scaled = (value: string): bigint => {
    const negative = value.startsWith('-');
    const unsigned = negative ? value.slice(1) : value;
    const [integer, fraction = ''] = unsigned.split('.');
    const result = BigInt(`${integer}${fraction.padEnd(scale, '0')}`);
    return negative ? -result : result;
  };
  const diff = scaled(leftValue) - scaled(rightValue);
  return diff < BigInt(0) ? -1 : diff > BigInt(0) ? 1 : 0;
}

/**
 * Divide decimal money by an integer divisor, rounded to money scale
 * (default 4, HALF_EVEN) — e.g. equal bill split.
 */
export function divideCurrency(value: CurrencyInput, divisor: number, decimals = 4): Money {
  const normalized = decimalString(value) ?? '0';
  if (divisor === 0) return '0';
  const negative = normalized.startsWith('-');
  const unsigned = negative ? normalized.slice(1) : normalized;
  const [integer, fraction = ''] = unsigned.split('.');
  const scaled = BigInt(`${integer}${fraction.padEnd(decimals, '0')}`);
  const d = BigInt(Math.abs(divisor));
  let quotient = scaled / d;
  const remainder = scaled % d;
  if (remainder * BigInt(2) > d || (remainder * BigInt(2) === d && quotient % BigInt(2) === BigInt(1))) {
    quotient += BigInt(1);
  }
  const digits = quotient.toString().padStart(decimals + 1, '0');
  const resultInteger = decimals ? digits.slice(0, -decimals) : digits;
  let resultFraction = decimals ? digits.slice(-decimals) : '';
  resultFraction = resultFraction.replace(/0+$/, '');
  const result = `${resultInteger}${resultFraction ? `.${resultFraction}` : ''}`;
  return negative && result !== '0' ? `-${result}` : result;
}

/**
 * Parse Indonesian formatted currency string back to number
 * Handles formats like "Rp 1.000.000", "1.000.000", "1000000"
 * @param value - The currency string to parse
 * @returns Parsed number or 0 if parsing fails
 */
export function parseCurrency(value: string | number | null | undefined): number {
  if (value === null || value === undefined) {
    return 0;
  }

  if (typeof value === 'number') {
    return value;
  }

  // Remove currency symbol and whitespace
  let cleaned = value
    .replace(/Rp/gi, '')
    .replace(/\s/g, '')
    .replace(/[^\d.,-]/g, '');

  // Handle empty string
  if (!cleaned) {
    return 0;
  }

  // Indonesian format: dots are thousand separators, comma is decimal
  // Remove thousand separators (dots)
  cleaned = cleaned.replace(/\./g, '');

  // Replace comma with dot for decimal
  cleaned = cleaned.replace(/,/g, '.');

  const parsed = Number(cleaned);

  return Number.isNaN(parsed) ? 0 : parsed;
}

/**
 * Format amount for display in transaction history
 * Shows different colors for positive/negative amounts
 * @param amount - The amount to format
 * @param options - Formatting options
 * @returns Object with formatted string and type (credit/debit)
 */
export function formatTransactionAmount(
  amount: CurrencyInput,
  options: { withDecimals?: boolean } = {}
): { formatted: string; type: 'credit' | 'debit'; isPositive: boolean } {
  const normalized = decimalString(amount) ?? '0';
  const absAmount = normalized.replace(/^-/, '');
  const formatted = formatCurrency(absAmount, options);

  return {
    formatted,
    type: normalized.startsWith('-') ? 'debit' : 'credit',
    isPositive: !normalized.startsWith('-'),
  };
}

/**
 * Convert number to words in Indonesian (e.g., 1000 -> "seribu rupiah")
 * @param amount - The amount to convert
 * @returns Amount in Indonesian words
 */
export function numberToWords(amount: number): string {
  const units = [
    '',
    'satu',
    'dua',
    'tiga',
    'empat',
    'lima',
    'enam',
    'tujuh',
    'delapan',
    'sembilan',
  ];
  const teens = [
    'sepuluh',
    'sebelas',
    'dua belas',
    'tiga belas',
    'empat belas',
    'lima belas',
    'enam belas',
    'tujuh belas',
    'delapan belas',
    'sembilan belas',
  ];
  const tens = [
    '',
    'sepuluh',
    'dua puluh',
    'tiga puluh',
    'empat puluh',
    'lima puluh',
    'enam puluh',
    'tujuh puluh',
    'delapan puluh',
    'sembilan puluh',
  ];
  const scales = ['', 'ribu', 'juta', 'miliar', 'triliun'];

  if (amount === 0) {
    return 'nol';
  }

  const convertChunk = (n: number): string => {
    if (n === 0) return '';
    if (n < 10) return units[n];
    if (n < 12) return teens[n - 10];
    if (n < 20) return units[n - 10] + ' belas';
    if (n < 100) {
      const ten = Math.floor(n / 10);
      const unit = n % 10;
      return tens[ten] + (unit ? ' ' + units[unit] : '');
    }
    if (n < 200) return 'seratus' + (n % 100 ? ' ' + convertChunk(n % 100) : '');
    if (n < 1000) {
      const hundred = Math.floor(n / 100);
      return units[hundred] + ' ratus' + (n % 100 ? ' ' + convertChunk(n % 100) : '');
    }
    return '';
  };

  const convert = (n: number): string => {
    if (n === 0) return '';
    let result = '';
    let scaleIndex = 0;

    while (n > 0) {
      // BUG-FE-012: Guard for numbers > triliun — prevent "undefined" in output
      if (scaleIndex >= scales.length) {
        result = convertChunk(n % 1000) + ' (overflow)' + (result ? ' ' + result : '');
        break;
      }
      const chunk = n % 1000;
      if (chunk > 0) {
        let chunkText = convertChunk(chunk);
        if (chunk === 1 && scaleIndex === 1) {
          // Special case: "seribu" not "satu ribu"
          chunkText = 'seribu';
        } else {
          chunkText += ' ' + scales[scaleIndex];
        }
        result = chunkText + (result ? ' ' + result : '');
      }
      n = Math.floor(n / 1000);
      scaleIndex++;
    }

    return result;
  };

  const absAmount = Math.floor(Math.abs(amount));
  const words = convert(absAmount);

  return words.trim() || 'nol';
}

/**
 * Check if a string is a valid currency amount
 * @param value - The value to validate
 * @returns True if valid currency amount
 */
export function isValidCurrency(value: string | number): boolean {
  if (typeof value === 'number') {
    return !isNaN(value) && isFinite(value);
  }

  if (!value || typeof value !== 'string') {
    return false;
  }

  // Check if it contains at least some digits
  if (!/\d/.test(value)) {
    return false;
  }

  // Check if it's a valid number string (with or without formatting)
  const cleaned = value.replace(/Rp/gi, '').replace(/\s/g, '');
  const numValue = parseCurrency(cleaned);

  return !isNaN(numValue) && isFinite(numValue) && cleaned.trim().length > 0;
}

/**
 * Round currency amount to specific decimals
 * @param amount - The amount to round
 * @param decimals - Number of decimal places (default: 0 for IDR)
 * @returns Rounded amount
 */
export function roundCurrency(amount: number, decimals?: number): number;
export function roundCurrency(amount: string, decimals?: number): string;
export function roundCurrency(amount: CurrencyInput, decimals: number = 0): number | string {
  const normalized = decimalString(amount);
  if (!normalized) return typeof amount === 'string' ? '0' : 0;
  const rounded = roundDecimal(normalized, decimals);
  return typeof amount === 'string' ? rounded : Number(rounded);
}

/**
 * Calculate percentage change between two values
 * @param oldValue - Original value
 * @param newValue - New value
 * @returns Percentage change (can be negative)
 */
export function calculatePercentageChange(
  oldValue: number,
  newValue: number
): { percentage: number; formatted: string; isPositive: boolean } {
  if (oldValue === 0) {
    return {
      percentage: newValue > 0 ? 100 : 0,
      formatted: newValue > 0 ? '+100%' : '0%',
      isPositive: newValue > 0,
    };
  }

  const change = ((newValue - oldValue) / Math.abs(oldValue)) * 100;
  const rounded = Math.round(change * 100) / 100;

  return {
    percentage: rounded,
    formatted: `${rounded >= 0 ? '+' : ''}${rounded.toFixed(2)}%`,
    isPositive: rounded >= 0,
  };
}
