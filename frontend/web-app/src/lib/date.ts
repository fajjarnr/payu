/**
 * Date Utility Functions for PayU Digital Banking
 * Handles Indonesian locale formatting and timezone handling
 */

/**
 * Format date to Indonesian locale string
 * @param date - Date to format (Date object, ISO string, or timestamp)
 * @param options - Formatting options
 * @returns Formatted date string
 */
export function formatDate(
  date: Date | string | number | null | undefined,
  options: {
    format?: 'short' | 'long' | 'full' | 'year-month' | 'day-month' | 'weekday';
    locale?: string;
  } = {}
): string {
  const { format = 'short', locale = 'id-ID' } = options;

  if (!date) {
    return '-';
  }

  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;

  // Check if invalid date
  if (isNaN(dateObj.getTime())) {
    return '-';
  }

  const formatOptionsMap: Record<string, Intl.DateTimeFormatOptions> = {
    short: {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    },
    long: {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    },
    full: {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    },
    'year-month': {
      month: 'long',
      year: 'numeric',
    },
    'day-month': {
      day: 'numeric',
      month: 'long',
    },
    weekday: {
      weekday: 'long',
    },
  };
  const formatOptions = formatOptionsMap[format];

  return dateObj.toLocaleDateString(locale, formatOptions);
}

/**
 * Format date and time to Indonesian locale string
 * @param date - Date to format
 * @param options - Formatting options
 * @returns Formatted datetime string
 */
export function formatDateTime(
  date: Date | string | number | null | undefined,
  options: {
    showSeconds?: boolean;
    locale?: string;
    format?: 'short' | 'long';
  } = {}
): string {
  const { showSeconds = false, locale = 'id-ID', format = 'short' } = options;

  if (!date) {
    return '-';
  }

  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;

  if (isNaN(dateObj.getTime())) {
    return '-';
  }

  const timeOptions: Intl.DateTimeFormatOptions = {
    hour: '2-digit',
    minute: '2-digit',
    ...(showSeconds && { second: '2-digit' }),
  };

  const dateOptions: Intl.DateTimeFormatOptions =
    format === 'long'
      ? {
          day: 'numeric',
          month: 'long',
          year: 'numeric',
        }
      : {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
        };

  const formattedDate = dateObj.toLocaleDateString(locale, dateOptions);
  const formattedTime = dateObj.toLocaleTimeString(locale, timeOptions);

  return `${formattedTime}, ${formattedDate}`;
}

/**
 * Format relative time (e.g., "2 jam yang lalu", "besok")
 * @param date - Date to compare
 * @param options - Formatting options
 * @returns Relative time string
 */
export function relativeTime(
  date: Date | string | number | null | undefined,
  options: {
    locale?: string;
    style?: 'long' | 'short' | 'narrow';
    now?: Date;
  } = {}
): string {
  const { style = 'long', now = new Date() } = options;

  if (!date) {
    return '-';
  }

  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;

  if (isNaN(dateObj.getTime())) {
    return '-';
  }

  const diffMs = now.getTime() - dateObj.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);
  // BUG-FE-011: Use proper calendar month difference instead of 30-day approximation
  const diffMonths = (now.getFullYear() - dateObj.getFullYear()) * 12 + (now.getMonth() - dateObj.getMonth());
  const diffYears = Math.floor(diffDays / 365);

  // Future
  if (diffMs < 0) {
    const absDiffSeconds = Math.abs(diffSeconds);
    const absDiffMinutes = Math.abs(diffMinutes);
    const absDiffHours = Math.abs(diffHours);
    const absDiffDays = Math.abs(diffDays);

    if (absDiffSeconds < 60) {
      return style === 'short' ? 'dalam beberapa detik' : 'dalam beberapa detik';
    }
    if (absDiffMinutes < 60) {
      return style === 'short' ? `dalam ${absDiffMinutes} mnt` : `dalam ${absDiffMinutes} menit`;
    }
    if (absDiffHours < 24) {
      return style === 'short' ? `dalam ${absDiffHours} jam` : `dalam ${absDiffHours} jam`;
    }
    if (absDiffDays === 1) {
      return 'besok';
    }
    if (absDiffDays < 7) {
      return style === 'short' ? `dalam ${absDiffDays} hari` : `dalam ${absDiffDays} hari`;
    }
  }

  // Past
  if (diffSeconds < 60) {
    return 'baru saja';
  }
  if (diffMinutes < 60) {
    return style === 'short' ? `${diffMinutes} mnt` : `${diffMinutes} menit yang lalu`;
  }
  if (diffHours < 24) {
    return style === 'short' ? `${diffHours} jam` : `${diffHours} jam yang lalu`;
  }
  if (diffDays === 1) {
    return 'kemarin';
  }
  if (diffDays === 2) {
    return 'lusa';
  }
  if (diffDays < 7) {
    return style === 'short' ? `${diffDays} hari` : `${diffDays} hari yang lalu`;
  }
  if (diffDays < 30) {
    const weeks = Math.floor(diffDays / 7);
    return style === 'short' ? `${weeks} mgg` : `${weeks} minggu yang lalu`;
  }
  if (diffMonths < 12) {
    return style === 'short' ? `${diffMonths} bln` : `${diffMonths} bulan yang lalu`;
  }
  return style === 'short' ? `${diffYears} thn` : `${diffYears} tahun yang lalu`;
}

/**
 * Format date for API requests (ISO 8601)
 * @param date - Date to format
 * @returns ISO string or null
 */
export function toISOString(date: Date | string | number | null | undefined): string | null {
  if (!date) {
    return null;
  }

  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;

  if (isNaN(dateObj.getTime())) {
    return null;
  }

  return dateObj.toISOString();
}

/**
 * Parse date from various formats
 * @param date - Date string or object to parse
 * @returns Date object or null
 */
export function parseDate(date: string | Date | null | undefined): Date | null {
  if (!date) {
    return null;
  }

  if (date instanceof Date) {
    return isNaN(date.getTime()) ? null : date;
  }

  const parsed = new Date(date);
  return isNaN(parsed.getTime()) ? null : parsed;
}

/**
 * Check if date is today
 * @param date - Date to check
 * @returns True if date is today
 */
export function isToday(date: Date | string | number): boolean {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  const today = new Date();

  return (
    dateObj.getDate() === today.getDate() &&
    dateObj.getMonth() === today.getMonth() &&
    dateObj.getFullYear() === today.getFullYear()
  );
}

/**
 * Check if date is in the past
 * @param date - Date to check
 * @returns True if date is in the past
 */
export function isPast(date: Date | string | number): boolean {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return dateObj.getTime() < new Date().getTime();
}

/**
 * Check if date is in the future
 * @param date - Date to check
 * @returns True if date is in the future
 */
export function isFuture(date: Date | string | number): boolean {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return dateObj.getTime() > new Date().getTime();
}

/**
 * Get start of day
 * @param date - Date to get start of day for (default: today)
 * @returns Date object set to midnight
 */
export function startOfDay(date: Date | string | number = new Date()): Date {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  dateObj.setHours(0, 0, 0, 0);
  return dateObj;
}

/**
 * Get end of day
 * @param date - Date to get end of day for (default: today)
 * @returns Date object set to 23:59:59.999
 */
export function endOfDay(date: Date | string | number = new Date()): Date {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  dateObj.setHours(23, 59, 59, 999);
  return dateObj;
}

/**
 * Get start of month
 * @param date - Date to get start of month for (default: today)
 * @returns Date object set to first day of month at midnight
 */
export function startOfMonth(date: Date | string | number = new Date()): Date {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  dateObj.setDate(1);
  dateObj.setHours(0, 0, 0, 0);
  return dateObj;
}

/**
 * Get end of month
 * @param date - Date to get end of month for (default: today)
 * @returns Date object set to last day of month at 23:59:59.999
 */
export function endOfMonth(date: Date | string | number = new Date()): Date {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  dateObj.setMonth(dateObj.getMonth() + 1);
  dateObj.setDate(0);
  dateObj.setHours(23, 59, 59, 999);
  return dateObj;
}

/**
 * Add days to date
 * @param date - Date to add days to
 * @param days - Number of days to add
 * @returns New date with days added
 */
export function addDays(date: Date | string | number, days: number): Date {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  dateObj.setDate(dateObj.getDate() + days);
  return dateObj;
}

/**
 * Subtract days from date
 * @param date - Date to subtract days from
 * @param days - Number of days to subtract
 * @returns New date with days subtracted
 */
export function subtractDays(date: Date | string | number, days: number): Date {
  return addDays(date, -days);
}

/**
 * Add months to date
 * @param date - Date to add months to
 * @param months - Number of months to add
 * @returns New date with months added
 */
export function addMonths(date: Date | string | number, months: number): Date {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  dateObj.setMonth(dateObj.getMonth() + months);
  return dateObj;
}

/**
 * Subtract months from date
 * @param date - Date to subtract months from
 * @param months - Number of months to subtract
 * @returns New date with months subtracted
 */
export function subtractMonths(date: Date | string | number, months: number): Date {
  return addMonths(date, -months);
}

/**
 * Get difference between two dates in days
 * @param date1 - First date
 * @param date2 - Second date
 * @returns Difference in days (can be negative)
 */
export function diffInDays(date1: Date | string | number, date2: Date | string | number): number {
  const d1 = typeof date1 === 'string' || typeof date1 === 'number' ? new Date(date1) : new Date(date1);
  const d2 = typeof date2 === 'string' || typeof date2 === 'number' ? new Date(date2) : new Date(date2);

  const diffMs = d1.getTime() - d2.getTime();
  return Math.floor(diffMs / (1000 * 60 * 60 * 24));
}

/**
 * Format date range
 * @param startDate - Start date
 * @param endDate - End date
 * @param options - Formatting options
 * @returns Formatted date range string
 */
export function formatDateRange(
  startDate: Date | string | number,
  endDate: Date | string | number,
  options: { locale?: string; format?: 'short' | 'long' } = {}
): string {
  const { locale = 'id-ID', format = 'short' } = options;

  const start = typeof startDate === 'string' || typeof startDate === 'number' ? new Date(startDate) : startDate;
  const end = typeof endDate === 'string' || typeof endDate === 'number' ? new Date(endDate) : endDate;

  if (isNaN(start.getTime()) || isNaN(end.getTime())) {
    return '-';
  }

  // Same day
  if (start.toDateString() === end.toDateString()) {
    return formatDate(start, { format, locale });
  }

  // Same month
  if (
    start.getMonth() === end.getMonth() &&
    start.getFullYear() === end.getFullYear()
  ) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _dayFormat = format === 'short' ? 'd MMM' : 'd MMMM';
    return `${start.toLocaleDateString(locale, { day: 'numeric', month: format === 'short' ? 'short' : 'long' })} - ${end.toLocaleDateString(locale, { day: 'numeric', month: format === 'short' ? 'short' : 'long', year: 'numeric' })}`;
  }

  // Same year
  if (start.getFullYear() === end.getFullYear()) {
    return `${formatDate(start, { format: 'day-month', locale })} - ${formatDate(end, { format, locale })}`;
  }

  // Different years
  return `${formatDate(start, { format, locale })} - ${formatDate(end, { format, locale })}`;
}

/**
 * Get age from birth date
 * @param birthDate - Birth date
 * @returns Age in years
 */
export function getAge(birthDate: Date | string | number): number {
  const birth = typeof birthDate === 'string' || typeof birthDate === 'number' ? new Date(birthDate) : birthDate;
  const today = new Date();

  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--;
  }

  return age;
}

/**
 * Format duration in human-readable format
 * @param seconds - Duration in seconds
 * @param options - Formatting options
 * @returns Formatted duration string
 */
export function formatDuration(
  seconds: number,
  options: { locale?: string; style?: 'long' | 'short' } = {}
): string {
  const { style = 'long' } = options;

  if (seconds < 0) {
    return '-';
  }

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  const parts: string[] = [];

  if (hours > 0) {
    parts.push(style === 'short' ? `${hours}j` : `${hours} jam`);
  }
  if (minutes > 0) {
    parts.push(style === 'short' ? `${minutes}m` : `${minutes} menit`);
  }
  if (secs > 0 || parts.length === 0) {
    parts.push(style === 'short' ? `${secs}d` : `${secs} detik`);
  }

  return parts.join(' ');
}

/**
 * Check if date is within specified range
 * @param date - Date to check
 * @param startDate - Range start
 * @param endDate - Range end
 * @returns True if date is within range
 */
export function isWithinRange(
  date: Date | string | number,
  startDate: Date | string | number,
  endDate: Date | string | number
): boolean {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  const start = typeof startDate === 'string' || typeof startDate === 'number' ? new Date(startDate) : startDate;
  const end = typeof endDate === 'string' || typeof endDate === 'number' ? new Date(endDate) : endDate;

  return d.getTime() >= start.getTime() && d.getTime() <= end.getTime();
}

/**
 * Get timezone offset in minutes
 * @param date - Date to get offset for (default: now)
 * @returns Offset in minutes (e.g., +420 for WIB/UTC+7)
 */
export function getTimezoneOffset(date: Date | string | number = new Date()): number {
  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : new Date(date);
  return -dateObj.getTimezoneOffset();
}

/**
 * Format date with time suffix (pagi, siang, sore, malam)
 * @param date - Date to format
 * @param options - Formatting options
 * @returns Formatted date with time suffix
 */
export function formatDateTimeWithSuffix(
  date: Date | string | number | null | undefined,
  options: { locale?: string } = {}
): string {
  const { locale = 'id-ID' } = options;

  if (!date) {
    return '-';
  }

  const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;

  if (isNaN(dateObj.getTime())) {
    return '-';
  }

  const hour = dateObj.getHours();
  let timeSuffix: string;

  if (hour >= 4 && hour < 10) {
    timeSuffix = 'pagi';
  } else if (hour >= 10 && hour < 15) {
    timeSuffix = 'siang';
  } else if (hour >= 15 && hour < 18) {
    timeSuffix = 'sore';
  } else {
    timeSuffix = 'malam';
  }

  const timeStr = dateObj.toLocaleTimeString(locale, {
    hour: '2-digit',
    minute: '2-digit',
  });

  return `${timeSuffix} pukul ${timeStr}, ${formatDate(dateObj, { format: 'long', locale })}`;
}
