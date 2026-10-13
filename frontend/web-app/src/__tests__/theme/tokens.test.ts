import { describe, expect, it } from 'vitest';
import {
  darkComponentTokens,
  darkTokens,
  lightComponentTokens,
  lightTokens,
} from '@/theme/tokens';

// Config-level intent. The rendered values are checked (including antd's
// generated aliases) by antd-theme.test.tsx.

describe('theme token configuration', () => {
  it('light seeds are the measured AA-safe values', () => {
    expect(lightTokens.colorPrimary).toBe('#0B7A55');
    expect(lightTokens.colorSuccess).toBe('#0B7A55');
    expect(lightTokens.colorInfo).toBe('#0B7A55');
    expect(lightTokens.colorLink).toBe('#0B7A55');
    expect(lightTokens.colorError).toBe('#C62828');
    expect(lightTokens.colorWarning).toBe('#B45309');
    expect(lightTokens.colorTextPlaceholder).toBe('#6B7280');
    expect(lightTokens.colorPrimaryBg).toBe('#E6FFF7');
    expect(lightTokens.colorErrorBg).toBe('#FEF2F2');
  });

  it('keeps typography scale tokens (fontSize 14, headings 24/20/16/16/14)', () => {
    expect(lightTokens.fontSize).toBe(14);
    expect(lightTokens.fontSizeHeading1).toBe(24);
    expect(lightTokens.fontSizeHeading2).toBe(20);
    expect(lightTokens.fontSizeHeading3).toBe(16);
    expect(lightTokens.fontSizeHeading4).toBe(16);
    expect(lightTokens.fontSizeHeading5).toBe(14);
    expect(lightTokens.borderRadius).toBe(12);
    expect(lightTokens.borderRadiusLG).toBe(16);
  });

  it('component tokens keep control heights and per-theme table headers', () => {
    expect(lightComponentTokens.Button?.controlHeight).toBe(48);
    expect(lightComponentTokens.Button?.controlHeightSM).toBe(44);
    expect(lightComponentTokens.Button?.fontWeight).toBe(700);
    expect(lightComponentTokens.Input?.controlHeight).toBe(48);
    expect(lightComponentTokens.Table?.headerColor).toBe('#6B7280');
    expect(darkComponentTokens.Table?.headerColor).toBe('#ACACAC');
  });

  it('dark mode overrides every role the algorithm would otherwise remap or derive', () => {
    expect(darkTokens.colorTextLightSolid).toBe('#0A2E22');
    expect(darkTokens.colorLinkHover).toBe('#22ca9a');
    expect(darkTokens.colorLinkActive).toBe('#22ca9a');
    expect(darkTokens.colorErrorText).toBe('#ff9a9a');
    expect(darkTokens.colorWarningTextActive).toBe('#e8b727');
    expect(darkTokens.colorWarningBorder).toBe('#e8b727');
    expect(darkTokens.colorPrimaryBg).toBe('#082F24');
    expect(darkTokens.colorErrorBorder).toBe('#ff8a8a');
  });

  it('light mode does not carry the dark-only ink label token', () => {
    expect(lightTokens.colorTextLightSolid).toBeUndefined();
  });
});
