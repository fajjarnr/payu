/**
 * Which routes render the app shell (sidebar + header + content box).
 *
 * The error boundary and the 404 page replace the page component, so without
 * this rule they dropped the shell and the app frame vanished mid-session —
 * a layout jump bigger than the grid inconsistency it was meant to report.
 */
const SHELL_LESS_ROUTES: Record<string, true> = {
  '/': true,
  '/login': true,
  '/forgot-password': true,
  '/onboarding': true,
  '/merchant/register': true,
};

export function usesAppShell(pathname: string): boolean {
  // Strip the locale prefix (/id/dashboard -> /dashboard).
  const path = pathname.replace(/^\/[a-z]{2}(?=\/|$)/, '') || '/';
  if (SHELL_LESS_ROUTES[path]) return false;
  // The backoffice console has its own layout.
  if (path.startsWith('/backoffice')) return false;
  return true;
}
