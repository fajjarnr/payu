'use client';

/**
 * Example component demonstrating A/B testing integration
 *
 * This component shows three different ways to use the A/B testing system:
 * 1. Using the useExperiment hook directly
 * 2. Using the ExperimentVariant component
 * 3. Using the FeatureFlag component
 */

import React, { useState } from 'react';
import { useExperiment } from '@/hooks/useExperiment';
import { ExperimentVariant } from '@/components/experiments';
import { FeatureFlag } from '@/components/experiments';

declare global {
  interface Window {
    gtag?: (...args: unknown[]) => void;
  }
}

// =============================================================================
// Example 1: Direct useExperiment hook usage
// =============================================================================

export function CheckoutButtonHook() {
  const { variantKey, isLoading } = useExperiment('checkout_button_color');

  if (isLoading) {
    return <button className="btn-gray animate-pulse">Loading...</button>;
  }

  // Variant A: Blue button (control)
  // Variant B: Green button
  // Variant C: Red button with urgency text
  const buttonColor =
    variantKey === 'variant_b' ? 'bg-green-500' :
    variantKey === 'variant_c' ? 'bg-red-500' :
    'bg-blue-500';

  const buttonText =
    variantKey === 'variant_c' ? 'Checkout Now - Limited Time!' :
    'Checkout';

  return (
    <button className={`${buttonColor} text-white px-6 py-3 rounded-lg`}>
      {buttonText}
    </button>
  );
}

// =============================================================================
// Example 2: ExperimentVariant component usage
// =============================================================================

interface ControlCheckoutFlowProps {
  onComplete: () => void;
}

function ControlCheckoutFlow({ onComplete }: ControlCheckoutFlowProps) {
  return (
    <div className="space-y-4 p-4 border rounded-lg">
      <h2 className="text-xl font-bold">Standard Checkout</h2>
      <p>Complete your purchase in 3 simple steps.</p>
      <button
        onClick={onComplete}
        className="bg-blue-500 text-white px-4 py-2 rounded"
      >
        Continue
      </button>
    </div>
  );
}

function SimplifiedCheckoutFlow({ onComplete }: ControlCheckoutFlowProps) {
  return (
    <div className="space-y-4 p-4 border rounded-lg bg-green-50">
      <h2 className="text-xl font-bold text-green-700">Quick Checkout</h2>
      <p>One-click checkout for faster experience.</p>
      <button
        onClick={onComplete}
        className="bg-green-500 text-white px-4 py-2 rounded"
      >
        Buy Now
      </button>
    </div>
  );
}

function DetailedCheckoutFlow({ onComplete }: ControlCheckoutFlowProps) {
  return (
    <div className="space-y-4 p-4 border rounded-lg">
      <h2 className="text-xl font-bold">Detailed Checkout</h2>
      <p>Review all details before completing your purchase.</p>
      <div className="space-y-2">
        <div>Step 1: Shipping</div>
        <div>Step 2: Payment</div>
        <div>Step 3: Review</div>
      </div>
      <button
        onClick={onComplete}
        className="bg-blue-500 text-white px-4 py-2 rounded"
      >
        Next Step
      </button>
    </div>
  );
}

export function CheckoutPageVariant() {
  const [isComplete, setIsComplete] = useState(false);

  if (isComplete) {
    return <div className="p-4 bg-green-100 text-green-800 rounded">Order Complete!</div>;
  }

  return (
    <ExperimentVariant
      experimentKey="checkout_flow_design"
      fallback={<ControlCheckoutFlow onComplete={() => setIsComplete(true)} />}
    >
      {{
        control: <ControlCheckoutFlow onComplete={() => setIsComplete(true)} />,
        simplified: <SimplifiedCheckoutFlow onComplete={() => setIsComplete(true)} />,
        detailed: <DetailedCheckoutFlow onComplete={() => setIsComplete(true)} />,
      }}
    </ExperimentVariant>
  );
}

// =============================================================================
// Example 3: FeatureFlag component usage
// =============================================================================

function NewPromoBanner() {
  return (
    <div className="bg-gradient-to-r from-purple-500 to-pink-500 text-white p-4 rounded-lg">
      <h3 className="text-lg font-bold">Special Offer!</h3>
      <p>Get 20% off your next purchase with code SAVE20</p>
    </div>
  );
}

function OldPromoBanner() {
  return (
    <div className="bg-gray-200 text-gray-800 p-4 rounded-lg">
      <p>Check out our latest promotions</p>
    </div>
  );
}

export function PromoBannerFeatureFlag() {
  return (
    <FeatureFlag
      experimentKey="new_promo_banner"
      enabledVariant="enabled"
      fallback={<OldPromoBanner />}
    >
      <NewPromoBanner />
    </FeatureFlag>
  );
}

// =============================================================================
// Example 4: Conversion tracking with useExperiment
// =============================================================================

export function PurchaseCompleteWithTracking() {
  const { variantKey, trackConversion } = useExperiment('checkout_flow_v2', {
    trackImpression: false, // Don't track impression, only conversion
  });

  const handlePurchaseComplete = async () => {
    // ... purchase logic ...

    // Track the conversion
    if (variantKey) {
      await trackConversion('purchase_completed', {
        amount: 150000,
        currency: 'IDR',
        variant: variantKey,
        timestamp: new Date().toISOString(),
      });
    }

    alert('Purchase completed!');
  };

  return (
    <div className="space-y-4">
      <p>Thank you for your order!</p>
      <button
        onClick={handlePurchaseComplete}
        className="bg-green-500 text-white px-4 py-2 rounded"
      >
        Complete Purchase
      </button>
    </div>
  );
}

// =============================================================================
// Example 5: Multi-variant experiment with callbacks
// =============================================================================

export function CheckoutWithCallbacks() {
  const { variantKey, isLoading } = useExperiment('checkout_design_v3', {
    onVariantAssigned: (assignedVariant) => {
      console.log(`User assigned to variant: ${assignedVariant}`);
      // Send to analytics
      if (typeof window !== 'undefined' && window.gtag) {
        window.gtag('event', 'experiment_view', {
          experiment_key: 'checkout_design_v3',
          variant: assignedVariant,
        });
      }
    },
    onError: (error) => {
      console.error('Failed to load experiment:', error);
      // Fallback to control
    },
  });

  if (isLoading) {
    return <div>Loading checkout...</div>;
  }

  const designStyle =
    variantKey === 'variant_b' ? 'border-2 border-green-500' :
    variantKey === 'variant_c' ? 'shadow-xl rounded-2xl' :
    'border border-gray-300';

  return (
    <div className={`p-6 ${designStyle}`}>
      <h1 className="text-2xl font-bold mb-4">Checkout</h1>
      <p>Current variant: {variantKey || 'control'}</p>
      {/* Checkout form content */}
    </div>
  );
}

// =============================================================================
// Example 6: Combining multiple experiments
// =============================================================================

export function CombinedExperimentsCheckout() {
  const buttonExperiment = useExperiment('checkout_button_color');
  const flowExperiment = useExperiment('checkout_flow_design');
  const promoExperiment = useExperiment('promo_banner_enabled');

  if (buttonExperiment.isLoading || flowExperiment.isLoading || promoExperiment.isLoading) {
    return <div>Loading...</div>;
  }

  const showPromo = promoExperiment.variantKey === 'enabled';

  return (
    <div className="space-y-6">
      {showPromo && (
        <div className="bg-yellow-100 p-4 rounded">
          Special offer! Free shipping on orders over $50
        </div>
      )}

      <div className="space-y-4">
        <h1 className="text-2xl font-bold">Checkout</h1>
        <p>Flow: {flowExperiment.variantKey || 'control'}</p>
        <p>Button: {buttonExperiment.variantKey || 'control'}</p>

        <button
          className={
            buttonExperiment.variantKey === 'variant_b'
              ? 'bg-green-500 text-white px-6 py-3 rounded'
              : 'bg-blue-500 text-white px-6 py-3 rounded'
          }
        >
          Complete Purchase
        </button>
      </div>
    </div>
  );
}

// =============================================================================
// Master example component showing all patterns
// =============================================================================

export default function ABTestingExamples() {
  const [activeExample, setActiveExample] = useState<'hook' | 'variant' | 'flag' | 'tracking'>('hook');

  return (
    <div className="space-y-8 p-6 max-w-4xl mx-auto">
      <h1 className="text-3xl font-black tracking-tight text-bank-green">
        A/B Testing Examples
      </h1>

      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => setActiveExample('hook')}
          className={`px-4 py-2 rounded ${
            activeExample === 'hook' ? 'bg-bank-green text-white' : 'bg-gray-200'
          }`}
        >
          Hook Example
        </button>
        <button
          onClick={() => setActiveExample('variant')}
          className={`px-4 py-2 rounded ${
            activeExample === 'variant' ? 'bg-bank-green text-white' : 'bg-gray-200'
          }`}
        >
          Variant Component
        </button>
        <button
          onClick={() => setActiveExample('flag')}
          className={`px-4 py-2 rounded ${
            activeExample === 'flag' ? 'bg-bank-green text-white' : 'bg-gray-200'
          }`}
        >
          Feature Flag
        </button>
        <button
          onClick={() => setActiveExample('tracking')}
          className={`px-4 py-2 rounded ${
            activeExample === 'tracking' ? 'bg-bank-green text-white' : 'bg-gray-200'
          }`}
        >
          Conversion Tracking
        </button>
      </div>

      <div className="border rounded-xl p-6 bg-card">
        {activeExample === 'hook' && <CheckoutButtonHook />}
        {activeExample === 'variant' && <CheckoutPageVariant />}
        {activeExample === 'flag' && <PromoBannerFeatureFlag />}
        {activeExample === 'tracking' && <PurchaseCompleteWithTracking />}
      </div>

      <div className="text-sm text-gray-500">
        <p>Note: These examples demonstrate different patterns for A/B testing.</p>
        <p>In production, experiments are managed via the ab-testing-service backend.</p>
      </div>
    </div>
  );
}
