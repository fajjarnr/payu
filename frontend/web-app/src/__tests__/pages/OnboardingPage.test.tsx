/* eslint-disable @next/next/no-img-element, jsx-a11y/alt-text */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import OnboardingPage from '@/app/[locale]/onboarding/page';

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => {
    const map: Record<string, string> = {
      'steps.identity': 'Identity',
      'steps.profile': 'Profile',
      'steps.complete': 'Complete',
      'step1.title': 'Upload Your ID',
      'step1.clickToUpload': 'Click to upload',
      'step2.title': 'Complete Your Profile',
      'step2.nik': 'NIK',
      'step2.fullName': 'Full Name',
      'step2.email': 'Email',
      'step2.username': 'Username',
      'branding.title': 'PayU',
    };
    return map[key] || key;
  },
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

vi.mock('@/lib/api', () => ({
  default: {
    post: vi.fn(),
  },
}));

vi.mock('@tanstack/react-query', () => ({
  useMutation: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
    isError: false,
  }),
}));

vi.mock('react-hook-form', () => ({
  useForm: () => ({
    register: () => ({}),
    handleSubmit: (fn: (...args: unknown[]) => void) => (e: Event) => { e?.preventDefault?.(); fn({}); },
    formState: { errors: {} },
    watch: () => '',
    setValue: vi.fn(),
    trigger: vi.fn(),
  }),
}));

vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: { children?: React.ReactNode }) => <div {...props}>{children}</div>,
    form: ({ children, ...props }: { children?: React.ReactNode }) => <form {...props}>{children}</form>,
    button: ({ children, ...props }: { children?: React.ReactNode }) => <button {...props}>{children}</button>,
    p: ({ children, ...props }: { children?: React.ReactNode }) => <p {...props}>{children}</p>,
    span: ({ children, ...props }: { children?: React.ReactNode }) => <span {...props}>{children}</span>,
  },
  AnimatePresence: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock('@/components/ui/stepper', () => ({
  default: () => <div data-testid="stepper">Stepper</div>,
  Stepper: () => <div data-testid="stepper">Stepper</div>,
}));

vi.mock('@/lib/navigation', () => ({
  Link: ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  ),
}));

vi.mock('next/image', () => ({
  default: (props: Record<string, unknown>) => <img {...props} />,
}));

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render the onboarding page', () => {
    render(<OnboardingPage />);
    expect(screen.getByText('PayU')).toBeInTheDocument();
  });

  it('should render first step - identity upload', () => {
    render(<OnboardingPage />);
    expect(screen.getByText('Upload Your ID')).toBeInTheDocument();
  });
});
