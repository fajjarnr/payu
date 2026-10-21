import { screen, fireEvent, act } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { vi } from 'vitest';
import BannerCarousel from '@/components/cms/BannerCarousel';
import PromoPopup from '@/components/cms/PromoPopup';
import EmergencyAlert from '@/components/cms/EmergencyAlert';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Banned legacy stems (ANT §6 A-01): must not appear in rendered output.
const BANNED_STEMS = [
  'bank-green',
  'bank-emerald',
  'bg-muted',
  'text-muted-foreground',
  'border-border',
  'bg-card',
];
function expectNoLegacyClasses(container: HTMLElement) {
  const html = container.innerHTML;
  for (const stem of BANNED_STEMS) {
    expect(html, `legacy stem "${stem}"`).not.toContain(stem);
  }
}

const mockReplace = vi.fn();
const mockPush = vi.fn();
vi.mock('@/lib/navigation', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/navigation')>()),
  useRouter: () => ({ replace: mockReplace, push: mockPush }),
}));

const bannersState: {
  data: Array<Record<string, unknown>> | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
} = { data: null, isLoading: false, error: null, refetch: vi.fn() };
const popupsState: {
  data: Array<Record<string, unknown>> | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
} = { data: null, isLoading: false, error: null, refetch: vi.fn() };
const alertsState: {
  data: Array<Record<string, unknown>> | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
} = { data: null, isLoading: false, error: null, refetch: vi.fn() };

vi.mock('@/hooks', () => ({
  useBanners: () => bannersState,
  usePopups: () => popupsState,
  useEmergencyAlerts: () => alertsState,
}));

const mockBanners = [
  {
    id: '1',
    title: 'Summer Sale',
    description: 'Get 50% off on all items',
    imageUrl: 'https://example.com/banner1.jpg',
    actionUrl: 'https://example.com/sale',
    actionType: 'LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: {},
  },
  {
    id: '2',
    title: 'New Arrival',
    description: 'Check out our latest collection',
    imageUrl: 'https://example.com/banner2.jpg',
    actionUrl: '/products/new',
    actionType: 'DEEP_LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: {},
  },
];

const mockPopups = [
  {
    id: 'popup-1',
    title: 'Special Offer',
    description: 'Get 20% off on your next purchase',
    imageUrl: 'https://example.com/popup1.jpg',
    actionUrl: '/offers/special',
    actionType: 'DEEP_LINK',
    startDate: '2024-01-01',
    endDate: '2099-12-31',
    metadata: {},
  },
  {
    id: 'popup-2',
    title: 'Second Offer',
    description: 'Another promo for you',
    imageUrl: 'https://example.com/popup2.jpg',
    actionUrl: '/offers/second',
    actionType: 'DEEP_LINK',
    startDate: '2024-01-01',
    endDate: '2099-12-31',
    metadata: {},
  },
];

const mockAlerts = [
  {
    id: 'alert-1',
    title: 'System Maintenance',
    description: 'Scheduled maintenance on Sunday 2-4 AM',
    actionUrl: '/maintenance',
    actionType: 'DEEP_LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: { alertType: 'WARNING' },
  },
  {
    id: 'alert-2',
    title: 'New Feature Available',
    description: 'Check out our new analytics dashboard',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: { alertType: 'INFO' },
  },
  {
    id: 'alert-3',
    title: 'Outage Detected',
    description: 'Service disruption in progress',
    actionUrl: 'https://example.com/status',
    actionType: 'LINK',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    metadata: { alertType: 'ERROR' },
  },
];

const mockLocalStorage = {
  getItem: vi.fn((): string | null => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};
const mockSessionStorage = {
  getItem: vi.fn((): string | null => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: mockLocalStorage });
Object.defineProperty(window, 'sessionStorage', { value: mockSessionStorage });

function mockMatchMedia(matches: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(() => ({
      matches,
      media: '(prefers-reduced-motion: reduce)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
    })),
  });
}

describe('BannerCarousel ANT fixes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    mockMatchMedia(false);
    bannersState.data = [...mockBanners];
    bannersState.isLoading = false;
    bannersState.error = null;
    bannersState.refetch = vi.fn();
    global.open = vi.fn();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('R-06: fetch failure renders error UI with retry (never silent null)', () => {
    bannersState.data = null;
    bannersState.error = new Error('Failed to fetch');
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={5000} />);
    expect(container.firstChild).not.toBeNull();
    expect(screen.getByText('Gagal memuat banner')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Coba lagi' }));
    expect(bannersState.refetch).toHaveBeenCalled();
  });

  it('R-06: empty list with no error stays null', () => {
    bannersState.data = [];
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={5000} />);
    expect(container.firstChild).toBeNull();
  });

  // Why: antd Carousel (slick) renders every slide + clones on first paint, so
  // queryByText can never be null. Autoplay state is asserted on the live slide
  // via aria-current pagination instead of slide text presence.
  it('F-07: autoplay pauses on hover', () => {
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={1000} />);
    const wrapper = container.firstChild as HTMLElement;
    const currentOf = () =>
      container.querySelector('[aria-current="true"]')?.getAttribute('aria-label');
    fireEvent.mouseEnter(wrapper);
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    expect(currentOf()).toBe('Ke banner 1');
  });

  it('F-07: autoplay pauses on focus and resumes on blur', () => {
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={1000} />);
    const wrapper = container.firstChild as HTMLElement;
    const currentOf = () =>
      container.querySelector('[aria-current="true"]')?.getAttribute('aria-label');
    fireEvent.focus(wrapper);
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    expect(currentOf()).toBe('Ke banner 1');
    fireEvent.blur(wrapper);
    act(() => {
      vi.advanceTimersByTime(500);
    });
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(currentOf()).toBe('Ke banner 2');
  });

  it('F-07: prefers-reduced-motion disables autoplay', () => {
    mockMatchMedia(true);
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={1000} />);
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(container.querySelector('[aria-current="true"]')?.getAttribute('aria-label')).toBe(
      'Ke banner 1'
    );
  });

  it('F-07: scale decoration disabled under reduced motion', () => {
    const { container, unmount } = renderWithIntl(<BannerCarousel autoPlayInterval={1000} />);
    expect(container.innerHTML).toContain('group-hover/item:scale-105');
    unmount();
    mockMatchMedia(true);
    const reduced = renderWithIntl(<BannerCarousel autoPlayInterval={1000} />);
    expect(reduced.container.innerHTML).not.toContain('scale-105');
  });

  it('slide has exactly one accessible name (no alt/title duplication)', () => {
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={5000} />);
    const slide = screen.getByRole('button', { name: 'Summer Sale' });
    expect(slide).toBeInTheDocument();
    const imgs = container.querySelectorAll('img');
    expect(imgs.length).toBeGreaterThan(0);
    imgs.forEach((img) => expect(img.getAttribute('alt')).toBe(''));
  });

  it('F-02: arrows render on all viewports with a >=2px focus-visible ring', () => {
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={5000} />);
    const prev = screen.getByRole('button', { name: 'Banner sebelumnya' });
    const next = screen.getByRole('button', { name: 'Banner berikutnya' });
    expect(prev.closest('.hidden')).toBeNull();
    expect(next.closest('.hidden')).toBeNull();
    // The slide is a custom button, so it carries the shared focus-ring
    // utility; the arrows are antd buttons and ship their own ring.
    expect(container.querySelector('.cms-slide')?.className).toContain('payu-focus-ring');
    const css = readFileSync(join(process.cwd(), 'src/app/globals.css'), 'utf8');
    expect(css).toMatch(/\.payu-focus-ring:focus-visible/);
    expect(css).toMatch(/outline:\s*3px/);
  });

  it('A-01: no legacy token-bypass classes in output', () => {
    const { container } = renderWithIntl(<BannerCarousel autoPlayInterval={5000} />);
    expectNoLegacyClasses(container);
  });
});

describe('PromoPopup ANT fixes', () => {
  const advanceTimers = (ms: number) => {
    act(() => {
      vi.advanceTimersByTime(ms);
    });
  };
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    popupsState.data = [...mockPopups];
    popupsState.isLoading = false;
    popupsState.error = null;
    popupsState.refetch = vi.fn();
    mockLocalStorage.getItem.mockReturnValue(null);
    mockSessionStorage.getItem.mockReturnValue(null);
    mockPush.mockClear();
    global.open = vi.fn();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('F-03: timer must not steal focus; promo is announced politely with a user trigger', () => {
    const before = document.activeElement;
    renderWithIntl(<PromoPopup delay={2000} />);
    advanceTimers(2000);
    expect(document.activeElement).toBe(before);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Special Offer');
    expect(
      screen.getByRole('button', { name: /Lihat Promo/ }),
    ).toBeInTheDocument();
  });

  it('F-03: clicking the trigger opens the promo dialog', () => {
    renderWithIntl(<PromoPopup delay={2000} />);
    advanceTimers(2000);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getAllByText('Special Offer').length).toBeGreaterThan(0);
  });

  it('F-03: pagination dots are real buttons that navigate', () => {
    renderWithIntl(<PromoPopup delay={0} />);
    advanceTimers(100);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    const dots = screen.getAllByRole('button', { name: /Ke promo/ });
    expect(dots).toHaveLength(2);
    fireEvent.click(dots[1]);
    expect(screen.getAllByText('Second Offer').length).toBeGreaterThan(0);
  });

  it('A-05: close control is labeled "Tutup promo"', () => {
    renderWithIntl(<PromoPopup delay={0} />);
    advanceTimers(100);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    expect(screen.getByRole('button', { name: 'Tutup promo' })).toBeInTheDocument();
  });

  it('R-06: fetch failure renders error UI with retry', () => {
    popupsState.data = null;
    popupsState.error = new Error('CMS down');
    const { container } = renderWithIntl(<PromoPopup delay={0} />);
    expect(container.firstChild).not.toBeNull();
    expect(screen.getByText('Gagal memuat promo')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Coba lagi' }));
    expect(popupsState.refetch).toHaveBeenCalled();
  });

  it('A-01: no legacy token-bypass classes in output', () => {
    const { container } = renderWithIntl(<PromoPopup delay={0} />);
    advanceTimers(100);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    expectNoLegacyClasses(container);
  });

  it('A-04: "Jangan Tampilkan Lagi" removes the promo instead of re-offering it', () => {
    popupsState.data = [mockPopups[0]];
    renderWithIntl(<PromoPopup delay={0} />);
    advanceTimers(100);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    fireEvent.click(screen.getByRole('button', { name: /Jangan Tampilkan Lagi/i }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Lihat Promo/ })).not.toBeInTheDocument();
  });

  it('F-08: the dismissal is announced politely', () => {
    popupsState.data = [mockPopups[0]];
    renderWithIntl(<PromoPopup delay={0} />);
    advanceTimers(100);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    fireEvent.click(screen.getByRole('button', { name: /Jangan Tampilkan Lagi/i }));
    expect(screen.getByRole('status')).toHaveTextContent('Promo ditutup: Special Offer');
  });

  it('A-04: the claim CTA is hidden when the promo has no target', () => {
    popupsState.data = [{ ...mockPopups[0], actionUrl: undefined }];
    renderWithIntl(<PromoPopup delay={0} />);
    advanceTimers(100);
    fireEvent.click(screen.getByRole('button', { name: /Lihat Promo/ }));
    expect(screen.queryByRole('button', { name: /Klaim Sekarang/i })).not.toBeInTheDocument();
  });
});

describe('EmergencyAlert ANT fixes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    alertsState.data = [...mockAlerts];
    alertsState.isLoading = false;
    alertsState.error = null;
    alertsState.refetch = vi.fn();
    mockLocalStorage.getItem.mockReturnValue(null);
    mockPush.mockClear();
    global.open = vi.fn();
  });

  it('R-06: fetch failure renders error UI with retry', () => {
    alertsState.data = null;
    alertsState.error = new Error('CMS down');
    const { container } = renderWithIntl(<EmergencyAlert />);
    expect(container.firstChild).not.toBeNull();
    expect(screen.getByText('Gagal memuat peringatan')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Coba lagi' }));
    expect(alertsState.refetch).toHaveBeenCalled();
  });

  it('R-06: zero alerts stays silent (null)', () => {
    alertsState.data = [];
    const { container } = renderWithIntl(<EmergencyAlert />);
    expect(container.firstChild).toBeNull();
  });

  it('severity maps 1:1 to Alert type', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);
    const alerts = container.querySelectorAll('.ant-alert');
    expect(alerts).toHaveLength(3);
    expect(container.querySelectorAll('.ant-alert-warning')).toHaveLength(1);
    expect(container.querySelectorAll('.ant-alert-info')).toHaveLength(1);
    expect(container.querySelectorAll('.ant-alert-error')).toHaveLength(1);
  });

  it('F-03: wrapper focusable ⟺ clickable; non-clickable alert is inert', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);
    const clickable = screen.getByText('System Maintenance').closest('div[class]') as HTMLElement;
    void clickable;
    const wrappers = Array.from(container.firstChild!.childNodes) as HTMLElement[];
    expect(wrappers).toHaveLength(3);
    // alert-1 (actionUrl) and alert-3 (actionUrl) focusable; alert-2 (none) not.
    expect(wrappers[0].getAttribute('tabindex')).toBe('0');
    expect(wrappers[1].hasAttribute('tabindex')).toBe(false);
    expect(wrappers[2].getAttribute('tabindex')).toBe('0');
    fireEvent.click(screen.getByText('New Feature Available'));
    expect(mockPush).not.toHaveBeenCalled();
    expect(global.open).not.toHaveBeenCalled();
  });

  it('dismiss stops propagation (no nested-interactive navigation)', () => {
    renderWithIntl(<EmergencyAlert />);
    fireEvent.click(screen.getAllByRole('button', { name: /^Tutup peringatan/ })[0]);
    expect(mockLocalStorage.setItem).toHaveBeenCalled();
    expect(mockPush).not.toHaveBeenCalled();
    expect(global.open).not.toHaveBeenCalled();
  });

  it('A-05: dismiss and validity strings are localized', () => {
    renderWithIntl(<EmergencyAlert />);
    expect(screen.getAllByRole('button', { name: /^Tutup peringatan/ })).toHaveLength(3);
    expect(screen.getAllByText(/Berlaku hingga/)).toHaveLength(3);
  });

  it('A-01: no legacy token-bypass classes in output', () => {
    const { container } = renderWithIntl(<EmergencyAlert />);
    expectNoLegacyClasses(container);
  });

  it('F-03: keys on the dismiss button never trigger the alert link', () => {
    renderWithIntl(<EmergencyAlert />);
    const dismiss = screen.getAllByRole('button', { name: /^Tutup peringatan/ })[0];
    fireEvent.keyDown(dismiss, { key: 'Enter' });
    fireEvent.keyDown(dismiss, { key: ' ' });
    expect(mockPush).not.toHaveBeenCalled();
    expect(global.open).not.toHaveBeenCalled();
  });

  it('F-03: the alert link still activates from the keyboard', () => {
    renderWithIntl(<EmergencyAlert />);
    const link = screen.getByText('System Maintenance').closest('[role="link"]') as HTMLElement;
    fireEvent.keyDown(link, { key: 'Enter' });
    expect(mockPush).toHaveBeenCalledWith('/maintenance');
  });

  it('F-08: the dismissal is announced politely', () => {
    renderWithIntl(<EmergencyAlert />);
    fireEvent.click(screen.getAllByRole('button', { name: /^Tutup peringatan/ })[0]);
    expect(screen.getByRole('status')).toHaveTextContent(
      'Peringatan ditutup: System Maintenance'
    );
  });

  it('A-03: every dismiss button names its own alert', () => {
    renderWithIntl(<EmergencyAlert />);
    const labels = screen
      .getAllByRole('button', { name: /^Tutup peringatan/ })
      .map((button) => button.getAttribute('aria-label'));
    expect(new Set(labels).size).toBe(labels.length);
  });
});
