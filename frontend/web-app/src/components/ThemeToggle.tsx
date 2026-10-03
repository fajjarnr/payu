'use client';

import * as React from 'react';
import { Moon, Sun } from '@/components/icons';
import { useUIStore } from '@/stores';
import { useTranslations } from 'next-intl';
import { Button } from 'antd';
import clsx from 'clsx';

export default function ThemeToggle() {
  const theme = useUIStore((s) => s.theme);
  const toggleTheme = useUIStore((s) => s.toggleTheme);
  const t = useTranslations('common');
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

  const isDark = theme === 'dark';

  return (
    <Button
      type="text"
      shape="circle"
      onClick={toggleTheme}
      data-testid="theme-toggle-button"
      aria-label={t('toggleTheme')}
      icon={
        isDark ? (
          <Sun className="h-6 w-6" aria-hidden="true" />
        ) : (
          <Moon className="h-6 w-6" aria-hidden="true" />
        )
      }
      style={{ width: 48, height: 48 }}
      className={clsx(
        'flex items-center justify-center rounded-full transition-all cursor-pointer shadow-md border border-emerald-500/10 bg-card',
        'hover:bg-emerald-500/5 hover:border-emerald-500/30 active:scale-95',
        isDark ? 'text-amber-400' : 'text-emerald-600',
      )}
    />
  );
}
