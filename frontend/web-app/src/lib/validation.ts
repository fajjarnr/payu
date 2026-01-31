/**
 * Validation Utility Functions for PayU Digital Banking
 * Handles Indonesian-specific validations (NIK, phone, account numbers)
 */

/**
 * Indonesian phone number validation
 * Supports formats: +628, 08, 62 (followed by 8-12 digits)
 */
export function validatePhoneNumber(phone: string | null | undefined): {
  isValid: boolean;
  error?: string;
  normalized?: string;
} {
  if (!phone) {
    return { isValid: false, error: 'Nomor telepon wajib diisi' };
  }

  // Remove all non-numeric characters
  const cleaned = phone.replace(/\D/g, '');

  // Check length: Indonesian mobile numbers are 10-13 digits total
  if (cleaned.length < 10 || cleaned.length > 13) {
    return { isValid: false, error: 'Nomor telepon harus 10-13 digit' };
  }

  // Check if it starts with valid Indonesian prefix
  const hasValidPrefix =
    cleaned.startsWith('08') ||
    cleaned.startsWith('628') ||
    cleaned.startsWith('6208');

  if (!hasValidPrefix) {
    return {
      isValid: false,
      error: 'Nomor telepon harus dimulai dengan 08 atau +628',
    };
  }

  // Normalize to 08 format for storage
  let normalized = cleaned;
  if (normalized.startsWith('628')) {
    normalized = '0' + normalized.substring(2);
  } else if (normalized.startsWith('6208')) {
    normalized = '0' + normalized.substring(3);
  }

  return { isValid: true, normalized };
}

/**
 * Email validation with comprehensive checks
 */
export function validateEmail(email: string | null | undefined): {
  isValid: boolean;
  error?: string;
  normalized?: string;
} {
  if (!email) {
    return { isValid: false, error: 'Email wajib diisi' };
  }

  const trimmed = email.trim();

  // Basic format check
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(trimmed)) {
    return { isValid: false, error: 'Format email tidak valid' };
  }

  // Check for consecutive dots
  if (/\.\./.test(trimmed)) {
    return { isValid: false, error: 'Email tidak boleh mengandung titik beruntun' };
  }

  // Check local part length (before @)
  const localPart = trimmed.split('@')[0];
  if (localPart.length > 64) {
    return { isValid: false, error: 'Bagian sebelum @ terlalu panjang (maksimal 64 karakter)' };
  }

  // Check total length
  if (trimmed.length > 254) {
    return { isValid: false, error: 'Email terlalu panjang (maksimal 254 karakter)' };
  }

  // Check for common typos in popular domains
  const domain = trimmed.split('@')[1].toLowerCase();
  const commonDomains = {
    'gmail.co': 'gmail.com',
    'yahoo.co': 'yahoo.com',
    'outlook.co': 'outlook.com',
    'hotmail.co': 'hotmail.com',
    'ymail.co': 'ymail.com',
  };

  if (commonDomains[domain as keyof typeof commonDomains]) {
    return {
      isValid: false,
      error: `Apakah Anda maksud ${commonDomains[domain as keyof typeof commonDomains]}?`,
    };
  }

  return { isValid: true, normalized: trimmed.toLowerCase() };
}

/**
 * Indonesian NIK (Nomor Induk Kependudukan) validation
 * NIK is 16 digits, contains province, regency, district, and serial info
 */
export function validateNIK(nik: string | null | undefined): {
  isValid: boolean;
  error?: string;
  masked?: string;
} {
  if (!nik) {
    return { isValid: false, error: 'NIK wajib diisi' };
  }

  // Remove non-numeric characters
  const cleaned = nik.replace(/\D/g, '');

  // Check length
  if (cleaned.length !== 16) {
    return { isValid: false, error: 'NIK harus 16 digit' };
  }

  // Check province code (first 2 digits)
  const provinceCode = parseInt(cleaned.substring(0, 2));
  if (provinceCode < 1 || provinceCode > 94) {
    // Special codes like 91-94 for foreign citizens
    return { isValid: false, error: 'Kode provinsi tidak valid' };
  }

  // Check if not all same digits (basic validation)
  if (/^(\d)\1{15}$/.test(cleaned)) {
    return { isValid: false, error: 'NIK tidak valid' };
  }

  // Mask for display (show first 6 and last 4, mask middle 6)
  const masked = cleaned.substring(0, 6) + '******' + cleaned.substring(12);

  return { isValid: true, masked };
}

/**
 * Bank account number validation (Indonesian format)
 */
export function validateAccountNumber(account: string | null | undefined): {
  isValid: boolean;
  error?: string;
  masked?: string;
} {
  if (!account) {
    return { isValid: false, error: 'Nomor rekening wajib diisi' };
  }

  // Remove spaces and dashes (common formatting)
  const cleaned = account.replace(/[\s-]/g, '');

  // Check length (Indonesian accounts are typically 10-16 digits)
  if (cleaned.length < 10 || cleaned.length > 16) {
    return { isValid: false, error: 'Nomor rekening harus 10-16 digit' };
  }

  // Check if all digits
  if (!/^\d+$/.test(cleaned)) {
    return { isValid: false, error: 'Nomor rekening hanya boleh berisi angka' };
  }

  // Check if not all same digits
  if (/^(\d)\1{9,}$/.test(cleaned)) {
    return { isValid: false, error: 'Nomor rekening tidak valid' };
  }

  // Mask for display (show first 4 and last 4)
  const masked = cleaned.substring(0, 4) + '*'.repeat(cleaned.length - 8) + cleaned.substring(cleaned.length - 4);

  return { isValid: true, masked };
}

/**
 * Indonesian postal code validation
 */
export function validatePostalCode(code: string | null | undefined): {
  isValid: boolean;
  error?: string;
} {
  if (!code) {
    return { isValid: false, error: 'Kode pos wajib diisi' };
  }

  const cleaned = code.replace(/\D/g, '');

  // Indonesian postal codes are 5 digits
  if (cleaned.length !== 5) {
    return { isValid: false, error: 'Kode pos harus 5 digit' };
  }

  // Valid range (roughly)
  const codeNum = parseInt(cleaned);
  if (codeNum < 10000 || codeNum > 99999) {
    return { isValid: false, error: 'Kode pos tidak valid' };
  }

  return { isValid: true };
}

/**
 * Tax ID (NPWP) validation for Indonesia
 * Format: XX.XXX.XXX.X-XXX.XXX
 */
export function validateNPWP(npwp: string | null | undefined): {
  isValid: boolean;
  error?: string;
  normalized?: string;
} {
  if (!npwp) {
    return { isValid: false, error: 'NPWP wajib diisi' };
  }

  // Remove formatting
  const cleaned = npwp.replace(/[\.\-]/g, '');

  // NPWP is 15 digits
  if (cleaned.length !== 15) {
    return { isValid: false, error: 'NPWP harus 15 digit' };
  }

  // Check if all digits
  if (!/^\d+$/.test(cleaned)) {
    return { isValid: false, error: 'NPWP hanya boleh berisi angka' };
  }

  // Check tax office code (first 2 digits, 01-09)
  const taxOfficeCode = parseInt(cleaned.substring(0, 2));
  if (taxOfficeCode < 1 || taxOfficeCode > 9) {
    return { isValid: false, error: 'Kode Kantor Pajak tidak valid' };
  }

  // Format as XX.XXX.XXX.X-XXX.XXX
  const normalized =
    cleaned.substring(0, 2) +
    '.' +
    cleaned.substring(2, 5) +
    '.' +
    cleaned.substring(5, 8) +
    '.' +
    cleaned.substring(8, 9) +
    '-' +
    cleaned.substring(9, 12) +
    '.' +
    cleaned.substring(12);

  return { isValid: true, normalized };
}

/**
 * Credit card number validation (Luhn algorithm)
 */
export function validateCreditCard(cardNumber: string | null | undefined): {
  isValid: boolean;
  error?: string;
  type?: string;
  masked?: string;
} {
  if (!cardNumber) {
    return { isValid: false, error: 'Nomor kartu wajib diisi' };
  }

  const cleaned = cardNumber.replace(/\s/g, '');

  // Check length (13-19 digits)
  if (cleaned.length < 13 || cleaned.length > 19) {
    return { isValid: false, error: 'Nomor kartu tidak valid' };
  }

  // Check if all digits
  if (!/^\d+$/.test(cleaned)) {
    return { isValid: false, error: 'Nomor kartu hanya boleh berisi angka' };
  }

  // Luhn algorithm
  let sum = 0;
  let isEven = false;

  for (let i = cleaned.length - 1; i >= 0; i--) {
    let digit = parseInt(cleaned[i]);

    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    isEven = !isEven;
  }

  if (sum % 10 !== 0) {
    return { isValid: false, error: 'Nomor kartu tidak valid' };
  }

  // Detect card type
  let type = 'Unknown';
  if (/^4/.test(cleaned)) {
    type = 'Visa';
  } else if (/^5[1-5]/.test(cleaned)) {
    type = 'Mastercard';
  } else if (/^3[47]/.test(cleaned)) {
    type = 'American Express';
  } else if (/^6(?:011|5)/.test(cleaned)) {
    type = 'Discover';
  }

  // Mask (show first 6 and last 4)
  const masked =
    cleaned.substring(0, 6) + '*'.repeat(cleaned.length - 10) + cleaned.substring(cleaned.length - 4);

  return { isValid: true, type, masked };
}

/**
 * CVV/CVC validation
 */
export function validateCVV(cvv: string | null | undefined, cardType?: string): {
  isValid: boolean;
  error?: string;
} {
  if (!cvv) {
    return { isValid: false, error: 'CVV wajib diisi' };
  }

  const cleaned = cvv.replace(/\D/g, '');

  // Amex has 4 digits, others have 3
  const requiredLength = cardType === 'American Express' ? 4 : 3;

  if (cleaned.length !== requiredLength) {
    return { isValid: false, error: `CVV harus ${requiredLength} digit` };
  }

  return { isValid: true };
}

/**
 * Password strength validation
 */
export function validatePassword(password: string | null | undefined): {
  isValid: boolean;
  strength: 'weak' | 'medium' | 'strong';
  errors: string[];
} {
  const errors: string[] = [];

  if (!password) {
    return {
      isValid: false,
      strength: 'weak',
      errors: ['Password wajib diisi'],
    };
  }

  if (password.length < 8) {
    errors.push('Password minimal 8 karakter');
  }

  if (password.length > 128) {
    errors.push('Password maksimal 128 karakter');
  }

  if (!/[a-z]/.test(password)) {
    errors.push('Password harus mengandung huruf kecil');
  }

  if (!/[A-Z]/.test(password)) {
    errors.push('Password harus mengandung huruf besar');
  }

  if (!/\d/.test(password)) {
    errors.push('Password harus mengandung angka');
  }

  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    errors.push('Password harus mengandung karakter spesial');
  }

  // Calculate strength
  let strength: 'weak' | 'medium' | 'strong' = 'weak';
  const criteriaMet = 5 - errors.filter(e => e.includes('karakter')).length;

  if (criteriaMet >= 4 && password.length >= 12) {
    strength = 'strong';
  } else if (criteriaMet >= 3 && password.length >= 8) {
    strength = 'medium';
  }

  return {
    isValid: errors.length === 0,
    strength,
    errors,
  };
}

/**
 * Amount validation for transactions
 */
export function validateAmount(
  amount: number | string | null | undefined,
  options: {
    min?: number;
    max?: number;
    allowZero?: boolean;
  } = {}
): {
  isValid: boolean;
  error?: string;
  parsed?: number;
} {
  const { min = 10000, max = 50000000, allowZero = false } = options;

  if (amount === null || amount === undefined || amount === '') {
    return { isValid: false, error: 'Nominal wajib diisi' };
  }

  const parsed = typeof amount === 'string' ? parseFloat(amount.replace(/[^\d.,-]/g, '')) : amount;

  if (isNaN(parsed)) {
    return { isValid: false, error: 'Nominal tidak valid' };
  }

  if (!allowZero && parsed === 0) {
    return { isValid: false, error: 'Nominal tidak boleh nol' };
  }

  if (parsed < min) {
    return { isValid: false, error: `Nominal minimal ${new Intl.NumberFormat('id-ID').format(min)}` };
  }

  if (parsed > max) {
    return { isValid: false, error: `Nominal maksimal ${new Intl.NumberFormat('id-ID').format(max)}` };
  }

  return { isValid: true, parsed };
}

/**
 * Name validation (Indonesian context)
 */
export function validateName(name: string | null | undefined): {
  isValid: boolean;
  error?: string;
  normalized?: string;
} {
  if (!name) {
    return { isValid: false, error: 'Nama wajib diisi' };
  }

  const trimmed = name.trim();

  if (trimmed.length < 2) {
    return { isValid: false, error: 'Nama terlalu pendek' };
  }

  if (trimmed.length > 100) {
    return { isValid: false, error: 'Nama terlalu panjang (maksimal 100 karakter)' };
  }

  // Check for valid characters (letters, spaces, dots, commas, hyphens)
  if (!/^[a-zA-Z\s\.\,\-\']+$/.test(trimmed)) {
    return { isValid: false, error: 'Nama hanya boleh mengandung huruf dan karakter spesial tertentu' };
  }

  // Check for multiple consecutive spaces
  if (/\s{2,}/.test(trimmed)) {
    return { isValid: false, error: 'Nama tidak boleh mengandung spasi beruntun' };
  }

  // Normalize: capitalize each word, remove extra spaces
  const normalized = trimmed
    .split(/\s+/)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');

  return { isValid: true, normalized };
}

/**
 * OTP validation
 */
export function validateOTP(otp: string | null | undefined, length: number = 6): {
  isValid: boolean;
  error?: string;
} {
  if (!otp) {
    return { isValid: false, error: 'OTP wajib diisi' };
  }

  const cleaned = otp.replace(/\s/g, '');

  if (cleaned.length !== length) {
    return { isValid: false, error: `OTP harus ${length} digit` };
  }

  if (!/^\d+$/.test(cleaned)) {
    return { isValid: false, error: 'OTP hanya boleh berisi angka' };
  }

  return { isValid: true };
}
