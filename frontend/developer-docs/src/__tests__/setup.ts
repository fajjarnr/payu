import '@testing-library/jest-dom';
import { vi } from 'vitest';

// Mock next-intl
vi.mock('next-intl', () => ({
  useTranslations: vi.fn(),
  useLocale: vi.fn(() => 'id'),
  getTranslations: vi.fn(),
}));

// Mock next-intl/server
vi.mock('next-intl/server', () => ({
  getMessages: vi.fn(() => ({})),
  getRequestConfig: vi.fn(),
}));

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: vi.fn(() => ({
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  })),
  usePathname: vi.fn(() => '/'),
  useSearchParams: vi.fn(() => new URLSearchParams()),
}));

// Mock lucide-react icons
vi.mock('lucide-react', () => ({
  ArrowRight: () => null,
  CheckCircle2: () => null,
  Terminal: () => null,
  Code2: () => null,
  BookOpen: () => null,
  Zap: () => null,
  QrCode: () => null,
  Clock: () => null,
  Smartphone: () => null,
  Shield: () => null,
  AlertTriangle: () => null,
  Copy: () => null,
  FileCode: () => null,
  Package: () => null,
}));
