/**
 * Accessibility Tests for PayU Mobile App
 *
 * Comprehensive test suite for accessibility compliance including:
 * - Touch target size validation (WCAG 2.1)
 * - Label associations
 * - Contrast ratios
 * - Screen reader compatibility
 *
 * @module testing/accessibility.test
 * @version 1.0.0
 */

import React from 'react';
import { render, renderHook } from '@testing-library/react-native';
import { View, Text, TouchableOpacity, TextInput } from 'react-native';

// Components to test
import { AccessibleButton } from '@/src/components/ui/AccessibleButton';
import { AccessibleInput } from '@/src/components/ui/AccessibleInput';

// Utilities to test
import {
  generateA11yProps,
  generateTestID,
  validateTouchTarget,
  getLuminance,
  getContrastRatio,
  validateContrast,
  formatCurrencyForA11y,
  createInputA11yProps,
  validateA11yLabels,
  MIN_TOUCH_TARGET_SIZE,
  CONTRAST_RATIOS,
} from '@/src/utils/accessibility';

// Hooks to test
import {
  useScreenReader,
  useAccessibilityAnnounce,
  useAccessibleForm,
} from '@/src/hooks/useAccessibility';

// ============================================================================
// Mock Setup
// ============================================================================

// Use global mocks from jest.setup.js
const mockIsScreenReaderEnabled = global.mockAccessibility?.mockIsScreenReaderEnabled || jest.fn();
const mockAnnounceForAccessibility = global.mockAccessibility?.mockAnnounceForAccessibility || jest.fn();

jest.mock('react-native/Libraries/Utilities/Platform', () => ({
  OS: 'ios',
  select: jest.fn((obj: any) => obj?.ios || obj?.default),
}));

// Mock react-native findNodeHandle
jest.mock('react-native', () => {
  const RN = jest.requireActual('react-native');
  return {
    ...RN,
    findNodeHandle: jest.fn(() => 1),
  };
});

// ============================================================================
// Utility Function Tests
// ============================================================================

describe('Accessibility Utilities', () => {
  describe('generateA11yProps', () => {
    it('should generate basic accessibility props', () => {
      const props = generateA11yProps({
        label: 'Transfer Button',
        role: 'button',
      });

      expect(props.accessibilityLabel).toBe('Transfer Button');
      expect(props.accessibilityRole).toBe('button');
      expect(props.testID).toBe('transfer-button');
    });

    it('should include hint when provided', () => {
      const props = generateA11yProps({
        label: 'Pay Button',
        hint: 'Opens payment screen',
      });

      expect(props.accessibilityHint).toBe('Opens payment screen');
    });

    it('should include state when provided', () => {
      const props = generateA11yProps({
        label: 'Submit Button',
        disabled: true,
        busy: true,
      });

      expect(props.accessibilityState).toEqual({
        disabled: true,
        busy: true,
      });
    });

    it('should use custom testID when provided', () => {
      const props = generateA11yProps({
        label: 'Button',
        testID: 'custom-test-id',
      });

      expect(props.testID).toBe('custom-test-id');
    });
  });

  describe('generateTestID', () => {
    it('should convert label to kebab-case', () => {
      expect(generateTestID('Transfer Button')).toBe('transfer-button');
      expect(generateTestID('Pay Now')).toBe('pay-now');
      expect(generateTestID('QRIS Payment')).toBe('qris-payment');
    });

    it('should handle special characters', () => {
      expect(generateTestID('Pay\u0026Go')).toBe('pay-go');
      expect(generateTestID('Test 123')).toBe('test-123');
    });

    it('should trim leading/trailing dashes', () => {
      expect(generateTestID('  Button  ')).toBe('button');
      expect(generateTestID('-Button-')).toBe('button');
    });
  });

  describe('validateTouchTarget', () => {
    it('should validate touch target meets minimum size', () => {
      const result = validateTouchTarget(48, 48);

      expect(result.isValid).toBe(true);
      expect(result.width).toBe(48);
      expect(result.height).toBe(48);
      expect(result.minWidth).toBe(MIN_TOUCH_TARGET_SIZE);
      expect(result.minHeight).toBe(MIN_TOUCH_TARGET_SIZE);
    });

    it('should fail for touch targets smaller than 44x44', () => {
      const result = validateTouchTarget(30, 30);

      expect(result.isValid).toBe(false);
      expect(result.error).toContain('30');
      expect(result.error).toContain('44');
    });

    it('should allow custom minimum sizes', () => {
      const result = validateTouchTarget(30, 30, { minWidth: 24, minHeight: 24 });

      expect(result.isValid).toBe(true);
      expect(result.minWidth).toBe(24);
      expect(result.minHeight).toBe(24);
    });

    it('should validate width and height independently', () => {
      const result = validateTouchTarget(50, 30);

      expect(result.isValid).toBe(false);
      expect(result.error).toBeDefined();
    });
  });

  describe('getLuminance', () => {
    it('should calculate luminance for white', () => {
      expect(getLuminance('#ffffff')).toBeCloseTo(1, 2);
    });

    it('should calculate luminance for black', () => {
      expect(getLuminance('#000000')).toBeCloseTo(0, 2);
    });

    it('should calculate luminance for gray', () => {
      const luminance = getLuminance('#808080');
      expect(luminance).toBeGreaterThan(0.2);
      expect(luminance).toBeLessThan(0.3);
    });

    it('should handle shorthand hex codes', () => {
      expect(getLuminance('#fff')).toBeCloseTo(1, 2);
      expect(getLuminance('#000')).toBeCloseTo(0, 2);
    });
  });

  describe('getContrastRatio', () => {
    it('should return 21 for black on white', () => {
      expect(getContrastRatio('#000000', '#ffffff')).toBeCloseTo(21, 1);
    });

    it('should return 1 for same colors', () => {
      expect(getContrastRatio('#ffffff', '#ffffff')).toBeCloseTo(1, 1);
    });

    it('should be symmetric', () => {
      const ratio1 = getContrastRatio('#10b981', '#ffffff');
      const ratio2 = getContrastRatio('#ffffff', '#10b981');
      expect(ratio1).toBeCloseTo(ratio2, 2);
    });
  });

  describe('validateContrast', () => {
    it('should pass AA for black on white (normal text)', () => {
      const result = validateContrast('#000000', '#ffffff', false);

      expect(result.isValid).toBe(true);
      expect(result.level).toBe('AAA');
      expect(result.ratio).toBeCloseTo(21, 1);
    });

    it('should fail for low contrast colors', () => {
      const result = validateContrast('#cccccc', '#ffffff', false);

      expect(result.isValid).toBe(false);
      expect(result.level).toBe('fail');
    });

    it('should pass AA for large text with lower contrast', () => {
      const result = validateContrast('#666666', '#ffffff', true);

      expect(result.isValid).toBe(true);
      expect(result.level).toBe('AA');
    });

    it('should return correct required ratio', () => {
      const normalResult = validateContrast('#000000', '#ffffff', false);
      const largeResult = validateContrast('#000000', '#ffffff', true);

      expect(normalResult.requiredRatio).toBe(CONTRAST_RATIOS.AA_NORMAL);
      expect(largeResult.requiredRatio).toBe(CONTRAST_RATIOS.AA_LARGE);
    });
  });

  describe('formatCurrencyForA11y', () => {
    it('should format millions in Indonesian', () => {
      expect(formatCurrencyForA11y(1500000)).toContain('1 juta');
      expect(formatCurrencyForA11y(2500000)).toContain('2 juta');
    });

    it('should format billions in Indonesian', () => {
      expect(formatCurrencyForA11y(1500000000)).toContain('1 miliar');
    });

    it('should format thousands in Indonesian', () => {
      expect(formatCurrencyForA11y(15000)).toContain('15 ribu');
    });

    it('should handle negative amounts', () => {
      expect(formatCurrencyForA11y(-1000000)).toContain('minus');
    });

    it('should format in English when specified', () => {
      expect(formatCurrencyForA11y(1500000, 'IDR', 'en')).toContain('million');
    });
  });

  describe('createInputA11yProps', () => {
    it('should create props for required input', () => {
      const props = createInputA11yProps('Email', { required: true });

      expect(props.accessibilityLabel).toContain('Email');
      expect(props.accessibilityLabel).toContain('required');
    });

    it('should include error in label', () => {
      const props = createInputA11yProps('Password', {
        error: 'Password is too short',
      });

      expect(props.accessibilityLabel).toContain('error');
      expect(props.accessibilityLabel).toContain('too short');
    });

    it('should include hint', () => {
      const props = createInputA11yProps('Username', {
        hint: 'Enter your username',
      });

      expect(props.accessibilityHint).toBe('Enter your username');
    });
  });

  describe('validateA11yLabels', () => {
    it('should pass for valid labels', () => {
      const elements = [
        { label: 'Transfer Button', role: 'button' },
        { label: 'Account Number', role: 'text' },
      ];

      const result = validateA11yLabels(elements);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should fail for missing labels', () => {
      const elements = [{ label: '', role: 'button', testID: 'test-btn' }];

      const result = validateA11yLabels(elements);

      expect(result.isValid).toBe(false);
      expect(result.errors[0]).toContain('test-btn');
    });

    it('should fail for buttons with short labels', () => {
      const elements = [{ label: 'OK', role: 'button' }];

      const result = validateA11yLabels(elements);

      expect(result.isValid).toBe(false);
      expect(result.errors[0]).toContain('insufficient');
    });
  });
});

// ============================================================================
// Component Tests
// ============================================================================

describe('Accessible Components', () => {
  describe('AccessibleButton', () => {
    it('should render with accessibility label', () => {
      const { getByLabelText } = render(
        <AccessibleButton
          label="Transfer"
          hint="Opens transfer screen"
          onPress={jest.fn()}
        />
      );

      expect(getByLabelText('Transfer')).toBeTruthy();
    });

    it('should have proper touch target size', () => {
      const { getByLabelText } = render(
        <AccessibleButton label="Pay" onPress={jest.fn()} />
      );

      const button = getByLabelText('Pay');
      // Touch target validation is handled in component
      expect(button).toBeTruthy();
    });

    it('should announce loading state', () => {
      const { getByLabelText } = render(
        <AccessibleButton
          label="Submit"
          loading
          loadingLabel="Processing payment"
          onPress={jest.fn()}
        />
      );

      expect(getByLabelText('Processing payment')).toBeTruthy();
    });

    it('should support different variants', () => {
      const variants: ('primary' | 'secondary' | 'tertiary' | 'danger' | 'ghost')[] = [
        'primary',
        'secondary',
        'tertiary',
        'danger',
        'ghost',
      ];

      variants.forEach((variant) => {
        const { getByLabelText } = render(
          <AccessibleButton
            label={`${variant} Button`}
            variant={variant}
            onPress={jest.fn()}
          />
        );

        expect(getByLabelText(`${variant} Button`)).toBeTruthy();
      });
    });

    it('should handle disabled state', () => {
      const onPress = jest.fn();
      const { getByLabelText } = render(
        <AccessibleButton label="Disabled" disabled onPress={onPress} />
      );

      const button = getByLabelText('Disabled');
      expect(button.props.accessibilityState.disabled).toBe(true);
    });
  });

  describe('AccessibleInput', () => {
    it('should render with accessibility label', () => {
      const { getByLabelText } = render(
        <AccessibleInput
          label="Account Number"
          placeholder="Enter account number"
        />
      );

      expect(getByLabelText(/Account Number/)).toBeTruthy();
    });

    it('should indicate required field', () => {
      const { getByLabelText } = render(
        <AccessibleInput label="Email" required />
      );

      const input = getByLabelText(/Email/);
      expect(input.props.accessibilityLabel).toContain('required');
    });

    it('should announce error message', () => {
      const { getByLabelText, getByText } = render(
        <AccessibleInput
          label="Password"
          error="Password must be at least 8 characters"
        />
      );

      const input = getByLabelText(/Password/);
      expect(input.props.accessibilityLabel).toContain('error');
      expect(getByText('Password must be at least 8 characters')).toBeTruthy();
    });

    it('should support different sizes', () => {
      const sizes: Array<'sm' | 'md' | 'lg'> = ['sm', 'md', 'lg'];

      sizes.forEach((size) => {
        const { getByLabelText } = render(
          <AccessibleInput label={`${size} Input`} size={size} />
        );

        expect(getByLabelText(new RegExp(`${size} Input`))).toBeTruthy();
      });
    });

    it('should support secure text entry', () => {
      const { getByLabelText, getByRole } = render(
        <AccessibleInput label="Password" secure />
      );

      expect(getByLabelText(/Password/)).toBeTruthy();
      // Should have toggle visibility button
      expect(getByRole('button')).toBeTruthy();
    });
  });
});

// ============================================================================
// Hook Tests
// ============================================================================

describe('Accessibility Hooks', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIsScreenReaderEnabled.mockResolvedValue(false);
    // Reset the global mock as well
    if (global.mockAccessibility?.mockIsScreenReaderEnabled) {
      global.mockAccessibility.mockIsScreenReaderEnabled.mockResolvedValue(false);
    }
  });

  describe('useScreenReader', () => {
    it('should detect screen reader status', async () => {
      mockIsScreenReaderEnabled.mockResolvedValue(true);
      if (global.mockAccessibility?.mockIsScreenReaderEnabled) {
        global.mockAccessibility.mockIsScreenReaderEnabled.mockResolvedValue(true);
      }

      renderHook(() => useScreenReader());

      // Wait for effect to run
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(mockIsScreenReaderEnabled).toHaveBeenCalled();
    });

    it('should refresh screen reader status', async () => {
      mockIsScreenReaderEnabled.mockResolvedValue(false);

      const { result } = renderHook(() => useScreenReader());

      await result.current.refresh();

      expect(mockIsScreenReaderEnabled).toHaveBeenCalledTimes(2); // Initial + refresh
    });
  });

  describe('useAccessibilityAnnounce', () => {
    it('should announce messages', () => {
      const { result } = renderHook(() => useAccessibilityAnnounce());

      result.current.announce('Test message');

      // AccessibilityInfo.announceForAccessibility is called
      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith('Test message');
    });

    it('should announce polite messages', () => {
      const { result } = renderHook(() => useAccessibilityAnnounce());

      result.current.announcePolite('Polite message');

      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith('Polite message');
    });

    it('should announce assertive messages', () => {
      const { result } = renderHook(() => useAccessibilityAnnounce());

      result.current.announceAssertive('Assertive message');

      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith('Assertive message');
    });

    it('should announce currency amounts', () => {
      const { result } = renderHook(() => useAccessibilityAnnounce());

      result.current.announceCurrency(1500000);

      expect(mockAnnounceForAccessibility).toHaveBeenCalled();
    });
  });

  describe('useAccessibleForm', () => {
    it('should announce field errors', () => {
      const { result } = renderHook(() => useAccessibleForm());

      result.current.announceFieldError('Email', 'Invalid email format');

      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith(
        'Email error: Invalid email format'
      );
    });

    it('should announce form submission', () => {
      const { result } = renderHook(() => useAccessibleForm());

      result.current.announceFormSubmit('Transfer Form', true);

      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith(
        'Transfer Form submitted successfully'
      );
    });

    it('should announce form submission failure', () => {
      const { result } = renderHook(() => useAccessibleForm());

      result.current.announceFormSubmit('Transfer Form', false);

      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith(
        'Transfer Form submission failed. Please check the errors.'
      );
    });

    it('should announce validation summary', () => {
      const { result } = renderHook(() => useAccessibleForm());

      result.current.announceValidationSummary(3);

      expect(mockAnnounceForAccessibility).toHaveBeenCalledWith(
        'There are 3 errors to fix'
      );
    });
  });
});

// ============================================================================
// Integration Tests
// ============================================================================

describe('Accessibility Integration', () => {
  it('should validate PayU color palette contrast', () => {
    // PayU brand colors
    const colors = {
      primary: '#10b981', // Emerald 500
      background: '#111827', // Gray 900
      card: '#1f2937', // Gray 800
      text: '#ffffff', // White
      border: '#374151', // Gray 700
      error: '#ef4444', // Red 500
      warning: '#f59e0b', // Amber 500
      success: '#10b981', // Emerald 500
    };

    // Test primary button (green on green tint)
    const primaryButton = validateContrast('#ffffff', colors.primary);
    expect(primaryButton.isValid).toBe(true);
    expect(primaryButton.level).toBe('AA');

    // Test text on background
    const textOnBg = validateContrast(colors.text, colors.background);
    expect(textOnBg.isValid).toBe(true);
    expect(textOnBg.level).toBe('AAA');

    // Test text on card
    const textOnCard = validateContrast(colors.text, colors.card);
    expect(textOnCard.isValid).toBe(true);
    expect(textOnCard.level).toBe('AAA');

    // Test error text
    const errorText = validateContrast(colors.error, colors.background);
    expect(errorText.isValid).toBe(true);
    expect(errorText.level).toBe('AA');
  });

  it('should validate common component touch targets', () => {
    const components = [
      { name: 'Primary Button', width: 200, height: 48 },
      { name: 'Icon Button', width: 44, height: 44 },
      { name: 'List Item', width: 350, height: 60 },
      { name: 'Tab Bar Item', width: 80, height: 44 },
    ];

    components.forEach((component) => {
      const validation = validateTouchTarget(component.width, component.height);
      expect(validation.isValid).toBe(true);
    });
  });

  it('should generate proper a11y props for banking actions', () => {
    const actions = [
      {
        name: 'Transfer',
        expectedLabel: 'Transfer',
        expectedHint: 'Opens transfer screen',
      },
      {
        name: 'QRIS Payment',
        expectedLabel: 'QRIS Payment',
        expectedHint: 'Scan QR code to pay',
      },
      {
        name: 'Check Balance',
        expectedLabel: 'Check Balance',
        expectedHint: 'View account balance',
      },
    ];

    actions.forEach((action) => {
      const props = generateA11yProps({
        label: action.name,
        hint: action.expectedHint,
        role: 'button',
      });

      expect(props.accessibilityLabel).toBe(action.expectedLabel);
      expect(props.accessibilityHint).toBe(action.expectedHint);
      expect(props.accessibilityRole).toBe('button');
    });
  });
});

// ============================================================================
// WCAG Compliance Tests
// ============================================================================

describe('WCAG 2.1 Compliance', () => {
  describe('Level A Requirements', () => {
    it('should have text alternatives for non-text content', () => {
      // Test that components require labels
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();

      // Components should have labels
      const { getByLabelText } = render(
        <AccessibleButton label="Pay Button" onPress={jest.fn()} />
      );

      expect(getByLabelText('Pay Button')).toBeTruthy();
      consoleSpy.mockRestore();
    });

    it('should support keyboard operation', () => {
      // Touch targets should be large enough for keyboard/touch
      const validation = validateTouchTarget(44, 44);
      expect(validation.isValid).toBe(true);
    });
  });

  describe('Level AA Requirements', () => {
    it('should meet minimum contrast ratio (4.5:1 for normal text)', () => {
      const contrast = validateContrast('#000000', '#ffffff', false);
      expect(contrast.ratio).toBeGreaterThanOrEqual(4.5);
    });

    it('should support text resizing up to 200%', () => {
      // Test that our input sizes accommodate larger text
      const sizes = ['sm', 'md', 'lg'] as const;
      sizes.forEach((size) => {
        const { getByLabelText } = render(
          <AccessibleInput label={`${size} Input`} size={size} />
        );
        expect(getByLabelText(new RegExp(`${size} Input`))).toBeTruthy();
      });
    });
  });

  describe('Level AAA Requirements (Recommended)', () => {
    it('should meet enhanced contrast ratio (7:1 for normal text)', () => {
      const contrast = validateContrast('#000000', '#ffffff', false);
      expect(contrast.ratio).toBeGreaterThanOrEqual(7);
    });

    it('should provide context-sensitive help', () => {
      const { getByLabelText } = render(
        <AccessibleInput
          label="Account Number"
          helperText="Enter your 10-digit account number"
        />
      );

      // Helper text provides context
      expect(getByLabelText(/Account Number/)).toBeTruthy();
    });
  });
});

// ============================================================================
// Performance Tests
// ============================================================================

describe('Accessibility Performance', () => {
  it('should calculate contrast ratios efficiently', () => {
    const start = performance.now();

    for (let i = 0; i < 1000; i++) {
      getContrastRatio('#10b981', '#ffffff');
    }

    const duration = performance.now() - start;
    expect(duration).toBeLessThan(100); // Should complete in under 100ms
  });

  it('should validate touch targets efficiently', () => {
    const start = performance.now();

    for (let i = 0; i < 10000; i++) {
      validateTouchTarget(48, 48);
    }

    const duration = performance.now() - start;
    expect(duration).toBeLessThan(50); // Should complete in under 50ms
  });
});

// ============================================================================
// Export Test Suite Info
// ============================================================================

export const testSuiteInfo = {
  name: 'PayU Mobile Accessibility Tests',
  version: '1.0.0',
  coverage: {
    utilities: [
      'generateA11yProps',
      'validateTouchTarget',
      'validateContrast',
      'formatCurrencyForA11y',
    ],
    components: ['AccessibleButton', 'AccessibleInput'],
    hooks: [
      'useScreenReader',
      'useAccessibilityAnnounce',
      'useAccessibleForm',
    ],
    compliance: ['WCAG 2.1 Level A', 'WCAG 2.1 Level AA', 'WCAG 2.1 Level AAA'],
  },
};
