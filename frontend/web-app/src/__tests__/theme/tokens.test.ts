import { describe, expect, it } from 'vitest';
import {
  darkComponentTokens,
  darkTokens,
  lightComponentTokens,
  lightTokens,
} from '@/theme/tokens';

// Config-level intent. The rendered values are checked (including antd's
// generated aliases) by antd-theme.test.tsx.
// Re-skin 2026-10-27: seeds follow wise/DESIGN.md — lime primary #9fe870
// with ink labels, ink text, error/warning from the Wise semantic family.

describe('theme token configuration', () => {
  it('light seeds are the Wise lime primary plus the measured error/warning fixes', () => {
    expect(lightTokens.colorPrimary).toBe('#9fe870');
    expect(lightTokens.colorSuccess).toBe('#2ead4b');
    expect(lightTokens.colorInfo).toBe('#38c8ff');
    expect(lightTokens.colorError).toBe('#d03238');
    expect(lightTokens.colorWarning).toBe('#ffd11a');
    expect(lightTokens.colorTextPlaceholder).toBe('#6B7280');
    expect(lightTokens.colorPrimaryBg).toBeUndefined();
    expect(lightTokens.colorPrimaryBorder).toBeUndefined();
    // Wise primary is 1.47:1 on white, so it NEVER renders as text: text
    // roles are pinned to deep ink/body shades that clear AA.
    expect(lightTokens.colorLink).toBe('#054d28');
    expect(lightTokens.colorPrimaryText).toBe('#054d28');
    expect(lightTokens.colorSuccessText).toBe('#054d28');
    expect(lightTokens.colorTextDescription).toBe('#454745');
    // Wise hover/active are EXPLICIT lighter tints (antd's algorithm would
    // darken, which inverts the Wise interaction language).
    expect(lightTokens.colorPrimaryHover).toBe('#cdffad');
    expect(lightTokens.colorPrimaryActive).toBe('#c5edab');
  });

  it('keeps typography scale tokens (fontSize 14, headings 32/24/16/16/14; radius 12/24)', () => {
    expect(lightTokens.fontSize).toBe(14);
    expect(lightTokens.fontSizeHeading1).toBe(32);
    expect(lightTokens.fontSizeHeading2).toBe(24);
    expect(lightTokens.fontSizeHeading3).toBe(16);
    expect(lightTokens.fontSizeHeading4).toBe(16);
    expect(lightTokens.fontSizeHeading5).toBe(14);
    expect(lightTokens.borderRadius).toBe(12);
    expect(lightTokens.borderRadiusLG).toBe(24);
  });

  it('component tokens keep control heights and per-theme table headers', () => {
    expect(lightComponentTokens.Button?.controlHeight).toBe(48);
    expect(lightComponentTokens.Button?.controlHeightSM).toBe(44);
    expect(lightComponentTokens.Button?.fontWeight).toBe(600);
    expect(lightComponentTokens.Input?.controlHeight).toBe(48);
    expect(lightComponentTokens.Table?.headerColor).toBe('#454745');
    expect(darkComponentTokens.Table?.headerColor).toBe('#e8ebe6');
    // Ink labels on primary fills: 13.05:1 on the lime primary.
    expect(darkComponentTokens.Button?.dangerColor).toBe('#000000');
    expect(lightComponentTokens.Button?.primaryColor).toBe('#0e0f0c');
    expect(darkComponentTokens.Button?.primaryColor).toBe('#0e0f0c');
    // Tag text-on-tint (badge-positive/negative): deep text on pale tints.
    expect(lightComponentTokens.Tag?.colorText).toBe('#054d28');
    expect(lightComponentTokens.Tag?.colorErrorText).toBeUndefined();
    expect(darkComponentTokens.Tag?.colorErrorText).toBe('#ff9a9a');
    expect(darkComponentTokens.Tag?.colorErrorBg).toBe('#320707');
  });

  it('dark mode is the ink derivation, not the light seeds', () => {
    expect(darkTokens.colorErrorText).toBe('#ff9a9a');
    expect(darkTokens.colorWarningTextActive).toBe('#e8b727');
    expect(darkTokens.colorWarningBorder).toBe('#e8b727');
    expect(darkTokens.colorErrorBorder).toBe('#ff8a8a');
    // No lime-family FILL overrides remain (antd generates them from
    // #9fe870), except the explicit Wise hover/active tints.
    expect(darkTokens.colorPrimaryHover).toBe('#cdffad');
    expect(darkTokens.colorPrimaryActive).toBe('#c5edab');
    // The dark algorithm derives a 3.84:1 link-hover; pin the pale tint.
    expect(darkTokens.colorLinkHover).toBe('#e2f6d5');
    expect(darkTokens.colorLinkActive).toBe('#9fe870');
    expect(darkTokens.colorTextLightSolid).toBeUndefined();
    expect(darkTokens.colorPrimaryBg).toBeUndefined();
    // Text roles resolve to the light lime tint on the ink surface.
    expect(darkTokens.colorPrimaryText).toBe('#cdffad');
    expect(darkTokens.colorLink).toBe('#cdffad');
    expect(darkTokens.colorSuccessText).toBe('#cdffad');
    expect(darkTokens.colorTextDescription).toBe('#A6A6A6');
    expect(darkTokens.colorBgLayout).toBe('#0e0f0c');
    expect(darkTokens.colorBgContainer).toBe('#1a1c1a');
  });
});
