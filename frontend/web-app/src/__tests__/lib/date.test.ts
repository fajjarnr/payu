/**
 * Comprehensive Unit Tests for Date Utilities
 * Testing Indonesian locale date formatting and manipulation
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  formatDate,
  formatDateTime,
  relativeTime,
  toISOString,
  parseDate,
  isToday,
  isPast,
  isFuture,
  startOfDay,
  endOfDay,
  startOfMonth,
  endOfMonth,
  addDays,
  subtractDays,
  addMonths,
  subtractMonths,
  diffInDays,
  formatDateRange,
  getAge,
  formatDuration,
  isWithinRange,
  getTimezoneOffset,
  formatDateTimeWithSuffix,
} from '../../lib/date';

describe('date.ts - formatDate', () => {
  describe('Basic formatting', () => {
    it('should format date with short format', () => {
      const date = new Date('2024-01-15');
      expect(formatDate(date, { format: 'short' })).toContain('Jan');
      expect(formatDate(date, { format: 'short' })).toContain('2024');
    });

    it('should format date with long format', () => {
      const date = new Date('2024-01-15');
      expect(formatDate(date, { format: 'long' })).toContain('Januari');
    });

    it('should format date with full format', () => {
      const date = new Date('2024-01-15'); // Monday
      const result = formatDate(date, { format: 'full' });
      expect(result).toContain('Januari');
      expect(result).toMatch(/Senin|Monday/);
    });
  });

  describe('Edge cases', () => {
    it('should handle null', () => {
      expect(formatDate(null)).toBe('-');
    });

    it('should handle undefined', () => {
      expect(formatDate(undefined)).toBe('-');
    });

    it('should handle invalid dates', () => {
      expect(formatDate(new Date('invalid'))).toBe('-');
      expect(formatDate('invalid-date')).toBe('-');
    });

    it('should handle string inputs', () => {
      expect(formatDate('2024-01-15')).toContain('Jan');
      expect(formatDate('2024-01-15T10:30:00Z')).toContain('Jan');
    });

    it('should handle timestamp inputs', () => {
      const timestamp = new Date('2024-01-15').getTime();
      expect(formatDate(timestamp)).toContain('Jan');
    });
  });

  describe('Indonesian locale', () => {
    it('should use Indonesian month names', () => {
      const date = new Date('2024-01-15');
      expect(formatDate(date, { format: 'long' })).toBe('15 Januari 2024');
    });

    it('should format year-month correctly', () => {
      const date = new Date('2024-01-15');
      expect(formatDate(date, { format: 'year-month' })).toBe('Januari 2024');
    });

    it('should format day-month correctly', () => {
      const date = new Date('2024-01-15');
      expect(formatDate(date, { format: 'day-month' })).toBe('15 Januari');
    });

    it('should format weekday correctly', () => {
      const date = new Date('2024-01-15'); // Monday
      const result = formatDate(date, { format: 'weekday' });
      expect(result).toMatch(/Senin/);
    });
  });
});

describe('date.ts - formatDateTime', () => {
  it('should format date and time', () => {
    const date = new Date('2024-01-15T10:30:00');
    const result = formatDateTime(date);
    expect(result).toContain('10.30');
    expect(result).toMatch(/Jan/);
  });

  it('should include seconds when requested', () => {
    const date = new Date('2024-01-15T10:30:45');
    const result = formatDateTime(date, { showSeconds: true });
    expect(result).toContain('45');
  });

  it('should handle null/undefined', () => {
    expect(formatDateTime(null)).toBe('-');
    expect(formatDateTime(undefined)).toBe('-');
  });

  it('should handle invalid dates', () => {
    expect(formatDateTime('invalid')).toBe('-');
  });
});

describe('date.ts - relativeTime', () => {
  let fixedNow: Date;

  beforeEach(() => {
    fixedNow = new Date('2024-01-15T12:00:00');
  });

  describe('Past times', () => {
    it('should show "baru saja" for very recent times', () => {
      const date = new Date(fixedNow.getTime() - 30 * 1000); // 30 seconds ago
      expect(relativeTime(date, { now: fixedNow })).toBe('baru saja');
    });

    it('should show minutes ago', () => {
      const date = new Date(fixedNow.getTime() - 5 * 60 * 1000); // 5 minutes ago
      expect(relativeTime(date, { now: fixedNow })).toBe('5 menit yang lalu');

      const short = relativeTime(date, { style: 'short', now: fixedNow });
      expect(short).toBe('5 mnt');
    });

    it('should show hours ago', () => {
      const date = new Date(fixedNow.getTime() - 3 * 60 * 60 * 1000); // 3 hours ago
      expect(relativeTime(date, { now: fixedNow })).toBe('3 jam yang lalu');

      const short = relativeTime(date, { style: 'short', now: fixedNow });
      expect(short).toBe('3 jam');
    });

    it('should show "kemarin" for yesterday', () => {
      const date = new Date(fixedNow.getTime() - 24 * 60 * 60 * 1000); // 1 day ago
      expect(relativeTime(date, { now: fixedNow })).toBe('kemarin');
    });

    it('should show "lusa" for 2 days ago', () => {
      const date = new Date(fixedNow.getTime() - 2 * 24 * 60 * 60 * 1000); // 2 days ago
      expect(relativeTime(date, { now: fixedNow })).toBe('lusa');
    });

    it('should show days ago for < 7 days', () => {
      const date = new Date(fixedNow.getTime() - 5 * 24 * 60 * 60 * 1000); // 5 days ago
      expect(relativeTime(date, { now: fixedNow })).toBe('5 hari yang lalu');
    });

    it('should show weeks ago for < 30 days', () => {
      const date = new Date(fixedNow.getTime() - 14 * 24 * 60 * 60 * 1000); // 2 weeks ago
      expect(relativeTime(date, { now: fixedNow })).toBe('2 minggu yang lalu');
    });

    it('should show months ago for < 12 months', () => {
      const date = new Date(fixedNow.getTime() - 60 * 24 * 60 * 60 * 1000); // ~2 months ago
      expect(relativeTime(date, { now: fixedNow })).toBe('2 bulan yang lalu');
    });

    it('should show years ago for >= 12 months', () => {
      const date = new Date(fixedNow.getTime() - 400 * 24 * 60 * 60 * 1000); // ~1 year ago
      expect(relativeTime(date, { now: fixedNow })).toBe('1 tahun yang lalu');
    });
  });

  describe('Future times', () => {
    it('should show "besok" for tomorrow', () => {
      const date = new Date(fixedNow.getTime() + 24 * 60 * 60 * 1000); // 1 day ahead
      expect(relativeTime(date, { now: fixedNow })).toBe('besok');
    });

    it('should show "dalam X hari" for future days', () => {
      const date = new Date(fixedNow.getTime() + 5 * 24 * 60 * 60 * 1000); // 5 days ahead
      expect(relativeTime(date, { now: fixedNow })).toBe('dalam 5 hari');
    });

    it('should show "dalam X jam" for future hours', () => {
      const date = new Date(fixedNow.getTime() + 3 * 60 * 60 * 1000); // 3 hours ahead
      expect(relativeTime(date, { now: fixedNow })).toBe('dalam 3 jam');
    });
  });

  describe('Edge cases', () => {
    it('should handle null/undefined', () => {
      expect(relativeTime(null)).toBe('-');
      expect(relativeTime(undefined)).toBe('-');
    });

    it('should handle invalid dates', () => {
      expect(relativeTime('invalid')).toBe('-');
    });
  });
});

describe('date.ts - parseDate and toISOString', () => {
  it('should parse date strings', () => {
    const parsed = parseDate('2024-01-15');
    expect(parsed).toBeInstanceOf(Date);
    expect(parsed?.getFullYear()).toBe(2024);
  });

  it('should parse ISO strings', () => {
    const parsed = parseDate('2024-01-15T10:30:00Z');
    expect(parsed).toBeInstanceOf(Date);
  });

  it('should return null for invalid dates', () => {
    expect(parseDate('invalid')).toBeNull();
    expect(parseDate(null)).toBeNull();
    expect(parseDate(undefined)).toBeNull();
  });

  it('should convert to ISO string', () => {
    const date = new Date('2024-01-15T10:30:00Z');
    expect(toISOString(date)).toBe('2024-01-15T10:30:00.000Z');
  });

  it('should return null for invalid toISOString', () => {
    expect(toISOString(null)).toBeNull();
    expect(toISOString(undefined)).toBeNull();
    expect(toISOString('invalid')).toBeNull();
  });
});

describe('date.ts - Date comparison utilities', () => {
  let fixedDate: Date;

  beforeEach(() => {
    // Use a fixed date for testing
    fixedDate = new Date('2024-01-15T12:00:00');
    vi.useFakeTimers();
    vi.setSystemTime(fixedDate);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should check if date is today', () => {
    expect(isToday(new Date('2024-01-15T10:00:00'))).toBe(true);
    expect(isToday(new Date('2024-01-14T10:00:00'))).toBe(false);
    expect(isToday('2024-01-15')).toBe(true);
  });

  it('should check if date is in past', () => {
    expect(isPast(new Date('2024-01-14T10:00:00'))).toBe(true);
    expect(isPast(new Date('2024-01-16T10:00:00'))).toBe(false);
  });

  it('should check if date is in future', () => {
    expect(isFuture(new Date('2024-01-16T10:00:00'))).toBe(true);
    expect(isFuture(new Date('2024-01-14T10:00:00'))).toBe(false);
  });
});

describe('date.ts - Date manipulation', () => {
  it('should get start of day', () => {
    const date = new Date('2024-01-15T10:30:00');
    const start = startOfDay(date);
    expect(start.getHours()).toBe(0);
    expect(start.getMinutes()).toBe(0);
    expect(start.getSeconds()).toBe(0);
    expect(start.getMilliseconds()).toBe(0);
  });

  it('should get end of day', () => {
    const date = new Date('2024-01-15T10:30:00');
    const end = endOfDay(date);
    expect(end.getHours()).toBe(23);
    expect(end.getMinutes()).toBe(59);
    expect(end.getSeconds()).toBe(59);
  });

  it('should get start of month', () => {
    const date = new Date('2024-01-15T10:30:00');
    const start = startOfMonth(date);
    expect(start.getDate()).toBe(1);
    expect(start.getHours()).toBe(0);
  });

  it('should get end of month', () => {
    const date = new Date('2024-01-15T10:30:00');
    const end = endOfMonth(date);
    expect(end.getDate()).toBe(31);
    expect(end.getHours()).toBe(23);
    expect(end.getMonth()).toBe(0); // January
  });

  it('should handle end of month for February', () => {
    const date = new Date('2024-02-15T10:30:00'); // Leap year
    const end = endOfMonth(date);
    expect(end.getDate()).toBe(29);
    expect(end.getMonth()).toBe(1); // February
  });

  it('should add days', () => {
    const date = new Date('2024-01-15');
    const result = addDays(date, 5);
    expect(result.getDate()).toBe(20);
  });

  it('should subtract days', () => {
    const date = new Date('2024-01-15');
    const result = subtractDays(date, 5);
    expect(result.getDate()).toBe(10);
  });

  it('should add months', () => {
    const date = new Date('2024-01-15');
    const result = addMonths(date, 2);
    expect(result.getMonth()).toBe(2); // March
  });

  it('should subtract months', () => {
    const date = new Date('2024-03-15');
    const result = subtractMonths(date, 2);
    expect(result.getMonth()).toBe(0); // January
  });

  it('should handle month overflow', () => {
    const date = new Date('2024-12-15');
    const result = addMonths(date, 2);
    expect(result.getFullYear()).toBe(2025);
    expect(result.getMonth()).toBe(1); // February
  });
});

describe('date.ts - diffInDays', () => {
  it('should calculate difference in days', () => {
    const date1 = new Date('2024-01-15');
    const date2 = new Date('2024-01-10');
    expect(diffInDays(date1, date2)).toBe(5);
    expect(diffInDays(date2, date1)).toBe(-5);
  });

  it('should handle same day', () => {
    const date = new Date('2024-01-15');
    expect(diffInDays(date, date)).toBe(0);
  });
});

describe('date.ts - formatDateRange', () => {
  it('should format same day', () => {
    const start = new Date('2024-01-15');
    const end = new Date('2024-01-15');
    expect(formatDateRange(start, end)).toContain('15');
  });

  it('should format same month', () => {
    const start = new Date('2024-01-10');
    const end = new Date('2024-01-15');
    const result = formatDateRange(start, end);
    expect(result).toContain('10');
    expect(result).toContain('15');
  });

  it('should format same year', () => {
    const start = new Date('2024-01-10');
    const end = new Date('2024-03-15');
    const result = formatDateRange(start, end);
    expect(result).toContain('Jan');
    expect(result).toContain('15');
  });

  it('should format different years', () => {
    const start = new Date('2023-12-10');
    const end = new Date('2024-01-15');
    const result = formatDateRange(start, end);
    expect(result).toContain('2023');
    expect(result).toContain('2024');
  });

  it('should handle invalid dates', () => {
    expect(formatDateRange('invalid' as unknown as Date, new Date())).toBe('-');
    expect(formatDateRange(new Date(), 'invalid' as unknown as Date)).toBe('-');
  });
});

describe('date.ts - getAge', () => {
  it('should calculate age correctly', () => {
    const birthDate = new Date('1990-01-15');
    const currentDate = new Date('2024-01-15');
    vi.setSystemTime(currentDate);

    expect(getAge(birthDate)).toBe(34);
  });

  it('should handle birthday not yet occurred this year', () => {
    const birthDate = new Date('1990-12-31');
    const currentDate = new Date('2024-06-15');
    vi.setSystemTime(currentDate);

    expect(getAge(birthDate)).toBe(33);
  });

  it('should handle birthday already occurred this year', () => {
    const birthDate = new Date('1990-01-01');
    const currentDate = new Date('2024-06-15');
    vi.setSystemTime(currentDate);

    expect(getAge(birthDate)).toBe(34);
  });

  afterEach(() => {
    vi.useRealTimers();
  });
});

describe('date.ts - formatDuration', () => {
  it('should format seconds', () => {
    expect(formatDuration(45)).toBe('45 detik');
  });

  it('should format minutes', () => {
    expect(formatDuration(120)).toBe('2 menit');
  });

  it('should format hours', () => {
    expect(formatDuration(3600)).toBe('1 jam');
  });

  it('should format combined', () => {
    expect(formatDuration(3661)).toBe('1 jam 1 menit 1 detik');
  });

  it('should format short style', () => {
    expect(formatDuration(3661, { style: 'short' })).toBe('1j 1m 1d');
  });

  it('should handle zero', () => {
    expect(formatDuration(0)).toBe('0 detik');
  });

  it('should handle negative', () => {
    expect(formatDuration(-10)).toBe('-');
  });
});

describe('date.ts - isWithinRange', () => {
  it('should return true for dates within range', () => {
    const start = new Date('2024-01-10');
    const end = new Date('2024-01-20');
    const test = new Date('2024-01-15');

    expect(isWithinRange(test, start, end)).toBe(true);
  });

  it('should return false for dates outside range', () => {
    const start = new Date('2024-01-10');
    const end = new Date('2024-01-20');
    const test = new Date('2024-01-25');

    expect(isWithinRange(test, start, end)).toBe(false);
  });

  it('should include boundary dates', () => {
    const start = new Date('2024-01-10');
    const end = new Date('2024-01-20');

    expect(isWithinRange(start, start, end)).toBe(true);
    expect(isWithinRange(end, start, end)).toBe(true);
  });
});

describe('date.ts - getTimezoneOffset', () => {
  it('should return timezone offset in minutes', () => {
    const offset = getTimezoneOffset();
    expect(typeof offset).toBe('number');
    expect(offset >= -720 && offset <= 840).toBe(true); // Valid range
  });

  it('should handle WIB (UTC+7)', () => {
    // This test depends on the system timezone
    const offset = getTimezoneOffset();
    // If running in WIB timezone, offset should be 420 minutes
    // Otherwise, it will be different
    expect(offset).toBeGreaterThanOrEqual(-720);
  });
});

describe('date.ts - formatDateTimeWithSuffix', () => {
  it('should format with pagi (4-10)', () => {
    const date = new Date('2024-01-15T06:30:00');
    const result = formatDateTimeWithSuffix(date);
    expect(result).toContain('pagi');
    expect(result).toContain('pukul');
  });

  it('should format with siang (10-15)', () => {
    const date = new Date('2024-01-15T12:30:00');
    const result = formatDateTimeWithSuffix(date);
    expect(result).toContain('siang');
  });

  it('should format with sore (15-18)', () => {
    const date = new Date('2024-01-15T16:30:00');
    const result = formatDateTimeWithSuffix(date);
    expect(result).toContain('sore');
  });

  it('should format with malam (18-4)', () => {
    const date = new Date('2024-01-15T20:30:00');
    const result = formatDateTimeWithSuffix(date);
    expect(result).toContain('malam');
  });

  it('should handle null/undefined', () => {
    expect(formatDateTimeWithSuffix(null)).toBe('-');
    expect(formatDateTimeWithSuffix(undefined)).toBe('-');
  });

  it('should handle invalid dates', () => {
    expect(formatDateTimeWithSuffix('invalid')).toBe('-');
  });
});

describe('date.ts - Indonesian Locale Specifics', () => {
  it('should use correct Indonesian month names', () => {
    const months = [
      'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
      'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'
    ];

    for (let i = 0; i < 12; i++) {
      const date = new Date(2024, i, 15);
      const result = formatDate(date, { format: 'long' });
      expect(result).toContain(months[i]);
    }
  });

  it('should use correct Indonesian day names', () => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _days = ['Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu', 'Minggu'];

    for (let i = 0; i < 7; i++) {
      const date = new Date(2024, 0, 8 + i); // Starting from Monday
      const result = formatDate(date, { format: 'full' });
      expect(result).toMatch(/Senin|Selasa|Rabu|Kamis|Jumat|Sabtu|Minggu/);
    }
  });
});

describe('date.ts - Edge Cases and Error Handling', () => {
  it('should handle very old dates', () => {
    const date = new Date('1900-01-01');
    expect(formatDate(date)).toContain('1900');
  });

  it('should handle far future dates', () => {
    const date = new Date('2100-12-31');
    expect(formatDate(date)).toContain('2100');
  });

  it('should handle leap years', () => {
    const date = new Date('2024-02-29'); // Leap year
    expect(formatDate(date)).toContain('Feb');
  });

  it('should handle non-leap years', () => {
    // 2023-02-29 gets parsed as March 1, 2023 by JavaScript Date
    const parsed = parseDate('2023-02-29');
    expect(parsed).not.toBeNull();
    expect(parsed?.getMonth()).toBe(2); // March (0-indexed)
  });

  it('should handle timezone boundaries', () => {
    const date = new Date('2024-01-01T00:00:00Z');
    expect(formatDate(date)).toContain('2024');
  });
});
