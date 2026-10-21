import type { ThemeConfig } from 'antd';

export type SeedTokens = NonNullable<ThemeConfig['token']>;
export type ComponentTokens = NonNullable<ThemeConfig['components']>;

const FONT_FAMILY = 'Inter, system-ui, sans-serif';

/**
 * PRODUCT DECISION 2026-10-14: the brand emerald is the vivid `#00D09C`
 * (PayU green since the antd migration, commit 7cd9f2abd). A previous
 * revision darkened it to #0B7A55 for WCAG AA; the product owner rejected
 * that look and the vivid green is restored verbatim.
 *
 * Known gap, accepted by that decision: `#00D09C` measures 2.00:1 on white,
 * so it fails AA as small text (links, tag text, stat text on pages that read
 * `token.colorPrimary`) and as a fill under a white button label. The contrast
 * guard (`antd-theme.test.tsx`) keeps every other role enforced and lists the
 * emerald roles as explicit, dated exceptions instead of silently skipping
 * them. If AA compliance for the green is required later, the fix is per
 * usage (darker text token, or ink labels on primary fills), not a palette
 * swap.
 *
 * Everything unrelated to the emerald keeps its measured fix from the same
 * revision: error seeds, warning seeds, placeholder, table headers.
 */

const EMERALD = '#00D09C'; // brand emerald; 2.00:1 on white (see PRODUCT DECISION above)
const RED = '#C62828'; // 5.62:1 on white
const RED_HOVER = '#A61B1B'; // 7.52:1
const RED_ACTIVE = '#8B1A1A'; // darker than hover, above 7.52:1
const AMBER = '#B45309'; // 5.02:1
const AMBER_HOVER = '#9A4A08'; // 6.26:1
const AMBER_ACTIVE = '#80400A'; // 7.91:1

export const lightTokens: SeedTokens = {
  colorPrimary: EMERALD,
  colorSuccess: EMERALD,
  colorInfo: EMERALD,
  colorError: RED,
  colorWarning: AMBER,
  colorTextPlaceholder: '#6B7280', // 4.83:1; antd default rgba(0,0,0,0.25) is ~2.3:1
  // antd v6 paints "secondary" typography and menu group titles with
  // colorTextDescription = rgba(0,0,0,0.45) -> 3.36:1 on white (fails AA).
  // Darker than DESIGN.md text-secondary (#6B7280) so secondary text also
  // clears AA on the layout (#f5f5f5) and on the landing page's tinted cards:
  // 4.65:1 on #ebebeb, 5.1:1 on #f5f5f5, 5.5:1 on white.
  colorTextDescription: '#626975',
  // Brand green as TEXT: the vivid emerald measures 1.99:1 on white, so every
  // link / accent / success label uses the deep brand green (5.3:1). The
  // emerald stays on fills (buttons, tags, charts) per the product decision.
  colorLink: '#0B7A55',
  colorPrimaryText: '#0B7A55',
  colorPrimaryTextHover: '#095F43',
  colorPrimaryTextActive: '#074A34',
  colorSuccessText: '#0B7A55',
  colorSuccessTextHover: '#095F43',
  colorSuccessTextActive: '#074A34',
  colorErrorHover: RED_HOVER,
  colorErrorActive: RED_ACTIVE,
  colorWarningHover: AMBER_HOVER,
  colorWarningActive: AMBER_ACTIVE,
  colorErrorText: RED,
  colorErrorTextHover: RED_HOVER,
  colorErrorTextActive: RED_ACTIVE,
  colorWarningText: AMBER,
  colorWarningTextHover: AMBER_HOVER,
  colorWarningTextActive: AMBER_ACTIVE,
  colorWarningBg: '#FFFBEB', // 4.84:1 with warning text
  colorErrorBg: '#FEF2F2', // 5.14:1 with error text
  colorWarningBorder: AMBER,
  colorErrorBorder: RED,
  borderRadius: 12,
  borderRadiusLG: 16,
  // antd v6 dropped the `borderRadiusXL` alias token; set 24px per-component
  // (e.g. Modal/Sheet) when those migrations land.
  fontFamily: FONT_FAMILY,
  fontSize: 14,
  // DESIGN.md role scale — antd Title levels map to spec roles, not antd defaults
  fontSizeHeading1: 24,
  fontSizeHeading2: 20,
  fontSizeHeading3: 16,
  fontSizeHeading4: 16,
  fontSizeHeading5: 14,
  // DESIGN.md display/caption/label roles documented on shared antd tokens —
  // antd has no fontSizeDisplay/fontSizeCaption/fontSizeLabel seeds, so these
  // roles read fontSizeXL (32, display), fontSizeSM (12, caption) and
  // fontSizeSM (12, label, weight 500 at the call site).
  fontSizeXL: 32,
  fontSizeSM: 12,
};

export const darkTokens: SeedTokens = {
  ...lightTokens,
  colorError: '#FF6B6B', // dark seed, maps to #dc5e5e (5.09:1 on #141414)
  colorWarning: '#FFB800', // dark seed, maps to #dc9f03 (7.90:1 on #141414)
  colorErrorHover: '#ff8a8a',
  colorErrorActive: '#ff9a9a', // 9.07:1
  colorErrorText: '#ff9a9a',
  colorErrorTextHover: '#ff8a8a',
  colorErrorTextActive: '#ff9a9a',
  colorErrorBorder: '#ff8a8a',
  colorWarningHover: '#e8b727', // 9.86:1 on #141414
  colorWarningActive: '#e8b727',
  colorWarningText: '#FFB800', // 10.62:1 on #141414
  colorWarningTextHover: '#e8b727',
  colorWarningTextActive: '#e8b727',
  colorWarningBorder: '#e8b727',
  colorWarningBg: '#2c2211',
  colorErrorBg: '#2c1a1b',
  colorTextPlaceholder: '#9CA3AF', // 7.26:1 on #141414
  colorTextDescription: '#A6A6A6', // 7.58:1 on #141414 (dark secondary ink)
  // Dark mode needs a lighter brand green for text (10:1 on #141414).
  colorLink: '#35D6A0',
  colorPrimaryText: '#35D6A0',
  colorPrimaryTextHover: '#6FE3BC',
  colorPrimaryTextActive: '#4BD9A8',
  colorSuccessText: '#35D6A0',
  colorSuccessTextHover: '#6FE3BC',
  colorSuccessTextActive: '#4BD9A8',
};

export const INK_ON_EMERALD = '#06281F'; // ~8:1 on #00D09C; ink labels on primary fills (F-01, per usage)

// Card art (the debit-card metaphor on the dashboard). It is a fixed brand
// artifact — the physical card looks the same in light and dark mode — so it
// lives here with the other brand literals instead of a raw hsl() in
// globals.css (A-01: every colour comes from the theme module).
export const CARD_ART_GRADIENT =
  'linear-gradient(135deg, #1A7440 0%, #1F5C38 50%, #145233 100%)';
const sharedButtonTokens = {
  // White on the vivid emerald fill is 2.00:1 (accepted brand gap); ink
  // labels on primary fills restore AA without a palette swap.
  primaryColor: INK_ON_EMERALD,
};

const sharedComponentTokens: ComponentTokens = {
  Button: {
    controlHeight: 48,
    controlHeightLG: 56,
    controlHeightSM: 44,
    fontWeight: 700,
    ...sharedButtonTokens,
  },
  Card: {
    borderRadiusLG: 16,
    boxShadowTertiary:
      '0 2px 8px -2px rgba(0, 0, 0, 0.05), 0 4px 16px -4px rgba(0, 0, 0, 0.08)',
  },
  Input: {
    controlHeight: 48,
    borderRadius: 12,
  },
};

export const lightComponentTokens: ComponentTokens = {
  ...sharedComponentTokens,
  Table: {
    headerBg: 'transparent',
    headerColor: '#6B7280', // 4.83:1 on surface; replaces rgba(0,0,0,0.45) (3.36:1)
  },
};

export const darkComponentTokens: ComponentTokens = {
  ...sharedComponentTokens,
  Button: {
    ...sharedButtonTokens,
    // Danger labels only. White on the dark-mapped red fill (#dc5e5e) is
    // 3.62:1; black is 5.8:1. Primary buttons keep ink labels per F-01.
    dangerColor: '#000000',
  },
  Table: {
    headerBg: 'transparent',
    headerColor: '#ACACAC', // 8.12:1 on #141414; the old rgba(0,0,0,0.45) was near-invisible in dark mode
  },
};
