/**
 * Explicit palette for canvas-rendered charts (analytics page).
 *
 * Canvas cannot resolve CSS var() at draw time, so the palette is explicit.
 *
 * PRODUCT DECISION 2026-10-14: these are the pre-existing PayU chart colors,
 * restored verbatim (vivid emerald #00D09C family and the original amber /
 * violet / rose / slate). A previous revision darkened them for the 3:1
 * non-text threshold; the product owner restored the original look. The
 * known gap: on white, #00D09C (2.00:1), #34D399, #F59E0B, and the #9CA3AF
 * secondary sit below 3:1; see the header of src/theme/tokens.ts.
 */

export interface ChartPalette {
  /** Category series, in assignment order. */
  series: readonly string[];
  /** Secondary series color (the "Keluar" column next to the primary one). */
  secondary: string;
  /** Axis label color. */
  axisLabel: string;
  /** Slice separator, matching the chart surface. */
  stroke: string;
}

const RESTORED: ChartPalette = {
  series: ['#00D09C', '#34D399', '#F59E0B', '#8B5CF6', '#F43F5E', '#64748B'],
  secondary: '#9CA3AF',
  axisLabel: '#6B7280',
  stroke: '#FFFFFF',
};

export function chartPalette(_isDark: boolean): ChartPalette {
  return RESTORED;
}
