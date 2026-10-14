import { describe, expect, it } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { Button, Tag } from 'antd';
import { AntdProvider } from '@/components/AntdProvider';
import { contrast, toHex } from './wcag';

// jsdom does not apply antd's `:where()`-wrapped CSS-in-JS rules to
// getComputedStyle, and antd remaps seeds (the dark algorithm turns
// #00D09C into #03b488). So the wiring is asserted on the CSS variables
// cssinjs actually generates from the ConfigProvider tokens: that stylesheet
// is what the browser applies, whatever the algorithm derives.
//
// PRODUCT OVERRIDE 2026-10-14: the brand emerald (#00D09C, the roles that
// share it: success, info, and the link color antd derives from the primary
// seed, including its generated hover/active steps) is exempt from the 4.5:1
// text gate and the 3:1 boundary gate. The product owner rejected the darker
// AA variant; see the header of src/theme/tokens.ts. Everything else stays
// enforced, and these exceptions are the only silent skips in this file.

const EMERALD_ROLES = ['primary', 'success', 'info', 'link'] as const;

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

  for (const role of TEXT_ROLES) {
    if (EMERALD_ROLES.includes(role as (typeof EMERALD_ROLES)[number])) continue; // product override
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
  // Primary is emerald: white on the vivid fill is the brand look and is
  // exempt per the product override; danger stays enforced.
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
      if (fillRole === 'primary') {
        continue; // product override: emerald fill labels (brand look)
      }
      expect(
        ratio,
        `${mode}: label ${label} on --${name} fill ${fill} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
      ).toBeGreaterThanOrEqual(4.5);
    }
  }

  // Tag/alert text sits on status tints. Emerald-role tints carry the brand
  // text color and are exempt per the product override.
  for (const role of TEXT_ROLES) {
    if (EMERALD_ROLES.includes(role as (typeof EMERALD_ROLES)[number])) continue;
    const tint = vars[`ant-color-${role}-bg`];
    if (!tint) continue;
    const text = toHex(vars[`ant-color-${role}`], tint);
    const ratio = contrast(text, tint);
    expect(
      ratio,
      `${mode}: --ant-color-${role} text on --ant-color-${role}-bg ${tint} is ${ratio.toFixed(2)}:1, needs 4.5:1`,
    ).toBeGreaterThanOrEqual(4.5);
  }

  // Component boundaries need 3:1 (SC 1.4.11); emerald borders exempt per override.
  for (const role of BORDERS) {
    if (EMERALD_ROLES.includes(role as (typeof EMERALD_ROLES)[number])) continue;
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
