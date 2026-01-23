import React from 'react';
import { render, screen } from '@testing-library/react';
import { Skeleton, SkeletonCard, SkeletonText, SkeletonTransaction, SkeletonChart, SkeletonStatsGrid } from '@/components/ui/Skeleton';

describe('Skeleton Components', () => {
 describe('Skeleton', () => {
  it('renders with default classes', () => {
   render(<Skeleton />);
   const skeleton = document.querySelector('.animate-pulse');
   expect(skeleton).toHaveClass('bg-gray-100', 'dark:bg-gray-800', 'rounded');
  });

  it('accepts custom className', () => {
   render(<Skeleton className="w-full h-20" />);
   const skeleton = document.querySelector('.animate-pulse');
   expect(skeleton).toHaveClass('w-full', 'h-20');
  });
 });

 describe('SkeletonCard', () => {
  it('renders card skeleton with structure', () => {
   render(<SkeletonCard />);
   const card = document.querySelector('.bg-card');
   expect(card).toBeInTheDocument();
   expect(card?.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });
 });

 describe('SkeletonText', () => {
  it('renders default 3 lines', () => {
   render(<SkeletonText />);
   const lines = document.querySelectorAll('.animate-pulse');
   expect(lines.length).toBe(3);
  });

  it('renders specified number of lines', () => {
   render(<SkeletonText lines={5} />);
   const lines = document.querySelectorAll('.animate-pulse');
   expect(lines.length).toBe(5);
  });

  it('last line has 3/4 width', () => {
   render(<SkeletonText />);
   const lines = document.querySelectorAll('.animate-pulse');
   const lastLine = lines[lines.length - 1];
   expect(lastLine).toHaveClass('w-3/4');
  });
 });

 describe('SkeletonTransaction', () => {
  it('renders transaction skeleton with icon, text, and amount', () => {
   render(<SkeletonTransaction />);
   const skeletons = document.querySelectorAll('.animate-pulse');
   expect(skeletons.length).toBeGreaterThan(0);
   expect(document.querySelector('.h-12')).toBeInTheDocument();
  });
 });

 describe('SkeletonChart', () => {
  it('renders chart skeleton with 12 bars', () => {
   render(<SkeletonChart />);
   const bars = document.querySelectorAll('.h-full');
   expect(bars.length).toBe(12);
  });
 });

 describe('SkeletonStatsGrid', () => {
  it('renders default 4 skeleton cards', () => {
   render(<SkeletonStatsGrid />);
   const cards = document.querySelectorAll('.bg-card');
   expect(cards.length).toBe(4);
  });

  it('renders specified number of cards', () => {
   render(<SkeletonStatsGrid count={6} />);
   const cards = document.querySelectorAll('.bg-card');
   expect(cards.length).toBe(6);
  });
 });
});
