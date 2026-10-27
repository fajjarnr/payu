import type { ThemeConfig } from 'antd';

export type SeedTokens = NonNullable<ThemeConfig['token']>;
export type ComponentTokens = NonNullable<ThemeConfig['components']>;

const FONT_FAMILY = 'Inter, system-ui, sans-serif';

/**
 * Wise re-skin 2026-10-27: seeds follow `wise/DESIGN.md` — a lime primary
 * (`#9fe870`) with ink labels, ink text, sage layout, and the Wise semantic
 * family. Every pairing below was measured, not guessed:
 *
 * - Ink `#0e0f0c` on lime `#9fe870`: 13.05:1 (primary button labels, F-01).
 * - Lime on white is 1.47:1, so the primary/success/info fills NEVER render
 *   as text: their text roles pin deep shades (`positive-deep #054d28`,
 *   10.01:1 on white). The contrast guard (`antd-theme.test.tsx`) enforces
 *   this structurally.
 * - Wise hover/active go LIGHTER (`primary-active #cdffad`, `primary-neutral
 *   #c5edab`) — the inverse of antd's darkening algorithm — so they are set
 *   explicitly, never derived.
 * - Text-input and tertiary-button borders are ink (`#0e0f0c`, 1px), which
 *   also satisfies SC 1.4.11 (3:1) on white.
 * - Dark mode is an ink derivation (DESIGN.md has no dark tokens): layout
 *   `#0e0f0c`, containers `#1a1c1a`, text roles resolve to the light lime
 *   tint `#cdffad` (16.94:1 on ink).
 */

// Wise core (wise/DESIGN.md colors.*)
const LIME = '#9fe870'; // primary; 1.47:1 on white (fills only), 13.05:1 under ink labels
const LIME_HOVER = '#cdffad'; // primary-active
const LIME_ACTIVE = '#c5edab'; // primary-neutral
const LIME_PALE = '#e2f6d5'; // primary-pale
const INK = '#0e0f0c'; // text + dark surface; 19.23:1 on white
const INK_DEEP = '#163300'; // 13.93:1 on white
const BODY = '#454745'; // secondary text; 9.37:1 on white
const CANVAS_SOFT = '#e8ebe6'; // sage layout surface
const POSITIVE = '#2ead4b'; // success seed (fills); 2.92:1 on white, never text
const POSITIVE_DEEP = '#054d28'; // success/warning-safe text; 10.01:1 on white
const INFO = '#38c8ff'; // info seed (fills); 1.93:1 on white, never text
const WARNING = '#ffd11a'; // warning seed (fills); 1.46:1 on white, never text
const WARNING_CONTENT = '#4a3b1c'; // text on warning surfaces; 7.44:1 on #ffd11a
const NEGATIVE = '#d03238'; // 5.01:1 on white, both directions
const NEGATIVE_HOVER = '#a72027'; // 7.29:1 under white labels
const NEGATIVE_ACTIVE = '#a7000d'; // 7.93:1 under white labels
const NEGATIVE_BG = '#320707'; // dark maroon callouts; 17.96:1 under white

export const lightTokens: SeedTokens = {
  colorPrimary: LIME,
  colorSuccess: POSITIVE,
  colorInfo: INFO,
  colorError: NEGATIVE,
  colorWarning: WARNING,
  colorPrimaryHover: LIME_HOVER,
  colorPrimaryActive: LIME_ACTIVE,
  colorText: INK,
  colorTextSecondary: BODY,
  colorTextPlaceholder: '#6B7280', // 4.83:1; antd default rgba(0,0,0,0.25) is ~2.3:1
  // Wise `mute #868685` is 3.64:1 on white (fails AA as small text), so
  // secondary/tertiary text uses Wise `body #454745` instead (9.37:1).
  colorTextDescription: BODY,
  // Lime-family fills NEVER render as text; every link / accent / success
  // label uses positive-deep (10.01:1 on white).
  colorLink: POSITIVE_DEEP,
  colorPrimaryText: POSITIVE_DEEP,
  colorPrimaryTextHover: INK_DEEP,
  colorPrimaryTextActive: INK,
  colorSuccessText: POSITIVE_DEEP,
  colorSuccessTextHover: INK_DEEP,
  colorSuccessTextActive: INK,
  colorInfoText: POSITIVE_DEEP,
  colorErrorHover: NEGATIVE_HOVER,
  colorErrorActive: NEGATIVE_ACTIVE,
  colorWarningHover: WARNING,
  colorWarningActive: WARNING,
  colorErrorText: NEGATIVE,
  colorErrorTextHover: NEGATIVE_HOVER,
  colorErrorTextActive: NEGATIVE_ACTIVE,
  colorWarningText: WARNING_CONTENT,
  colorWarningTextHover: WARNING_CONTENT,
  colorWarningTextActive: WARNING_CONTENT,
  colorWarningBg: '#FFFBEB',
  colorErrorBg: '#FEF2F2',
  colorWarningBorder: WARNING_CONTENT,
  colorErrorBorder: NEGATIVE,
  colorSuccessBg: LIME_PALE,
  colorSuccessBorder: POSITIVE_DEEP,
  colorBgLayout: CANVAS_SOFT,
  colorBgContainer: '#ffffff',
  colorBorder: INK,
  colorBorderSecondary: CANVAS_SOFT,
  borderRadius: 12,
  borderRadiusLG: 24,
  // antd v6 dropped the `borderRadiusXL` alias token; set 24px per-component
  // (Card, Modal, Button) in the component tokens below.
  fontFamily: FONT_FAMILY,
  fontSize: 14,
  // wise/DESIGN.md product roles: display-sm 32/600, display-xs 24/600.
  fontSizeHeading1: 32,
  fontSizeHeading2: 24,
  fontSizeHeading3: 16,
  fontSizeHeading4: 16,
  fontSizeHeading5: 14,
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
  colorSuccessBg: '#0d2b16',
  colorSuccessBorder: '#cdffad',
  colorText: CANVAS_SOFT,
  colorTextSecondary: '#A6A6A6',
  colorTextPlaceholder: '#9CA3AF', // 7.26:1 on #141414
  colorTextDescription: '#A6A6A6', // 7.58:1 on #141414 (dark secondary ink)
  // Dark mode text roles resolve to the light lime tint (16.94:1 on ink).
  colorLink: LIME_HOVER,
  colorLinkHover: LIME_PALE,
  colorLinkActive: LIME,
  colorPrimaryText: LIME_HOVER,
  colorPrimaryTextHover: LIME_PALE,
  colorPrimaryTextActive: LIME,
  colorSuccessText: LIME_HOVER,
  colorSuccessTextHover: LIME_PALE,
  colorSuccessTextActive: LIME,
  colorInfoText: LIME_HOVER,
  colorBgLayout: INK,
  colorBgContainer: '#1a1c1a',
  colorBorder: CANVAS_SOFT,
  colorBorderSecondary: '#2a2d2a',
};

export const ON_PRIMARY = '#0e0f0c'; // 13.05:1 on #9fe870; ink labels on primary fills (F-01, per usage)

// Wise fixed surfaces (wise/DESIGN.md): the dark card and footer are ALWAYS
// ink, in both themes — so pages read these constants, not literals (A-01).
export const INK_SURFACE = '#0e0f0c';
export const INK_SURFACE_SOFT = '#1a1c1a';
export const INK_SURFACE_BORDER = '#2a2d2a';
export const LIME_ACCENT = '#9fe870';
export const SAGE_ON_INK = '#e8ebe6';

// Card art (the debit-card metaphor on the dashboard). It is a fixed brand
// artifact — the physical card looks the same in light and dark mode — so it
// lives here with the other brand literals instead of a raw hsl() in
// globals.css (A-01: every colour comes from the theme module).
export const CARD_ART_GRADIENT =
  'linear-gradient(135deg, #163300 0%, #0e0f0c 60%, #1a1c1a 100%)';

// Brand panel (the dark marketing column on login / merchant register). It is
// always dark in both themes: Wise ink with sage/pale text (15.98:1).
export const BRAND_PANEL_BG = '#0e0f0c';
export const BRAND_PANEL_FG = '#e8ebe6';
export const BRAND_PANEL_MUTED = '#e2f6d5';

// Danger panel (the security "panic protocol" card). A fixed deep maroon
// keeps the white label at 17.96:1 in both themes.
export const DANGER_PANEL_BG = '#320707';
const sharedButtonTokens = {
  // Ink labels on the lime fill (F-01): 13.05:1, no palette swap needed.
  primaryColor: ON_PRIMARY,
  borderRadiusLG: 24,
};

const sharedComponentTokens: ComponentTokens = {
  Button: {
    controlHeight: 48,
    controlHeightLG: 56,
    controlHeightSM: 44,
    fontWeight: 600,
    ...sharedButtonTokens,
  },
  Card: {
    // Wise elevation is surface contrast (white on sage), not shadow.
    borderRadiusLG: 24,
    boxShadowTertiary: 'none',
    colorBorderSecondary: CANVAS_SOFT,
  },
  Modal: {
    borderRadiusLG: 24,
  },
  Input: {
    controlHeight: 48,
    borderRadius: 12,
    colorBorder: INK,
  },
  Select: {
    controlHeight: 48,
    colorBorder: INK,
  },
  Tag: {
    // badge-positive: pale tint + deep text (8.76:1).
    colorText: POSITIVE_DEEP,
    borderRadiusSM: 9999,
  },
  Menu: {
    // The selected item's label is text on the sage fill: deep shade (AA).
    itemSelectedColor: POSITIVE_DEEP,
    itemSelectedBg: LIME_PALE,
  },
  Tabs: {
    itemSelectedColor: POSITIVE_DEEP,
  },
};

export const lightComponentTokens: ComponentTokens = {
  ...sharedComponentTokens,
  Table: {
    headerBg: CANVAS_SOFT,
    headerColor: BODY, // 7.79:1 on sage
  },
};

export const darkComponentTokens: ComponentTokens = {
  ...sharedComponentTokens,
  Menu: {
    itemSelectedColor: LIME_HOVER,
    itemSelectedBg: '#1a1c1a',
  },
  Tabs: {
    itemSelectedColor: LIME_HOVER,
  },
  Button: {
    ...sharedButtonTokens,
    // Danger labels only: white on the dark-mapped red fill (#dc5e5e) is
    // 3.62:1; black is 5.8:1. Primary buttons keep ink labels per F-01.
    dangerColor: '#000000',
  },
  Tag: {
    ...sharedComponentTokens.Tag,
    // badge-negative on ink: light red text on the dark maroon tint.
    colorErrorText: '#ff9a9a',
    colorErrorBg: NEGATIVE_BG,
  },
  Table: {
    headerBg: 'transparent',
    headerColor: '#e8ebe6', // 15.31:1 on #141414-class surfaces
  },
};
