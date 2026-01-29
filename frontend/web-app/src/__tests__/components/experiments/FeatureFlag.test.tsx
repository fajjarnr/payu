import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi } from 'vitest';
import { FeatureFlag } from '@/components/experiments/FeatureFlag';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock useExperiment hook
const mockUseExperiment = vi.fn();

vi.mock('@/hooks/useExperiment', () => ({
  useExperiment: (experimentKey: string, options?: any) => mockUseExperiment(experimentKey, options),
}));

expect.extend(toHaveNoViolations);

describe('FeatureFlag', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render children when feature is enabled', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag experimentKey="new_feature" enabledVariant="enabled">
        <div>New Feature Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('New Feature Content')).toBeInTheDocument();
  });

  it('should render fallback when feature is disabled', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="new_feature"
        enabledVariant="enabled"
        fallback={<div>Old Feature Content</div>}
      >
        <div>New Feature Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Old Feature Content')).toBeInTheDocument();
    expect(screen.queryByText('New Feature Content')).not.toBeInTheDocument();
  });

  it('should render fallback when loading', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: true,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="new_feature"
        enabledVariant="enabled"
        fallback={<div>Loading...</div>}
      >
        <div>New Feature Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('should render nothing when disabled and no fallback provided', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
    });

    const { container } = renderWithIntl(
      <FeatureFlag experimentKey="new_feature" enabledVariant="enabled">
        <div>New Feature Content</div>
      </FeatureFlag>
    );

    expect(container.firstChild).toBeNull();
  });

  it('should call useExperiment with correct experiment key', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag experimentKey="my_experiment" enabledVariant="enabled">
        <div>Content</div>
      </FeatureFlag>
    );

    expect(mockUseExperiment).toHaveBeenCalledWith('my_experiment', { trackImpression: true });
  });

  it('should pass trackImpression option to useExperiment', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="new_feature"
        enabledVariant="enabled"
        trackImpression={false}
      >
        <div>Content</div>
      </FeatureFlag>
    );

    expect(mockUseExperiment).toHaveBeenCalledWith('new_feature', { trackImpression: false });
  });

  it('should call onAccess callback when feature is enabled', () => {
    const onAccess = vi.fn();

    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="new_feature"
        enabledVariant="enabled"
        onAccess={onAccess}
      >
        <div>Content</div>
      </FeatureFlag>
    );

    expect(onAccess).toHaveBeenCalledWith(true, 'enabled');
  });

  it('should call onAccess callback when feature is disabled', () => {
    const onAccess = vi.fn();

    mockUseExperiment.mockReturnValue({
      variantKey: 'control',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="new_feature"
        enabledVariant="enabled"
        onAccess={onAccess}
      >
        <div>Content</div>
      </FeatureFlag>
    );

    expect(onAccess).toHaveBeenCalledWith(false, 'control');
  });

  it('should not call onAccess while loading', () => {
    const onAccess = vi.fn();

    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: true,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="new_feature"
        enabledVariant="enabled"
        onAccess={onAccess}
      >
        <div>Content</div>
      </FeatureFlag>
    );

    expect(onAccess).not.toHaveBeenCalled();
  });

  it('should handle different enabled variants', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'variant_b',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag experimentKey="test" enabledVariant="variant_b">
        <div>Variant B Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Variant B Content')).toBeInTheDocument();
  });

  it('should handle multiple variants', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'variant_c',
      isLoading: false,
    });

    const { container } = renderWithIntl(
      <FeatureFlag
        experimentKey="test"
        enabledVariant="variant_b"
        fallback={<div>Fallback</div>}
      >
        <div>Variant B Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Fallback')).toBeInTheDocument();
    expect(screen.queryByText('Variant B Content')).not.toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    const { container } = renderWithIntl(
      <FeatureFlag experimentKey="test" enabledVariant="enabled">
        <div>Accessible Content</div>
      </FeatureFlag>
    );

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should render complex children components', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    const ComplexComponent = () => (
      <div>
        <h1>Title</h1>
        <p>Description</p>
        <button>Click</button>
      </div>
    );

    renderWithIntl(
      <FeatureFlag experimentKey="test" enabledVariant="enabled">
        <ComplexComponent />
      </FeatureFlag>
    );

    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Description')).toBeInTheDocument();
    expect(screen.getByText('Click')).toBeInTheDocument();
  });

  it('should handle null variant key', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: false,
    });

    const { container } = renderWithIntl(
      <FeatureFlag
        experimentKey="test"
        enabledVariant="enabled"
        fallback={<div>Fallback</div>}
      >
        <div>Enabled Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Fallback')).toBeInTheDocument();
  });

  it('should handle undefined variant key', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: undefined,
      isLoading: false,
    });

    const { container } = renderWithIntl(
      <FeatureFlag
        experimentKey="test"
        enabledVariant="enabled"
        fallback={<div>Fallback</div>}
      >
        <div>Enabled Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Fallback')).toBeInTheDocument();
  });

  it('should render fallback during loading when trackImpression is false', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: null,
      isLoading: true,
    });

    renderWithIntl(
      <FeatureFlag
        experimentKey="test"
        enabledVariant="enabled"
        trackImpression={false}
        fallback={<div>Loading...</div>}
      >
        <div>Content</div>
      </FeatureFlag>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('should work with fragment children', () => {
    mockUseExperiment.mockReturnValue({
      variantKey: 'enabled',
      isLoading: false,
    });

    renderWithIntl(
      <FeatureFlag experimentKey="test" enabledVariant="enabled">
        <>
          <div>First</div>
          <div>Second</div>
        </>
      </FeatureFlag>
    );

    expect(screen.getByText('First')).toBeInTheDocument();
    expect(screen.getByText('Second')).toBeInTheDocument();
  });
});
