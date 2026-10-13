import type { ThemeConfig } from 'antd';

export type SeedTokens = NonNullable<ThemeConfig['token']>;
export type ComponentTokens = NonNullable<ThemeConfig['components']>;

const FONT_FAMILY = 'Inter, system-ui, sans-serif';

/**
 * antd renders seed colors as text (links, form errors, tags, alert text) and as
 * fills under labels (primary and danger buttons), so every seed must pass
 * WCAG AA (4.5:1) against the surface it sits on, and generated hover/active
 * steps must too. antd's own palette generation does not guarantee that
 * (default hover steps lighten below 4.5, dark steps darken below 4.5), so the
 * steps are set explicitly.
 *
 * One hue cannot serve both jobs on both surfaces:
 * - Light: the vivid brand emerald (#00D09C) is 2.00:1 on white, so light mode
 *   uses the repo's own AA-safe primary (#0B7A55, 5.34:1, = CSS --color-primary).
 *   Error is #C62828 (5.62:1, white label on it passes; #E03131 at 4.51:1 drops
 *   below 4.5 on tinted tag backgrounds), warning #B45309 (5.02:1).
 * - Dark (#141414): the vivid brand colors pass (emerald renders as #03b488 at
 *   6.92:1, error #dc5e5e at 5.09:1, warning #dc9f03 at 7.90:1), so dark keeps
 *   the brand. Primary labels switch to dark ink (colorTextLightSolid #0A2E22,
 *   5.53:1 on the fill) because white on emerald is 2.00:1.
 *
 * colorSuccess/colorInfo equal colorPrimary in both themes, preserving the
 * single-emerald identity of the original config (all three seeds were #00D09C).
 *
 * Tints and borders are set explicitly too: generated values from the darker
 * emerald seed fail (success-bg renders as a mid gray #a8bab1; borders land
 * near 2:1). Chosen tints are measured: primary text on primary tint 5.09:1,
 * success 5.11:1, warning 4.84:1, error 5.14:1 (dark 4.57:1+).
 *
 * Ratios cited above are measured. Guards:
 * - src/__tests__/theme/tokens.test.ts checks the configured values (light and
 *   unmapped dark aliases render verbatim).
 * - src/__tests__/theme/antd-theme.test.tsx extracts the CSS variables cssinjs
 *   actually generates for both algorithms and re-checks every pair, including
 *   generated aliases (hover/active steps, tag-on-tint) that config tests cannot see.
 */

const EMERALD_TEXT = '#0B7A55'; // 5.34:1 on white
const EMERALD_HOVER = '#0A6B48'; // 6.54:1
const EMERALD_ACTIVE = '#08543A'; // 8.98:1
const RED = '#C62828'; // 5.62:1 on white
const RED_HOVER = '#A61B1B'; // 7.52:1
const RED_ACTIVE = '#8B1A1A'; // darker than hover, above 7.52:1
const AMBER = '#B45309'; // 5.02:1
const AMBER_HOVER = '#9A4A08'; // 6.26:1
const AMBER_ACTIVE = '#80400A'; // 7.91:1
const EMERALD_VIVID = '#00D09C'; // dark-mode brand emerald
const EMERALD_STEP = '#22ca9a'; // antd maps the vivid seed to #03b488; the step is the bright variant
const RED_VIVID = '#FF6B6B'; // dark seed, maps to #dc5e5e (5.09:1)
const RED_STEP = '#ff9a9a'; // 9.07:1 on #141414
const RED_STEP_HOVER = '#ff8a8a';
const AMBER_STEP = '#e8b727'; // 9.86:1 on #141414
const INK = '#0A2E22'; // 5.53:1 on the dark primary fill

export const lightTokens: SeedTokens = {
  colorPrimary: EMERALD_TEXT,
  colorSuccess: EMERALD_TEXT,
  colorInfo: EMERALD_TEXT,
  colorLink: EMERALD_TEXT,
  colorError: RED,
  colorWarning: AMBER,
  colorTextPlaceholder: '#6B7280', // 4.83:1; antd default rgba(0,0,0,0.25) is ~2.3:1
  colorPrimaryHover: EMERALD_HOVER,
  colorPrimaryActive: EMERALD_ACTIVE,
  colorLinkHover: EMERALD_HOVER,
  colorLinkActive: EMERALD_ACTIVE,
  colorSuccessHover: EMERALD_HOVER,
  colorSuccessActive: EMERALD_ACTIVE,
  colorInfoHover: EMERALD_HOVER,
  colorInfoActive: EMERALD_ACTIVE,
  colorErrorHover: RED_HOVER,
  colorErrorActive: RED_ACTIVE,
  colorWarningHover: AMBER_HOVER,
  colorWarningActive: AMBER_ACTIVE,
  colorPrimaryText: EMERALD_TEXT,
  colorPrimaryTextHover: EMERALD_HOVER,
  colorPrimaryTextActive: EMERALD_ACTIVE,
  colorSuccessText: EMERALD_TEXT,
  colorSuccessTextHover: EMERALD_HOVER,
  colorSuccessTextActive: EMERALD_ACTIVE,
  colorInfoText: EMERALD_TEXT,
  colorInfoTextHover: EMERALD_HOVER,
  colorInfoTextActive: EMERALD_ACTIVE,
  colorErrorText: RED,
  colorErrorTextHover: RED_HOVER,
  colorErrorTextActive: RED_ACTIVE,
  colorWarningText: AMBER,
  colorWarningTextHover: AMBER_HOVER,
  colorWarningTextActive: AMBER_ACTIVE,
  colorPrimaryBg: '#E6FFF7', // 5.09:1 with primary text
  colorSuccessBg: '#F0FDF4', // 5.11:1
  colorInfoBg: '#E6FFF7',
  colorWarningBg: '#FFFBEB', // 4.84:1
  colorErrorBg: '#FEF2F2', // 5.14:1
  colorPrimaryBorder: EMERALD_TEXT, // 5.34:1 as a boundary
  colorSuccessBorder: EMERALD_TEXT,
  colorInfoBorder: EMERALD_TEXT,
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
};

export const darkTokens: SeedTokens = {
  ...lightTokens,
  colorPrimary: EMERALD_VIVID,
  colorSuccess: EMERALD_VIVID,
  colorInfo: EMERALD_VIVID,
  colorLink: EMERALD_VIVID,
  colorError: RED_VIVID,
  colorWarning: '#FFB800',
  colorTextPlaceholder: '#9CA3AF', // 7.26:1 on #141414
  // Primary fills keep the emerald; labels need dark ink (5.53:1) because
  // white on #00D09C is 2.00:1. Both solid button label vars inherit this.
  colorTextLightSolid: INK,
  colorPrimaryHover: EMERALD_STEP,
  colorPrimaryActive: EMERALD_STEP,
  colorPrimaryText: EMERALD_STEP,
  colorPrimaryTextHover: EMERALD_STEP,
  colorPrimaryTextActive: EMERALD_STEP,
  colorSuccessHover: EMERALD_STEP,
  colorSuccessActive: EMERALD_STEP,
  colorSuccessText: EMERALD_STEP,
  colorSuccessTextHover: EMERALD_STEP,
  colorSuccessTextActive: EMERALD_STEP,
  colorInfoHover: EMERALD_STEP,
  colorInfoActive: EMERALD_STEP,
  colorInfoText: EMERALD_STEP,
  colorInfoTextHover: EMERALD_STEP,
  colorInfoTextActive: EMERALD_STEP,
  colorLinkHover: EMERALD_STEP,
  colorLinkActive: EMERALD_STEP,
  colorErrorHover: RED_STEP_HOVER,
  colorErrorActive: RED_STEP,
  colorErrorText: RED_STEP,
  colorErrorTextHover: RED_STEP_HOVER,
  colorErrorTextActive: RED_STEP,
  colorErrorBorder: RED_STEP_HOVER,
  colorWarningHover: AMBER_STEP,
  colorWarningActive: AMBER_STEP,
  colorWarningText: '#FFB800', // 10.62:1 on #141414; the light #B45309 overflows from the spread
  colorWarningTextHover: AMBER_STEP,
  colorWarningTextActive: AMBER_STEP,
  colorWarningBorder: AMBER_STEP,
  colorPrimaryBg: '#082F24', // tag text stays >=4.5:1 on it in both text roles
  colorSuccessBg: '#112521',
  colorInfoBg: '#082F24',
  colorWarningBg: '#2c2211',
  colorErrorBg: '#2c1a1b',
};

const sharedComponentTokens: ComponentTokens = {
  Button: {
    controlHeight: 48,
    controlHeightLG: 56,
    controlHeightSM: 44,
    fontWeight: 700,
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
    // Dark labels: ink on the emerald fill (5.53:1 on #03b488). The red fill
    // renders as #dc5e5e, which fails with both white (3.62:1) and ink
    // (4.07:1); black gives 5.8:1, and the red seed keeps its 4.26:1 for
    // borders and icons (3:1 non-text threshold).
    primaryColor: INK,
    dangerColor: '#000000',
  },
  Table: {
    headerBg: 'transparent',
    headerColor: '#ACACAC', // 8.12:1 on #141414; the old rgba(0,0,0,0.45) was near-invisible in dark mode
  },
};
