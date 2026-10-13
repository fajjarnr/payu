import { describe, expect, it } from 'vitest';
import { chartPalette } from '@/lib/chart-palette';
import { contrast } from '../theme/wcag';

// Chart marks (pie slices, category dots, column bars) are graphical objects:
// SC 1.4.11 requires 3:1 against the surface behind them.

describe('chart palette', () => {
  it('light mode: every series, secondary, and axis label passes 3:1 on white', () => {
    const palette = chartPalette(false);
    for (const [name, color] of [
      ...palette.series.map((c, i) => [`series[${i}]`, c] as const),
      ['secondary', palette.secondary] as const,
    ]) {
      const ratio = contrast(color, '#FFFFFF');
      expect(ratio, `light ${name} ${color} on white is ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(3);
    }
    // Axis labels are 10px text, not graphics.
    const axisRatio = contrast(palette.axisLabel, '#FFFFFF');
    expect(axisRatio, `light axisLabel ${palette.axisLabel} is ${axisRatio.toFixed(2)}:1`).toBeGreaterThanOrEqual(4.5);
  });

  it('dark mode: every series, secondary, and axis label passes 3:1 on #141414', () => {
    const palette = chartPalette(true);
    for (const [name, color] of [
      ...palette.series.map((c, i) => [`series[${i}]`, c] as const),
      ['secondary', palette.secondary] as const,
    ]) {
      const ratio = contrast(color, '#141414');
      expect(ratio, `dark ${name} ${color} on #141414 is ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(3);
    }
    const axisRatio = contrast(palette.axisLabel, '#141414');
    expect(axisRatio, `dark axisLabel ${palette.axisLabel} is ${axisRatio.toFixed(2)}:1`).toBeGreaterThanOrEqual(4.5);
  });
});
