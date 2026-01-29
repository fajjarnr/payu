import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import { ExperimentVariant } from '@/components/experiments/ExperimentVariant';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock useExperiment hook
const mockUseExperiment = vi.fn();

vi.mock('@/hooks/useExperiment', () => ({
  useExperiment: (experimentKey: string, options?: any) => mockUseExperiment(experimentKey, options),
}));

expect.extend(toHaveNoViolations);

describe('ExperimentVariant', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render control variant by default', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="checkout_flow">
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Control Version')).toBeInTheDocument();
    expect(screen.queryByText('Variant A')).not.toBeInTheDocument();
  });

  it('should render variant_a when assigned', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'variant_a',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="checkout_flow">
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Variant A')).toBeInTheDocument();
    expect(screen.queryByText('Control Version')).not.toBeInTheDocument();
  });

  it('should render variant_b when assigned', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'variant_b',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="checkout_flow">
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
          variant_b: <div>Variant B</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Variant B')).toBeInTheDocument();
  });

  it('should render fallback while loading', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: true,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant
        experimentKey="checkout_flow"
        fallback={<div>Loading experiment...</div>}
      >
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Loading experiment...')).toBeInTheDocument();
  });

  it('should render nothing while loading if no fallback provided', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: true,
      isError: false,
    });

    const { container } = renderWithIntl(
      <ExperimentVariant experimentKey="checkout_flow">
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(container.firstChild).toBeNull();
  });

  it('should render errorFallback when there is an error', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: false,
      isError: true,
    });

    renderWithIntl(
      <ExperimentVariant
        experimentKey="checkout_flow"
        errorFallback={<div>Failed to load experiment</div>}
      >
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Failed to load experiment')).toBeInTheDocument();
  });

  it('should render nothing on error if no errorFallback provided', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: false,
      isError: true,
    });

    const { container } = renderWithIntl(
      <ExperimentVariant experimentKey="checkout_flow">
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(container.firstChild).toBeNull();
  });

  it('should use defaultVariant when variantKey is null', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant
        experimentKey="checkout_flow"
        defaultVariant="variant_a"
      >
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Variant A')).toBeInTheDocument();
  });

  it('should fall back to control if variant not found', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'non_existent_variant',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="checkout_flow">
        {{
          control: <div>Control Version</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Control Version')).toBeInTheDocument();
  });

  it('should fall back to fallback if no control and variant not found', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'non_existent_variant',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant
        experimentKey="checkout_flow"
        fallback={<div>Default Content</div>}
      >
        {{
          variant_a: <div>Variant A</div>,
          variant_b: <div>Variant B</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Default Content')).toBeInTheDocument();
  });

  it('should call useExperiment with correct experiment key', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="my_experiment">
        {{
          control: <div>Content</div>,
        }}
      </ExperimentVariant>
    );

    expect(mockUseExperiment).toHaveBeenCalledWith('my_experiment', undefined);
  });

  it('should pass experimentOptions to useExperiment', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
      isError: false,
    });

    const options = { trackImpression: false };

    renderWithIntl(
      <ExperimentVariant
        experimentKey="my_experiment"
        experimentOptions={options}
      >
        {{
          control: <div>Content</div>,
        }}
      </ExperimentVariant>
    );

    expect(mockUseExperiment).toHaveBeenCalledWith('my_experiment', options);
  });

  it('should handle complex variant components', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'variant_a',
      isLoading: false,
      isError: false,
    });

    const VariantA = () => (
      <div>
        <h1>New Design</h1>
        <button>Click Me</button>
      </div>
    );

    renderWithIntl(
      <ExperimentVariant experimentKey="test">
        {{
          control: <div>Old Design</div>,
          variant_a: <VariantA />,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('New Design')).toBeInTheDocument();
    expect(screen.getByText('Click Me')).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
      isError: false,
    });

    const { container } = renderWithIntl(
      <ExperimentVariant experimentKey="test">
        {{
          control: <div>Accessible Content</div>,
        }}
      </ExperimentVariant>
    );

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should handle undefined variantKey', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: undefined,
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant
        experimentKey="test"
        defaultVariant="variant_a"
      >
        {{
          control: <div>Control</div>,
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Variant A')).toBeInTheDocument();
  });

  it('should render multiple components in a variant', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="test">
        {{
          control: (
            <>
              <div>First Component</div>
              <div>Second Component</div>
              <div>Third Component</div>
            </>
          ),
          variant_a: <div>Variant A</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('First Component')).toBeInTheDocument();
    expect(screen.getByText('Second Component')).toBeInTheDocument();
    expect(screen.getByText('Third Component')).toBeInTheDocument();
  });

  it('should handle empty children object', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
      isError: false,
    });

    const { container } = renderWithIntl(
      <ExperimentVariant
        experimentKey="test"
        fallback={<div>Fallback</div>}
      >
        {{}}
      </ExperimentVariant>
    );

    expect(screen.getByText('Fallback')).toBeInTheDocument();
  });

  it('should prioritize defaultVariant over control when both exist', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant
        experimentKey="test"
        defaultVariant="variant_b"
      >
        {{
          control: <div>Control</div>,
          variant_a: <div>Variant A</div>,
          variant_b: <div>Variant B</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Variant B')).toBeInTheDocument();
  });

  it('should handle variantKey with special characters', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'variant_v2.1',
      isLoading: false,
      isError: false,
    });

    renderWithIntl(
      <ExperimentVariant experimentKey="test">
        {{
          control: <div>Control</div>,
          'variant_v2.1': <div>Variant V2.1</div>,
        }}
      </ExperimentVariant>
    );

    expect(screen.getByText('Variant V2.1')).toBeInTheDocument();
  });
});
