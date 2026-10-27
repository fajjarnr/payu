import { describe, expect, it } from 'vitest';
import { chartPalette } from '@/lib/chart-palette';
import { contrast } from '@/__tests__/theme/wcag';

// Re-skin 2026-10-27: the canvas palette is derived from the Wise semantic
// family (wise/DESIGN.md). Every series mark clears the 3:1 non-text
// threshold on its theme surface; axis labels clear 4.5:1 as text.

describe('chart palette (Wise semantic family)', () => {
  it('light and dark return theme-specific palettes', () => {
    expect(chartPalette(false)).not.toEqual(chartPalette(true));
  });

  it('leads with the Wise ink-deep and positive-deep family', () => {
    expect(chartPalette(false).series[0]).toBe('#163300');
    expect(chartPalette(false).series[1]).toBe('#054d28');
    expect(chartPalette(true).series[0]).toBe('#9fe870');
    expect(chartPalette(true).series[1]).toBe('#cdffad');
  });

  it('every light series mark clears 3:1 on white', () => {
    for (const mark of chartPalette(false).series) {
      const ratio = contrast(mark, '#FFFFFF');
      expect(ratio, `${mark} on white is ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(3);
    }
  });

  it('every dark series mark clears 3:1 on the ink surface', () => {
    for (const mark of chartPalette(true).series) {
      const ratio = contrast(mark, '#0e0f0c');
      expect(ratio, `${mark} on #0e0f0c is ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(3);
    }
  });

  it('axis labels clear 4.5:1 on their surface', () => {
    const light = chartPalette(false);
    const dark = chartPalette(true);
    expect(contrast(light.axisLabel, '#FFFFFF')).toBeGreaterThanOrEqual(4.5);
    expect(contrast(dark.axisLabel, '#0e0f0c')).toBeGreaterThanOrEqual(4.5);
    expect(light.stroke).toBe('#FFFFFF');
    expect(dark.stroke).toBe('#0e0f0c');
  });

  it('contains no legacy emerald values', () => {
    for (const palette of [chartPalette(false), chartPalette(true)]) {
      for (const color of [...palette.series, palette.secondary, palette.axisLabel]) {
        expect(color).not.toBe('#00D09C');
        expect(color).not.toBe('#0B7A55');
      }
    }
  });
});
