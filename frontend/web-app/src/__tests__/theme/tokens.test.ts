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
  it('light seeds are the brand emerald plus the measured error/warning fixes', () => {
    // PRODUCT DECISION 2026-10-14: emerald restored to #00D09C (brand since
    // the antd migration); known AA gap accepted, see tokens.ts header.
    expect(lightTokens.colorPrimary).toBe('#00D09C');
    expect(lightTokens.colorSuccess).toBe('#00D09C');
    expect(lightTokens.colorInfo).toBe('#00D09C');
    expect(lightTokens.colorError).toBe('#C62828');
    expect(lightTokens.colorWarning).toBe('#B45309');
    expect(lightTokens.colorTextPlaceholder).toBe('#6B7280');
    expect(lightTokens.colorPrimaryBg).toBeUndefined();
    expect(lightTokens.colorPrimaryBorder).toBeUndefined();
    // Text roles are pinned to the deep brand green (the emerald stays a fill).
    expect(lightTokens.colorLink).toBe('#0B7A55');
    expect(lightTokens.colorPrimaryText).toBe('#0B7A55');
    expect(lightTokens.colorSuccessText).toBe('#0B7A55');
    expect(lightTokens.colorTextDescription).toBe('#626975');
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
    // Ink labels on primary fills (F-01): AA on the vivid emerald without a palette swap.
    expect(darkComponentTokens.Button?.dangerColor).toBe('#000000');
    expect(lightComponentTokens.Button?.primaryColor).toBe('#06281F');
    expect(darkComponentTokens.Button?.primaryColor).toBe('#06281F');
  });

  it('dark mode keeps the error/warning measured fixes, no emerald overrides', () => {
    expect(darkTokens.colorErrorText).toBe('#ff9a9a');
    expect(darkTokens.colorWarningTextActive).toBe('#e8b727');
    expect(darkTokens.colorWarningBorder).toBe('#e8b727');
    expect(darkTokens.colorErrorBorder).toBe('#ff8a8a');
    // No emerald-family FILL overrides remain (antd generates them from
    // #00D09C).
    expect(darkTokens.colorPrimaryHover).toBeUndefined();
    expect(darkTokens.colorLinkHover).toBeUndefined();
    expect(darkTokens.colorTextLightSolid).toBeUndefined();
    expect(darkTokens.colorPrimaryBg).toBeUndefined();
    // The emerald stays a fill colour; roles that render as TEXT are pinned to
    // the brand green so they clear AA (see docs/ANT_GUIDELINES.md §2.2).
    expect(darkTokens.colorPrimaryText).toBe('#35D6A0');
    expect(darkTokens.colorLink).toBe('#35D6A0');
    expect(darkTokens.colorSuccessText).toBe('#35D6A0');
    expect(darkTokens.colorTextDescription).toBe('#A6A6A6');
  });
});
