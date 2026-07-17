import { describe, it, expect, vi } from 'vitest';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
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

describe('PersonalizedGreeting', () => {
  it('should render personalized greeting with VIP badge', () => {
    const { container } = renderWithIntl(
      <PersonalizedGreeting showTimeBased={true} showSegment={true} />
    );

    expect(container.textContent).toContain('Test');
  });

  it('should render without VIP badge when showSegment is false', () => {
    const { container } = renderWithIntl(
      <PersonalizedGreeting showTimeBased={true} showSegment={false} />
    );

    // Should not have VIP badge styling
    expect(container.querySelector('.from-amber-500')).toBeNull();
  });
});

describe('PersonalizedWelcomeBanner', () => {
  it('should render welcome banner for VIP users', () => {
    const { container } = renderWithIntl(<PersonalizedWelcomeBanner />);

    // Component uses Indonesian greeting text
    expect(container.textContent).toMatch(/(Selamat|Welcome)/);
    expect(container.textContent).toContain('Test');
    expect(container.textContent).toContain('PERSONALIZED EXPERIENCE');
  });

  it('should render VIP benefits message', () => {
    const { container } = renderWithIntl(<PersonalizedWelcomeBanner />);

    expect(container.textContent).toContain('Discover personalized offers for you');
  });
});
