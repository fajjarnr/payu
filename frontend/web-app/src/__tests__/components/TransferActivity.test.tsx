import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import TransferActivity from '@/components/dashboard/TransferActivity';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

describe('TransferActivity', () => {
 it('renders transfer activity section', () => {
   renderWithIntl(<TransferActivity />);

   expect(screen.getByText('Aktivitas Terakhir')).toBeInTheDocument();
   expect(screen.getByText('Kirim Cepat')).toBeInTheDocument();
  });

 it('renders empty transaction state when account is unavailable', () => {
  renderWithIntl(<TransferActivity />);

  expect(screen.getByText('Belum Ada Transaksi')).toBeInTheDocument();
 });

 it('does not fabricate transaction amounts', () => {
  renderWithIntl(<TransferActivity />);

  expect(screen.queryByText(/Rp 7.500.000/i)).not.toBeInTheDocument();
 });

 it('renders quick transfer section with icons', () => {
   renderWithIntl(<TransferActivity />);

   expect(screen.getByText('Kategori Favorit')).toBeInTheDocument();
   expect(screen.getByText('Bank')).toBeInTheDocument();
   expect(screen.getByText('E-Wallet')).toBeInTheDocument();
   expect(screen.getByText('Tagihan')).toBeInTheDocument();
  });

 it('applies responsive classes for mobile view', () => {
  const { container } = renderWithIntl(<TransferActivity />);
  
  const mainGrid = container.querySelector('.grid');
  expect(mainGrid).toHaveClass('grid-cols-1', 'lg:grid-cols-12');
  
  const cardView = container.querySelector('.md\\:hidden');
  expect(cardView).toBeInTheDocument();
 });

 it('shows action buttons at bottom', () => {
   renderWithIntl(<TransferActivity />);

   expect(screen.getByText('Ulangi Transfer Terakhir')).toBeInTheDocument();
   expect(screen.getByText('Riwayat Lengkap')).toBeInTheDocument();
   expect(screen.getByText('Kirim Sekarang')).toBeInTheDocument();
  });

 it('renders transfer recent contacts in quick transfer', () => {
   const { container } = renderWithIntl(<TransferActivity />);

   expect(screen.getByText('Kontak Terbaru')).toBeInTheDocument();

   const userAvatars = container.querySelectorAll('.rounded-xl');
   expect(userAvatars.length).toBeGreaterThan(0);
  });

 it('displays transfer categories', () => {
   renderWithIntl(<TransferActivity />);

   expect(screen.getByText('Bank')).toBeInTheDocument();
   expect(screen.getByText('E-Wallet')).toBeInTheDocument();
 });
});
