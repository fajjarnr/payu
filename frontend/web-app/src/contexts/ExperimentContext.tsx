'use client';

import React, { createContext, useContext, ReactNode } from 'react';

export interface ExperimentState {
  // Map of experiment key to variant key
  variants: Record<string, string>;
  // Loading state per experiment
  loading: Record<string, boolean>;
  // Error state per experiment
  errors: Record<string, string | null>;
}

export interface ExperimentContextValue extends ExperimentState {
  // Set variant for an experiment
  setVariant: (experimentKey: string, variantKey: string) => void;
  // Set loading state for an experiment
  setLoading: (experimentKey: string, isLoading: boolean) => void;
  // Set error for an experiment
  setError: (experimentKey: string, error: string | null) => void;
  // Get variant for an experiment
  getVariant: (experimentKey: string) => string | null;
  // Check if experiment is loading
  isLoading: (experimentKey: string) => boolean;
}

const ExperimentContext = createContext<ExperimentContextValue | undefined>(
  undefined
);

export interface ExperimentProviderProps {
  children: ReactNode;
  initialState?: Partial<ExperimentState>;
}

export function ExperimentProvider({
  children,
  initialState = {},
}: ExperimentProviderProps) {
  const [state, setState] = React.useState<ExperimentState>({
    variants: {},
    loading: {},
    errors: {},
    ...initialState,
  });

  const setVariant = (experimentKey: string, variantKey: string) => {
    setState((prev) => ({
      ...prev,
      variants: {
        ...prev.variants,
        [experimentKey]: variantKey,
      },
    }));
  };

  const setLoading = (experimentKey: string, isLoading: boolean) => {
    setState((prev) => ({
      ...prev,
      loading: {
        ...prev.loading,
        [experimentKey]: isLoading,
      },
    }));
  };

  const setError = (experimentKey: string, error: string | null) => {
    setState((prev) => ({
      ...prev,
      errors: {
        ...prev.errors,
        [experimentKey]: error,
      },
    }));
  };

  const getVariant = (experimentKey: string): string | null => {
    return state.variants[experimentKey] || null;
  };

  const isLoading = (experimentKey: string): boolean => {
    return state.loading[experimentKey] || false;
  };

  const value: ExperimentContextValue = {
    ...state,
    setVariant,
    setLoading,
    setError,
    getVariant,
    isLoading,
  };

  return (
    <ExperimentContext.Provider value={value}>
      {children}
    </ExperimentContext.Provider>
  );
}

export function useExperimentContext(): ExperimentContextValue {
  const context = useContext(ExperimentContext);
  if (context === undefined) {
    throw new Error(
      'useExperimentContext must be used within an ExperimentProvider'
    );
  }
  return context;
}

export default ExperimentContext;
