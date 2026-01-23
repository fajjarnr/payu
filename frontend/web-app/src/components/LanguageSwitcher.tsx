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
    const currentPath = pathname.replace(`/${locale}`, '');
    router.push(`/${newLocale}${currentPath}`);
    setIsOpen(false);
  };

  const currentLocale = locales.find(l => l.code === locale) || locales[0];

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={clsx(
          'flex items-center gap-2 px-3 py-2 rounded-lg transition-colors',
          'text-xs font-bold tracking-widest border border-border',
          'hover:bg-muted hover:border-primary/30',
          'focus:outline-none focus:ring-2 focus:ring-primary/20'
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
