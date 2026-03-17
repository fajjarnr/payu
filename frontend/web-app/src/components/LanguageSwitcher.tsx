'use client';

import React, { useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { Languages, ChevronDown } from 'lucide-react';
import clsx from 'clsx';

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

  const switchLocale = (newLocale: string) => {
    const localeCodes = locales.map(l => l.code);
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
    } else {
      if (newLocale !== 'id') {
        // Add prefix if not default locale
        newPath = `/${newLocale}${pathname === '/' ? '' : pathname}`;
      }
    }
    
    // Clean up double slashes
    newPath = newPath.replace(/\/+/g, '/');
    if (newPath !== '/' && newPath.endsWith('/')) {
      newPath = newPath.slice(0, -1);
    }

    // BUG-FE-068: Preserve query string when switching locale
    const queryString = typeof window !== 'undefined' ? window.location.search : '';
    // Force hard navigation to ensure middleware and server components re-run
    window.location.href = (newPath || '/') + queryString;
    setIsOpen(false);
  };

  const currentLocale = locales.find(l => l.code === locale) || locales[0];

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        data-testid="language-switcher-button"
        className={clsx(
          'flex items-center gap-2 px-4 py-2.5 rounded-xl transition-all',
          'text-xs font-bold tracking-widest border border-emerald-500/10 bg-card shadow-md',
          'hover:bg-emerald-500/5 hover:border-emerald-500/30 text-foreground',
          'focus:outline-none focus:ring-4 focus:ring-emerald-500/10'
        )}
        aria-label={t('changeLanguage')}
      >
        <Languages className="h-4 w-4" />
        <span className="hidden sm:inline">{currentLocale.flag}</span>
        <span className="hidden sm:inline uppercase">{currentLocale.code}</span>
        <ChevronDown className={clsx(
          'h-3 w-3 transition-transform',
          isOpen ? 'rotate-180' : ''
        )} />
      </button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-40"
            onClick={() => setIsOpen(false)}
          />
          <div className={clsx(
            'absolute right-0 top-full mt-2 z-50',
            'bg-card rounded-xl shadow-lg border border-border',
            'py-2 min-w-[160px]',
            'animate-in fade-in slide-in-from-top-2 duration-200'
          )}>
            {locales.map((loc) => (
              <button
                key={loc.code}
                onClick={() => switchLocale(loc.code)}
                data-testid={`locale-${loc.code}`}
                className={clsx(
                  'w-full flex items-center gap-3 px-4 py-2.5',
                  'text-xs font-semibold transition-colors',
                  'hover:bg-muted hover:text-foreground',
                  locale === loc.code ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'
                )}
              >
                <span className="text-lg">{loc.flag}</span>
                <span>{loc.label}</span>
                {locale === loc.code && (
                  <span className="ml-auto text-primary">✓</span>
                )}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
