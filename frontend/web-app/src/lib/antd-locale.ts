import type { Locale } from 'antd/es/locale';
import idID from 'antd/locale/id_ID';

/**
 * Single-locale app (Indonesian only): antd always renders id_ID.
 * The argument is kept so call sites stay locale-agnostic.
 */
export function getAntdLocale(_locale: string): Locale {
  return idID;
}
