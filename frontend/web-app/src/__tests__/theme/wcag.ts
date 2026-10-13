// WCAG 2.x contrast math shared by the theme token tests.
// One place so the invariant tests and the rendered-CSS test cannot drift apart.

export function luminance(hex: string): number {
  const h = hex.replace('#', '');
  const full =
    h.length === 3
      ? h
          .split('')
          .map((c) => c + c)
          .join('')
      : h;
  const channels = [0, 2, 4].map((i) => parseInt(full.slice(i, i + 2), 16) / 255);
  const [r, g, b] = channels.map((c) =>
    c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4,
  );
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

export function contrast(fg: string, bg: string): number {
  const [a, b] = [luminance(fg), luminance(bg)];
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}

/** Resolves `#hex` and `rgba(r,g,b,a)` (composited over `bg`) to a hex string. */
export function toHex(value: string, bg: string): string {
  const v = value.trim();
  const rgba = v.match(/^rgba?\(([^)]+)\)$/);
  if (!rgba) return v;
  const parts = rgba[1].split(',').map((p) => Number(p.trim()));
  const [r, g, b, a = 1] = parts;
  const base = bg.replace('#', '');
  const bgChannels = [0, 2, 4].map((i) => parseInt(base.slice(i, i + 2), 16));
  const mixed = [r, g, b].map((c, i) => Math.round(c * a + bgChannels[i] * (1 - a)));
  return `#${mixed.map((c) => c.toString(16).padStart(2, '0')).join('')}`;
}
