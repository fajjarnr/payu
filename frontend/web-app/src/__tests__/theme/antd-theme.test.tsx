import { describe, expect, it } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { Button, Tag } from 'antd';
import { AntdProvider } from '@/components/AntdProvider';
import { contrast, toHex } from './wcag';

// jsdom does not apply antd's `:where()`-wrapped CSS-in-JS rules to
// getComputedStyle, and antd remaps seeds (the dark algorithm turns
// #9fe870 into a derived tint). So the wiring is asserted on the CSS
// variables cssinjs actually generates from the ConfigProvider tokens: that
// stylesheet is what the browser applies, whatever the algorithm derives.
// Re-skin 2026-10-27: the lime primary (#9fe870) is 1.47:1 on white, so it
// NEVER renders as text — the TEXT_ROLES below pin deep AA-safe shades for
// the lime family's text states instead. Primary button labels are ink
// (#0e0f0c): 13.05:1 on the lime fill in light mode, 13.05:1+ on the dark
// remap. Both fill/label pairs are asserted below.


async function renderedVars(): Promise<Record<string, string>> {
  await waitFor(() => {
    const css = Array.from(document.querySelectorAll('style'))
      .map((s) => s.textContent ?? '')
      .join('');
    if (!css.includes('--ant-color-primary')) throw new Error('tokens not injected yet');
  });
  const css = Array.from(document.querySelectorAll('style'))
    .map((s) => s.textContent ?? '')
    .join('');
  const vars: Record<string, string> = {};
  for (const m of css.matchAll(/--([a-z0-9-]+):([^;}]+)/g)) vars[m[1]] = m[2];
  return vars;
}

function clearInjectedStyles() {
  document.querySelectorAll('style').forEach((s) => s.remove());
}

const TEXT_ROLES = ['primary', 'success', 'info', 'link', 'error', 'warning'] as const;
const STATES = ['', '-hover', '-active', '-text', '-text-hover', '-text-active'] as const;

function assertTheme(vars: Record<string, string>, mode: string) {
  const bg = vars['ant-color-bg-container'];

  for (const role of ['link', 'error'] as const) {
    for (const state of STATES) {
      const name = `ant-color-${role}${state}`;
      if (!vars[name]) continue;
      const ratio = contrast(toHex(vars[name], bg), bg);
      expect(
        ratio,
        `${mode}: --${name} (${vars[name]}) on ${bg} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
      ).toBeGreaterThanOrEqual(4.5);
    }
  }
  // Warning seed (#ffd11a, 1.46:1) NEVER renders as text: warning text is
  // warning-content (#4a3b1c). Assert the text states directly.
  for (const name of ['ant-color-warning-text', 'ant-color-warning-text-hover', 'ant-color-warning-text-active'] as const) {
    const value = vars[name];
    if (!value) continue;
    const ratio = contrast(toHex(value, bg), bg);
    expect(
      ratio,
      `${mode}: --${name} (${value}) on ${bg} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
    ).toBeGreaterThanOrEqual(4.5);
  }

  // Secondary text is rgba; composite it over the surface first.
  const secondary = toHex(vars['ant-color-text-secondary'], bg);
  const secondaryRatio = contrast(secondary, bg);
  expect(
    secondaryRatio,
    `${mode}: --ant-color-text-secondary (${vars['ant-color-text-secondary']} -> ${secondary}) on ${bg} is ${secondaryRatio.toFixed(2)}:1`,
  ).toBeGreaterThanOrEqual(4.5);

  // antd v6 paints "secondary" typography, menu group titles and card
  // subtitles with colorTextDescription — its default (rgba(0,0,0,0.45))
  // measures 3.36:1 on white, so the theme pins an AA value.
  const description = toHex(vars['ant-color-text-description'], bg);
  const descriptionRatio = contrast(description, bg);
  expect(
    descriptionRatio,
    `${mode}: --ant-color-text-description (${vars['ant-color-text-description']} -> ${description}) on ${bg} is ${descriptionRatio.toFixed(2)}:1`,
  ).toBeGreaterThanOrEqual(4.5);

  // Lime-family TEXT roles (link, primary-text, success-text) are pinned
  // deep shades — the lime fills stay on fills only, so every label that
  // reads as text clears AA.
  for (const name of [
    'ant-color-link',
    'ant-color-primary-text',
    'ant-color-success-text',
  ]) {
    const value = vars[name];
    if (!value) continue;
    const ratio = contrast(toHex(value, bg), bg);
    expect(
      ratio,
      `${mode}: --${name} (${value}) on ${bg} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
    ).toBeGreaterThanOrEqual(4.5);
  }

  // Placeholder is text.
  const placeholderRatio = contrast(toHex(vars['ant-color-text-placeholder'], bg), bg);
  expect(placeholderRatio, `${mode}: placeholder on ${bg}`).toBeGreaterThanOrEqual(4.5);

  // Solid fill labels (primary and danger buttons), all interaction states.
  // Primary labels are ink on the lime fill (13.05:1 light; the dark remap
  // keeps ink on a light tint, asserted in both modes).
  const labels: Record<string, string> = {
    primary: 'ant-button-primary-color',
    error: 'ant-button-danger-color',
  };
  for (const fillRole of ['primary', 'error'] as const) {
    const label = toHex(vars[labels[fillRole]] ?? vars['ant-color-text-light-solid'], bg);
    for (const state of ['', '-hover', '-active']) {
      const name = `ant-color-${fillRole}${state}`;
      const fill = toHex(vars[name], bg);
      const ratio = contrast(label, fill);
      expect(
        ratio,
        `${mode}: label ${label} on --${name} fill ${fill} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
      ).toBeGreaterThanOrEqual(4.5);
    }
  }

  // Tag/alert text sits on status tints. The rendered label always uses the
  // `-text` role (Tag component tokens + globals.css override), never the
  // raw seed — so assert text-role-on-tint for every role.
  for (const role of TEXT_ROLES) {
    const tint = vars[`ant-color-${role}-bg`];
    if (!tint) continue;
    const textVar = vars[`ant-color-${role}-text`] ?? vars[`ant-color-${role}`];
    const text = toHex(textVar, tint);
    const ratio = contrast(text, tint);
    expect(
      ratio,
      `${mode}: --ant-color-${role}-text ${textVar} on --ant-color-${role}-bg ${tint} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
    ).toBeGreaterThanOrEqual(4.5);
  }

  // Avatar text on tints (A-01): transfer favorite/review/success avatars.
  // The primary tint is a light-mode surface (dark cards use solid ink, and
  // the dark Tag tokens re-pin the palette), so only the neutral pair is
  // asserted in dark mode.
  const avatarPairs: Array<[string | undefined, string | undefined]> = [
    ...(mode === 'light' ? [[vars['ant-color-text'], vars['ant-color-primary-bg']] as [string | undefined, string | undefined]] : []),
    [vars['ant-color-text-secondary'], vars['ant-color-fill-tertiary']],
  ];
  for (const [fg, tint] of avatarPairs) {
    if (!fg || !tint) continue;
    const tintOnSurface = toHex(tint, bg);
    const textOnTint = toHex(fg, tintOnSurface);
    const ratio = contrast(textOnTint, tintOnSurface);
    expect(
      ratio,
      `${mode}: avatar text ${fg} on ${tint} (over ${bg}) is ${ratio.toFixed(2)}:1, needs 4.5:1`,
    ).toBeGreaterThanOrEqual(4.5);
  }

  // Component boundaries need 3:1 (SC 1.4.11). The lime family never draws
  // a boundary on white (input borders are ink); only text-safe roles loop here.
  for (const role of ['warning', 'error'] as const) {
    const border = vars[`ant-color-${role}-border`];
    if (!border) continue;
    const ratio = contrast(toHex(border, bg), bg);
    expect(
      ratio,
      `${mode}: --ant-color-${role}-border ${border} on ${bg} is ${ratio.toFixed(2)}:1, needs 3:1`,
    ).toBeGreaterThanOrEqual(3);
  }

  expect(bg, `${mode}: surface extracted`).toMatch(/^#[0-9a-f]{6}$/);
}

describe('AntdProvider rendered tokens satisfy WCAG 2.2 AA in both themes', () => {
  it('light mode', async () => {
    clearInjectedStyles();
    render(
      <AntdProvider>
        <Button type="primary">Kirim</Button>
        <Button type="primary" danger>
          Hapus
        </Button>
        <Tag color="success">ok</Tag>
        <Tag color="error">bad</Tag>
        <Tag color="warning">warn</Tag>
        <Tag color="processing">proc</Tag>
      </AntdProvider>,
    );
    assertTheme(await renderedVars(), 'light');
  });

  it('dark mode', async () => {
    clearInjectedStyles();
    render(
      <AntdProvider isDark>
        <Button type="primary">Kirim</Button>
        <Button type="primary" danger>
          Hapus
        </Button>
        <Tag color="success">ok</Tag>
        <Tag color="error">bad</Tag>
        <Tag color="warning">warn</Tag>
        <Tag color="processing">proc</Tag>
      </AntdProvider>,
    );
    assertTheme(await renderedVars(), 'dark');
  });
});
