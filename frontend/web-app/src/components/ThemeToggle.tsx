'use client';

import * as React from 'react';
import { Moon, Sun } from '@/components/icons';
import { useUIStore } from '@/stores';
import { useTranslations } from 'next-intl';
import { Button, theme } from 'antd';

export default function ThemeToggle() {
  const appTheme = useUIStore((s) => s.theme);
  const toggleTheme = useUIStore((s) => s.toggleTheme);
  const t = useTranslations('common');
  const { token } = theme.useToken();
  const [mounted, setMounted] = React.useState(false);

  // Avoid hydration mismatch
  React.useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return (
      <div
        data-testid="theme-toggle-skeleton"
        aria-hidden="true"
        style={{
          width: 48,
          height: 48,
          borderRadius: '50%',
          backgroundColor: token.colorFillTertiary,
        }}
      />
    );
  }

  const isDark = appTheme === 'dark';

  return (
    <Button
      type="text"
      shape="circle"
      onClick={toggleTheme}
      data-testid="theme-toggle-button"
      aria-label={t('toggleTheme')}
      aria-pressed={isDark}
      icon={
        isDark ? (
          <Sun style={{ width: 24, height: 24 }} aria-hidden="true" />
        ) : (
          <Moon style={{ width: 24, height: 24 }} aria-hidden="true" />
        )
      }
      style={{
        width: 48,
        height: 48,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        border: `1px solid ${token.colorBorderSecondary}`,
        backgroundColor: token.colorBgContainer,
        color: token.colorText,
      }}
    />
  );
}
