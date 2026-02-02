'use client';

import * as React from 'react';
import { Moon, Sun } from 'lucide-react';
import { useTheme } from 'next-themes';
import clsx from 'clsx';

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  // Avoid hydration mismatch
  React.useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return (
      <div className="w-12 h-12 rounded-full border border-border bg-white/[0.03] animate-pulse" />
    );
  }

  return (
    <button
      onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
      data-testid="theme-toggle-button"
      className={clsx(
        'w-12 h-12 flex items-center justify-center rounded-full transition-all cursor-pointer shadow-md border border-emerald-500/10 bg-card',
        'hover:bg-emerald-500/5 hover:border-emerald-500/30 active:scale-95 transition-all',
        theme === 'dark' ? 'text-amber-400' : 'text-emerald-600'
      )}
      aria-label="Toggle theme"
    >
      {theme === 'dark' ? (
        <Sun className="h-6 w-6" aria-hidden="true" />
      ) : (
        <Moon className="h-6 w-6" aria-hidden="true" />
      )}
    </button>
  );
}
