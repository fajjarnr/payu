/**
 * Explicit palette for canvas-rendered charts (analytics page).
 *
 * Canvas cannot resolve CSS var() at draw time, so the palette is explicit and
 * selected per theme. Every value is measured: chart marks (pie slices,
 * category dots, column bars) are graphical objects needing 3:1 (SC 1.4.11)
 * and axis labels are 10px text needing 4.5:1 (SC 1.4.3) on their surface.
 *
 * Guard: src/__tests__/lib/chart-palette.test.ts fails if any value drops
 * below threshold on either surface (#FFFFFF light, #141414 dark).
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

const LIGHT: ChartPalette = {
  series: ['#0B7A55', '#D97706', '#7C3AED', '#E11D48', '#64748B', '#0E7490'],
  secondary: '#64748B',
  axisLabel: '#6B7280',
  stroke: '#FFFFFF',
};

const DARK: ChartPalette = {
  series: ['#00D09C', '#34D399', '#F59E0B', '#8B5CF6', '#F43F5E', '#94A3B8'],
  secondary: '#94A3B8',
  axisLabel: '#ACACAC',
  stroke: '#141414',
};

export function chartPalette(isDark: boolean): ChartPalette {
  return isDark ? DARK : LIGHT;
}
