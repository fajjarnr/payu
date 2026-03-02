# Customer Segmentation Frontend Integration

## Overview

This document describes the customer segmentation integration in the PayU web application frontend. The implementation provides personalized offers, VIP status detection, and targeted promotional content based on user segments.

## Location

```
/home/ubuntu/payu/frontend/web-app/src/
├── services/
│   └── SegmentationService.ts
├── hooks/
│   ├── useUserSegment.ts
│   ├── useSegmentedOffers.ts
│   └── useVIPStatus.ts
├── components/
│   └── personalization/
│       ├── SegmentedOffers.tsx
│       ├── VIPBadge.tsx
│       ├── TargetedPromos.tsx
│       ├── PersonalizedGreeting.tsx
│       └── index.ts
├── __tests__/
│   └── components/
│       └── personalization/
│           ├── SegmentedOffers.test.tsx
│           ├── VIPBadge.test.tsx
│           ├── PersonalizedGreeting.test.tsx
│           └── TargetedPromos.test.tsx
└── types/
    └── index.ts (updated with segmentation types)
```

## API Endpoints

The following API endpoints are used:

- `GET /api/v1/segments/user/{userId}` - Get user's segment memberships
- `GET /api/v1/segments/{id}` - Get segment details
- `GET /api/v1/segments` - Get all segments
- `GET /api/v1/segments/user/{userId}/offers` - Get segmented offers for user
- `GET /api/v1/segments/{id}/offers` - Get offers by segment
- `GET /api/v1/segments/{id}/users` - Get users in segment

## Types

### SegmentTier
```typescript
type SegmentTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND' | 'VIP';
```

### SegmentStatus
```typescript
type SegmentStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';
```

### CustomerSegment
```typescript
interface CustomerSegment {
  id: string;
  name: string;
  description: string;
  tier: SegmentTier;
  minBalance: number;
  maxBalance?: number;
  benefits: string[];
  requirements: string[];
  createdAt: string;
  updatedAt: string;
}
```

### SegmentMembership
```typescript
interface SegmentMembership {
  id: string;
  userId: string;
  segmentId: string;
  segment: CustomerSegment;
  status: SegmentStatus;
  joinedAt: string;
  validUntil?: string;
  score: number;
}
```

### SegmentedOffer
```typescript
interface SegmentedOffer {
  id: string;
  title: string;
  description: string;
  segmentId: string;
  segmentTier: SegmentTier;
  offerType: 'CASHBACK' | 'DISCOUNT' | 'REWARD_POINTS' | 'FREE_TRANSFER' | 'BONUS_INTEREST';
  value: number;
  currency?: string;
  percentage?: number;
  validFrom: string;
  validUntil: string;
  terms: string[];
  imageUrl?: string;
  promoCode?: string;
  minTransaction?: number;
  maxReward?: number;
  isActive: boolean;
  createdAt: string;
}
```

## Services

### SegmentationService

Singleton service for API communication with segmentation endpoints.

**Methods:**
- `getUserSegments(userId: string)` - Get user's segments
- `getSegmentById(segmentId: string)` - Get segment details
- `getAllSegments()` - Get all segments
- `getSegmentedOffers(userId, page, size)` - Get segmented offers for user
- `getOffersBySegment(segmentId, page, size)` - Get offers by segment
- `getSegmentUsers(segmentId, page, size)` - Get users in segment
- `isVIPSegment(tier)` - Check if tier is VIP
- `getTierPriority(tier)` - Get tier priority number

## Hooks

### useUserSegment

Fetch user's segments with caching.

**Returns:**
```typescript
{
  currentMembership: SegmentMembership | undefined;
  currentTier: SegmentTier | undefined;
  isVIP: boolean;
  nextTier: SegmentTier | undefined;
  progressToNext: number | undefined;
  totalScore: number | undefined;
  invalidateSegments: () => void;
  // ... other useQuery properties
}
```

### useSegmentedOffers

Fetch offers for user's segment with filtering.

**Returns:**
```typescript
{
  offers: SegmentedOffer[];
  cashbackOffers: SegmentedOffer[];
  discountOffers: SegmentedOffer[];
  rewardOffers: SegmentedOffer[];
  freeTransferOffers: SegmentedOffer[];
  totalCount: number;
  invalidateOffers: () => void;
  // ... other useQuery properties
}
```

### useVIPStatus

Check if user is VIP segment and get benefits.

**Returns:**
```typescript
{
  isVIP: boolean;
  tier: SegmentTier | null;
  tierLabel: string;
  tierColor: string;
  benefits: string[];
  hasVIPAccess: boolean;
  prioritySupport: boolean;
  exclusiveOffers: boolean;
  higherLimits: boolean;
  feeWaivers: boolean;
}
```

## Components

### SegmentedOffers

Display personalized offers based on segment.

**Props:**
```typescript
interface SegmentedOffersProps {
  className?: string;
  maxOffers?: number;
}
```

**Usage:**
```tsx
<SegmentedOffers maxOffers={3} />
```

### VIPBadge

Show VIP badge for premium users.

**Props:**
```typescript
interface VIPBadgeProps {
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  showIcon?: boolean;
  variant?: 'badge' | 'card' | 'inline';
  className?: string;
}
```

**Usage:**
```tsx
<VIPBadge size="md" variant="badge" showLabel={true} />
```

### TargetedPromos

Promotional content based on segment.

**Props:**
```typescript
interface TargetedPromosProps {
  offerType?: SegmentedOffer['offerType'];
  className?: string;
  maxPromos?: number;
}
```

**Usage:**
```tsx
<TargetedPromos offerType="CASHBACK" maxPromos={2} />
```

### PersonalizedGreeting

Greeting based on segment tier and time of day.

**Props:**
```typescript
interface PersonalizedGreetingProps {
  showTimeBased?: boolean;
  showSegment?: boolean;
  className?: string;
}
```

**Usage:**
```tsx
<PersonalizedGreeting showTimeBased={true} showSegment={true} />
```

### PersonalizedWelcomeBanner

Full welcome banner with progress indicator.

**Props:**
```typescript
interface PersonalizedWelcomeBannerProps {
  className?: string;
}
```

**Usage:**
```tsx
<PersonalizedWelcomeBanner />
```

## Dashboard Integration

### BalanceCard

The VIPBadge is integrated into the BalanceCard component:

```tsx
// In BalanceCard header
<div className="flex items-center gap-2">
  <VIPBadge size="sm" variant="badge" />
  {/* ... */}
</div>
```

### DashboardLayout

The PersonalizedGreeting is integrated into the header:

```tsx
// In DashboardLayout header
<div className="hidden sm:block">
  <PersonalizedGreeting showTimeBased={true} showSegment={true} />
  {/* ... */}
</div>
```

### Main Dashboard Page

The SegmentedOffers is integrated into the main page:

```tsx
// In page.tsx
<StaggerItem>
  <SegmentedOffers maxOffers={3} />
</StaggerItem>
```

## Exports

### services/index.ts
```typescript
export { default as SegmentationService, type CustomerSegment, type SegmentMembership, type SegmentedOffer, type UserSegmentsResponse, type SegmentedOffersResponse, type SegmentTier, type SegmentStatus } from './SegmentationService';
```

### hooks/index.ts
```typescript
export { useUserSegment, useSegmentDetails, useAllSegments } from './useUserSegment';
export { useSegmentedOffers, useOffersBySegment, useVIPOffers } from './useSegmentedOffers';
export { useVIPStatus, type VIPStatus } from './useVIPStatus';
```

### components/personalization/index.ts
```typescript
export { default as SegmentedOffers } from './SegmentedOffers';
export { default as VIPBadge, VIPStatusIndicator } from './VIPBadge';
export { default as TargetedPromos, QuickPromoBanner } from './TargetedPromos';
export { default as PersonalizedGreeting, PersonalizedWelcomeBanner } from './PersonalizedGreeting';
```

## Testing

Test files are provided for all components:

- `SegmentedOffers.test.tsx` - Test offers display and filtering
- `VIPBadge.test.tsx` - Test VIP badge rendering in different variants
- `PersonalizedGreeting.test.tsx` - Test personalized greeting logic
- `TargetedPromos.test.tsx` - Test targeted promos and filtering

## Styling

Components use the Premium Emerald design system:
- Primary color: `bank-green` (#10b981)
- Secondary color: `bank-emerald` (#059669)
- VIP gradient: `from-amber-500 to-orange-600`
- Border radius: `rounded-xl` for cards, `rounded-full` for badges
- Shadows: `shadow-xl` and `shadow-2xl` for depth

## Caching Strategy

- User segments: 5 minutes stale time, 30 minutes garbage collection
- Segment details: 10 minutes stale time, 30 minutes garbage collection
- Segmented offers: 2 minutes stale time, 5 minutes garbage collection
- VIP offers: 3 minutes stale time, 10 minutes garbage collection

## Related Files

- Backend service: `/home/ubuntu/payu/backend/promotion-service/`
- API documentation: See backend service OpenAPI specs
- CHANGELOG.md: Updated with this implementation
