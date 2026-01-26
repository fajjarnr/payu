import { notFound } from 'next/navigation';
import { getRequestConfig } from 'next-intl/server';
import { locales, defaultLocale } from './config';

export default getRequestConfig(async ({ locale }) => {
  // Ensure locale is defined, fallback to default
  const resolvedLocale = locale || defaultLocale;

  if (!locales.includes(resolvedLocale as (typeof locales)[number])) notFound();

  return {
    locale: resolvedLocale,
    messages: (await import(`../../messages/${resolvedLocale}.json`)).default
  };
});
