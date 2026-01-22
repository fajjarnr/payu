import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StatsCharts from '@/components/dashboard/StatsCharts';

describe('StatsCharts', () => {
  it('renders investment performance section', () => {
    render(<StatsCharts />);
    
    expect(screen.getByText('Performa Investasi')).toBeInTheDocument();
    expect(screen.getByText('+12.5%')).toBeInTheDocument();
  });

  it('renders investment breakdown items', () => {
    render(<StatsCharts />);
    
    expect(screen.getByText('Saham')).toBeInTheDocument();
    expect(screen.getByText('(60%)')).toBeInTheDocument();
    expect(screen.getByText('Obligasi')).toBeInTheDocument();
    expect(screen.getByText('(25%)')).toBeInTheDocument();
    expect(screen.getByText('Emas')).toBeInTheDocument();
    expect(screen.getByText('(15%)')).toBeInTheDocument();
  });

  it('renders spending overview section', () => {
    render(<StatsCharts />);
    
    expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
  });

  it('displays monthly spending bars', () => {
    const { container } = render(<StatsCharts />);
    
    expect(screen.getByText('Jan')).toBeInTheDocument();
    expect(screen.getByText('Feb')).toBeInTheDocument();
    expect(screen.getByText('Mar')).toBeInTheDocument();
    expect(screen.getByText('Apr')).toBeInTheDocument();
    expect(screen.getByText('Mei')).toBeInTheDocument();
    expect(screen.getByText('Jun')).toBeInTheDocument();
    expect(screen.getByText('Jul')).toBeInTheDocument();
  });

  it('applies responsive grid layout', () => {
    render(<StatsCharts />);
    
    expect(screen.getByText('Performa Investasi')).toBeInTheDocument();
  });

  it('shows total investment value', () => {
    render(<StatsCharts />);
    
    expect(screen.getByText('Total Nilai')).toBeInTheDocument();
    expect(screen.getByText('Rp 8.750.000')).toBeInTheDocument();
  });

  it('renders donut chart SVG', () => {
    const { container } = render(<StatsCharts />);
    
    const svgs = container.querySelectorAll('svg');
    const chartSvg = Array.from(svgs).find(svg => svg.classList.contains('-rotate-90'));
    expect(chartSvg).toBeInTheDocument();
  });

  it('displays active bar tooltip', () => {
    render(<StatsCharts />);
    
    expect(screen.getByText('Rp 3.500.000')).toBeInTheDocument();
  });

  it('applies mobile-specific styling', () => {
    const { container } = render(<StatsCharts />);
    
    const investmentSection = container.querySelector('.bg-card');
    expect(investmentSection).toHaveClass('rounded-[2rem]', 'sm:rounded-[2.5rem]');
  });
});
