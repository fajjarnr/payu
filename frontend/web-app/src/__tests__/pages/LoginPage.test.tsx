/* eslint-disable @next/next/no-img-element, jsx-a11y/alt-text */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import LoginPage from '@/app/[locale]/login/page';

// Mock next-intl
vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => {
    const map: Record<string, string> = {
      welcomeBack: 'Welcome Back',
      subtitle: 'Sign in to your account',
      loginButton: 'Sign In',
      'branding.title': 'PayU',
      forgotPassword: 'Forgot password?',
      noAccount: "Don't have an account?",
      register: 'Register',
    };
    return map[key] || key;
  },
}));

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

// Mock stores
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: null,
    isAuthenticated: false,
    setUser: vi.fn(),
    setAuthenticated: vi.fn(),
  }),
}));

// Mock navigation lib
vi.mock('@/lib/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  Link: ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  ),
}));

// Mock next/image
vi.mock('next/image', () => ({
  default: (props: Record<string, unknown>) => <img {...props} />,
}));

// Mock framer-motion
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

// Mock tanstack query
vi.mock('@tanstack/react-query', () => ({
  useMutation: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  }),
}));

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render the login form', () => {
    render(<LoginPage />);

    expect(screen.getByTestId('username-input')).toBeInTheDocument();
    expect(screen.getByTestId('password-input')).toBeInTheDocument();
  });

  it('should render the login submit button', () => {
    render(<LoginPage />);

    expect(screen.getByTestId('login-submit-button')).toBeInTheDocument();
  });

  it('should render branding elements', () => {
    render(<LoginPage />);

    // "PayU" appears in both logo span and branding title
    const payuElements = screen.getAllByText('PayU');
    expect(payuElements.length).toBeGreaterThanOrEqual(1);
  });

  it('should render forgot password link', () => {
    render(<LoginPage />);

    expect(screen.getByTestId('forgot-password-link')).toBeInTheDocument();
  });
});
