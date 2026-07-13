/**
 * Currency Utility Functions for PayU Digital Banking
 * Handles Indonesian Rupiah (IDR) formatting and parsing
 */

/**
 * Format a number to Indonesian Rupiah currency string
 * @param amount - The amount to format
 * @param options - Formatting options
 * @returns Formatted currency string (e.g., "Rp 1.000.000" or "Rp 1.000.000,00")
 */
export function formatCurrency(
  amount: number | string | null | undefined,
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

  // Convert to number
  const numAmount = typeof amount === 'string' ? Number(amount.replace(/,/g, '')) : amount;

  // Handle NaN
  if (isNaN(numAmount)) {
    return `${symbol} 0`;
  }

  // Indonesian number format: uses dot for thousands, comma for decimals
  const formatter = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: withDecimals ? 2 : 0,
    maximumFractionDigits: withDecimals ? 2 : 0,
    notation: compact ? 'compact' : 'standard',
    compactDisplay: 'short',
  });

  let formatted = formatter.format(numAmount);

  // Intl.NumberFormat adds "Rp" with a space, but we want custom formatting
  // For standard Indonesian format: Rp 1.000.000 (without decimals) or Rp 1.000.000,00 (with decimals)
  if (symbol !== 'Rp') {
    // Replace default symbol if custom one provided
    formatted = formatted.replace('Rp', symbol);
  }

  return formatted;
}

/**
 * Format currency amount without symbol (for editable inputs)
 * @param amount - The amount to format
 * @param options - Formatting options
 * @returns Formatted number string (e.g., "1.000.000")
 */
export function formatCurrencyWithoutSymbol(
  amount: number | string | null | undefined,
  options: { withDecimals?: boolean; locale?: string } = {}
): string {
  const { withDecimals = false, locale = 'id-ID' } = options;

  if (amount === null || amount === undefined) {
    return '0';
  }

  const numAmount = typeof amount === 'string' ? parseFloat(amount) : amount;

  if (isNaN(numAmount)) {
    return '0';
  }

  const formatter = new Intl.NumberFormat(locale, {
    minimumFractionDigits: withDecimals ? 2 : 0,
    maximumFractionDigits: withDecimals ? 2 : 0,
  });

  return formatter.format(numAmount);
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

  const parsed = parseFloat(cleaned);

  return isNaN(parsed) ? 0 : parsed;
}

/**
 * Format amount for display in transaction history
 * Shows different colors for positive/negative amounts
 * @param amount - The amount to format
 * @param options - Formatting options
 * @returns Object with formatted string and type (credit/debit)
 */
export function formatTransactionAmount(
  amount: number,
  options: { withDecimals?: boolean } = {}
): { formatted: string; type: 'credit' | 'debit'; isPositive: boolean } {
  const absAmount = Math.abs(amount);
  const formatted = formatCurrency(absAmount, options);

  return {
    formatted,
    type: amount >= 0 ? 'credit' : 'debit',
    isPositive: amount >= 0,
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
export function roundCurrency(amount: number, decimals: number = 0): number {
  // BUG-FE-047: Math.round(amount * multiplier) / multiplier has floating point errors
  // e.g., Math.round(1.005 * 100) / 100 = 1.00 (not 1.01)
  const multiplier = 10 ** decimals;
  return Math.round((amount + Number.EPSILON) * multiplier) / multiplier;
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
