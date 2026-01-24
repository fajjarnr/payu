import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PersonalizedGreeting, PersonalizedWelcomeBanner } from '@/components/personalization/PersonalizedGreeting';

// Mock the hooks
vi.mock('@/hooks/useUserSegment', () => ({
  useUserSegment: () => ({
    currentTier: 'VIP',
    isVIP: true,
    currentMembership: {
      id: '1',
      userId: 'test-user',
      segmentId: 'vip-segment',
      segment: {
        id: 'vip-segment',
        name: 'VIP Member',
        description: 'VIP tier members',
        tier: 'VIP',
        minBalance: 10000000,
        benefits: ['All benefits'],
        requirements: ['High balance'],
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01',
      },
      status: 'ACTIVE',
      joinedAt: '2024-01-01',
      score: 100,
    },
    progressToNext: undefined,
    nextTier: undefined,
    totalScore: 100,
  }),
}));

vi.mock('@/stores', () => ({
  useAuthStore: () => ({
    user: { id: 'test-user', fullName: 'John Doe' },
  }),
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('PersonalizedGreeting', () => {
  it('should render personalized greeting with VIP badge', () => {
    render(
      <PersonalizedGreeting showTimeBased={true} showSegment={true} />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('John Doe');
  });

  it('should render without VIP badge when showSegment is false', () => {
    const { container } = render(
      <PersonalizedGreeting showTimeBased={true} showSegment={false} />,
      { wrapper: createWrapper() }
    );

    // Should not have VIP badge styling
    expect(container.querySelector('.from-amber-500')).toBeNull();
  });
});

describe('PersonalizedWelcomeBanner', () => {
  it('should render welcome banner for VIP users', () => {
    render(
      <PersonalizedWelcomeBanner />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('Welcome back');
    expect(screen.textContent).toContain('John');
    expect(screen.textContent).toContain('PERSONALIZED EXPERIENCE');
  });

  it('should render VIP benefits message', () => {
    render(
      <PersonalizedWelcomeBanner />,
      { wrapper: createWrapper() }
    );

    expect(screen.textContent).toContain('exclusive benefits');
  });
});
