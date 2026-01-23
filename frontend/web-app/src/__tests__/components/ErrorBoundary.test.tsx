import React from 'react';
import { render, screen } from '@testing-library/react';
import { ErrorBoundary } from '@/components/ErrorBoundary';

describe('ErrorBoundary', () => {
 const ThrowError = ({ shouldThrow }: { shouldThrow: boolean }) => {
  if (shouldThrow) {
   throw new Error('Test error');
  }
  return <div>No error</div>;
 };

 it('renders children when there is no error', () => {
  render(
   <ErrorBoundary>
    <ThrowError shouldThrow={false} />
   </ErrorBoundary>
  );
  expect(screen.getByText('No error')).toBeInTheDocument();
 });

 it('catches and displays error when child component throws', () => {
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

  render(
   <ErrorBoundary>
    <ThrowError shouldThrow={true} />
   </ErrorBoundary>
  );

  expect(screen.getByText('Terjadi Kesalahan')).toBeInTheDocument();
  expect(screen.getByText(/terjadi kesalahan yang tidak terduga/i)).toBeInTheDocument();
  expect(screen.getByText('Muat Ulang Halaman')).toBeInTheDocument();

  consoleSpy.mockRestore();
 });

 it('displays custom fallback when provided', () => {
  const fallback = <div>Custom error fallback</div>;
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

  render(
   <ErrorBoundary fallback={fallback}>
    <ThrowError shouldThrow={true} />
   </ErrorBoundary>
  );

  expect(screen.getByText('Custom error fallback')).toBeInTheDocument();
  expect(screen.queryByText('Terjadi Kesalahan')).not.toBeInTheDocument();

  consoleSpy.mockRestore();
 });
});
