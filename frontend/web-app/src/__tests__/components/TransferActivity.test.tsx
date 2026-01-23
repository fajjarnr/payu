import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import TransferActivity from '@/components/dashboard/TransferActivity';

describe('TransferActivity', () => {
 it('renders transfer activity section', () => {
   render(<TransferActivity />);

   expect(screen.getByText('Aktivitas Terakhir')).toBeInTheDocument();
   expect(screen.getByText('Kirim Cepat')).toBeInTheDocument();
  });

 it('renders recent transfers in desktop table view', () => {
  render(<TransferActivity />);
  
  expect(screen.getAllByText('Alex Johnson')).toHaveLength(2);
  expect(screen.getAllByText('Tagihan Netflix')).toHaveLength(2);
  expect(screen.getAllByText('John Doe')).toHaveLength(2);
  expect(screen.getAllByText('Maria Garcia')).toHaveLength(2);
 });

 it('displays transaction amounts correctly', () => {
  render(<TransferActivity />);
  
  expect(screen.getAllByText(/Rp 7.500.000/i)).toHaveLength(2);
  expect(screen.getAllByText(/Rp 159.000/i)).toHaveLength(2);
 });

 it('renders quick transfer section with icons', () => {
   const { container } = render(<TransferActivity />);

   expect(screen.getByText('Kategori Favorit')).toBeInTheDocument();
   expect(container.textContent).toContain('🏦');
   expect(container.textContent).toContain('📱');
   expect(container.textContent).toContain('📄');
  });

 it('applies responsive classes for mobile view', () => {
  const { container } = render(<TransferActivity />);
  
  const mainGrid = container.querySelector('.grid');
  expect(mainGrid).toHaveClass('grid-cols-1', 'lg:grid-cols-12');
  
  const cardView = container.querySelector('.md\\:hidden');
  expect(cardView).toBeInTheDocument();
 });

 it('shows action buttons at bottom', () => {
   render(<TransferActivity />);

   expect(screen.getByText('Ulangi Transfer Terakhir')).toBeInTheDocument();
   expect(screen.getByText('Riwayat Lengkap')).toBeInTheDocument();
   expect(screen.getByText('Kirim Sekarang')).toBeInTheDocument();
  });

 it('renders transfer recent contacts in quick transfer', () => {
   const { container } = render(<TransferActivity />);

   expect(screen.getByText('Kontak Terbaru')).toBeInTheDocument();

   const userAvatars = container.querySelectorAll('.rounded-xl');
   expect(userAvatars.length).toBeGreaterThan(0);
  });

 it('displays category and account information', () => {
   render(<TransferActivity />);

   expect(screen.getAllByText('Transfer ke')).toHaveLength(6);
   expect(screen.getAllByText('Langganan')).toHaveLength(2);
  });
});
