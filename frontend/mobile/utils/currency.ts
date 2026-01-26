export const formatCurrency = (amount: number, currency: string = 'IDR'): string => {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
};

export const parseCurrency = (formatted: string): number => {
  // Remove all non-digit characters except decimal point
  const cleaned = formatted.replace(/[^\d.,]/g, '');
  // Replace comma with dot for decimal
  const normalized = cleaned.replace(/,/g, '.');
  return parseFloat(normalized) || 0;
};

export const formatAmount = (amount: number): string => {
  return new Intl.NumberFormat('id-ID').format(amount);
};
