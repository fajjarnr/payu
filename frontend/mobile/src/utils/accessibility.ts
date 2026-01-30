/**
 * Accessibility Utilities for PayU Mobile App
 *
 * Provides helper functions for generating accessibility props,
 * checking compliance, and managing screen reader interactions.
 *
 * @module accessibility
 * @version 1.0.0
 */

import { AccessibilityProps, AccessibilityRole, AccessibilityState } from 'react-native';

// ============================================================================
// Types & Interfaces
// ============================================================================

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

/**
 * Configuration for generating accessibility props
 */
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

/**
 * Touch target validation result
 */
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

/**
 * Color contrast validation result
 */
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

// ============================================================================
// Constants
// ============================================================================

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

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Generates comprehensive accessibility props for a component
 *
 * @param config - Accessibility configuration object
 * @returns Complete accessibility props for React Native components
 *
 * @example
 * ```tsx
 * const props = generateA11yProps({
 *   label: 'Transfer button',
 *   hint: 'Opens transfer screen',
 *   role: 'button',
 *   testID: 'transfer-btn'
 * });
 * ```
 */
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

/**
 * Generates a testID from a label string
 *
 * @param label - The accessibility label
 * @returns A kebab-case testID
 *
 * @example
 * ```ts
 * generateTestID('Transfer Button'); // 'transfer-button'
 * ```
 */
export function generateTestID(label: string): string {
  return label
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

/**
 * Validates touch target size meets WCAG 2.1 minimum requirements
 *
 * @param width - Element width in pixels
 * @param height - Element height in pixels
 * @param options - Optional validation parameters
 * @returns Validation result with details
 *
 * @example
 * ```ts
 * const result = validateTouchTarget(48, 48);
 * if (!result.isValid) {
 *   console.warn(result.error);
 * }
 * ```
 */
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

/**
 * Calculates relative luminance of a color (WCAG 2.1 formula)
 *
 * @param color - Hex color string (e.g., '#10b981' or '#fff')
 * @returns Relative luminance value (0-1)
 */
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

/**
 * Calculates contrast ratio between two colors
 *
 * @param foreground - Foreground color (hex)
 * @param background - Background color (hex)
 * @returns Contrast ratio (1-21)
 *
 * @example
 * ```ts
 * const ratio = getContrastRatio('#ffffff', '#000000'); // 21
 * ```
 */
export function getContrastRatio(foreground: string, background: string): number {
  const l1 = getLuminance(foreground);
  const l2 = getLuminance(background);
  const lighter = Math.max(l1, l2);
  const darker = Math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * Validates color contrast meets WCAG 2.1 standards
 *
 * @param foreground - Foreground color (hex)
 * @param background - Background color (hex)
 * @param isLargeText - Whether the text is large (18pt+ or 14pt+ bold)
 * @returns Contrast validation result
 *
 * @example
 * ```ts
 * const result = validateContrast('#10b981', '#ffffff', false);
 * console.log(result.level); // 'AA' | 'AAA' | 'fail'
 * ```
 */
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

/**
 * Formats currency amount for screen reader announcement
 *
 * @param amount - Numeric amount
 * @param currency - Currency code (default: 'IDR')
 * @returns Formatted string for screen reader
 *
 * @example
 * ```ts
 * formatCurrencyForA11y(1500000); // '1 juta 500 ribu rupiah'
 * formatCurrencyForA11y(1500000, 'IDR', 'en'); // '1 million 500 thousand rupiah'
 * ```
 */
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
    // English
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

/**
 * Creates accessibility props for a form input
 *
 * @param label - Input label
 * @param options - Additional options
 * @returns Accessibility props for input component
 */
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

/**
 * Creates accessibility announcement for dynamic content changes
 *
 * @param message - Message to announce
 * @param priority - Announcement priority
 * @returns Formatted announcement object
 */
export function createAnnouncement(
  message: string,
  priority: 'polite' | 'assertive' = 'polite'
): { message: string; priority: 'polite' | 'assertive' } {
  return { message, priority };
}

/**
 * Validates that all interactive elements have proper accessibility labels
 *
 * @param elements - Array of elements to validate
 * @returns Validation results
 */
export function validateA11yLabels(
  elements: Array<{ label?: string; testID?: string; role?: string }>
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

// ============================================================================
// Export Default
// ============================================================================

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
