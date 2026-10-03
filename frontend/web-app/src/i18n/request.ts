import { getRequestConfig } from 'next-intl/server';
import { locales, defaultLocale } from './config';

export default getRequestConfig(async ({ locale }) => {

  const resolvedLocale = locale || defaultLocale;
  
  const finalLocale = locales.includes(resolvedLocale as typeof locales[number]) ? resolvedLocale : defaultLocale;

  return {
    locale: finalLocale,
    messages: (await import(`../../messages/${finalLocale}.json`)).default
  };
});
