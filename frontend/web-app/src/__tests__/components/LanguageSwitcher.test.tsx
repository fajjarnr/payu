import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { useRouter, usePathname } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import LanguageSwitcher from '@/components/LanguageSwitcher';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(),
}));

vi.mock('next-intl', () => ({
  useLocale: vi.fn(),
  useTranslations: vi.fn((key) => (key: string) => key),
}));

describe('LanguageSwitcher', () => {
  const mockRouter = {
    push: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
  };

  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue(mockRouter);
    vi.mocked(usePathname).mockReturnValue('/id/dashboard');
    vi.mocked(useLocale).mockReturnValue('id');
  });

  it('should render current language correctly', () => {
    render(<LanguageSwitcher />);

    expect(screen.getByText('🇮🇩')).toBeInTheDocument();
    expect(screen.getByText('id')).toBeInTheDocument();
  });

  it('should open dropdown when clicked', () => {
    render(<LanguageSwitcher />);

    const button = screen.getByRole('button', { name: /changeLanguage/i });
    fireEvent.click(button);

    expect(screen.getByText('Indonesia')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
  });

  it('should close dropdown when clicking outside', () => {
    render(<LanguageSwitcher />);

    const button = screen.getByRole('button', { name: /changeLanguage/i });
    fireEvent.click(button);

    expect(screen.getByText('Indonesia')).toBeInTheDocument();

    const overlay = document.querySelector('.fixed');
    if (overlay) {
      fireEvent.click(overlay);
    }

    expect(screen.queryByText('Indonesia')).not.toBeInTheDocument();
  });

  it('should switch locale when English is clicked', () => {
    vi.mocked(usePathname).mockReturnValue('/id/dashboard');

    render(<LanguageSwitcher />);

    const button = screen.getByRole('button', { name: /changeLanguage/i });
    fireEvent.click(button);

    const englishOption = screen.getByText('English');
    fireEvent.click(englishOption);

    expect(mockRouter.push).toHaveBeenCalledWith('/en/dashboard');
  });

  it('should show checkmark for current locale', () => {
    render(<LanguageSwitcher />);

    const button = screen.getByRole('button', { name: /changeLanguage/i });
    fireEvent.click(button);

    expect(screen.getByText('✓')).toBeInTheDocument();
  });
});
