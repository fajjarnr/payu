import { describe, expect, it } from 'vitest';
import { chartPalette } from '@/lib/chart-palette';

// PRODUCT DECISION 2026-10-14: the original PayU chart palette is restored
// verbatim (vivid emerald family). The product owner rejected the darker
// AA-safe variant that a previous revision had substituted, so this test
// pins the restored values instead of contrast thresholds. Known gap: the
// vivid marks fall below 3:1 on white (see src/lib/chart-palette.ts header).

describe('chart palette (restored brand colors)', () => {
  it('light and dark return the same restored palette', () => {
    expect(chartPalette(false)).toEqual(chartPalette(true));
  });

  it('leads with the brand emerald family', () => {
    expect(chartPalette(false).series[0]).toBe('#00D09C');
    expect(chartPalette(true).series[1]).toBe('#34D399');
  });

  it('keeps the original secondary, axis label, and stroke', () => {
    const palette = chartPalette(false);
    expect(palette.secondary).toBe('#9CA3AF');
    expect(palette.axisLabel).toBe('#6B7280');
    expect(palette.stroke).toBe('#FFFFFF');
  });

  it('never silently swaps the emerald for the darker AA variant', () => {
    for (const palette of [chartPalette(false), chartPalette(true)]) {
      for (const color of palette.series) {
        expect(color).not.toBe('#0B7A55');
        expect(color).not.toBe('#D97706');
        expect(color).not.toBe('#7C3AED');
      }
    }
  });
});
