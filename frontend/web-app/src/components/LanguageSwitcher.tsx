'use client';

import React, { useState } from 'react';
import { useRouter, usePathname } from '@/lib/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { Languages, ChevronDown } from '@/components/icons';
import { Button, Dropdown, theme } from 'antd';
import type { MenuProps } from 'antd';

const locales = [
  { code: 'id', label: 'Indonesia', flag: '🇮🇩' },
  { code: 'en', label: 'English', flag: '🇬🇧' },
] as const;

export default function LanguageSwitcher() {
  const t = useTranslations('settings');
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(false);
  const { token } = theme.useToken();

  // Canonical path for a locale: the locale prefix is replaced (not added on
  // top), the default locale stays unprefixed (localePrefix: 'as-needed').
  const localePath = (newLocale: string) => {
    const localeCodes = locales.map((l) => l.code);
    const localePattern = new RegExp(`^/(${localeCodes.join('|')})(/|$)`);

    let newPath = pathname;
    if (localePattern.test(pathname)) {
      if (newLocale === 'id') {
        // Switch to default locale, remove prefix
        newPath = pathname.replace(localePattern, '/');
      } else {
        // Switch to other locale, replace prefix
        newPath = pathname.replace(localePattern, `/${newLocale}$2`);
      }
    } else if (newLocale !== 'id') {
      // Add prefix if not default locale
      newPath = `/${newLocale}${pathname === '/' ? '' : pathname}`;
    }

    newPath = newPath.replace(/\/+/g, '/');
    if (newPath !== '/' && newPath.endsWith('/')) {
      newPath = newPath.slice(0, -1);
    }

    return newPath || '/';
  };

  // BUG-FE-068: Preserve query string when switching locale
  const queryString = typeof window !== 'undefined' ? window.location.search : '';

  const switchLocale = (newLocale: string) => {
    // Use router.push for client-side navigation with locale preservation
    router.push(localePath(newLocale) + queryString);
    setIsOpen(false);
  };

  const currentLocale = locales.find((l) => l.code === locale) || locales[0];

  const items: MenuProps['items'] = locales.map((loc) => ({
    key: loc.code,
    // A-04: a real link — the browser can open it in a new tab and assistive
    // tech announces a link, not a menu item that navigates via the router.
    label: (
      <a
        href={localePath(loc.code) + queryString}
        onClick={(e) => {
          e.preventDefault();
          switchLocale(loc.code);
        }}
        style={{ display: 'inline-flex', alignItems: 'center', gap: 12, minHeight: 24 }}
      >
        <span aria-hidden="true">{loc.flag}</span>
        <span>{loc.label}</span>
      </a>
    ),
  }));

  return (
    <Dropdown
      menu={{ items, selectable: true, selectedKeys: [locale] }}
      trigger={['click']}
      open={isOpen}
      onOpenChange={setIsOpen}
      placement="bottomRight"
    >
      <Button
        data-testid="language-switcher-button"
        aria-label={t('changeLanguage')}
        aria-haspopup="menu"
        aria-expanded={isOpen}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          padding: '8px 16px',
          borderRadius: token.borderRadius,
          minHeight: 44,
          minWidth: 44,
          border: `1px solid ${token.colorBorderSecondary}`,
          backgroundColor: token.colorBgContainer,
          color: token.colorText,
          fontSize: 12,
          fontWeight: 700,
          letterSpacing: '0.1em',
        }}
      >
        <Languages style={{ width: 16, height: 16 }} aria-hidden="true" />
        <span aria-hidden="true">{currentLocale.flag}</span>
        <span style={{ textTransform: 'uppercase' }}>{currentLocale.code}</span>
        <ChevronDown
          aria-hidden="true"
          className="motion-safe:transition-transform duration-200"
          style={{
            width: 12,
            height: 12,
            transform: isOpen ? 'rotate(180deg)' : undefined,
          }}
        />
      </Button>
    </Dropdown>
  );
}
