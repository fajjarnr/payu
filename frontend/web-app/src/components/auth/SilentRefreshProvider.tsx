'use client';

import { useSilentRefresh } from '@/hooks/useSilentRefresh';

/**
 * SilentRefreshProvider
 *
 * Mounts the useSilentRefresh hook in the component tree.
 * Must be a separate 'use client' component so it can be used
 * inside Server Component layouts (like DashboardLayout).
 *
 * Place this inside any authenticated layout to ensure the
 * token is proactively refreshed before it expires.
 */
export function SilentRefreshProvider({ children }: { children: React.ReactNode }) {
  useSilentRefresh();
  return <>{children}</>;
}
