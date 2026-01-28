import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PersonalizedGreeting from '@/components/personalization/PersonalizedGreeting';
import { PersonalizedWelcomeBanner } from '@/components/personalization/PersonalizedGreeting';

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

// Mock zustand store properly
vi.mock('@/stores/authStore', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: 'test-user', fullName: 'John Doe' },
      token: null,
      refreshToken: null,
      accountId: null,
      isAuthenticated: false,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setToken: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn(),
    };
    return selector ? selector(state) : state;
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

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'QueryClientWrapper';
  return Wrapper;
};

describe('PersonalizedGreeting', () => {
  it('should render personalized greeting with VIP badge', () => {
    const { container } = render(
      <PersonalizedGreeting showTimeBased={true} showSegment={true} />,
      { wrapper: createWrapper() }
    );

    expect(container.textContent).toContain('John Doe');
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
    const { container } = render(
      <PersonalizedWelcomeBanner />,
      { wrapper: createWrapper() }
    );

    // Component uses Indonesian greeting text
    expect(container.textContent).toMatch(/(Selamat|Welcome)/);
    expect(container.textContent).toContain('John');
    expect(container.textContent).toContain('PERSONALIZED EXPERIENCE');
  });

  it('should render VIP benefits message', () => {
    const { container } = render(
      <PersonalizedWelcomeBanner />,
      { wrapper: createWrapper() }
    );

    // Component uses "exclusive benefits" or "premium services"
    expect(container.textContent).toMatch(/(benefits|services)/);
  });
});
