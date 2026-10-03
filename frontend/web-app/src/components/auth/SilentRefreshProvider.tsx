'use client';

import { useSilentRefresh } from '@/hooks/useSilentRefresh';

/**
 * Must be a separate 'use client' component for use inside Server Component layouts.
 * Place inside any authenticated layout to ensure the token is proactively refreshed before it expires.
 */
export function SilentRefreshProvider({ children }: { children: React.ReactNode }) {
  useSilentRefresh();
  return <>{children}</>;
}
