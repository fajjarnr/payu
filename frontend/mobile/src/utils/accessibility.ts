/** Accessibility helpers: prop generation, WCAG 2.1 touch-target and contrast validation, screen-reader formatting. */

import { AccessibilityProps, AccessibilityRole, AccessibilityState } from 'react-native';


/**
 * Extended accessibility props with additional PayU-specific attributes
 */
export interface PayUAccessibilityProps extends AccessibilityProps {
  /** Unique identifier for testing and accessibility */
  testID?: string;
  /** Accessibility label for screen readers */
  accessibilityLabel?: string;
  /** Hint describing the action result */
  accessibilityHint?: string;
  /** Role of the element */
  accessibilityRole?: AccessibilityRole;
  /** Current state of the element */
  accessibilityState?: AccessibilityState;
  /** Whether the element is focused */
  accessibilityFocused?: boolean;
  /** Language for the accessibility text */
  accessibilityLanguage?: string;
  /** Value for elements like sliders, switches */
  accessibilityValue?: {
    min?: number;
    max?: number;
    now?: number;
    text?: string;
  };
}

export interface A11yConfig {
  /** Primary label describing the element */
  label: string;
  /** Optional hint describing what happens on action */
  hint?: string;
  /** Accessibility role */
  role?: AccessibilityRole;
  /** Whether the element is disabled */
  disabled?: boolean;
  /** Whether the element is selected */
  selected?: boolean;
  /** Whether the element is checked */
  checked?: boolean;
  /** Whether the element is busy */
  busy?: boolean;
  /** Whether the element is expanded */
  expanded?: boolean;
  /** Test ID for automation */
  testID?: string;
  /** Language code (e.g., 'id', 'en') */
  language?: string;
}

export interface TouchTargetValidation {
  /** Whether the touch target meets minimum size */
  isValid: boolean;
  /** Current width of the element */
  width: number;
  /** Current height of the element */
  height: number;
  /** Minimum required width (default: 44) */
  minWidth: number;
  /** Minimum required height (default: 44) */
  minHeight: number;
  /** Error message if invalid */
  error?: string;
}

export interface ContrastValidation {
  /** Whether the contrast ratio meets WCAG standards */
  isValid: boolean;
  /** Calculated contrast ratio */
  ratio: number;
  /** Required minimum ratio */
  requiredRatio: number;
  /** WCAG level achieved (AA or AAA) */
  level?: 'AA' | 'AAA' | 'fail';
  /** Foreground color */
  foreground: string;
  /** Background color */
  background: string;
}


/** Minimum touch target size per WCAG 2.1 guidelines */
export const MIN_TOUCH_TARGET_SIZE = 44;

/** WCAG 2.1 contrast ratio requirements */
export const CONTRAST_RATIOS = {
  /** Normal text AA requirement */
  AA_NORMAL: 4.5,
  /** Large text AA requirement */
  AA_LARGE: 3,
  /** Normal text AAA requirement */
  AAA_NORMAL: 7,
  /** Large text AAA requirement */
  AAA_LARGE: 4.5,
  /** UI components AA requirement */
  AA_UI: 3,
};

/** Common accessibility roles for banking app */
export const A11Y_ROLES = {
  BUTTON: 'button',
  LINK: 'link',
  HEADER: 'header',
  IMAGE: 'image',
  TEXT: 'text',
  SEARCH: 'search',
  SUMMARY: 'summary',
  SWITCH: 'switch',
  ADJUSTABLE: 'adjustable',
  CHECKBOX: 'checkbox',
  RADIO: 'radio',
  NONE: 'none',
} as const;


/** Builds React Native accessibility props (label, hint, role, state) from an A11yConfig. */
export function generateA11yProps(config: A11yConfig): PayUAccessibilityProps {
  const props: PayUAccessibilityProps = {
    accessibilityLabel: config.label,
    testID: config.testID || generateTestID(config.label),
  };

  if (config.hint) {
    props.accessibilityHint = config.hint;
  }

  if (config.role) {
    props.accessibilityRole = config.role;
  }

  const state: AccessibilityState = {};
  if (config.disabled !== undefined) state.disabled = config.disabled;
  if (config.selected !== undefined) state.selected = config.selected;
  if (config.checked !== undefined) state.checked = config.checked;
  if (config.busy !== undefined) state.busy = config.busy;
  if (config.expanded !== undefined) state.expanded = config.expanded;

  if (Object.keys(state).length > 0) {
    props.accessibilityState = state;
  }

  if (config.language) {
    props.accessibilityLanguage = config.language;
  }

  return props;
}

export function generateTestID(label: string): string {
  return label
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

/** Validates touch target size against WCAG 2.1 minimums (default 44x44). */
export function validateTouchTarget(
  width: number,
  height: number,
  options?: { minWidth?: number; minHeight?: number }
): TouchTargetValidation {
  const minWidth = options?.minWidth ?? MIN_TOUCH_TARGET_SIZE;
  const minHeight = options?.minHeight ?? MIN_TOUCH_TARGET_SIZE;

  const isValid = width >= minWidth && height >= minHeight;

  return {
    isValid,
    width,
    height,
    minWidth,
    minHeight,
    error: isValid
      ? undefined
      : `Touch target (${width}x${height}) is smaller than minimum required (${minWidth}x${minHeight})`,
  };
}

/** Relative luminance of a hex color per the WCAG 2.1 formula. */
export function getLuminance(color: string): number {
  const hex = color.replace('#', '');
  const r = parseInt(hex.length === 3 ? hex[0] + hex[0] : hex.slice(0, 2), 16) / 255;
  const g = parseInt(hex.length === 3 ? hex[1] + hex[1] : hex.slice(2, 4), 16) / 255;
  const b = parseInt(hex.length === 3 ? hex[2] + hex[2] : hex.slice(4, 6), 16) / 255;

  const [R, G, B] = [r, g, b].map((c) => {
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  });

  return 0.2126 * R + 0.7152 * G + 0.0722 * B;
}

export function getContrastRatio(foreground: string, background: string): number {
  const l1 = getLuminance(foreground);
  const l2 = getLuminance(background);
  const lighter = Math.max(l1, l2);
  const darker = Math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

/** Validates color contrast against WCAG 2.1 AA/AAA thresholds. */
export function validateContrast(
  foreground: string,
  background: string,
  isLargeText: boolean = false
): ContrastValidation {
  const ratio = getContrastRatio(foreground, background);
  const requiredRatio = isLargeText ? CONTRAST_RATIOS.AA_LARGE : CONTRAST_RATIOS.AA_NORMAL;

  let level: 'AA' | 'AAA' | 'fail' = 'fail';
  if (ratio >= (isLargeText ? CONTRAST_RATIOS.AAA_LARGE : CONTRAST_RATIOS.AAA_NORMAL)) {
    level = 'AAA';
  } else if (ratio >= requiredRatio) {
    level = 'AA';
  }

  return {
    isValid: ratio >= requiredRatio,
    ratio: Math.round(ratio * 100) / 100,
    requiredRatio,
    level,
    foreground,
    background,
  };
}

/** Formats an amount as spoken Indonesian or English currency for screen readers. */
export function formatCurrencyForA11y(
  amount: number,
  currency: string = 'IDR',
  language: 'id' | 'en' = 'id'
): string {
  const absAmount = Math.abs(amount);
  const isNegative = amount < 0;

  let text = '';

  if (language === 'id') {
    if (absAmount >= 1000000000) {
      const billions = Math.floor(absAmount / 1000000000);
      const remainder = absAmount % 1000000000;
      text += `${billions} miliar`;
      if (remainder > 0) text += ` ${formatCurrencyForA11y(remainder, currency, language)}`;
    } else if (absAmount >= 1000000) {
      const millions = Math.floor(absAmount / 1000000);
      const remainder = absAmount % 1000000;
      text += `${millions} juta`;
      if (remainder > 0) text += ` ${formatCurrencyForA11y(remainder, currency, language)}`;
    } else if (absAmount >= 1000) {
      const thousands = Math.floor(absAmount / 1000);
      const remainder = absAmount % 1000;
      text += `${thousands} ribu`;
      if (remainder > 0) text += ` ${remainder}`;
    } else {
      text += `${absAmount}`;
    }
    text += ' rupiah';
    if (isNegative) text = `minus ${text}`;
  } else {
    if (absAmount >= 1000000000) {
      text += `${(absAmount / 1000000000).toFixed(2)} billion`;
    } else if (absAmount >= 1000000) {
      text += `${(absAmount / 1000000).toFixed(2)} million`;
    } else if (absAmount >= 1000) {
      text += `${(absAmount / 1000).toFixed(2)} thousand`;
    } else {
      text += `${absAmount}`;
    }
    text += ` ${currency.toLowerCase()}`;
    if (isNegative) text = `negative ${text}`;
  }

  return text;
}

export function createInputA11yProps(
  label: string,
  options?: {
    required?: boolean;
    error?: string;
    hint?: string;
    testID?: string;
  }
): PayUAccessibilityProps {
  const { required, error, hint, testID } = options || {};

  let accessibilityLabel = label;
  if (required) {
    accessibilityLabel += ', required';
  }
  if (error) {
    accessibilityLabel += `, error: ${error}`;
  }

  return generateA11yProps({
    label: accessibilityLabel,
    hint: hint || `Enter ${label}`,
    role: 'text',
    testID: testID || generateTestID(`${label}-input`),
  });
}

export function createAnnouncement(
  message: string,
  priority: 'polite' | 'assertive' = 'polite'
): { message: string; priority: 'polite' | 'assertive' } {
  return { message, priority };
}

export function validateA11yLabels(
  elements: { label?: string; testID?: string; role?: string }[]
): { isValid: boolean; errors: string[] } {
  const errors: string[] = [];

  elements.forEach((element, index) => {
    if (!element.label || element.label.trim() === '') {
      errors.push(`Element ${index + 1}${element.testID ? ` (${element.testID})` : ''} is missing an accessibility label`);
    }
    if (element.role === 'button' && (!element.label || element.label.length < 3)) {
      errors.push(`Button ${element.testID || index + 1} has an insufficient label`);
    }
  });

  return {
    isValid: errors.length === 0,
    errors,
  };
}


export default {
  generateA11yProps,
  generateTestID,
  validateTouchTarget,
  getLuminance,
  getContrastRatio,
  validateContrast,
  formatCurrencyForA11y,
  createInputA11yProps,
  createAnnouncement,
  validateA11yLabels,
  MIN_TOUCH_TARGET_SIZE,
  CONTRAST_RATIOS,
  A11Y_ROLES,
};
