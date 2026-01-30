import {
  validateEmail,
  validatePhoneNumber,
  validatePassword,
  validateAmount,
  maskAccountNumber,
  maskCardNumber,
} from '../validation';

describe('validateEmail', () => {
  it('should return true for valid email', () => {
    expect(validateEmail('test@example.com')).toBe(true);
  });

  it('should return true for valid email with subdomain', () => {
    expect(validateEmail('test@mail.example.com')).toBe(true);
  });

  it('should return true for valid email with plus sign', () => {
    expect(validateEmail('test+label@example.com')).toBe(true);
  });

  it('should return false for email without @ symbol', () => {
    expect(validateEmail('testexample.com')).toBe(false);
  });

  it('should return false for email without domain', () => {
    expect(validateEmail('test@')).toBe(false);
  });

  it('should return false for email without local part', () => {
    expect(validateEmail('@example.com')).toBe(false);
  });

  it('should return false for email without TLD', () => {
    expect(validateEmail('test@example')).toBe(false);
  });

  it('should return false for email with spaces', () => {
    expect(validateEmail('test @example.com')).toBe(false);
  });

  it('should return false for empty string', () => {
    expect(validateEmail('')).toBe(false);
  });

  it('should return false for email with multiple @ symbols', () => {
    expect(validateEmail('test@@example.com')).toBe(false);
  });
});

describe('validatePhoneNumber', () => {
  it('should return true for valid phone with 08 prefix', () => {
    expect(validatePhoneNumber('081234567890')).toBe(true);
  });

  it('should return true for valid phone with +62 prefix', () => {
    expect(validatePhoneNumber('+6281234567890')).toBe(true);
  });

  it('should return true for valid phone with 62 prefix', () => {
    expect(validatePhoneNumber('6281234567890')).toBe(true);
  });

  it('should return true for phone with spaces', () => {
    expect(validatePhoneNumber('0812 3456 7890')).toBe(true);
  });

  it('should return true for phone with hyphens', () => {
    expect(validatePhoneNumber('0812-3456-7890')).toBe(true);
  });

  it('should return false for phone with invalid prefix', () => {
    expect(validatePhoneNumber('071234567890')).toBe(false);
  });

  it('should return false for phone that is too short', () => {
    expect(validatePhoneNumber('081234')).toBe(false);
  });

  it('should return false for phone that is too long', () => {
    expect(validatePhoneNumber('081234567890123456')).toBe(false);
  });

  it('should return false for empty string', () => {
    expect(validatePhoneNumber('')).toBe(false);
  });

  it('should return false for phone with letters', () => {
    expect(validatePhoneNumber('0812abc7890')).toBe(false);
  });

  it('should return false for phone starting with 08 but with invalid second digit (0)', () => {
    expect(validatePhoneNumber('08012345678')).toBe(false);
  });
});

describe('validatePassword', () => {
  it('should return valid for strong password', () => {
    const result = validatePassword('StrongPass123');
    expect(result.isValid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('should return invalid for password less than 8 characters', () => {
    const result = validatePassword('Short1');
    expect(result.isValid).toBe(false);
    expect(result.errors).toContain('Password must be at least 8 characters');
  });

  it('should return invalid for password without uppercase', () => {
    const result = validatePassword('lowercase123');
    expect(result.isValid).toBe(false);
    expect(result.errors).toContain('Password must contain at least one uppercase letter');
  });

  it('should return invalid for password without lowercase', () => {
    const result = validatePassword('UPPERCASE123');
    expect(result.isValid).toBe(false);
    expect(result.errors).toContain('Password must contain at least one lowercase letter');
  });

  it('should return invalid for password without number', () => {
    const result = validatePassword('NoNumbersHere');
    expect(result.isValid).toBe(false);
    expect(result.errors).toContain('Password must contain at least one number');
  });

  it('should return multiple errors for weak password', () => {
    const result = validatePassword('weak');
    expect(result.isValid).toBe(false);
    expect(result.errors.length).toBeGreaterThan(1);
  });

  it('should return valid for password with exactly 8 characters', () => {
    const result = validatePassword('Pass1234');
    expect(result.isValid).toBe(true);
  });

  it('should return valid for password with special characters', () => {
    const result = validatePassword('Strong@Pass123');
    expect(result.isValid).toBe(true);
  });

  it('should return invalid for empty password', () => {
    const result = validatePassword('');
    expect(result.isValid).toBe(false);
    expect(result.errors).toContain('Password must be at least 8 characters');
  });
});

describe('validateAmount', () => {
  it('should return true for positive amount', () => {
    expect(validateAmount(100000)).toBe(true);
  });

  it('should return true for small positive amount', () => {
    expect(validateAmount(0.01)).toBe(true);
  });

  it('should return false for zero amount', () => {
    expect(validateAmount(0)).toBe(false);
  });

  it('should return false for negative amount', () => {
    expect(validateAmount(-100000)).toBe(false);
  });

  it('should return false for Infinity', () => {
    expect(validateAmount(Infinity)).toBe(false);
  });

  it('should return false for negative Infinity', () => {
    expect(validateAmount(-Infinity)).toBe(false);
  });

  it('should return false for NaN', () => {
    expect(validateAmount(NaN)).toBe(false);
  });

  it('should return true for large finite amount', () => {
    expect(validateAmount(999999999999)).toBe(true);
  });
});

describe('maskAccountNumber', () => {
  it('should mask account number except last 4 digits', () => {
    expect(maskAccountNumber('1234567890')).toBe('******7890');
  });

  it('should return account number as is if 4 characters or less', () => {
    expect(maskAccountNumber('1234')).toBe('1234');
  });

  it('should return account number as is if less than 4 characters', () => {
    expect(maskAccountNumber('123')).toBe('123');
  });

  it('should handle empty string', () => {
    expect(maskAccountNumber('')).toBe('');
  });

  it('should mask all but last 4 for long account number', () => {
    expect(maskAccountNumber('1234567890123456')).toBe('************3456');
  });

  it('should handle exactly 5 characters', () => {
    expect(maskAccountNumber('12345')).toBe('*2345');
  });
});

describe('maskCardNumber', () => {
  it('should mask first 8 digits of 16-digit card number', () => {
    expect(maskCardNumber('1234567890123456')).toBe('**** **** 9012 3456');
  });

  it('should mask card number with spaces', () => {
    expect(maskCardNumber('1234 5678 9012 3456')).toBe('**** **** 9012 3456');
  });

  it('should return original string if not 16 digits', () => {
    expect(maskCardNumber('1234567890')).toBe('1234567890');
  });

  it('should return original string if 15 digits', () => {
    expect(maskCardNumber('123456789012345')).toBe('123456789012345');
  });

  it('should return original string if 17 digits', () => {
    expect(maskCardNumber('12345678901234567')).toBe('12345678901234567');
  });

  it('should handle empty string', () => {
    expect(maskCardNumber('')).toBe('');
  });

  it('should handle card number with extra spaces', () => {
    expect(maskCardNumber('1234  5678  9012  3456')).toBe('**** **** 9012 3456');
  });
});
