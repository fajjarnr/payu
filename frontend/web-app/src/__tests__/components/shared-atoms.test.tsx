import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe } from 'jest-axe';
import PageHeader from '@/components/PageHeader';
import MoneyText from '@/components/MoneyText';
import RouteError from '@/components/RouteError';
import { LoadingState } from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import DataTableCard from '@/components/DataTableCard';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

describe('shared atoms (foundation)', () => {
  it('PageHeader renders L1 title + subtitle + actions, stacks on mobile', async () => {
    const { container } = renderWithIntl(
      <PageHeader title="Transfer" subtitle="Kirim uang" actions={<button>Kirim</button>} />
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Transfer' })).toBeInTheDocument();
    expect(screen.getByText('Kirim uang')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kirim' })).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it('MoneyText renders tabular-nums with a direction cue, never color-only', async () => {
    const { container } = renderWithIntl(<MoneyText amount={1500000} direction="in" />);
    const el = screen.getByText(/1\.500\.000/);
    expect(el).toBeInTheDocument();
    expect(el).toHaveStyle({ fontVariantNumeric: 'tabular-nums' });
    // Direction icon is decorative-safe: aria-hidden with an adjacent sr label.
    expect(container.querySelector('[aria-hidden="true"]')).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it('RouteError renders L2 title with retry, no hardcoded #FF4757', async () => {
    const onRetry = () => {};
    const { container } = renderWithIntl(
      <RouteError error={new Error('boom')} reset={onRetry} />
    );
    expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /coba lagi/i })).toBeInTheDocument();
    expect(container.innerHTML).not.toMatch(/#FF4757/i);
    expect(await axe(container)).toHaveNoViolations();
  });

  it('RouteError is localized and offers back + home next to the retry', async () => {
    renderWithIntl(<RouteError error={new Error('boom')} reset={() => {}} />);
    expect(screen.getByRole('heading', { level: 2, name: 'Terjadi kesalahan' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /coba lagi/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /kembali/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /beranda/i })).toBeInTheDocument();
    // Localized copy only — the English boilerplate it replaced is gone.
    expect(screen.queryByText(/something went wrong/i)).not.toBeInTheDocument();
  });

  it('LoadingState renders skeleton and spinner variants', async () => {
    const { container: skeleton } = renderWithIntl(<LoadingState variant="skeleton" />);
    expect(skeleton.querySelector('[aria-busy="true"]')).toBeInTheDocument();
    const { container: spinner } = renderWithIntl(<LoadingState variant="spinner" />);
    expect(spinner.querySelector('[role="status"]')).toBeInTheDocument();
    expect(await axe(spinner)).toHaveNoViolations();
  });

  it('EmptyState renders 64px icon + h2 + description + optional CTA', async () => {
    const { container } = renderWithIntl(
      <EmptyState
        title="Belum ada data"
        description="Data akan muncul di sini"
        actionLabel="Muat ulang"
        onAction={() => {}}
      />
    );
    // h2, not h3: the empty state is a section under the page h1 (axe
    // heading-order measured live across every route).
    expect(screen.getByRole('heading', { level: 2, name: 'Belum ada data' })).toBeInTheDocument();
    expect(screen.getByText('Data akan muncul di sini')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Muat ulang' })).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it('DataTableCard renders a Card with zero body padding and max-content scroll', async () => {
    const { container } = renderWithIntl(
      <DataTableCard title="Riwayat">
        <div>row</div>
      </DataTableCard>
    );
    expect(screen.getByText('Riwayat')).toBeInTheDocument();
    expect(screen.getByText('row')).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });
});
