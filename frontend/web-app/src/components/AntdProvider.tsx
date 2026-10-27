'use client';

import { StyleProvider } from '@ant-design/cssinjs';
import { App, ConfigProvider, theme as antdTheme } from 'antd';
import { registerMessageApi } from '@/lib/notify';
import type { ReactNode } from 'react';
import { getAntdLocale } from '@/lib/antd-locale';
import {
  darkComponentTokens,
  darkTokens,
  lightComponentTokens,
  lightTokens,
} from '@/theme/tokens';

export interface AntdProviderProps {
  children: ReactNode;
  /** next-intl locale code (single-locale app: always `id`). */
  locale?: string;
  /** Whether the app is currently in dark mode. */
  isDark?: boolean;
}
function NotifyRegistrar() {
  const { message } = App.useApp();
  registerMessageApi(message);
  return null;
}

/**
 * Ant Design root provider. Renders no DOM wrapper (`App component={false}`)
 * so wrapping the tree does not change the existing layout.
 */
export function AntdProvider({ children, locale = 'id', isDark = false }: AntdProviderProps) {
  return (
    <StyleProvider layer>
      <ConfigProvider
        locale={getAntdLocale(locale)}
        theme={{
          algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
          token: isDark ? darkTokens : lightTokens,
          components: isDark ? darkComponentTokens : lightComponentTokens,
        }}
      >
        <App component={false}>
          <NotifyRegistrar />
          {children}
        </App>
      </ConfigProvider>
    </StyleProvider>
  );
}
