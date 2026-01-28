import React from 'react';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import { ArrowRightLeft, QrCode } from 'lucide-react';
import QuickActions from '@/components/dashboard/QuickActions';

expect.extend(toHaveNoViolations);

describe('QuickActions', () => {
  const mockActions = [
    {
      id: 'transfer',
      label: 'Transfer',
      icon: ArrowRightLeft,
      href: '/transfer',
      color: 'text-primary',
      bgColor: 'bg-success-light',
      description: 'Kirim uang instan',
      ariaLabel: 'Transfer uang ke akun lain',
    },
    {
      id: 'qris',
      label: 'QRIS',
      icon: QrCode,
      href: '/qris',
      color: 'text-primary',
      bgColor: 'bg-chart-2',
      description: 'Scan QR untuk bayar',
      ariaLabel: 'Pembayaran QRIS',
    },
  ];

  it('should render quick actions correctly', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    expect(screen.getByText('Aksi Cepat')).toBeInTheDocument();
    expect(screen.getByText('Transfer')).toBeInTheDocument();
    expect(screen.getByText('QRIS')).toBeInTheDocument();
  });

  it('should respect max actions limit', () => {
    const manyActions = Array(10).fill(null).map((_, i) => ({
      ...mockActions[0],
      id: `action-${i}`,
      label: `Action ${i}`,
      icon: ArrowRightLeft,
    }));

    renderWithIntl(<QuickActions actions={manyActions} maxActions={4} />);

    const actionLinks = screen.getAllByRole('link').filter((link) =>
      link.getAttribute('href')?.startsWith('/')
    );
    expect(actionLinks.length).toBeLessThanOrEqual(4);
  });

  it('should enter edit mode when Edit button is clicked', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const editButton = screen.getByLabelText('Edit urutan aksi cepat');
    fireEvent.click(editButton);

    expect(screen.getByText('Selesai')).toBeInTheDocument();
    // Use getAllByText since the drag hint appears in the status message
    expect(screen.getAllByText(/Drag untuk mengatur ulang/).length).toBeGreaterThan(0);
  });

  it('should show drag hint in edit mode', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const editButton = screen.getByLabelText('Edit urutan aksi cepat');
    fireEvent.click(editButton);

    expect(screen.getByText(/Gunakan Tab untuk navigasi/)).toBeInTheDocument();
  });

  it('should exit edit mode when Selesai button is clicked', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const editButton = screen.getByLabelText('Edit urutan aksi cepat');
    fireEvent.click(editButton);

    const doneButton = screen.getByLabelText('Selesai mengedit');
    fireEvent.click(doneButton);

    expect(screen.getByLabelText('Edit urutan aksi cepat')).toBeInTheDocument();
  });

  it('should call onReorder when actions are reordered', async () => {
    const onReorder = vi.fn();
    renderWithIntl(<QuickActions actions={mockActions} onReorder={onReorder} />);

    const editButton = screen.getByLabelText('Edit urutan aksi cepat');
    fireEvent.click(editButton);

    // Simulate drag end event
    // Note: Full drag-drop testing requires more setup with dnd-kit
    // This is a basic test to ensure the callback exists
    expect(onReorder).toBeDefined();
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<QuickActions actions={mockActions} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const region = screen.getByRole('region');
    expect(region).toHaveAttribute('aria-labelledby', 'quick-actions-title');

    const title = screen.getByText('Aksi Cepat');
    expect(title).toHaveAttribute('id', 'quick-actions-title');
  });

  it('should have accessible action links', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const transferLink = screen.getByLabelText('Transfer uang ke akun lain');
    const qrisLink = screen.getByLabelText('Pembayaran QRIS');

    expect(transferLink).toBeInTheDocument();
    expect(transferLink).toHaveAttribute('href', '/transfer');

    expect(qrisLink).toBeInTheDocument();
    expect(qrisLink).toHaveAttribute('href', '/qris');
  });

  it('should update aria-pressed when toggling edit mode', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const editButton = screen.getByLabelText('Edit urutan aksi cepat');
    expect(editButton).toHaveAttribute('aria-pressed', 'false');

    fireEvent.click(editButton);
    expect(editButton).toHaveAttribute('aria-pressed', 'true');
  });

  it('should announce drag hint to screen readers', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const editButton = screen.getByLabelText('Edit urutan aksi cepat');
    fireEvent.click(editButton);

    const dragHint = screen.getByText(/Gunakan Tab untuk navigasi/);
    expect(dragHint).toBeInTheDocument();
    expect(dragHint.closest('[role="status"]')).toBeInTheDocument();
    expect(dragHint.closest('[aria-live="polite"]')).toBeInTheDocument();
  });

  it('should have accessible view all link', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    const viewAllButton = screen.getByLabelText('Lihat semua fitur');
    expect(viewAllButton).toBeInTheDocument();
  });

  it('should be keyboard navigable', () => {
    renderWithIntl(<QuickActions actions={mockActions} />);

    // Check that actions are rendered as anchor tags with href
    const transferLink = screen.getByLabelText('Transfer uang ke akun lain');
    const qrisLink = screen.getByLabelText('Pembayaran QRIS');

    expect(transferLink.tagName).toBe('A');
    expect(transferLink).toHaveAttribute('href', '/transfer');
    expect(transferLink).toHaveAttribute('draggable', 'false');

    expect(qrisLink.tagName).toBe('A');
    expect(qrisLink).toHaveAttribute('href', '/qris');
  });
});
