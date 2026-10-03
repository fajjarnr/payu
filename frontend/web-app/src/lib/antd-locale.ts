import type { Locale } from 'antd/es/locale';
import enUS from 'antd/locale/en_US';
import idID from 'antd/locale/id_ID';

/**
 * Maps next-intl locale codes to antd locale objects.
 * Keep in sync with `locales` in `@/i18n/config`.
 */
const antdLocales: Record<string, Locale> = {
  id: idID,
  en: enUS,
};

/**
 * Resolve the antd locale for a next-intl locale code.
 * Falls back to English for unknown locales.
 */
export function getAntdLocale(locale: string): Locale {
  return antdLocales[locale] ?? enUS;
}
