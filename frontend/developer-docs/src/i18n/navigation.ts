import { createNavigation } from 'next-intl/navigation';
import { defineRouting } from 'next-intl/routing';

export const routing = defineRouting({
  locales: ['id', 'en'],
  defaultLocale: 'id',
});

export const { Link, redirect, usePathname, useRouter } =
  createNavigation(routing);
