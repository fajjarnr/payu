/**
 * Explicit palette for canvas-rendered charts (analytics page).
 *
 * Canvas cannot resolve CSS var() at draw time, so the palette is explicit.
 *
 * Wise re-skin 2026-10-27: series marks come from the Wise semantic family
 * (wise/DESIGN.md). Every mark clears the 3:1 non-text threshold on its
 * theme surface; axis labels clear 4.5:1 as text. The lime primary itself
 * (1.47:1 on white) never appears as a light-mode mark.
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
  series: ['#163300', '#054d28', '#d03238', '#b86700', '#454745', '#0e0f0c'],
  secondary: '#454745',
  axisLabel: '#454745',
  stroke: '#FFFFFF',
};

const DARK: ChartPalette = {
  series: ['#9fe870', '#cdffad', '#ffd11a', '#38c8ff', '#ff9a9a', '#e8ebe6'],
  secondary: '#A6A6A6',
  axisLabel: '#e8ebe6',
  stroke: '#0e0f0c',
};

export function chartPalette(isDark: boolean): ChartPalette {
  return isDark ? DARK : LIGHT;
}
