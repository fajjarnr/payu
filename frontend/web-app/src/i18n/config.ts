export const locales = ['id'] as const;
export const defaultLocale = 'id' as const;
export type Locale = (typeof locales)[number];
