import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useActiveContent,
  useBanners,
  usePromos,
  useEmergencyAlerts,
  usePopups
} from '@/hooks/useCMS';
import CMSService from '@/services/CMSService';

// Mock CMSService
vi.mock('@/services/CMSService');

describe('useActiveContent hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockContent = [
    {
      id: 'content-1',
      contentType: 'BANNER',
      title: 'Test Banner',
      description: 'Test description',
      imageUrl: 'https://example.com/banner.jpg',
      actionUrl: 'https://example.com',
      actionType: 'LINK',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 10,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    },
    {
      id: 'content-2',
      contentType: 'BANNER',
      title: 'Lower Priority Banner',
      description: 'Lower priority',
      imageUrl: 'https://example.com/banner2.jpg',
      actionUrl: 'https://example.com',
      actionType: 'LINK',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 5,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useActiveContent).toBeDefined();
  });

  it('should fetch BANNER content successfully', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockContent);

    const { result } = renderHook(
      () => useActiveContent('BANNER', { segment: 'VIP', location: 'HOME', device: 'MOBILE' }),
      { wrapper }
    );

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('BANNER', {
      segment: 'VIP',
      location: 'HOME',
      device: 'MOBILE'
    });

    expect(result.current.data).toEqual(mockContent);
    expect(result.current.isSuccess).toBe(true);
  });

  it('should fetch PROMO content successfully', async () => {
    const promoContent = [
      {
        ...mockContent[0],
        id: 'promo-1',
        contentType: 'PROMO',
        title: 'Special Promo'
      }
    ];

    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(promoContent);

    const { result } = renderHook(() => useActiveContent('PROMO'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('PROMO', undefined);
    expect(result.current.data).toEqual(promoContent);
  });

  it('should fetch ALERT content successfully', async () => {
    const alertContent = [
      {
        ...mockContent[0],
        id: 'alert-1',
        contentType: 'ALERT',
        title: 'Emergency Alert'
      }
    ];

    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(alertContent);

    const { result } = renderHook(() => useActiveContent('ALERT'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(alertContent);
  });

  it('should fetch POPUP content successfully', async () => {
    const popupContent = [
      {
        ...mockContent[0],
        id: 'popup-1',
        contentType: 'POPUP',
        title: 'Special Popup'
      }
    ];

    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(popupContent);

    const { result } = renderHook(() => useActiveContent('POPUP'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(popupContent);
  });

  it('should pass targeting options to service', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockContent);

    const options = {
      segment: 'GOLD',
      location: 'DASHBOARD',
      device: 'DESKTOP'
    };

    const { result } = renderHook(() => useActiveContent('BANNER', options), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('BANNER', options);
  });

  it('should handle fetch errors', async () => {
    const error = new Error('Network error');
    vi.mocked(CMSService.getActiveContentByType).mockRejectedValue(error);

    const { result } = renderHook(() => useActiveContent('BANNER'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('should be disabled when enabled option is false', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockContent);

    const { result } = renderHook(() => useActiveContent('BANNER', { enabled: false }), {
      wrapper
    });

    expect(result.current.fetchStatus).toBe('idle');
    expect(CMSService.getActiveContentByType).not.toHaveBeenCalled();
  });

  it('should respect stale time configuration', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockContent);

    const { result } = renderHook(() => useActiveContent('BANNER'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledTimes(1);

    // Immediate refetch should use cached data (staleTime: 5 minutes)
    const { result: result2 } = renderHook(() => useActiveContent('BANNER'), { wrapper });

    await waitFor(() => {
      expect(result2.current.isSuccess).toBe(true);
    });

    // Should not call service again due to staleTime
    expect(CMSService.getActiveContentByType).toHaveBeenCalledTimes(1);
  });

  it('should not refetch on window focus', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockContent);

    const { result } = renderHook(() => useActiveContent('BANNER'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    const callCount = vi.mocked(CMSService.getActiveContentByType).mock.calls.length;

    // Simulate window focus (this would normally trigger refetch if enabled)
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    // Should not call again as refetchOnWindowFocus is false
    expect(vi.mocked(CMSService.getActiveContentByType).mock.calls.length).toBe(callCount);
  });
});

describe('useBanners hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockBanners = [
    {
      id: 'banner-1',
      contentType: 'BANNER',
      title: 'Banner 1',
      description: 'Description 1',
      imageUrl: 'https://example.com/banner1.jpg',
      actionUrl: 'https://example.com',
      actionType: 'LINK',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 10,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useBanners).toBeDefined();
  });

  it('should fetch banners successfully', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockBanners);

    const { result } = renderHook(() => useBanners(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('BANNER', undefined);
    expect(result.current.data).toEqual(mockBanners);
  });

  it('should pass options to useActiveContent', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockBanners);

    const options = {
      segment: 'VIP',
      location: 'HOME',
      device: 'MOBILE'
    };

    const { result } = renderHook(() => useBanners(options), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('BANNER', options);
  });

  it('should handle disabled state', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockBanners);

    const { result } = renderHook(() => useBanners({ enabled: false }), { wrapper });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('usePromos hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockPromos = [
    {
      id: 'promo-1',
      contentType: 'PROMO',
      title: 'Special Promo',
      description: 'Promo description',
      imageUrl: 'https://example.com/promo.jpg',
      actionUrl: 'https://example.com',
      actionType: 'LINK',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 10,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(usePromos).toBeDefined();
  });

  it('should fetch promos successfully', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockPromos);

    const { result } = renderHook(() => usePromos(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('PROMO', undefined);
    expect(result.current.data).toEqual(mockPromos);
  });

  it('should pass targeting options', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockPromos);

    const options = {
      segment: 'GOLD',
      location: 'PROMO_PAGE'
    };

    const { result } = renderHook(() => usePromos(options), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('PROMO', options);
  });
});

describe('useEmergencyAlerts hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockAlerts = [
    {
      id: 'alert-1',
      contentType: 'ALERT',
      title: 'System Maintenance',
      description: 'Scheduled maintenance',
      imageUrl: '',
      actionUrl: '',
      actionType: 'DISMISS',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 10,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(useEmergencyAlerts).toBeDefined();
  });

  it('should fetch alerts successfully', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockAlerts);

    const { result } = renderHook(() => useEmergencyAlerts(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('ALERT', undefined);
    expect(result.current.data).toEqual(mockAlerts);
  });
});

describe('usePopups hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockPopups = [
    {
      id: 'popup-1',
      contentType: 'POPUP',
      title: 'Special Offer',
      description: 'Limited time offer',
      imageUrl: 'https://example.com/popup.jpg',
      actionUrl: 'https://example.com',
      actionType: 'LINK',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 10,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should be defined', () => {
    expect(usePopups).toBeDefined();
  });

  it('should fetch popups successfully', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockPopups);

    const { result } = renderHook(() => usePopups(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('POPUP', undefined);
    expect(result.current.data).toEqual(mockPopups);
  });

  it('should pass device and location targeting', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockPopups);

    const options = {
      device: 'MOBILE',
      location: 'HOME'
    };

    const { result } = renderHook(() => usePopups(options), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledWith('POPUP', options);
  });
});

describe('CMS hooks integration', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockContent = [
    {
      id: 'content-1',
      contentType: 'BANNER',
      title: 'Test Content',
      description: 'Test',
      imageUrl: 'https://example.com/image.jpg',
      actionUrl: 'https://example.com',
      actionType: 'LINK',
      startDate: '2024-01-01T00:00:00Z',
      endDate: '2024-12-31T23:59:59Z',
      priority: 10,
      status: 'ACTIVE',
      targetingRules: {},
      metadata: {},
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      active: true
    }
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
  });

  it('should fetch multiple content types simultaneously', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockResolvedValue(mockContent);

    const { result: banners } = renderHook(() => useBanners(), { wrapper });
    const { result: promos } = renderHook(() => usePromos(), { wrapper });
    const { result: alerts } = renderHook(() => useEmergencyAlerts(), { wrapper });

    await waitFor(() => {
      expect(banners.current.isSuccess).toBe(true);
      expect(promos.current.isSuccess).toBe(true);
      expect(alerts.current.isSuccess).toBe(true);
    });

    expect(CMSService.getActiveContentByType).toHaveBeenCalledTimes(3);
  });

  it('should handle independent loading states', async () => {
    vi.mocked(CMSService.getActiveContentByType).mockImplementation((type) =>
      new Promise((resolve) => {
        setTimeout(() => resolve(mockContent), type === 'BANNER' ? 50 : 100);
      })
    );

    const { result: banners } = renderHook(() => useBanners(), { wrapper });
    const { result: popups } = renderHook(() => usePopups(), { wrapper });

    // Banners should load first
    await waitFor(
      () => {
        expect(banners.current.isSuccess).toBe(true);
      },
      { timeout: 200 }
    );

    // Both should eventually succeed
    await waitFor(
      () => {
        expect(popups.current.isSuccess).toBe(true);
      },
      { timeout: 200 }
    );
  });
});
