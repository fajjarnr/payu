import { describe, expect, it } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { Button, Tag } from 'antd';
import { AntdProvider } from '@/components/AntdProvider';
import { contrast, toHex } from './wcag';

// jsdom does not apply antd's `:where()`-wrapped CSS-in-JS rules to
// getComputedStyle, and antd remaps seeds (the dark algorithm turns
// #00D09C into #03b488 and #FF4757 into #dc5e5e). So the wiring is asserted on
// the CSS variables cssinjs actually generates from the ConfigProvider tokens:
// that stylesheet is what the browser applies, whatever the algorithm derives.
// Any regression in a generated alias (hover/active steps, tints, borders)
// fails here, which a config-level test cannot see.

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
const BORDERS = ['primary', 'success', 'info', 'warning', 'error'] as const;

function assertTheme(vars: Record<string, string>, mode: string) {
  const bg = vars['ant-color-bg-container'];
  const hexOf = (name: string) => toHex(vars[`ant-color-${name}`] ?? '', bg);

  for (const role of TEXT_ROLES) {
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

  // Secondary text is rgba; composite it over the surface first.
  const secondary = toHex(vars['ant-color-text-secondary'], bg);
  const secondaryRatio = contrast(secondary, bg);
  expect(
    secondaryRatio,
    `${mode}: --ant-color-text-secondary (${vars['ant-color-text-secondary']} -> ${secondary}) on ${bg} is ${secondaryRatio.toFixed(2)}:1`,
  ).toBeGreaterThanOrEqual(4.5);

  // Placeholder is text.
  const placeholderRatio = contrast(toHex(vars['ant-color-text-placeholder'], bg), bg);
  expect(placeholderRatio, `${mode}: placeholder on ${bg}`).toBeGreaterThanOrEqual(4.5);

  // Solid fill labels (primary and danger buttons), all interaction states.
  // Labels read from the button-specific vars so each fill can carry its own
  // label color (dark mode: ink on emerald, white on red).
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

  // Tag/alert text sits on status tints.
  for (const role of TEXT_ROLES) {
    const tint = vars[`ant-color-${role}-bg`];
    if (!tint) continue;
    const text = toHex(vars[`ant-color-${role}`], tint);
    const ratio = contrast(text, tint);
    expect(
      ratio,
      `${mode}: --ant-color-${role} text on --ant-color-${role}-bg ${tint} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
    ).toBeGreaterThanOrEqual(4.5);
  }

  // Component boundaries need 3:1 (SC 1.4.11).
  for (const role of BORDERS) {
    const border = vars[`ant-color-${role}-border`];
    if (!border) continue;
    const ratio = contrast(toHex(border, bg), bg);
    expect(
      ratio,
      `${mode}: --ant-color-${role}-border ${border} on ${bg} is ${ratio.toFixed(2)}:1, needs 3:1`,
    ).toBeGreaterThanOrEqual(3);
  }

  expect(hexOf('primary'), `${mode}: primary seed exists`).toMatch(/^#[0-9a-f]{6}$/);
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
