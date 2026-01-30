import {
  formatDate,
  formatDateTime,
  formatTime,
  formatRelativeTime,
  isToday,
  isYesterday,
} from '../date';

describe('formatDate', () => {
  it('should format Date object to Indonesian date format', () => {
    const date = new Date('2024-01-15');
    expect(formatDate(date)).toBe('15 Jan 2024');
  });

  it('should format date string to Indonesian date format', () => {
    expect(formatDate('2024-01-15')).toBe('15 Jan 2024');
  });

  it('should format ISO date string', () => {
    expect(formatDate('2024-12-25T10:30:00.000Z')).toBe('25 Des 2024');
  });

  it('should format first day of year', () => {
    expect(formatDate('2024-01-01')).toBe('1 Jan 2024');
  });

  it('should format last day of year', () => {
    expect(formatDate('2024-12-31')).toBe('31 Des 2024');
  });

  it('should handle leap year date', () => {
    expect(formatDate('2024-02-29')).toBe('29 Feb 2024');
  });
});

describe('formatDateTime', () => {
  it('should format Date object to Indonesian date and time format', () => {
    const date = new Date('2024-01-15T14:30:00');
    const result = formatDateTime(date);
    expect(result).toContain('15 Jan 2024');
    expect(result).toContain('14');
    expect(result).toContain('30');
  });

  it('should format date string to date and time format', () => {
    const result = formatDateTime('2024-01-15T09:15:00');
    expect(result).toContain('15 Jan 2024');
    expect(result).toContain('09');
    expect(result).toContain('15');
  });

  it('should format midnight time', () => {
    const result = formatDateTime('2024-01-15T00:00:00');
    expect(result).toContain('15 Jan 2024');
    expect(result).toContain('00');
  });

  it('should format end of day time', () => {
    const result = formatDateTime('2024-01-15T23:59:00');
    expect(result).toContain('15 Jan 2024');
    expect(result).toContain('23');
    expect(result).toContain('59');
  });
});

describe('formatTime', () => {
  it('should format Date object to time format', () => {
    const date = new Date('2024-01-15T14:30:00');
    const result = formatTime(date);
    expect(result).toContain('14');
    expect(result).toContain('30');
  });

  it('should format date string to time format', () => {
    const result = formatTime('2024-01-15T09:05:00');
    expect(result).toContain('09');
    expect(result).toContain('05');
  });

  it('should format midnight', () => {
    const result = formatTime('2024-01-15T00:00:00');
    expect(result).toContain('00');
  });

  it('should format noon', () => {
    const result = formatTime('2024-01-15T12:00:00');
    expect(result).toContain('12');
  });
});

describe('formatRelativeTime', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should return "Just now" for current time', () => {
    const now = new Date();
    expect(formatRelativeTime(now)).toBe('Just now');
  });

  it('should return "Just now" for 30 seconds ago', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const thirtySecondsAgo = new Date(now.getTime() - 30 * 1000);
    expect(formatRelativeTime(thirtySecondsAgo)).toBe('Just now');
  });

  it('should return minutes ago for recent times', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const fiveMinutesAgo = new Date(now.getTime() - 5 * 60 * 1000);
    expect(formatRelativeTime(fiveMinutesAgo)).toBe('5m ago');
  });

  it('should return hours ago for times within 24 hours', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const threeHoursAgo = new Date(now.getTime() - 3 * 60 * 60 * 1000);
    expect(formatRelativeTime(threeHoursAgo)).toBe('3h ago');
  });

  it('should return days ago for times within a week', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000);
    expect(formatRelativeTime(threeDaysAgo)).toBe('3d ago');
  });

  it('should return formatted date for times older than a week', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const tenDaysAgo = new Date(now.getTime() - 10 * 24 * 60 * 60 * 1000);
    expect(formatRelativeTime(tenDaysAgo)).toBe(formatDate(tenDaysAgo));
  });

  it('should handle boundary of 59 minutes', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const fiftyNineMinutesAgo = new Date(now.getTime() - 59 * 60 * 1000);
    expect(formatRelativeTime(fiftyNineMinutesAgo)).toBe('59m ago');
  });

  it('should handle boundary of 23 hours', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const twentyThreeHoursAgo = new Date(now.getTime() - 23 * 60 * 60 * 1000);
    expect(formatRelativeTime(twentyThreeHoursAgo)).toBe('23h ago');
  });

  it('should handle boundary of 6 days', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const sixDaysAgo = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000);
    expect(formatRelativeTime(sixDaysAgo)).toBe('6d ago');
  });

  it('should handle boundary of 7 days', () => {
    const now = new Date();
    jest.setSystemTime(now);
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    expect(formatRelativeTime(sevenDaysAgo)).toBe(formatDate(sevenDaysAgo));
  });
});

describe('isToday', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2024-01-15T12:00:00'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should return true for today', () => {
    expect(isToday(new Date('2024-01-15T10:30:00'))).toBe(true);
  });

  it('should return true for today with different time', () => {
    expect(isToday(new Date('2024-01-15T23:59:59'))).toBe(true);
  });

  it('should return false for yesterday', () => {
    expect(isToday(new Date('2024-01-14T12:00:00'))).toBe(false);
  });

  it('should return false for tomorrow', () => {
    expect(isToday(new Date('2024-01-16T12:00:00'))).toBe(false);
  });

  it('should return false for same day different year', () => {
    expect(isToday(new Date('2023-01-15T12:00:00'))).toBe(false);
  });

  it('should work with date string', () => {
    expect(isToday('2024-01-15')).toBe(true);
    expect(isToday('2024-01-14')).toBe(false);
  });
});

describe('isYesterday', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2024-01-15T12:00:00'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should return true for yesterday', () => {
    expect(isYesterday(new Date('2024-01-14T10:30:00'))).toBe(true);
  });

  it('should return true for yesterday with different time', () => {
    expect(isYesterday(new Date('2024-01-14T23:59:59'))).toBe(true);
  });

  it('should return false for today', () => {
    expect(isYesterday(new Date('2024-01-15T12:00:00'))).toBe(false);
  });

  it('should return false for two days ago', () => {
    expect(isYesterday(new Date('2024-01-13T12:00:00'))).toBe(false);
  });

  it('should return false for same date different year', () => {
    expect(isYesterday(new Date('2023-01-14T12:00:00'))).toBe(false);
  });

  it('should work with date string', () => {
    expect(isYesterday('2024-01-14')).toBe(true);
    expect(isYesterday('2024-01-15')).toBe(false);
  });

  it('should handle month boundary', () => {
    jest.setSystemTime(new Date('2024-02-01T12:00:00'));
    expect(isYesterday(new Date('2024-01-31T12:00:00'))).toBe(true);
  });

  it('should handle year boundary', () => {
    jest.setSystemTime(new Date('2024-01-01T12:00:00'));
    expect(isYesterday(new Date('2023-12-31T12:00:00'))).toBe(true);
  });
});
