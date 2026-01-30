import React from 'react';
import { screen, fireEvent } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import MobileHeader from '@/components/MobileHeader';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock next/navigation
const mockBack = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    back: mockBack,
  }),
}));

expect.extend(toHaveNoViolations);

describe('MobileHeader', () => {
  const defaultProps = {
    title: 'Test Page',
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render header with title', () => {
    renderWithIntl(<MobileHeader {...defaultProps} />);

    expect(screen.getByText('Test Page')).toBeInTheDocument();
  });

  it('should render back button by default', () => {
    renderWithIntl(<MobileHeader {...defaultProps} />);

    const backButton = screen.getByRole('button');
    expect(backButton).toBeInTheDocument();
  });

  it('should not render back button when showBack is false', () => {
    renderWithIntl(<MobileHeader {...defaultProps} showBack={false} />);

    const backButton = screen.queryByRole('button');
    expect(backButton).not.toBeInTheDocument();
  });

  it('should call router.back when back button is clicked', () => {
    renderWithIntl(<MobileHeader {...defaultProps} />);

    const backButton = screen.getByRole('button');
    fireEvent.click(backButton);

    expect(mockBack).toHaveBeenCalledTimes(1);
  });

  it('should have correct styling classes', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const header = container.querySelector('header');
    expect(header).toHaveClass(
      'sticky',
      'top-0',
      'z-40',
      'bg-card/80',
      'backdrop-blur-md',
      'border-b',
      'border-border'
    );
  });

  it('should have proper height', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const header = container.querySelector('header');
    expect(header).toHaveClass('h-16');
  });

  it('should have padding for content alignment', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const header = container.querySelector('header');
    expect(header).toHaveClass('px-4');
  });

  it('should have no accessibility violations', async () => {
    const { container: _container } = renderWithIntl(<MobileHeader {...defaultProps} />);
    const results = await axe(_container);

    expect(results).toHaveNoViolations();
  });

  it('should render title with correct typography', () => {
    renderWithIntl(<MobileHeader {...defaultProps} />);

    const title = screen.getByText('Test Page');
    expect(title).toHaveClass('text-lg', 'font-black', 'text-foreground');
  });

  it('should have proper layout structure', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const header = container.querySelector('header');
    expect(header).toHaveClass('flex', 'items-center', 'justify-between');
  });

  it('should apply negative margin to back button for alignment', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const backButton = container.querySelector('button');
    expect(backButton).toHaveClass('-ml-2');
  });

  it('should have hover state on back button', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const backButton = container.querySelector('button');
    expect(backButton).toHaveClass('hover:bg-muted', 'transition-colors');
  });

  it('should render back button with rounded styling', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const backButton = container.querySelector('button');
    expect(backButton).toHaveClass('rounded-full');
  });

  it('should align items in a row with gap', () => {
    const { container } = renderWithIntl(<MobileHeader {...defaultProps} />);

    const headerContent = container.querySelector('.flex.items-center.gap-3');
    expect(headerContent).toBeInTheDocument();
  });

  it('should handle long titles', () => {
    renderWithIntl(<MobileHeader title="This is a very long page title that should be displayed properly" />);

    expect(screen.getByText('This is a very long page title that should be displayed properly')).toBeInTheDocument();
  });

  it('should handle special characters in title', () => {
    renderWithIntl(<MobileHeader title="Test & Page (2024)" />);

    expect(screen.getByText('Test & Page (2024)')).toBeInTheDocument();
  });

  it('should not crash with empty title', () => {
    expect(() => {
      renderWithIntl(<MobileHeader title="" />);
    }).not.toThrow();
  });

  it('should render consistently across different titles', () => {
    const { rerender } = renderWithIntl(<MobileHeader title="Page 1" />);
    expect(screen.getByText('Page 1')).toBeInTheDocument();

    rerender(<MobileHeader title="Page 2" />);
    expect(screen.getByText('Page 2')).toBeInTheDocument();
    expect(screen.queryByText('Page 1')).not.toBeInTheDocument();
  });
});
